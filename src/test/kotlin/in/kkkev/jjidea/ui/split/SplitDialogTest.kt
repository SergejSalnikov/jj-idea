package `in`.kkkev.jjidea.ui.split

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.SimpleContentRevision
import com.intellij.testFramework.junit5.RunInEdt
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.util.ui.UIUtil
import `in`.kkkev.jjidea.jj.ChangeId
import `in`.kkkev.jjidea.jj.CommitId
import `in`.kkkev.jjidea.jj.LogEntry
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("platform")
@TestApplication
@RunInEdt
class SplitDialogTest {
    private val project = projectFixture()

    @Test
    fun `parent description pre-populated with source description`() {
        val source = createEntry("src1", description = "source desc")
        val dialog = SplitDialog(project.get(), source, emptyList())

        dialog.parentDescriptionText shouldBe "source desc"
        disposeDialog(dialog)
    }

    @Test
    fun `child description pre-populated with source description`() {
        val source = createEntry("src1", description = "source desc")
        val dialog = SplitDialog(project.get(), source, emptyList())

        dialog.childDescriptionText shouldBe "source desc"
        disposeDialog(dialog)
    }

    @Test
    fun `description empty when source empty`() {
        val source = createEntry("src1", description = "")
        val dialog = SplitDialog(project.get(), source, emptyList())

        dialog.parentDescriptionText shouldBe ""
        dialog.childDescriptionText shouldBe ""
        disposeDialog(dialog)
    }

    @Test
    fun `all files start in parent tree`() {
        val changes = listOf(change("src/Main.kt"), change("src/Utils.kt"))
        val source = createEntry("src1", description = "desc")
        val dialog = SplitDialog(project.get(), source, changes)

        waitForRefresh(dialog.parentTree)
        dialog.parentTree.changes shouldBe changes
        dialog.childTree.changes.size shouldBe 0
        disposeDialog(dialog)
    }

    @Test
    fun `parallel checkbox defaults to unchecked`() {
        val source = createEntry("src1", description = "desc")
        val dialog = SplitDialog(project.get(), source, emptyList())

        dialog.parallelCheckBox.isSelected shouldBe false
        disposeDialog(dialog)
    }

    @Test
    fun `dynamic labels switch when parallel toggled`() {
        val source = createEntry("src1", description = "desc")
        val dialog = SplitDialog(project.get(), source, emptyList())

        // Default: linear mode
        dialog.childHeaderLabel.text shouldContain "Child"
        dialog.parentHeaderLabel.text shouldContain "Parent"

        // Toggle to parallel
        dialog.parallelCheckBox.isSelected = true
        dialog.parallelCheckBox.actionListeners.forEach { it.actionPerformed(null) }
        UIUtil.dispatchAllInvocationEvents()

        dialog.childHeaderLabel.text shouldContain "First"
        dialog.parentHeaderLabel.text shouldContain "Second"

        disposeDialog(dialog)
    }

    @Test
    fun `empty label visible when child tree is empty`() {
        val changes = listOf(change("src/Main.kt"))
        val source = createEntry("src1", description = "desc")
        val dialog = SplitDialog(project.get(), source, changes)

        dialog.childEmptyLabel.isVisible shouldBe true
        disposeDialog(dialog)
    }

    private fun createEntry(id: String, description: String = "") = LogEntry(
        repo = mockk(relaxed = true),
        id = ChangeId(id, id),
        commitId = CommitId(id, id),
        underlyingDescription = description
    )

    private fun change(path: String): Change {
        val filePath = LocalFilePath(path, false)
        return Change(null, SimpleContentRevision("", filePath, "1"))
    }

    private fun waitForRefresh(tree: com.intellij.openapi.vcs.changes.ui.AsyncChangesTreeImpl<*>) {
        var refreshed = false
        tree.invokeAfterRefresh { refreshed = true }
        val deadline = System.currentTimeMillis() + 5_000
        while (!refreshed && System.currentTimeMillis() < deadline) {
            UIUtil.dispatchAllInvocationEvents()
        }
        refreshed shouldBe true
    }

    private fun disposeDialog(dialog: DialogWrapper) {
        if (!dialog.isDisposed) dialog.close(DialogWrapper.CANCEL_EXIT_CODE)
    }
}
