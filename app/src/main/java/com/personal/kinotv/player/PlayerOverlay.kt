package com.personal.kinotv.player

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.personal.kinotv.data.WatchProgress

/**
 * Нижняя панель в стиле Netflix: название, серия, полоса времени и подсказки по кнопкам.
 * Появляется при нажатиях и сама прячется через несколько секунд.
 */
class PlayerOverlay(context: Context) : FrameLayout(context) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()

    private val title = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 22f
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    private val subtitle = TextView(context).apply {
        setTextColor(Color.parseColor("#B3B3B3"))
        textSize = 15f
        maxLines = 1
    }
    private val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 1000
        progressDrawable.setTint(Color.parseColor("#E53935"))
    }
    private val time = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 15f
    }
    private val status = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 24f
        gravity = Gravity.CENTER
        setPadding(dp(28), dp(12), dp(28), dp(12))
        background = GradientDrawable().apply {
            cornerRadius = dp(28).toFloat()
            setColor(Color.parseColor("#CC000000"))
        }
    }
    private val hint = TextView(context).apply {
        setTextColor(Color.parseColor("#B3B3B3"))
        textSize = 13f
        text = "OK — пауза   ←/→ — перемотка   ↑ — курсор (серии, озвучка)   «Назад» — выход"
    }

    private val hideRunnable = Runnable { visibility = GONE }

    init {
        visibility = GONE
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(48), dp(56), dp(48), dp(20))
            // Непрозрачный низ закрывает полосу управления самого плеера
            background = GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(Color.BLACK, Color.BLACK, Color.parseColor("#B3000000"), Color.TRANSPARENT),
            )
            addView(title)
            addView(subtitle)
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, dp(6))
                addView(bar, LinearLayout.LayoutParams(0, dp(6), 1f))
                addView(time, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(16) })
            }
            addView(row)
            addView(hint)
        }
        addView(panel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        // Надпись выше центра, чтобы не закрывать кнопки самого плеера
        addView(status, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL or Gravity.TOP).apply { topMargin = dp(90) })
        isFocusable = false
        isClickable = false
    }

    fun setTitle(text: String?) {
        title.text = text.orEmpty()
    }

    fun update(timeSec: Double, durationSec: Double, label: String?) {
        if (durationSec > 0) bar.progress = (timeSec / durationSec * 1000).toInt().coerceIn(0, 1000)
        time.text = WatchProgress.formatTime(timeSec) + " / " + WatchProgress.formatTime(durationSec)
        subtitle.text = label.orEmpty()
        subtitle.visibility = if (label.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    /** Показать панель; status — крупная надпись по центру («⏸», «+10 с»), исчезает вместе с панелью. */
    fun flash(statusText: String? = null, millis: Long = 3500) {
        status.text = statusText.orEmpty()
        status.visibility = if (statusText.isNullOrBlank()) View.GONE else View.VISIBLE
        visibility = VISIBLE
        removeCallbacks(hideRunnable)
        postDelayed(hideRunnable, millis)
    }

    fun hide() {
        removeCallbacks(hideRunnable)
        visibility = GONE
    }
}
