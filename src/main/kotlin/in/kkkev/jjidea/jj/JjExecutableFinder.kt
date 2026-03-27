package `in`.kkkev.jjidea.jj

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.diagnostic.Logger
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * Finds jj executables in known installation locations and validates them.
 *
 * Uses direct path searching instead of which/where to avoid spawning processes.
 * Handles Windows PATHEXT for executable extensions.
 * Reuses search paths from [InstallMethod] to avoid duplication.
 */
class JjExecutableFinder {
    private val log = Logger.getInstance(javaClass)
    private val timeout = TimeUnit.SECONDS.toMillis(5)

    /** All search paths: SYSTEM_PATH + paths from all detectable install methods */
    private val allSearchPaths: List<Path> by lazy {
        val installMethodPaths = InstallMethod.allAvailable
            .filterIsInstance<InstallMethod.DetectableInstallMethod>()
            .flatMap { it.searchPaths }
            .filterNotNull()
        (SYSTEM_PATH + installMethodPaths).distinct()
    }

    /**
     * A candidate jj executable found at a specific path.
     */
    data class Candidate(val path: Path, val source: String, val installMethod: InstallMethod)

    /**
     * A validated jj executable with confirmed version.
     */
    data class ValidatedExecutable(val path: Path, val version: JjVersion, val installMethod: InstallMethod)

    /**
     * Reason why a path is invalid.
     */
    enum class InvalidReason {
        NOT_FOUND,
        IS_DIRECTORY,
        NOT_EXECUTABLE,
        NOT_JJ,
        EXECUTION_FAILED
    }

    /**
     * Result of validating a path.
     */
    sealed class ValidationResult {
        data class Valid(val executable: ValidatedExecutable) : ValidationResult()
        data class Invalid(val reason: InvalidReason, val details: String? = null) : ValidationResult()
    }

    /**
     * Find the first executable meeting the minimum version constraint.
     * Searches configured path first, then PATH, then known installation locations.
     */
    fun findBestExecutable(configuredPath: String?, minVersion: JjVersion = JjVersion.MINIMUM): ValidatedExecutable? {
        val candidates = findCandidates(configuredPath)
        log.info("Found ${candidates.size} jj candidates")

        for (candidate in candidates) {
            when (val result = validate(candidate)) {
                is ValidationResult.Valid -> {
                    if (result.executable.version >= minVersion) {
                        log.info("Using jj at ${candidate.path} (version ${result.executable.version})")
                        return result.executable
                    } else {
                        log.info("Skipping jj at ${candidate.path}: version ${result.executable.version} < $minVersion")
                    }
                }

                is ValidationResult.Invalid -> {
                    log.debug("Invalid candidate ${candidate.path}: ${result.reason} - ${result.details}")
                }
            }
        }
        return null
    }

    /**
     * Find the first jj executable regardless of version.
     */
    fun findAnyExecutable(configuredPath: String?): ValidatedExecutable? {
        for (candidate in findCandidates(configuredPath)) {
            when (val result = validate(candidate)) {
                is ValidationResult.Valid -> return result.executable
                is ValidationResult.Invalid -> continue
            }
        }
        return null
    }

    /**
     * Validate a specific path or command name (e.g., configured by user).
     *
     * If the input contains a path separator, it's treated as a file path.
     * Otherwise, it's treated as a command name to search for in PATH and known locations.
     */
    fun validatePath(path: String): ValidationResult {
        val isCommandName = !path.contains("/") && !path.contains("\\")

        val resolvedPath = if (isCommandName) {
            // Search for command in PATH and known locations
            findCommandInPaths(path)
                ?: return ValidationResult.Invalid(InvalidReason.NOT_FOUND, "Command '$path' not found in PATH")
        } else {
            Paths.get(path)
        }

        return validatePathObj(resolvedPath, InstallMethod.Unknown)
    }

    /**
     * Find a command in SYSTEM_PATH and known locations.
     * Handles Windows PATHEXT automatically.
     */
    private fun findCommandInPaths(commandName: String): Path? {
        val found = allSearchPaths.findFile(commandName)
        if (found != null) {
            log.info("Found '$commandName' at: $found")
        } else {
            log.info("Command '$commandName' not found in PATH or known locations")
        }
        return found
    }

    /**
     * Find all candidate jj executables in search order.
     *
     * @param configuredPath The configured executable path/name. If it contains no path
     *        separators, it's treated as a command name to search for.
     */
    fun findCandidates(configuredPath: String?): List<Candidate> = try {
        buildList {
            // 1. Configured path/command (if set)
            if (!configuredPath.isNullOrBlank()) {
                val isCommandName = !configuredPath.contains("/") && !configuredPath.contains("\\")
                if (isCommandName) {
                    // Search for the command in all paths
                    findCommandInPaths(configuredPath)?.let { path ->
                        add(Candidate(path, "Configured", inferInstallMethod(path)))
                    }
                } else {
                    // Use as literal path
                    try {
                        add(Candidate(Paths.get(configuredPath), "Configured", InstallMethod.Unknown))
                    } catch (e: Exception) {
                        log.debug("Invalid configured path: $configuredPath")
                    }
                }
            }

            // 2. Search for "jj" in PATH and known locations (if not already found via configured path)
            if (configuredPath != "jj") {
                findCommandInPaths("jj")?.let { path ->
                    add(Candidate(path, "PATH", inferInstallMethod(path)))
                }
            }
        }.distinctBy {
            try {
                it.path.toAbsolutePath().normalize()
            } catch (_: Exception) {
                it.path
            }
        }
    } catch (e: Exception) {
        log.warn("Error finding jj candidates", e)
        emptyList()
    }

    private fun validate(candidate: Candidate): ValidationResult =
        validatePathObj(candidate.path, candidate.installMethod)

    private fun validatePathObj(path: Path, installMethod: InstallMethod): ValidationResult {
        try {
            // Check if path exists
            if (!Files.exists(path)) {
                return ValidationResult.Invalid(InvalidReason.NOT_FOUND, "Path does not exist")
            }

            // Check if it's a file (not directory)
            if (Files.isDirectory(path)) {
                return ValidationResult.Invalid(InvalidReason.IS_DIRECTORY, "Path is a directory")
            }

            // Check if executable
            if (!Files.isExecutable(path)) {
                return ValidationResult.Invalid(InvalidReason.NOT_EXECUTABLE, "File is not executable")
            }

            // Run --version to validate it's actually jj
            val commandLine = GeneralCommandLine(path.toString(), "--version")
                .withCharset(StandardCharsets.UTF_8)

            val handler = CapturingProcessHandler(commandLine)
            val output = handler.runProcess(timeout.toInt())

            if (output.exitCode != 0) {
                return ValidationResult.Invalid(InvalidReason.EXECUTION_FAILED, output.stderr)
            }

            val versionString = output.stdout.trim()
            val version = JjVersion.parse(versionString)
                ?: return ValidationResult.Invalid(InvalidReason.NOT_JJ, "Output doesn't look like jj: $versionString")

            return ValidationResult.Valid(ValidatedExecutable(path, version, inferInstallMethod(path, installMethod)))
        } catch (e: Exception) {
            log.debug("Error validating path $path: ${e.message}")
            return ValidationResult.Invalid(InvalidReason.EXECUTION_FAILED, e.message)
        }
    }

    private fun inferInstallMethod(path: Path, default: InstallMethod = InstallMethod.Unknown): InstallMethod {
        if (default != InstallMethod.Unknown) return default

        val pathStr = path.toAbsolutePath().toString().lowercase()
        return when {
            pathStr.contains("homebrew") || pathStr.contains("/opt/homebrew/") -> InstallMethod.Homebrew
            pathStr.contains(".cargo") -> InstallMethod.Cargo
            pathStr.contains("scoop") -> InstallMethod.Scoop
            pathStr.contains("chocolatey") -> InstallMethod.Chocolatey
            pathStr.contains("/snap/") -> InstallMethod.Snap
            else -> InstallMethod.Unknown
        }
    }
}
