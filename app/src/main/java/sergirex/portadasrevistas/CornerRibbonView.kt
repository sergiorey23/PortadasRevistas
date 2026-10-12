package sergirex.portadasrevistas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes

/**
 * A diagonal band across the top-right corner of its square bounds, with a short [text] on it
 * (the "old issue" tag on covers). Everything outside the band is left transparent, so the view
 * can sit over an image without hiding more than the corner.
 */
class CornerRibbonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val band = Path()

    private var preferredTextSize = 0f

    var text: CharSequence = ""
        set(value) {
            field = value
            contentDescription = value
            invalidate()
        }

    init {
        context.withStyledAttributes(attrs, R.styleable.CornerRibbonView, defStyleAttr, 0) {
            bandPaint.color = getColor(R.styleable.CornerRibbonView_ribbonColor, DEFAULT_COLOR)
            textPaint.color = getColor(R.styleable.CornerRibbonView_ribbonTextColor, DEFAULT_TEXT_COLOR)
            preferredTextSize = getDimension(R.styleable.CornerRibbonView_ribbonTextSize, 10f * resources.displayMetrics.scaledDensity)
            textPaint.textSize = preferredTextSize
            text = getString(R.styleable.CornerRibbonView_ribbonText).orEmpty()
        }
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun onDraw(canvas: Canvas) {
        val size = minOf(width, height).toFloat()
        if (size <= 0f) return
        val right = width.toFloat()

        // The band lies between two lines at distance `near` and `far` (measured along the edges)
        // from the top-right corner.
        val near = size * BAND_START
        val far = size * BAND_END
        band.rewind()
        band.moveTo(right - far, 0f)
        band.lineTo(right - near, 0f)
        band.lineTo(right, near)
        band.lineTo(right, far)
        band.close()
        canvas.drawPath(band, bandPaint)

        // The text sits on the band's centre line, running down to the right. The centre line is
        // only so long, so a longer label is shrunk to fit instead of running off the band.
        val centre = (near + far) / 4f
        fitTextTo(centre * SQRT_2 * 2f * TEXT_FILL)
        canvas.save()
        canvas.rotate(45f, right - centre, centre)
        val baseline = centre - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text.toString(), right - centre, baseline, textPaint)
        canvas.restore()
    }

    private fun fitTextTo(available: Float) {
        textPaint.textSize = preferredTextSize
        val needed = textPaint.measureText(text.toString())
        if (needed > available && needed > 0f) textPaint.textSize = preferredTextSize * available / needed
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Always square: the band is defined relative to the corner.
        val size = minOf(
            resolveSize(suggestedMinimumWidth, widthMeasureSpec),
            resolveSize(suggestedMinimumHeight, heightMeasureSpec)
        )
        setMeasuredDimension(size, size)
    }

    private companion object {
        const val DEFAULT_COLOR = 0xFFD32F2F.toInt()
        const val DEFAULT_TEXT_COLOR = 0xFFFFFFFF.toInt()
        const val BAND_START = 0.30f
        const val BAND_END = 0.78f
        const val TEXT_FILL = 0.82f
        const val SQRT_2 = 1.4142135f
    }
}
