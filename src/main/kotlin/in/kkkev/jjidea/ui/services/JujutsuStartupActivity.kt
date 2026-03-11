package `in`.kkkev.jjidea.ui.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Bootstraps the Jujutsu state model and tool window management on project open.
 *
 * Initializing [ToolWindowEnabler] triggers [JujutsuStateModel][`in`.kkkev.jjidea.jj.JujutsuStateModel]
 * construction (via [stateModel][`in`.kkkev.jjidea.jj.stateModel]), which subscribes to IDE events
 * and fires the initial root scan. ToolWindowEnabler then connects to the root state to manage
 * tool window visibility and log tab suppression.
 */
class JujutsuStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        ToolWindowEnabler.getInstance(project)
    }
}
