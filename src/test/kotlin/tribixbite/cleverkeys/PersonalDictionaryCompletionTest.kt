package tribixbite.cleverkeys

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PersonalDictionaryCompletionTest {
    private val email = "przykład.ten@gmail.com"
    private val index = PersonalDictionaryCompletion(listOf(email, "czarno-biały", "przykład", "don't"))

    @Test fun savedEmailIsReachableFromTheFirstCharacter() {
        assertThat(index.matches("p")).containsExactly(email)
        assertThat(index.matches("prz")).containsExactly(email)
    }
    @Test fun emailRemainsReachableAcrossEveryInternalPunctuation() {
        for (prefix in listOf("przykład.", "przykład.t", "przykład.ten@", "przykład.ten@gmail.")) {
            assertThat(index.matches(prefix)).containsExactly(email)
        }
    }
    @Test fun matchingIgnoresCaseButPreservesLiteralStoredCase() {
        val mixed = PersonalDictionaryCompletion(listOf("Jan.Kowalski@Example.com"))
        assertThat(mixed.matches("JAN.K")).containsExactly("Jan.Kowalski@Example.com")
    }
    @Test fun hyphenAndDigitsRemainInThePrefix() {
        val numbered = PersonalDictionaryCompletion(listOf("jan2@host.pl"))
        assertThat(numbered.matches("jan2@")).containsExactly("jan2@host.pl")
        assertThat(index.matches("czarno-b")).containsExactly("czarno-biały")
    }
    @Test fun noProseContractionsOrDuplicatesAndCompleteEntryRemainsLiteral() {
        assertThat(index.matches("przykład")).containsExactly(email)
        assertThat(index.matches("don")).isEmpty()
        assertThat(index.matches(email)).containsExactly(email)
        assertThat(PersonalDictionaryCompletion(listOf(email, email)).matches("p")).containsExactly(email)
    }
    @Test fun multipleEntriesHaveDeterministicShorterFirstOrdering() {
        val many = PersonalDictionaryCompletion(listOf("a.long@host.pl", "a@host.pl", "a.b@host.pl"))
        assertThat(many.matches("a")).containsExactly("a@host.pl", "a.b@host.pl", "a.long@host.pl").inOrder()
    }
    @Test fun boundedWholeTokenReadIncludesMiddleSuffix() {
        assertThat(PersonalDictionaryCompletion.read("tekst przykład.t", "en@gmail.com dalej", null))
            .isEqualTo(PersonalDictionaryCompletion.Token("przykład.t", "en@gmail.com"))
    }
    @Test fun trailingSpaceSelectionAndUnavailableReadsRejectCompletion() {
        assertThat(PersonalDictionaryCompletion.read("prz ", "", null)).isNull()
        assertThat(PersonalDictionaryCompletion.read("prz", "", "prz")).isNull()
        assertThat(PersonalDictionaryCompletion.read(null, "", null)).isNull()
        assertThat(PersonalDictionaryCompletion.read("prz", null, null)).isNull()
    }
    @Test fun oversizedOrTruncatedTokensAndControlsRejectCompletion() {
        assertThat(PersonalDictionaryCompletion.read("x".repeat(130), "", null)).isNull()
        assertThat(PersonalDictionaryCompletion.read("p", "x".repeat(130), null)).isNull()
        assertThat(PersonalDictionaryCompletion.read("prz\u0000", "", null)).isNull()
        assertThat(index.matches("x".repeat(129))).isEmpty()
    }
    @Test fun indexingExcludesInvalidOrPlainEntries() {
        val invalid = PersonalDictionaryCompletion(listOf("plain", "a@", "x".repeat(129) + "@a", "bad name@host.pl"))
        assertThat(invalid.matches("a")).isEmpty()
        assertThat(invalid.matches("p")).isEmpty()
        assertThat(invalid.matches("bad")).isEmpty()
    }
}
