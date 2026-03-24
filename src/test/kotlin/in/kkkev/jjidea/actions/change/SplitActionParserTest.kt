package `in`.kkkev.jjidea.actions.change

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SplitActionParserTest {
    @Nested
    inner class `parseRemainingChangeId` {
        @Test
        fun `parses change ID from working copy split`() {
            val stderr =
                """
                Selected changes : vwtlsktn 8d7f395b first part
                Remaining changes: ymlsxksm c882b108 test commit
                Working copy  (@) now at: ymlsxksm c882b108 test commit
                Parent commit (@-)      : vwtlsktn 8d7f395b first part
                """.trimIndent()

            val result = parseRemainingChangeId(stderr)

            result!!.full shouldBe "ymlsxksm"
        }

        @Test
        fun `parses change ID from non-working-copy split with rebased descendants`() {
            val stderr =
                """
                Rebased 1 descendant commits
                Selected changes : ppvwmllp bbaf2a0a first part
                Remaining changes: ulwnpxxq 3fae5248 test commit
                Working copy  (@) now at: ptsprstw 982a4452 (empty) (no description set)
                Parent commit (@-)      : ulwnpxxq 3fae5248 test commit
                """.trimIndent()

            val result = parseRemainingChangeId(stderr)

            result!!.full shouldBe "ulwnpxxq"
        }

        @Test
        fun `returns null for unrecognized output`() {
            val stderr = "Some unexpected output"

            val result = parseRemainingChangeId(stderr)

            result shouldBe null
        }

        @Test
        fun `returns null for empty string`() {
            val result = parseRemainingChangeId("")

            result shouldBe null
        }
    }
}
