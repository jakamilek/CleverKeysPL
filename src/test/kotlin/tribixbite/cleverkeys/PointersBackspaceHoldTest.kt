package tribixbite.cleverkeys

import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.objenesis.ObjenesisStd

/** Real pointer routing: hold is non-destructive and owns moves outside the original key. */
class PointersBackspaceHoldTest {
    private lateinit var pointers: Pointers
    private lateinit var handler: Pointers.IPointerEventHandler
    private lateinit var timers: Handler
    private lateinit var ptr: Pointers.Pointer
    private val ptrs = ArrayList<Pointers.Pointer>()

    @Before fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        handler = mockk(relaxed = true); timers = mockk(relaxed = true)
        every { handler.beginBackspaceHold() } returns true
        every { handler.backspaceKeyboardWidth() } returns 1000f
        val key = KeyValue.keyeventKey(0xE003, KeyEvent.KEYCODE_DEL, 0)
        ptr = Pointers.Pointer(1, KeyboardData.Key.EMPTY.withKeyValue(0,key), key, 900f, 100f,
            Pointers.Modifiers.EMPTY, Pointers.FLAG_P_DEFERRED_DOWN,
            testConfigSnapshot(swipe_typing_enabled = false, keyrepeat_enabled = false))
        pointers = ObjenesisStd().newInstance(Pointers::class.java)
        field("_handler", handler); field("_ptrs", ptrs); field("_longpress_handler", timers)
        ptrs.add(ptr)
    }
    @After fun teardown() { unmockkStatic(Log::class) }
    private fun field(name: String, value: Any) {
        Pointers::class.java.getDeclaredField(name).apply { isAccessible = true }.set(pointers,value)
    }
    private fun hold() {
        Pointers::class.java.getDeclaredMethod("handleLongPress", Pointers.Pointer::class.java)
            .apply { isAccessible = true }.invoke(pointers,ptr)
    }
    @Test fun holdWithoutMovingPreviewsWordEvenWithRepeatDisabled() {
        hold(); assertTrue(ptr.backspaceWordHold)
        verify(exactly = 1) { handler.beginBackspaceHold() }
        verify(exactly = 0) { handler.onPointerHold(any(),any()) }
        verify(exactly = 0) { handler.finishBackspaceHold(any()) }
    }
    @Test fun movingAcrossOtherKeysNeverActivatesTheirKeys() {
        hold(); pointers.onTouchMove(100f,200f,1)
        verify(exactly = 1) { handler.stepBackspaceHold(-1) }
        verify(exactly = 0) { handler.onPointerDown(any(),any()) }
        verify(exactly = 0) { handler.onPointerUp(any(),any()) }
        assertTrue(pointers.isSliding())
    }
    @Test fun movingRightInLeftHalfShrinksSelection() {
        hold(); pointers.onTouchMove(100f,200f,1); pointers.onTouchMove(130f,300f,1)
        verify(exactly = 1) { handler.stepBackspaceHold(-1) }
        verify(exactly = 1) { handler.stepBackspaceHold(1) }
    }
    @Test fun releaseCommitsSelectionExactlyOnceWithoutSendingDeleteKey() {
        hold(); pointers.onTouchUp(1); pointers.onTouchUp(1)
        verify(exactly = 1) { handler.finishBackspaceHold(true) }
        verify(exactly = 0) { handler.onPointerUp(any(),any()) }
        assertTrue(ptrs.isEmpty())
    }
    @Test fun cancelRestoresSelectionAndStopsTimerWithoutDeleting() {
        hold(); pointers.clear()
        verify(exactly = 1) { handler.finishBackspaceHold(false) }
        verify { timers.removeMessages(ptr.selectionDeleteWhat) }
        verify(exactly = 0) { handler.onPointerUp(any(),any()) }
        assertTrue(ptrs.isEmpty())
    }
    @Test fun furtherTouchIsIgnoredWhileHoldOwnsKeyboard() {
        hold()
        pointers.onTouchDown(20f,20f,2,ptr.key)
        assertEquals(1,ptrs.size)
        verify(exactly = 0) { handler.onPointerDown(any(),any()) }
    }
}
