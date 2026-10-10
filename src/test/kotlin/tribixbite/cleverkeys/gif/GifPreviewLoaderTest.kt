package tribixbite.cleverkeys.gif

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import coil.request.Disposable
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Real tile lifecycle; native Coil/WebP decoding has a separate instrumented test. */
class GifPreviewLoaderTest {
    @get:Rule val folder = TemporaryFolder()
    private lateinit var view: ImageView
    private lateinit var preview: GifPreviewLoader
    private var drawable: Drawable? = null
    private val calls = mutableListOf<Call>()
    private val gif = Gif(7, 8, 8)
    private data class Call(
        val file: File, val animated: Boolean, val success: (Drawable) -> Unit,
        val error: () -> Unit, val disposable: Disposable
    )
    private val loader = object : GifPreviewRequestLoader {
        override fun enqueue(file: File, animated: Boolean,
            onSuccess: (Drawable) -> Unit, onError: () -> Unit): Disposable {
            val disposable = mockk<Disposable>(relaxed = true)
            calls += Call(file, animated, onSuccess, onError, disposable)
            return disposable
        }
    }

    @Before fun setup() {
        drawable = null
        view = mockk(relaxed = true)
        every { view.drawable } answers { drawable }
        every { view.setImageDrawable(any()) } answers { drawable = firstArg() }
        preview = GifPreviewLoader(folder.root, view, loader, true)
    }
    @After fun cleanup() { preview.close(); unmockkAll() }
    private fun put(path: String): File = File(folder.root, path).apply {
        parentFile!!.mkdirs(); writeBytes(byteArrayOf(1))
    }
    private fun animation(): Drawable =
        mockk<Drawable>(relaxed = true, moreInterfaces = arrayOf(Animatable::class))
    private fun showFull() {
        put(gif.getFullPath()); preview.bind(gif); preview.setVisible(true)
    }

    @Test fun bindingOffscreenDoesNotDecode() {
        put(gif.getFullPath()); preview.bind(gif)
        assertTrue(calls.isEmpty())
        preview.setVisible(true)
        assertEquals(1, calls.size)
        assertEquals(File(folder.root, gif.getFullPath()), calls.single().file)
        assertTrue(calls.single().animated)
    }
    @Test fun staticOnlyPackUsesThumbnail() {
        put(gif.getThumbnailPath()); preview.bind(gif); preview.setVisible(true)
        assertFalse(calls.single().animated)
        assertEquals(File(folder.root, gif.getThumbnailPath()), calls.single().file)
    }
    @Test fun oldAndroidKeepsThumbnailEvenWithFullFile() {
        preview.close()
        preview = GifPreviewLoader(folder.root, view, loader, false)
        put(gif.getFullPath()); put(gif.getThumbnailPath())
        preview.bind(gif); preview.setVisible(true)
        assertFalse(calls.single().animated)
    }
    @Test fun missingOrEmptyFilesNeverRequest() {
        put(gif.getFullPath()).writeBytes(byteArrayOf())
        preview.bind(gif); preview.setVisible(true)
        assertTrue(calls.isEmpty()); assertNull(drawable)
    }
    @Test fun fullDecodeFailureFallsBackOnceToThumbnail() {
        put(gif.getThumbnailPath()); showFull()
        calls[0].error()
        assertEquals(2, calls.size); assertFalse(calls[1].animated)
        verify(exactly = 1) { calls[0].disposable.dispose() }
        calls[1].error()
        assertEquals(2, calls.size); assertNull(drawable)
    }
    @Test fun detachCancelsAndStopsAndReattachReloads() {
        showFull()
        val image = animation(); calls[0].success(image)
        verify(exactly = 1) { (image as Animatable).start() }
        preview.setVisible(false)
        verify(exactly = 1) { (image as Animatable).stop() }
        verify(exactly = 1) { calls[0].disposable.dispose() }
        assertNull(drawable)
        preview.setVisible(true)
        assertEquals(2, calls.size)
    }
    @Test fun recycledHolderIgnoresLateSuccessAndError() {
        showFull(); val old = calls[0]
        val next = Gif(8, 8, 8); put(next.getFullPath()); preview.bind(next)
        val image = animation(); old.success(image); old.error()
        assertNull(drawable); assertEquals(2, calls.size)
        verify(exactly = 0) { (image as Animatable).start() }
        verify(exactly = 1) { (image as Animatable).stop() }
        assertEquals(File(folder.root, next.getFullPath()), calls[1].file)
    }
    @Test fun closedPaneCannotRestartOrFallback() {
        put(gif.getThumbnailPath()); showFull()
        val old = calls[0]; preview.close()
        old.error(); val image = animation(); old.success(image)
        preview.setVisible(true); preview.bind(gif)
        assertEquals(1, calls.size); assertNull(drawable)
        verify(exactly = 0) { (image as Animatable).start() }
    }
    @Test fun clearForRecyclingRemovesBoundGif() {
        showFull(); preview.clear(); preview.setVisible(true)
        assertEquals(1, calls.size); assertNull(drawable)
        verify(exactly = 1) { calls[0].disposable.dispose() }
    }
}
