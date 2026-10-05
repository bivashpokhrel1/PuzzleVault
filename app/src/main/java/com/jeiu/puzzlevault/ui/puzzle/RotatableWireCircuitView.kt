package com.jeiu.puzzlevault.ui.puzzle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.random.Random

/**
 * 3×3 rotatable wire-circuit grid: tap a cell to rotate its wire segment 90°.
 * The goal is to connect the start node (left edge, middle row) to the end node
 * (right edge, middle row) through a continuous path of connected tiles.
 *
 * Each tile holds one of five wire shapes; rotation changes its active edge set.
 */
class RotatableWireCircuitView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val GRID_SIZE = 3
    }

    // ── tile shapes ──────────────────────────────────────────────────────────

    enum class TileShape {
        /** Connects left ↔ right (horizontal) or top ↔ bottom (vertical) after rotation. */
        STRAIGHT,
        /** Connects two adjacent edges — a pipe corner. */
        CORNER,
        /** Connects three edges — a T. */
        T_JUNCTION,
        /** Connects all four edges — a cross. */
        CROSS
    }

    /**
     * Returns the set of edge flags active for [shape] at [rotation] (0–3).
     * Flags: bit 0 = left, 1 = top, 2 = right, 3 = bottom.
     */
    private fun edgeMask(shape: TileShape, rotation: Int): Int {
        val base = when (shape) {
            TileShape.STRAIGHT  -> 0b0101  // left + right
            TileShape.CORNER    -> 0b0011  // left + top
            TileShape.T_JUNCTION -> 0b0111 // left + top + right
            TileShape.CROSS     -> 0b1111  // all four
        }
        // Rotate: each step shifts bits right; bit 0 wraps to bit 3
        var mask = base
        repeat(rotation and 3) {
            val bit0 = mask and 1
            mask = (mask ushr 1) or (bit0 shl 3)
        }
        return mask
    }

    // ── grid state ───────────────────────────────────────────────────────────

    private data class Tile(
        var shape: TileShape = TileShape.STRAIGHT,
        var rotation: Int = 0  // 0–3
    )

    private val grid: Array<Array<Tile>> = Array(GRID_SIZE) { row ->
        Array(GRID_SIZE) { Tile() }
    }

    // Start/end positions in grid coordinates
    private var startRow = 1     // middle left
    private var startCol = -1    // off-grid left
    private var endRow = 1       // middle right
    private var endCol = GRID_SIZE  // off-grid right

    private var onPuzzleSolved: (() -> Unit)? = null
    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    // ── paint ────────────────────────────────────────────────────────────────

    private val cellBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1E1E2E")
    }

    private val cellStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#7C4DFF")
    }

    private val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#64FFDA")
    }

    private val wireDimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#334444")
    }

    private val solvedWirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#00E676")
    }

    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FF6D00")
    }

    private val nodeEndPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#00E676")
    }

    init {
        generatePuzzle()
    }

    // ── public API ───────────────────────────────────────────────────────────

    fun setOnPuzzleSolved(callback: () -> Unit) {
        onPuzzleSolved = callback
    }

    fun reset() {
        generatePuzzle()
        invalidate()
    }

    // ── puzzle generation ────────────────────────────────────────────────────

    private fun generatePuzzle() {
        val rng = Random(System.currentTimeMillis())

        // 1. Build a random path from (startCol=-1, startRow=1) to (endCol=3, endRow=1)
        val pathCells = mutableListOf<Pair<Int, Int>>()

        // Force entry at col 0, row 1
        var cr = startRow
        var cc = 0
        pathCells.add(Pair(cr, cc))

        // Walk toward exit
        while (cc < GRID_SIZE - 1 || cr != endRow) {
            val moves = mutableListOf<Pair<Int, Int>>()
            if (cc < GRID_SIZE - 1) moves.add(Pair(cr, cc + 1))       // right
            if (cr > 0 && cr != startRow) moves.add(Pair(cr - 1, cc))  // up
            if (cr < GRID_SIZE - 1) moves.add(Pair(cr + 1, cc))       // down
            // remove moves that go backward (already visited)
            moves.removeAll { it in pathCells }
            if (moves.isEmpty()) break
            val next = moves[rng.nextInt(moves.size)]
            cr = next.first
            cc = next.second
            pathCells.add(Pair(cr, cc))
        }
        // Ensure last cell connects to exit
        val lastCell = pathCells.last()
        endRow = lastCell.first
        // endCol is GRID_SIZE (off-grid)

        // 2. Assign shapes and rotations so the path connects
        for (i in pathCells.indices) {
            val (r, c) = pathCells[i]
            val prev = if (i > 0) pathCells[i - 1] else Pair(startRow, startCol)
            val next = if (i < pathCells.size - 1) pathCells[i + 1] else Pair(endRow, endCol)

            val edges = mutableSetOf<Int>() // 0=left, 1=top, 2=right, 3=bottom
            if (prev.second < c) edges.add(0)   // came from left
            if (prev.second > c) edges.add(2)   // came from right
            if (prev.first < r) edges.add(1)    // came from top
            if (prev.first > r) edges.add(3)    // came from bottom
            if (next.second > c) edges.add(2)   // go right
            if (next.second < c) edges.add(0)   // go left
            if (next.first > r) edges.add(3)    // go down
            if (next.first < r) edges.add(1)    // go up

            // Choose shape matching the edge set
            val (shape, rot) = shapeForEdges(edges)
            grid[r][c] = Tile(shape, rot)
        }

        // 3. Fill remaining cells with random shapes
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (Pair(r, c) !in pathCells) {
                    val shape = TileShape.entries[rng.nextInt(TileShape.entries.size)]
                    grid[r][c] = Tile(shape, rng.nextInt(4))
                }
            }
        }

        // 4. Scramble rotations of non-path cells (path cells stay correct)
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (Pair(r, c) !in pathCells) {
                    grid[r][c].rotation = rng.nextInt(4)
                }
                // Also randomly rotate path cells so puzzle isn't trivially solved
                grid[r][c].rotation = (grid[r][c].rotation + rng.nextInt(1, 4)) % 4
            }
        }
    }

    /** Pick a shape and rotation whose edge mask equals [edges]. */
    private fun shapeForEdges(edges: Set<Int>): Pair<TileShape, Int> {
        val targetMask = edges.fold(0) { acc, e -> acc or (1 shl e) }
        for (shape in TileShape.entries) {
            for (rot in 0..3) {
                if (edgeMask(shape, rot) == targetMask) {
                    return Pair(shape, rot)
                }
            }
        }
        // Fallback: straight
        return Pair(TileShape.STRAIGHT, if (targetMask and 0b0101 == 0b0101) 0 else 1)
    }

    // ── solution check ───────────────────────────────────────────────────────

    private fun isSolved(): Boolean {
        // BFS from start, following connected edges
        val visited = Array(GRID_SIZE) { BooleanArray(GRID_SIZE) }
        val queue = ArrayDeque<Pair<Int, Int>>()

        // Start from left-edge entry: check cell (startRow, 0) for left edge
        val entryCell = grid[startRow][0]
        if ((edgeMask(entryCell.shape, entryCell.rotation) and 0b0001) == 0) return false
        queue.add(Pair(startRow, 0))
        visited[startRow][0] = true

        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeFirst()
            val mask = edgeMask(grid[r][c].shape, grid[r][c].rotation)

            // Check neighbors
            if ((mask and 0b0001) != 0 && c > 0 && !visited[r][c - 1]) {
                val nMask = edgeMask(grid[r][c - 1].shape, grid[r][c - 1].rotation)
                if ((nMask and 0b0100) != 0) { queue.add(Pair(r, c - 1)); visited[r][c - 1] = true }
            }
            if ((mask and 0b0010) != 0 && r > 0 && !visited[r - 1][c]) {
                val nMask = edgeMask(grid[r - 1][c].shape, grid[r - 1][c].rotation)
                if ((nMask and 0b1000) != 0) { queue.add(Pair(r - 1, c)); visited[r - 1][c] = true }
            }
            if ((mask and 0b0100) != 0 && c < GRID_SIZE - 1 && !visited[r][c + 1]) {
                val nMask = edgeMask(grid[r][c + 1].shape, grid[r][c + 1].rotation)
                if ((nMask and 0b0001) != 0) { queue.add(Pair(r, c + 1)); visited[r][c + 1] = true }
            }
            if ((mask and 0b1000) != 0 && r < GRID_SIZE - 1 && !visited[r + 1][c]) {
                val nMask = edgeMask(grid[r + 1][c].shape, grid[r + 1][c].rotation)
                if ((nMask and 0b0010) != 0) { queue.add(Pair(r + 1, c)); visited[r + 1][c] = true }
            }
        }

        // Check if the last cell in the rightmost column has right-edge active AND is visited
        val exitMask = edgeMask(grid[endRow][GRID_SIZE - 1].shape, grid[endRow][GRID_SIZE - 1].rotation)
        return visited[endRow][GRID_SIZE - 1] && (exitMask and 0b0100) != 0
    }

    // ── touch handling ───────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return false

        val col = ((event.x - offsetX) / cellSize).toInt()
        val row = ((event.y - offsetY) / cellSize).toInt()
        if (col !in 0 until GRID_SIZE || row !in 0 until GRID_SIZE) return false

        // Rotate tile clockwise
        grid[row][col].rotation = (grid[row][col].rotation + 1) % 4
        invalidate()

        if (isSolved()) {
            onPuzzleSolved?.invoke()
        }
        return true
    }

    // ── layout ───────────────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val totalSize = min(w, h).toFloat() * 0.88f
        cellSize = totalSize / GRID_SIZE
        offsetX = (w - cellSize * GRID_SIZE) / 2f
        offsetY = (h - cellSize * GRID_SIZE) / 2f
    }

    // ── drawing ──────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cellSize <= 0f) return

        val solved = isSolved()

        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                val tile = grid[row][col]
                val left = offsetX + col * cellSize
                val top = offsetY + row * cellSize
                val right = left + cellSize
                val bottom = top + cellSize
                val cx = left + cellSize / 2
                val cy = top + cellSize / 2
                val pad = cellSize * 0.04f

                // Cell background
                canvas.drawRoundRect(left + pad, top + pad, right - pad, bottom - pad, 8f, 8f, cellBgPaint)
                canvas.drawRoundRect(left + pad, top + pad, right - pad, bottom - pad, 8f, 8f, cellStrokePaint)

                val mask = edgeMask(tile.shape, tile.rotation)
                val paint = if (solved) solvedWirePaint else wirePaint

                // Draw wire paths inside cell
                val half = cellSize / 2
                if ((mask and 0b0001) != 0) canvas.drawLine(cx - half, cy, cx, cy, paint)  // left
                if ((mask and 0b0010) != 0) canvas.drawLine(cx, cy - half, cx, cy, paint)  // top
                if ((mask and 0b0100) != 0) canvas.drawLine(cx, cy, cx + half, cy, paint)  // right
                if ((mask and 0b1000) != 0) canvas.drawLine(cx, cy, cx, cy + half, paint)  // bottom

                // Center dot
                canvas.drawCircle(cx, cy, cellSize * 0.08f, wirePaint)
            }
        }

        // Start node (left edge)
        val startY = offsetY + startRow * cellSize + cellSize / 2
        canvas.drawCircle(offsetX - cellSize * 0.15f, startY, cellSize * 0.12f, nodePaint)

        // End node (right edge)
        val endY = offsetY + endRow * cellSize + cellSize / 2
        canvas.drawCircle(offsetX + GRID_SIZE * cellSize + cellSize * 0.15f, endY, cellSize * 0.12f, nodeEndPaint)

        // Entry / exit lines
        val midStartX = offsetX + cellSize / 2
        val midEndX = offsetX + (GRID_SIZE - 1) * cellSize + cellSize / 2
        canvas.drawLine(offsetX - cellSize * 0.3f, startY, midStartX, startY, nodePaint)
        canvas.drawLine(midEndX, endY, offsetX + GRID_SIZE * cellSize + cellSize * 0.3f, endY, nodeEndPaint)
    }
}