package `in`.kkkev.jjidea.ui.split

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vcs.FilePath
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import `in`.kkkev.jjidea.JujutsuBundle
import `in`.kkkev.jjidea.jj.Description
import `in`.kkkev.jjidea.jj.LogEntry
import `in`.kkkev.jjidea.jj.Revision
import `in`.kkkev.jjidea.ui.common.FileSelectionPanel
import `in`.kkkev.jjidea.ui.components.*
import `in`.kkkev.jjidea.ui.log.appendDecorations
import `in`.kkkev.jjidea.ui.log.appendStatusIndicators
import `in`.kkkev.jjidea.vcs.filePath
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane

/**
 * Result of the split dialog — the user's chosen parameters.
 */
data class SplitSpec(
    val revision: Revision,
    val filePaths: List<FilePath>,
    val description: Description,
    val parallel: Boolean
)

/**
 * Dialog for configuring a `jj split` operation.
 *
 * Shows source entry info, file selection with checkboxes (checked files stay in the first commit),
 * a summary label, a description field, and a "parallel commits" option.
 */
class SplitDialog(
    private val project: Project,
    private val sourceEntry: LogEntry,
    changes: List<com.intellij.openapi.vcs.changes.Change>
) : DialogWrapper(project) {
    var result: SplitSpec? = null
        private set

    internal val fileSelection = FileSelectionPanel(project)
    private val totalChanges = changes.size
    private val descriptionField = JBTextArea(sourceEntry.description.actual, 4, 0)
    private val parallelCheckBox = JBCheckBox(JujutsuBundle.message("dialog.split.parallel"))
    internal val summaryLabel = JLabel()

    /** Current description text, exposed for testing. */
    internal val descriptionText: String get() = descriptionField.text

    init {
        title = JujutsuBundle.message("dialog.split.title")
        setOKButtonText(JujutsuBundle.message("dialog.split.button"))
        fileSelection.setChanges(changes)
        fileSelection.addInclusionListener { updateSummary() }
        updateSummary()
        init()
    }

    private fun updateSummary() {
        val first = fileSelection.includedChanges.size
        val second = totalChanges - first
        summaryLabel.text = JujutsuBundle.message("dialog.split.summary", first, second)
    }

    override fun createCenterPanel(): JComponent {
        val topSection = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(createSectionLabel(JujutsuBundle.message("dialog.split.source")))
            add(createEntryPane(sourceEntry))
        }

        val filesLabel = createSectionLabel(JujutsuBundle.message("dialog.split.files"))

        val descriptionSection = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(
                summaryLabel.apply {
                    alignmentX = JPanel.LEFT_ALIGNMENT
                    border = JBUI.Borders.empty(4, 0)
                }
            )
            add(createSectionLabel(JujutsuBundle.message("dialog.split.description")))
            val scrollPane = JScrollPane(descriptionField).apply {
                alignmentX = JPanel.LEFT_ALIGNMENT
                preferredSize = Dimension(0, JBUI.scale(80))
                maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(80))
            }
            add(scrollPane)
            add(parallelCheckBox.apply { alignmentX = JPanel.LEFT_ALIGNMENT })
        }

        val wrapper = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8)
            val north = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(topSection)
                add(filesLabel)
            }
            add(north, BorderLayout.NORTH)
            add(fileSelection, BorderLayout.CENTER)
            add(descriptionSection, BorderLayout.SOUTH)
        }
        wrapper.preferredSize = Dimension(JBUI.scale(700), JBUI.scale(500))
        return wrapper
    }

    private fun createSectionLabel(text: String): JLabel {
        val label = JLabel(text)
        label.font = label.font.deriveFont(java.awt.Font.BOLD)
        label.alignmentX = JLabel.LEFT_ALIGNMENT
        label.border = JBUI.Borders.empty(4, 0)
        return label
    }

    private fun createEntryPane(entry: LogEntry) = IconAwareHtmlPane().apply {
        alignmentX = JPanel.LEFT_ALIGNMENT
        text = htmlString {
            appendStatusIndicators(entry)
            append(entry.id)
            append(" ")
            appendDescriptionAndEmptyIndicator(entry)
            append(" ")
            appendDecorations(entry)
        }
    }

    override fun doValidate(): ValidationInfo? {
        if (fileSelection.includedChanges.size >= totalChanges) {
            return ValidationInfo(JujutsuBundle.message("dialog.split.no.unchecked"), fileSelection)
        }
        return null
    }

    override fun doOKAction() {
        val filePaths = fileSelection.includedChanges.mapNotNull { it.filePath }

        result = SplitSpec(
            revision = sourceEntry.id,
            filePaths = filePaths,
            description = Description(descriptionField.text.trim()),
            parallel = parallelCheckBox.isSelected
        )
        super.doOKAction()
    }
}
