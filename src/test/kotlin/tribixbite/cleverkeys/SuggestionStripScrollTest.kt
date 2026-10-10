package tribixbite.cleverkeys

import android.view.ViewParent
import android.widget.HorizontalScrollView
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.objenesis.ObjenesisStd

/** Exercise the actual posted viewport reset without constructing android.jar views. */
class SuggestionStripScrollTest {
    private lateinit var bar: SuggestionBar
    private lateinit var scroller: HorizontalScrollView
    private var currentParent: ViewParent? = null
    private val queued = mutableListOf<Runnable>()

    @Before fun setup() {
        bar = spyk(ObjenesisStd().newInstance(SuggestionBar::class.java))
        scroller = mockk(relaxed = true)
        currentParent = scroller
        every { bar.parent } answers { currentParent }
        every { scroller.post(any()) } answers {
            queued.add(firstArg<Runnable>())
            true
        }
    }

    @After fun teardown() = unmockkAll()

    @Test fun aNewWordRevealsTheFirstCandidateEvenWithUnchangedContent() {
        repeat(2) {
            bar.resetScrollPosition()
            queued.removeAt(0).run()
        }
        verify(exactly = 2) { scroller.scrollTo(0, 0) }
    }

    @Test fun aQueuedResetDoesNotMoveAReplacedOrDetachedStrip() {
        bar.resetScrollPosition()
        currentParent = mockk<HorizontalScrollView>(relaxed = true)
        queued.removeAt(0).run()
        currentParent = scroller
        bar.resetScrollPosition()
        currentParent = null
        queued.removeAt(0).run()
        verify(exactly = 0) { scroller.scrollTo(any(), any()) }
    }

    @Test fun anUnhostedStripDoesNotQueueAReset() {
        currentParent = null
        bar.resetScrollPosition()
        verify(exactly = 0) { scroller.post(any()) }
    }
}
