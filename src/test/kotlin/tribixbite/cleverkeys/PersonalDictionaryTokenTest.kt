package tribixbite.cleverkeys

import org.junit.Assert.*
import org.junit.Test

class PersonalDictionaryTokenTest {
    private fun read(before: String, after: String = "") = PersonalDictionaryToken.read(before, after, null)
    @Test fun emailAndHyphenatedWordAreWholeEntries() {
        assertEquals("przykład.ten@gmail.com", read("przykład.ten@gmail.com")?.word)
        assertEquals("czarno-biały", read("czarno-biały")?.word)
    }
    @Test fun middleCursorJoinsBothSidesAndRetainsCase() {
        val token = read("Napisz do Przykład.ten@", "gmail.com teraz")!!
        assertEquals("Przykład.ten@gmail.com", token.word)
        assertEquals("Przykład.ten@", token.prefix)
        assertEquals("gmail.com", token.suffix)
    }
    @Test fun oneCompletionSpaceIsKeptOutsideTheEntry() {
        val token = read("czarno-biały ")!!
        assertEquals("czarno-biały", token.word)
        assertEquals(" ", token.separator)
        assertNull(read("czarno-biały  "))
    }
    @Test fun ordinaryWordsAndApostropheOnlyContractionsKeepExistingPath() {
        for (s in listOf("grzeje", "don't", "l’amour")) assertNull(read(s))
    }
    @Test fun unfinishedAddressesAndSentencePunctuationAreNotEntries() {
        for (s in listOf("adres@", "kot.", "mail@host.", "biały-", "-biały", "@host.pl")) assertNull(read(s))
    }
    @Test fun whitespaceControlsAndSelectionsCannotOfferFragments() {
        assertNull(read("a@\u0000b"))
        assertNull(PersonalDictionaryToken.read("a@b.pl", "", "b"))
        assertNull(PersonalDictionaryToken.read(null, "b.pl", null))
        assertNull(PersonalDictionaryToken.read("a@", null, null))
    }
    @Test fun boundedReadsRejectTruncatedTokensFromEitherDirection() {
        assertNull(read("a".repeat(129) + "@b"))
        assertNull(read("a@", "b".repeat(130)))
        assertNull(read("a".repeat(127) + "@b"))
        assertEquals(128, read("a".repeat(125) + "@pl")?.word?.length)
    }
    @Test fun digitsInAddressAreAllowedButNumericPunctuationIsNotAWord() {
        assertEquals("adres2@domena.pl", read("adres2@domena.pl")?.word)
        assertNull(read("3.14"))
    }
}
