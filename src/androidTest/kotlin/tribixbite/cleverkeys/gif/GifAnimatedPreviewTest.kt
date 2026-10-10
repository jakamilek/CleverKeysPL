package tribixbite.cleverkeys.gif

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.widget.ImageView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Real pinned Coil decoder and real tile; this fixture belongs only to the test APK. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28)
class GifAnimatedPreviewTest {
    @Test fun localWebpAnimatesAndStopsWhenTileLeavesGrid() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val root = File(context.cacheDir, "gif-preview-${System.nanoTime()}").apply { mkdirs() }
        val gif = Gif(7, 8, 8)
        val full = File(root, gif.getFullPath()).apply { parentFile!!.mkdirs() }
        full.writeBytes(Base64.decode(
            "UklGRoQAAABXRUJQVlA4WAoAAAACAAAABwAABwAAQU5JTQYAAAAAAAAAAABBTk1GKAAAAAAAAAAAAAcAAAcAAHgAAAJWUDhMDwAAAC8HwAEABxD9j/4HIqL/AQBBTk1GKAAAAAAAAAAAAAcAAAcAAHgAAABWUDhMDwAAAC8HwAEABxDR//4HIqL/AQA=",
            Base64.DEFAULT
        ))
        val imageLoader = ImageLoader.Builder(context)
            .components { add(ImageDecoderDecoder.Factory()) }.build()
        val arrived = CountDownLatch(1)
        val decoded = AtomicReference<Drawable?>()
        val failed = AtomicReference(false)
        val requests = CoilGifPreviewRequestLoader(context, imageLoader)
        val observed = object : GifPreviewRequestLoader {
            override fun enqueue(file: File, animated: Boolean,
                onSuccess: (Drawable) -> Unit, onError: () -> Unit) =
                requests.enqueue(file, animated,
                    { decoded.set(it); onSuccess(it); arrived.countDown() },
                    { failed.set(true); onError(); arrived.countDown() })
        }
        var preview: GifPreviewLoader? = null
        var view: ImageView? = null
        try {
            instrumentation.runOnMainSync {
                view = ImageView(context)
                preview = GifPreviewLoader(root, view!!, observed, true)
                preview!!.bind(gif)
                preview!!.setVisible(true)
            }
            assertTrue("local WebP decode timeout", arrived.await(10, TimeUnit.SECONDS))
            assertFalse("real WebP decoder failed", failed.get())
            instrumentation.runOnMainSync {
                val image = checkNotNull(decoded.get())
                assertTrue("full file must decode to an animation", image is Animatable)
                val animation = image as Animatable
                assertTrue(animation.isRunning)
                assertTrue(image.intrinsicWidth <= GifPreviewLoader.PREVIEW_SIZE_PX)
                assertTrue(image.intrinsicHeight <= GifPreviewLoader.PREVIEW_SIZE_PX)
                preview!!.setVisible(false)
                assertFalse(animation.isRunning)
                assertNull(view!!.drawable)
            }
        } finally {
            instrumentation.runOnMainSync { preview?.close() }
            imageLoader.shutdown()
            root.deleteRecursively()
        }
    }
}
