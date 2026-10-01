package tribixbite.cleverkeys.ml

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SwipeMLDataDecoderCandidatesTest {

    @Test
    fun decoderCandidates_roundTrip() {
        val data = SwipeMLData(
            "malina", "playground", 1000, 2000, 700,
            "latn_qwerty_pl", SwipeMLData.ENGINE_GEOMETRIC
        )
        data.addRawPoint(100f, 1000f, data.timestampUtc + 1)
        data.addRawPoint(200f, 1000f, data.timestampUtc + 2)
        data.addRegisteredKey("m")
        data.addRegisteredKey("a")
        data.setDecoderCandidates(listOf("malina", "malinę"), listOf(900, 500))

        val reloaded = SwipeMLData(data.toJSON())
        assertThat(reloaded.getDecoderCandidates()!!.map { it.word })
            .containsExactly("malina", "malinę").inOrder()
        assertThat(reloaded.getDecoderCandidates()!!.map { it.score })
            .containsExactly(900, 500).inOrder()
    }
}
