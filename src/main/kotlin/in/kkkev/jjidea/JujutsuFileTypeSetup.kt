package `in`.kkkev.jjidea

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileTypes.FileTypeManager
import `in`.kkkev.jjidea.vcs.JujutsuVcs.Companion.DOT_JJ

class JujutsuFileTypeSetup : AppLifecycleListener {
    override fun appStarted() {
        val ftm = FileTypeManager.getInstance()
        if (!ftm.isFileIgnored(DOT_JJ)) {
            ApplicationManager.getApplication().invokeLater {
                WriteAction.run<Nothing> {
                    ftm.ignoredFilesList = "${ftm.ignoredFilesList};$DOT_JJ"
                }
            }
        }
    }
}
