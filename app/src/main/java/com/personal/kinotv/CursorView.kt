package com.personal.kinotv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import kotlin.math.min

/**
 * Курсор, которым управляют стрелками пульта. Рисуется поверх всего,
 * в том числе поверх полноэкранного плеера.
 */
class CursorView(context: Context) : View(context) {

    var cursorX = 0f
        private set
    var cursorY = 0f
        private set

    /** Множитель скорости из настроек. */
    var speedFactor = 1f

    /** Прятать курсор через несколько секунд бездействия (включается в полноэкранном режиме). */
    var autoHide = false
        set(value) {
            field = value
            if (value) scheduleHide() else { removeCallbacks(hideRunnable); show() }
        }

    var onMove: ((Float, Float) -> Unit)? = null
    var onEdgeScroll: ((Int, Int) -> Unit)? = null

    val cursorVisible get() = visible

    private var visible = true
    private var up = false
    private var down = false
    private var left = false
    private var right = false
    private var holdStart = 0L
    private var lastFrame = 0L
    private var running = false
    private var scrollRemainder = 0f
    private var pulseUntil = 0L
    private var initialized = false

    private val density = resources.displayMetrics.density
    private val arrow = Path()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f * density; strokeJoin = Paint.Join.ROUND
    }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 229, 57, 53); style = Paint.Style.STROKE; strokeWidth = 3f * density
    }

    private val hideRunnable = Runnable { visible = false; invalidate() }

    private val frame = object : Runnable {
        override fun run() {
            step()
            if (up || down || left || right) postOnAnimation(this) else running = false
        }
    }

    init {
        // Классическая стрелка-указатель, кончик в точке (0,0)
        val s = density
        arrow.moveTo(0f, 0f)
        arrow.lineTo(0f, 22f * s)
        arrow.lineTo(5.5f * s, 16.5f * s)
        arrow.lineTo(9.5f * s, 25f * s)
        arrow.lineTo(13f * s, 23.5f * s)
        arrow.lineTo(9f * s, 15.5f * s)
        arrow.lineTo(16f * s, 15.5f * s)
        arrow.close()
        isFocusable = false
        isClickable = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (!initialized && w > 0) {
            cursorX = w / 2f
            cursorY = h / 2f
            initialized = true
        } else {
            cursorX = cursorX.coerceIn(0f, w - 1f)
            cursorY = cursorY.coerceIn(0f, h - 1f)
        }
    }

    /** Возвращает true, если клавиша — стрелка и мы её обработали. */
    fun onDirectionKey(keyCode: Int, pressed: Boolean): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> up = pressed
            KeyEvent.KEYCODE_DPAD_DOWN -> down = pressed
            KeyEvent.KEYCODE_DPAD_LEFT -> left = pressed
            KeyEvent.KEYCODE_DPAD_RIGHT -> right = pressed
            else -> return false
        }
        if (pressed) {
            show()
            if (!running) {
                running = true
                holdStart = SystemClock.uptimeMillis()
                lastFrame = holdStart
                nudge()
                postOnAnimation(frame)
            }
        } else if (!(up || down || left || right)) {
            scheduleHide()
        }
        return true
    }

    fun moveTo(x: Float, y: Float) {
        cursorX = x.coerceIn(0f, (width - 1f).coerceAtLeast(0f))
        cursorY = y.coerceIn(0f, (height - 1f).coerceAtLeast(0f))
        invalidate()
    }

    fun releaseAll() {
        up = false; down = false; left = false; right = false
    }

    fun show() {
        removeCallbacks(hideRunnable)
        if (!visible) { visible = true; invalidate() }
        scheduleHide()
    }

    fun pulse() {
        pulseUntil = SystemClock.uptimeMillis() + 250
        invalidate()
        postDelayed({ invalidate() }, 260)
    }

    private fun scheduleHide() {
        removeCallbacks(hideRunnable)
        if (autoHide) postDelayed(hideRunnable, 3000)
    }

    private fun dirX() = (if (right) 1 else 0) - (if (left) 1 else 0)
    private fun dirY() = (if (down) 1 else 0) - (if (up) 1 else 0)

    /** Короткое нажатие сдвигает курсор на несколько пикселей — для точного прицеливания. */
    private fun nudge() {
        moveBy(dirX() * 6f * density, dirY() * 6f * density, 0f, 0f)
    }

    private fun step() {
        val now = SystemClock.uptimeMillis()
        val dt = min(now - lastFrame, 50L) / 1000f
        lastFrame = now
        val held = (now - holdStart) / 1000f
        if (held < 0.15f) return // пауза, чтобы короткое нажатие не уводило курсор далеко
        val w = width.coerceAtLeast(1)
        // Плавный разгон: сначала медленно, потом быстрее
        val speed = w * (0.12f + min(held, 1.2f) * 0.6f) * speedFactor
        moveBy(dirX() * speed * dt, dirY() * speed * dt, held, dt)
    }

    private fun moveBy(dx: Float, dy: Float, held: Float, dt: Float) {
        if (width == 0) return
        val nx = (cursorX + dx).coerceIn(0f, width - 1f)
        val ny = (cursorY + dy).coerceIn(0f, height - 1f)
        // Курсор упёрся в верх/низ экрана — прокручиваем страницу
        val edge = 2f
        val pushingUp = dy < 0 && ny <= edge
        val pushingDown = dy > 0 && ny >= height - 1f - edge
        if ((pushingUp || pushingDown) && held > 0f) {
            val scrollSpeed = height * (0.6f + min(held, 1.5f) * 1.2f)
            scrollRemainder += (if (pushingDown) 1 else -1) * scrollSpeed * dt
            val px = scrollRemainder.toInt()
            if (px != 0) {
                scrollRemainder -= px
                onEdgeScroll?.invoke(0, px)
            }
        }
        if (nx != cursorX || ny != cursorY) {
            cursorX = nx
            cursorY = ny
            invalidate()
            onMove?.invoke(cursorX, cursorY)
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!visible) return
        if (SystemClock.uptimeMillis() < pulseUntil) {
            canvas.drawCircle(cursorX, cursorY, 14f * density, ring)
        }
        canvas.save()
        canvas.translate(cursorX, cursorY)
        canvas.drawPath(arrow, fill)
        canvas.drawPath(arrow, stroke)
        canvas.restore()
    }
}
