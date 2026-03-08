package `in`.kkkev.jjidea.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.ActionUpdateThreadAware
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup

class BackgroundActionGroup(vararg actions: AnAction) :
    DefaultActionGroup(*actions),
    ActionUpdateThreadAware.Recursive {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
