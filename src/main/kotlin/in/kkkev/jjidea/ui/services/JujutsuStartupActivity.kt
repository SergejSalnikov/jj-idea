package `in`.kkkev.jjidea.ui.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import `in`.kkkev.jjidea.jj.JjAvailabilityChecker
import `in`.kkkev.jjidea.jj.JjAvailabilityStatus

/**
 * Bootstraps the Jujutsu state model and tool window management on project open.
 *
 * Initializing [ToolWindowEnabler] triggers [JujutsuStateModel][`in`.kkkev.jjidea.jj.JujutsuStateModel]
 * construction (via [stateModel][`in`.kkkev.jjidea.jj.stateModel]), which subscribes to IDE events
 * and fires the initial root scan. ToolWindowEnabler then connects to the root state to manage
 * tool window visibility and log tab suppression.
 *
 * Also initializes [JjAvailabilityChecker] to monitor jj availability and notify users
 * of issues (not found, wrong version, invalid path).
 */
class JujutsuStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        ToolWindowEnabler.getInstance(project)

        // Initialize availability checking
        val checker = JjAvailabilityChecker.getInstance(project)
        checker.status.connect(project) { status ->
            when (status) {
                is JjAvailabilityStatus.Available -> JujutsuNotifications.clearAvailabilityNotification()
                else -> JujutsuNotifications.notifyJjUnavailable(project, status)
            }
        }
        checker.recheck()
    }
}
