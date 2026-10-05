package com.jeiu.puzzlevault.ui.puzzle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Grid Slide puzzle: slide numbered tiles into the correct order.
 * Tap a tile adjacent to the empty space to slide it.
 */
class GridSlideView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var gridSize = 4
    private var tiles = mutableListOf<Int>() // 0 = empty
    private var solution = listOf<Int>()
    private var emptyIndex = 0
    private var onPuzzleSolved: (() -> Unit)? = null
    private var moveCount = 0

    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val tileStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#7C4DFF")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 52f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D0D1A")
    }

    private val moveCountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B0BEC5")
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    init {
        resetTiles()
    }

    fun setGridSize(size: Int) {
        gridSize = size.coerceIn(2, 6)
        resetTiles()
    }

    fun setOnPuzzleSolved(callback: () -> Unit) {
        onPuzzleSolved = callback
    }

    fun reset() {
        resetTiles()
        invalidate()
    }

    fun getMoveCount(): Int = moveCount

    private fun resetTiles() {
        val total = gridSize * gridSize
        tiles = (1 until total).toMutableList()
        tiles.add(0) // empty at last position
        emptyIndex = total - 1
        solution = tiles.toList()
        moveCount = 0
        shuffle()
    }

    private fun shuffle() {
        // Perform random valid moves to ensure solvable state
        val random = kotlin.random.Random(System.currentTimeMillis())
        repeat(gridSize * gridSize * 20) {
            val neighbors = getNeighborIndices(emptyIndex)
            if (neighbors.isNotEmpty()) {
                val swapIdx = neighbors[random.nextInt(neighbors.size)]
                swap(emptyIndex, swapIdx)
            }
        }
    }

    private fun getNeighborIndices(idx: Int): List<Int> {
        val row = idx / gridSize
        val col = idx % gridSize
        val neighbors = mutableListOf<Int>()
        if (row > 0) neighbors.add(idx - gridSize)        // up
        if (row < gridSize - 1) neighbors.add(idx + gridSize) // down
        if (col > 0) neighbors.add(idx - 1)               // left
        if (col < gridSize - 1) neighbors.add(idx + 1)    // right
        return neighbors
    }

    private fun swap(i: Int, j: Int) {
        val temp = tiles[i]
        tiles[i] = tiles[j]
        tiles[j] = temp
        if (tiles[i] == 0) emptyIndex = i
        if (tiles[j] == 0) emptyIndex = j
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return false

        val cellW = width.toFloat() / gridSize
        val cellH = height.toFloat() / gridSize
        val col = (event.x / cellW).toInt()
        val row = (event.y / cellH).toInt()
        val idx = row * gridSize + col

        if (idx !in tiles.indices) return false

        val neighbors = getNeighborIndices(emptyIndex)
        if (idx in neighbors) {
            swap(emptyIndex, idx)
            moveCount++
            invalidate()

            if (tiles == solution) {
                onPuzzleSolved?.invoke()
            }
            return true
        }
        return false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cellW = width.toFloat() / gridSize
        val cellH = height.toFloat() / gridSize
        val padding = 4f

        for (i in tiles.indices) {
            val row = i / gridSize
            val col = i % gridSize
            val left = col * cellW + padding
            val top = row * cellH + padding
            val right = (col + 1) * cellW - padding
            val bottom = (row + 1) * cellH - padding
            val rect = RectF(left, top, right, bottom)

            if (tiles[i] == 0) {
                canvas.drawRoundRect(rect, 12f, 12f, emptyPaint)
            } else {
                val gradientColor = when {
                    tiles[i] <= gridSize -> Color.parseColor("#7C4DFF")
                    tiles[i] <= gridSize * 2 -> Color.parseColor("#448AFF")
                    tiles[i] <= gridSize * 3 -> Color.parseColor("#00BFA5")
                    else -> Color.parseColor("#FF6D00")
                }
                tilePaint.color = gradientColor
                canvas.drawRoundRect(rect, 12f, 12f, tilePaint)
                canvas.drawRoundRect(rect, 12f, 12f, tileStrokePaint)
                canvas.drawText(
                    tiles[i].toString(),
                    rect.centerX(),
                    rect.centerY() + textPaint.textSize / 3,
                    textPaint
                )
            }
        }

        // Move counter
        canvas.drawText(
            "Moves: $moveCount",
            width / 2f,
            height - 16f,
            moveCountPaint
        )
    }
}
