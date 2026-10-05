package com.jeiu.puzzlevault.ui.puzzle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Keypad puzzle: tap digits to reproduce a target sequence.
 * Correct taps glow green; wrong sequence resets with red flash.
 */
class KeypadPuzzleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var targetSequence: String = "4829"
    private var currentInput: String = ""
    private var isError = false
    private var errorResetTime = 0L
    private var onPuzzleSolved: (() -> Unit)? = null

    // Layout
    private val buttonRects = mutableMapOf<Char, Rect>()
    private val rows = 4
    private val cols = 3

    // Key map (phone layout: 123/456/789/*0#)
    private val keyLayout = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('*', '0', '#')
    )

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val buttonStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#7C4DFF")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        textAlign = Paint.Align.CENTER
    }

    private val displayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64FFDA")
        textSize = 56f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7C4DFF")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    fun setTargetSequence(sequence: String) {
        targetSequence = sequence
        currentInput = ""
        isError = false
        invalidate()
    }

    fun setOnPuzzleSolved(callback: () -> Unit) {
        onPuzzleSolved = callback
    }

    fun reset() {
        currentInput = ""
        isError = false
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutButtons()
    }

    private fun layoutButtons() {
        buttonRects.clear()
        val padTop = height * 0.25f // top 25% for display
        val padW = width.toFloat() / cols
        val padH = (height - padTop) / rows

        for (row in keyLayout.indices) {
            for (col in keyLayout[row].indices) {
                val key = keyLayout[row][col]
                val left = col * padW + padW * 0.15f
                val top = padTop + row * padH + padH * 0.1f
                val right = (col + 1) * padW - padW * 0.15f
                val bottom = padTop + (row + 1) * padH - padH * 0.1f
                buttonRects[key] = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return false
        if (isError && System.currentTimeMillis() - errorResetTime < 600) return true

        val key = findKeyAt(event.x, event.y) ?: return false

        if (key == '*' || key == '#') return true // ignore function keys for puzzle

        currentInput += key

        val correctSoFar = targetSequence.startsWith(currentInput)

        if (!correctSoFar) {
            isError = true
            errorResetTime = System.currentTimeMillis()
            postDelayed({
                currentInput = ""
                isError = false
                invalidate()
            }, 600)
        } else if (currentInput == targetSequence) {
            onPuzzleSolved?.invoke()
        }

        invalidate()
        return true
    }

    private fun findKeyAt(x: Float, y: Float): Char? {
        return buttonRects.entries.firstOrNull { (_, rect) ->
            rect.contains(x.toInt(), y.toInt())
        }?.key
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = System.currentTimeMillis()
        val showError = isError && (now - errorResetTime) < 500

        // Draw progress bar
        if (targetSequence.isNotEmpty()) {
            val progress = currentInput.length.toFloat() / targetSequence.length
            val progressWidth = width * 0.6f
            val startX = (width - progressWidth) / 2
            val progressY = height * 0.15f
            canvas.drawLine(startX, progressY, startX + progressWidth, progressY, Paint().apply {
                color = Color.parseColor("#333333")
                strokeWidth = 4f
            })
            canvas.drawLine(startX, progressY, startX + progressWidth * progress, progressY, progressPaint)
        }

        // Draw display (target sequence with filled-in chars)
        val displayY = height * 0.12f
        val displayText = buildString {
            for (i in targetSequence.indices) {
                append(if (i < currentInput.length) targetSequence[i] else '_')
                if (i < targetSequence.length - 1) append(' ')
            }
        }
        val displayColor = if (showError) Color.parseColor("#FF5252") else Color.parseColor("#64FFDA")
        displayPaint.color = displayColor
        canvas.drawText(displayText, width / 2f, displayY, displayPaint)

        // Draw progress dots under display
        val dotRadius = 12f
        val dotSpacing = 36f
        val dotsStartX = (width - (targetSequence.length - 1) * dotSpacing) / 2f
        val dotsY = height * 0.2f
        for (i in targetSequence.indices) {
            val dotColor = when {
                showError -> Color.parseColor("#FF5252")
                i < currentInput.length -> Color.parseColor("#00E676")
                else -> Color.parseColor("#444444")
            }
            Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = dotColor }.let { p ->
                canvas.drawCircle(dotsStartX + i * dotSpacing, dotsY, dotRadius, p)
            }
        }

        // Draw keypad buttons
        for ((key, rect) in buttonRects) {
            val bgColor = when {
                showError -> Color.parseColor("#3D0000")
                currentInput.isNotEmpty() && key in targetSequence &&
                key <= currentInput.last() -> Color.parseColor("#1B5E20")
                else -> Color.parseColor("#1E1E2E")
            }
            buttonPaint.color = bgColor
            canvas.drawRoundRect(
                rect.left.toFloat(), rect.top.toFloat(),
                rect.right.toFloat(), rect.bottom.toFloat(),
                12f, 12f, buttonPaint
            )
            canvas.drawRoundRect(
                rect.left.toFloat(), rect.top.toFloat(),
                rect.right.toFloat(), rect.bottom.toFloat(),
                12f, 12f, buttonStrokePaint
            )
            canvas.drawText(
                key.toString(),
                rect.exactCenterX(),
                rect.exactCenterY() + textPaint.textSize / 3,
                textPaint
            )
        }
    }
}
