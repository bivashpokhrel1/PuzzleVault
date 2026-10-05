package com.jeiu.puzzlevault.ui.puzzle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Wire Circuit puzzle: drag between connection points to wire them up.
 * Completed connections persist; incorrect ones briefly flash red.
 */
class WireCircuitView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val NODE_RADIUS = 36f
        private const val GRID_COLS = 4
        private const val GRID_ROWS = 3
    }

    data class ConnectionPoint(
        val id: String,
        val col: Int,
        val row: Int,
        var isConnected: Boolean = false
    )

    data class Wire(
        val from: ConnectionPoint,
        val to: ConnectionPoint
    )

    // Solution: pairs of node IDs that must be connected
    var solutionPairs: List<Pair<String, String>> = emptyList()

    private val nodes = mutableListOf<ConnectionPoint>()
    private val wires = mutableListOf<Wire>()
    private var selectedNode: ConnectionPoint? = null
    private var dragX = 0f
    private var dragY = 0f
    private var isDragging = false
    private var errorFlashWire: Wire? = null
    private var errorFlashTime = 0L
    private var onPuzzleSolved: (() -> Unit)? = null

    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val nodeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#FFFFFF")
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#64FFDA")
    }

    private val dragWirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#80CBC4")
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    private val errorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#FF5252")
    }

    init {
        // Default grid: 4x3 = 12 connection points
        for (row in 0 until GRID_ROWS) {
            for (col in 0 until GRID_COLS) {
                nodes.add(
                    ConnectionPoint(
                        id = "${('A' + row)}${col + 1}",
                        col = col,
                        row = row
                    )
                )
            }
        }
    }

    fun setOnPuzzleSolved(callback: () -> Unit) {
        onPuzzleSolved = callback
    }

    fun setSolution(pairs: List<Pair<String, String>>) {
        solutionPairs = pairs
        wires.clear()
        nodes.forEach { it.isConnected = false }
        invalidate()
    }

    fun reset() {
        wires.clear()
        nodes.forEach { it.isConnected = false }
        selectedNode = null
        isDragging = false
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val px = event.x
        val py = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedNode = findNodeAt(px, py)
                if (selectedNode != null) {
                    val (cx, cy) = nodeCenter(selectedNode!!)
                    dragX = cx
                    dragY = cy
                    isDragging = true
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    dragX = px
                    dragY = py
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isDragging && selectedNode != null) {
                    val target = findNodeAt(px, py)
                    if (target != null && target != selectedNode) {
                        attemptConnection(selectedNode!!, target)
                    }
                    isDragging = false
                    selectedNode = null
                    invalidate()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun attemptConnection(from: ConnectionPoint, to: ConnectionPoint) {
        if (from.isConnected || to.isConnected) return

        // Check if this pair is in the solution
        val isValid = solutionPairs.any { pair ->
            (pair.first == from.id && pair.second == to.id) ||
            (pair.first == to.id && pair.second == from.id)
        }

        if (isValid) {
            from.isConnected = true
            to.isConnected = true
            wires.add(Wire(from, to))
            checkCompletion()
        } else {
            errorFlashWire = Wire(from, to)
            errorFlashTime = System.currentTimeMillis()
            postDelayed({ errorFlashWire = null; invalidate() }, 500)
        }
    }

    private fun checkCompletion() {
        val allCorrect = solutionPairs.all { pair ->
            wires.any { wire ->
                (wire.from.id == pair.first && wire.to.id == pair.second) ||
                (wire.from.id == pair.second && wire.to.id == pair.first)
            }
        }
        if (allCorrect) {
            onPuzzleSolved?.invoke()
        }
    }

    private fun findNodeAt(x: Float, y: Float): ConnectionPoint? {
        return nodes.firstOrNull { node ->
            val (cx, cy) = nodeCenter(node)
            abs(x - cx) < NODE_RADIUS * 1.5f && abs(y - cy) < NODE_RADIUS * 1.5f
        }
    }

    private fun nodeCenter(node: ConnectionPoint): Pair<Float, Float> {
        val cellW = width.toFloat() / GRID_COLS
        val cellH = height.toFloat() / GRID_ROWS
        val cx = cellW * node.col + cellW / 2
        val cy = cellH * node.row + cellH / 2
        return Pair(cx, cy)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw completed wires
        for (wire in wires) {
            val (x1, y1) = nodeCenter(wire.from)
            val (x2, y2) = nodeCenter(wire.to)
            canvas.drawLine(x1, y1, x2, y2, wirePaint)
        }

        // Draw error flash wire
        val now = System.currentTimeMillis()
        errorFlashWire?.let { wire ->
            if (now - errorFlashTime < 400) {
                val (x1, y1) = nodeCenter(wire.from)
                val (x2, y2) = nodeCenter(wire.to)
                canvas.drawLine(x1, y1, x2, y2, errorPaint)
            }
        }

        // Draw drag preview
        if (isDragging && selectedNode != null) {
            val (sx, sy) = nodeCenter(selectedNode!!)
            canvas.drawLine(sx, sy, dragX, dragY, dragWirePaint)
        }

        // Draw nodes
        for (node in nodes) {
            val (cx, cy) = nodeCenter(node)

            val gradient = if (node.isConnected) {
                RadialGradient(cx, cy, NODE_RADIUS,
                    intArrayOf(Color.parseColor("#00E676"), Color.parseColor("#00C853")),
                    null, Shader.TileMode.CLAMP)
            } else {
                RadialGradient(cx, cy, NODE_RADIUS,
                    intArrayOf(Color.parseColor("#7C4DFF"), Color.parseColor("#651FFF")),
                    null, Shader.TileMode.CLAMP)
            }

            nodePaint.shader = gradient
            canvas.drawCircle(cx, cy, NODE_RADIUS, nodePaint)
            canvas.drawCircle(cx, cy, NODE_RADIUS, nodeStrokePaint)

            val labelY = cy + labelPaint.textSize / 3
            canvas.drawText(node.id, cx, labelY, labelPaint)
        }
    }
}
