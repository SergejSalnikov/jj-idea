package `in`.kkkev.jjidea.integration

import com.intellij.openapi.vfs.VirtualFile
import `in`.kkkev.jjidea.contract.JjStub
import `in`.kkkev.jjidea.contract.StubCommandExecutor
import `in`.kkkev.jjidea.jj.WorkingCopy
import `in`.kkkev.jjidea.jj.cli.AnnotationParser
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Integration tests exercising the annotation pipeline:
 * JjStub → StubCommandExecutor → AnnotationParser.parse() → AnnotationLine domain objects.
 */
@Tag("stub")
class AnnotationIntegrationTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var stub: JjStub
    private lateinit var executor: StubCommandExecutor

    @BeforeEach
    fun setUp() {
        stub = JjStub(tempDir)
        stub.init()
        executor = StubCommandExecutor(stub)
    }

    private fun virtualFile(relativePath: String): VirtualFile =
        mockk { every { path } returns relativePath }

    @Test
    fun `single-change file attributes all lines to same change`() {
        stub.createFile("hello.txt", "line1\nline2\nline3\n")
        stub.describe("Initial content")

        val result = executor.annotate(virtualFile("hello.txt"), WorkingCopy, AnnotationParser.TEMPLATE)
        val lines = AnnotationParser.parse(result.stdout)

        lines shouldHaveSize 3
        // All lines attributed to the same change
        val changeIds = lines.map { it.id.full }.toSet()
        changeIds shouldHaveSize 1
        changeIds.first().shouldNotBeBlank()
    }

    @Test
    fun `multi-change file attributes lines to different changes`() {
        stub.createFile("file.txt", "original line\n")
        stub.describe("First commit")
        stub.newChange("Second commit")
        stub.createFile("file.txt", "original line\nnew line\n")

        val result = executor.annotate(virtualFile("file.txt"), WorkingCopy, AnnotationParser.TEMPLATE)
        val lines = AnnotationParser.parse(result.stdout)

        lines shouldHaveSize 2
        // First line from first commit, second line from second commit
        lines[0].id.full shouldNotBe lines[1].id.full
    }

    @Test
    fun `annotation line fields populated correctly`() {
        stub.createFile("test.txt", "content\n")
        stub.describe("Test description")

        val result = executor.annotate(virtualFile("test.txt"), WorkingCopy, AnnotationParser.TEMPLATE)
        val lines = AnnotationParser.parse(result.stdout)

        lines shouldHaveSize 1
        val line = lines[0]
        line.id.full.shouldNotBeBlank()
        line.commitId.full.shouldNotBeBlank()
        line.author.name shouldBe "Test User"
        line.author.email shouldBe "test@example.com"
        line.description.actual shouldBe "Test description\n"
        line.lineContent shouldBe "content\n"
        line.lineNumber shouldBe 1
    }
}
