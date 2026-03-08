package `in`.kkkev.jjidea.actions.change

import com.intellij.icons.AllIcons
import com.intellij.openapi.ide.CopyPasteManager
import `in`.kkkev.jjidea.actions.nullAndDumbAwareAction
import java.awt.datatransfer.StringSelection

/**
 * Copy Description to clipboard.
 */
fun copyDescriptionAction(description: String?) =
    nullAndDumbAwareAction(description, "log.action.copy.description", AllIcons.Actions.Copy) {
        val selection = StringSelection(description)
        CopyPasteManager.getInstance().setContents(selection)
        log.info("Copied description to clipboard")
    }
