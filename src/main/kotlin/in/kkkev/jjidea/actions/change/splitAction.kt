package `in`.kkkev.jjidea.actions.change

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import `in`.kkkev.jjidea.actions.nullAndDumbAwareAction
import `in`.kkkev.jjidea.jj.ChangeService
import `in`.kkkev.jjidea.jj.LogEntry
import `in`.kkkev.jjidea.jj.invalidate
import `in`.kkkev.jjidea.ui.common.JujutsuIcons
import `in`.kkkev.jjidea.ui.split.SplitDialog

/**
 * Split action. Loads changes on a background thread, opens a dialog to configure
 * file selection, description, and options, then executes `jj split`.
 *
 * The entry must be mutable. After splitting, selects the original change ID.
 */
fun splitAction(project: Project, entry: LogEntry?) =
    nullAndDumbAwareAction(entry, "log.action.split", JujutsuIcons.Split) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val changes = ChangeService.loadChanges(target)

            ApplicationManager.getApplication().invokeLater {
                val dialog = SplitDialog(project, target, changes)
                if (!dialog.showAndGet()) return@invokeLater

                val spec = dialog.result ?: return@invokeLater
                target.repo.commandExecutor
                    .createCommand { split(spec.revision, spec.filePaths, spec.description, spec.parallel) }
                    .onSuccess {
                        target.repo.invalidate(select = target.id)
                        log.info("Split ${target.id}")
                    }
                    .onFailure { tellUser(project, "log.action.split.error") }
                    .executeAsync()
            }
        }
    }
