package `in`.kkkev.jjidea.ui.services

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.CustomizedDataContext
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import `in`.kkkev.jjidea.JujutsuBundle
import `in`.kkkev.jjidea.actions.performAction
import `in`.kkkev.jjidea.jj.JujutsuRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Utility for showing Jujutsu-related notifications to the user.
 */
object JujutsuNotifications {
    private const val GROUP_ID = "Jujutsu"

    // Track which roots we've already notified about to avoid spamming
    private val notifiedUninitializedRoots = ConcurrentHashMap.newKeySet<String>()

    /**
     * Show a notification that a VCS root is configured for Jujutsu but not initialized.
     * Includes actions to initialize or reconfigure the VCS mapping.
     *
     * Only shows once per root per session to avoid notification spam.
     */
    fun notifyUninitializedRoot(project: Project, repo: JujutsuRepository) {
        val rootPath = repo.directory.path

        // Only notify once per root
        if (!notifiedUninitializedRoots.add(rootPath)) {
            return
        }

        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(
                JujutsuBundle.message("notification.uninitialized.title"),
                JujutsuBundle.message("notification.uninitialized.content", repo.displayName),
                NotificationType.WARNING
            )

        // Action to initialize JJ - invoke registered action with directory context
        notification.addAction(object : NotificationAction(
            JujutsuBundle.message("notification.uninitialized.action.init")
        ) {
            override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                notification.expire()
                notifiedUninitializedRoots.remove(rootPath)
                performAction("Jujutsu.Init", createDataContext(project, repo.directory))
            }
        })

        // Action to open VCS settings
        notification.addAction(object : NotificationAction(
            JujutsuBundle.message("notification.uninitialized.action.settings")
        ) {
            override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                notification.expire()
                notifiedUninitializedRoots.remove(rootPath)
                ShowSettingsUtil.getInstance().showSettingsDialog(
                    project,
                    "project.propVCSSupport.Mappings"
                )
            }
        })

        notification.notify(project)
    }

    /**
     * Clear the notification tracking for a root (e.g., after it's initialized).
     */
    fun clearNotificationState(rootPath: String) {
        notifiedUninitializedRoots.remove(rootPath)
    }

    /**
     * Create a DataContext with project and file information.
     */
    private fun createDataContext(project: Project, file: VirtualFile) =
        CustomizedDataContext.withSnapshot(DataContext.EMPTY_CONTEXT) { sink ->
            sink[CommonDataKeys.PROJECT] = project
            sink[CommonDataKeys.VIRTUAL_FILE] = file
        }
}
