package `in`.kkkev.jjidea.ui.squash

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SquashDescriptionTest {
    @Test
    fun `both non-empty descriptions are joined with blank line`() {
        mergeDescriptions("parent desc", "source desc") shouldBe "parent desc\n\nsource desc"
    }

    @Test
    fun `source empty returns parent only`() {
        mergeDescriptions("parent desc", "") shouldBe "parent desc"
    }

    @Test
    fun `parent empty returns source only`() {
        mergeDescriptions("", "source desc") shouldBe "source desc"
    }

    @Test
    fun `both empty returns empty string`() {
        mergeDescriptions("", "") shouldBe ""
    }

    @Test
    fun `multiline descriptions are joined correctly`() {
        mergeDescriptions("line 1\nline 2", "line 3\nline 4") shouldBe "line 1\nline 2\n\nline 3\nline 4"
    }
}
