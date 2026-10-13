package tribixbite.cleverkeys

import android.os.Handler
import android.util.Log
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Drives real Shift release routing; a case edit must consume the tap before latching. */
class PointersWordCapitalizationTest {
    private lateinit var pointers: Pointers
    private lateinit var handler: Pointers.IPointerEventHandler
    private lateinit var ptrs: ArrayList<Pointers.Pointer>
    private val shift = KeyValue.getKeyByName("shift")
    private val shiftKey = KeyboardData.Key.EMPTY.withKeyValue(0, shift)
    private val snap = testConfigSnapshot(swipe_typing_enabled = false, short_gestures_enabled = false)

    @Before fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        handler = mockk(relaxed = true)
        every { handler.isShiftLocked() } returns false
        every { handler.tryWordCapitalization() } returns true
        val unsafe = unsafe()
        pointers = unsafe.javaClass.getMethod("allocateInstance", Class::class.java)
            .invoke(unsafe, Pointers::class.java) as Pointers
        ptrs = ArrayList()
        field("_handler", handler)
        field("_ptrs", ptrs)
        field("_longpress_handler", mockk<Handler>(relaxed = true))
    }
    @After fun teardown() { unmockkStatic(Log::class) }
    private fun unsafe(): Any = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe")
        .apply { isAccessible = true }.get(null)
    private fun field(name: String, value: Any) {
        val u = unsafe()
        val f = Pointers::class.java.getDeclaredField(name)
        val offset = u.javaClass.getMethod("objectFieldOffset", java.lang.reflect.Field::class.java)
            .invoke(u, f) as Long
        u.javaClass.getMethod("putObject", Any::class.java, Long::class.javaPrimitiveType, Any::class.java)
            .invoke(u, pointers, offset, value)
    }
    private fun release(flags: Int = Pointers.FLAG_P_LATCHABLE,
        mods: Pointers.Modifiers = Pointers.Modifiers.EMPTY, leftKey: Boolean = false) {
        val ptr = Pointers.Pointer(1, shiftKey, shift, 100f, 100f, mods, flags, snap)
        ptr.hasLeftStartingKey = leftKey
        ptrs.add(ptr)
        pointers.onTouchUp(1)
    }
    @Test fun handledTapDoesNotLatchShift() {
        release()
        verify(exactly = 1) { handler.tryWordCapitalization() }
        assertTrue(ptrs.isEmpty())
        verify(exactly = 0) { handler.onPointerUp(any(), any()) }
    }
    @Test fun unhandledTapRetainsNormalShiftLatch() {
        every { handler.tryWordCapitalization() } returns false
        release()
        assertEquals(1, ptrs.size)
        assertTrue(ptrs.single().hasFlagsAny(Pointers.FLAG_P_LATCHED))
    }
    @Test fun capsLockIsNotConvertedToCaseEditing() {
        every { handler.isShiftLocked() } returns true
        release()
        verify(exactly = 0) { handler.tryWordCapitalization() }
    }
    @Test fun longPressLockIsNotConvertedToCaseEditing() {
        release(Pointers.FLAG_P_LATCHABLE or Pointers.FLAG_P_LOCKED)
        verify(exactly = 0) { handler.tryWordCapitalization() }
    }
    @Test fun usedShiftChordDoesNotEditWord() {
        release(flags = 0)
        verify(exactly = 0) { handler.tryWordCapitalization() }
    }
    @Test fun otherModifiersDoNotEditWord() {
        release(mods = Pointers.Modifiers.EMPTY.with_extra_mod(KeyValue.getKeyByName("ctrl")))
        verify(exactly = 0) { handler.tryWordCapitalization() }
    }
    @Test fun directionalShiftGestureKeepsItsBehavior() {
        release(leftKey = true)
        verify(exactly = 0) { handler.tryWordCapitalization() }
    }
    @Test fun simultaneousPointerKeepsChordBehavior() {
        ptrs.add(Pointers.Pointer(2, shiftKey, shift, 0f, 0f,
            Pointers.Modifiers.EMPTY, 0, snap))
        release()
        verify(exactly = 0) { handler.tryWordCapitalization() }
    }
    @Test fun autocapLatchIsClearedAfterExplicitWordEdit() {
        ptrs.add(Pointers.Pointer(-1, shiftKey, shift, 0f, 0f,
            Pointers.Modifiers.EMPTY, Pointers.FLAG_P_FAKE or Pointers.FLAG_P_LATCHED or
                Pointers.FLAG_P_LATCHABLE, snap))
        release()
        verify(exactly = 1) { handler.tryWordCapitalization() }
        assertTrue(ptrs.isEmpty())
    }
}
