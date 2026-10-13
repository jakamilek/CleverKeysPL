package tribixbite.cleverkeys.gif

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.Disposable
import coil.request.ImageRequest
import coil.size.Scale
import java.io.File

/** One local preview. Requests and drawables belong only to an attached grid tile. */
internal class GifPreviewLoader(
    private val filesDir: File,
    private val imageView: ImageView,
    private val loader: GifPreviewRequestLoader,
    private val canAnimateWebp: Boolean
) {
    private var gif: Gif? = null
    private var visible = false
    private var closed = false
    private var generation = 0L
    private var request: Disposable? = null

    fun bind(value: Gif) {
        release()
        gif = value
        if (visible && !closed) loadPreferred(value)
    }

    fun setVisible(value: Boolean) {
        if (closed || visible == value) return
        visible = value
        release()
        if (value) gif?.let(::loadPreferred)
    }

    fun clear() {
        visible = false
        gif = null
        release()
    }

    fun close() {
        closed = true
        clear()
    }

    private fun release() {
        generation++ // Invalidate callbacks before cancellation can deliver an error.
        request?.dispose()
        request = null
        (imageView.drawable as? Animatable)?.stop()
        imageView.setImageDrawable(null)
    }

    private fun loadPreferred(value: Gif) {
        val full = File(filesDir, value.getFullPath())
        if (canAnimateWebp && full.isFile && full.length() > 0) {
            load(value, full, animated = true)
        } else {
            loadThumbnail(value)
        }
    }

    private fun loadThumbnail(value: Gif) {
        val thumb = File(filesDir, value.getThumbnailPath())
        if (thumb.isFile && thumb.length() > 0) load(value, thumb, animated = false)
    }

    private fun load(value: Gif, file: File, animated: Boolean) {
        val ticket = generation
        fun current() = !closed && visible && gif == value && generation == ticket
        val pending = loader.enqueue(file, animated,
            onSuccess = { drawable ->
                if (current()) {
                    imageView.setImageDrawable(drawable)
                    if (animated) (drawable as? Animatable)?.start()
                } else {
                    (drawable as? Animatable)?.stop()
                }
            },
            onError = {
                if (current()) {
                    release()
                    if (animated) loadThumbnail(value)
                }
            }
        )
        // Also safe if a loader delivers an immediate callback during enqueue.
        if (current()) request = pending else pending.dispose()
    }

    companion object {
        const val PREVIEW_SIZE_PX = 200
    }
}

/** Platform boundary: lifecycle tests drive the real tile without an Android decoder. */
internal interface GifPreviewRequestLoader {
    fun enqueue(
        file: File,
        animated: Boolean,
        onSuccess: (Drawable) -> Unit,
        onError: () -> Unit
    ): Disposable
}

internal class CoilGifPreviewRequestLoader(
    private val context: Context,
    private val imageLoader: ImageLoader
) : GifPreviewRequestLoader {
    override fun enqueue(
        file: File,
        animated: Boolean,
        onSuccess: (Drawable) -> Unit,
        onError: () -> Unit
    ): Disposable = imageLoader.enqueue(
        ImageRequest.Builder(context)
            .data(file)
            // FIT bounds both decoded dimensions, including extreme aspect ratios.
            .size(GifPreviewLoader.PREVIEW_SIZE_PX)
            .scale(Scale.FIT)
            // Never retain native animated drawables for an entire 100-item page.
            .memoryCachePolicy(if (animated) CachePolicy.DISABLED else CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .crossfade(false)
            .target(onSuccess = onSuccess, onError = { onError() })
            .build()
    )
}
