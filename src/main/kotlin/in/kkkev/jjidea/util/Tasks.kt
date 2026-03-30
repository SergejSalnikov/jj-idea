package `in`.kkkev.jjidea.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import java.awt.Component

fun runInBackground(action: () -> Unit) = ApplicationManager.getApplication().executeOnPooledThread { action() }

fun runLater(action: () -> Unit) = ApplicationManager.getApplication().invokeLater { action() }

fun runLaterInModal(component: Component, action: () -> Unit) =
    ApplicationManager.getApplication().invokeLater({ action() }, ModalityState.stateForComponent(component))
