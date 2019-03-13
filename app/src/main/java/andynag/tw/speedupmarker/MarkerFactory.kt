package andynag.tw.speedupmarker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.custom_marker.view.*

data class CustomMarker(
    val id: String,
    val number: String
)

class MarkerFactory(private val context: Context) {

    var useCache: Boolean = false

    private val cache = LruCache<CustomMarker, Bitmap>(1000)

    fun getMarkerBitmap(customMarker: CustomMarker): Bitmap {
        return if (useCache) {
            cache[customMarker] ?: createBitmap(customMarker)
        } else {
            createBitmap(customMarker)
        }
    }

    private fun createBitmap(customMarker: CustomMarker): Bitmap {
        val view = LayoutInflater.from(context).inflate(R.layout.custom_marker, null)
        val textNumber = view.textNumber

        textNumber.text = customMarker.number

        view.layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_4444)
        val c = Canvas(bitmap)
        view.draw(c)

        cache.put(customMarker, bitmap)

        return bitmap
    }

}