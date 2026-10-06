package com.jeiu.puzzlevault.ui.puzzle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.LinkedList
import kotlin.math.min
import kotlin.random.Random

/**
 * 3×3 rotatable wire-circuit puzzle.
 *
 * Tap a cell to rotate its wire segment 90° clockwise. The goal is to form a
 * continuous path from the start node (left edge, middle row) to the end node
 * (right edge, middle row).
 *
 * Power-flow is evaluated via BFS after every rotation. When the target is
 * reached [onPuzzleSolved] fires and the circuit lights green.
 *
 * Uses [WireTile] / [TileType] / [Direction] for tile logic.
 */
class RotatableWireView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val GRID_SIZE = 3
    }

    // ── Grid state ──────────────────────────────────────────────────────

    /** 3×3 flat list, row-major. */
    private val tiles = MutableList(GRID_SIZE * GRID_SIZE) { WireTile(TileType.EMPTY) }

    /** Indices of tiles on the winning path. */
    private val pathCells = mutableSetOf<Int>()

    /** Powered tile indices (live BFS result). */
    private val powered = mutableSetOf<Int>()

    /** Start row on the left edge (off-grid). */
    private var startRow = 1

    /** End row on the right edge (off-grid). */
    private var endRow = 1

    private var isSolved = false
    private var onPuzzleSolved: (() -> Unit)? = null

    // ── Layout ──────────────────────────────────────────────────────────

    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    // ── Paints ──────────────────────────────────────────────────────────

    private val cellBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1E1E2E")
    }

    private val pathBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0D3B2E")
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
        color = Color.parseColor("#333344")
    }

    private val poweredWirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#00E676")
        setShadowLayer(14f, 0f, 0f, Color.parseColor("#6600E676"))
    }

    private val nodeStartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FF9100")
    }

    private val nodeEndPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#00E676")
    }

    init {
        generatePuzzle()
    }

    // ── Public API ──────────────────────────────────────────────────────

    fun setOnPuzzleSolved(callback: () -> Unit) {
        onPuzzleSolved = callback
    }

    fun reset() {
        isSolved = false
        powered.clear()
        pathCells.clear()
        generatePuzzle()
        invalidate()
    }

    // ── Puzzle generation ───────────────────────────────────────────────

    /**
     * 1. Walk a random non-backtracking path from (startRow, 0) to the right edge.
     * 2. Assign [WireTile] types + rotations so the path connects.
     * 3. Fill remaining cells with random types + rotations.
     * 4. Scramble all rotations (path cells too) so the puzzle isn't trivially solved.
     */
    private fun generatePuzzle() {
        val rng = Random(System.currentTimeMillis())

        // 1. Build random path
        val path = mutableListOf<Int>()  // flat indices
        var cr = startRow
        var cc = 0
        path.add(cr * GRID_SIZE + cc)

        while (cc < GRID_SIZE - 1 || cr != endRow) {
            val moves = mutableListOf<Pair<Int, Int>>()
            if (cc < GRID_SIZE - 1) moves.add(Pair(cr, cc + 1))       // right
            if (cr > 0) moves.add(Pair(cr - 1, cc))                   // up
            if (cr < GRID_SIZE - 1) moves.add(Pair(cr + 1, cc))       // down
            moves.removeAll { (r, c) -> (r * GRID_SIZE + c) in path }
            if (moves.isEmpty()) break
            val next = moves[rng.nextInt(moves.size)]
            cr = next.first
            cc = next.second
            path.add(cr * GRID_SIZE + cc)
        }
        endRow = path.last() / GRID_SIZE
        pathCells.clear()
        pathCells.addAll(path)

        // 2. Assign types/rotations to path cells
        for (i in path.indices) {
            val idx = path[i]
            val r = idx / GRID_SIZE
            val c = idx % GRID_SIZE

            val prevC = if (i > 0) path[i - 1] % GRID_SIZE else -1
            val prevR = if (i > 0) path[i - 1] / GRID_SIZE else r
            val nextC = if (i < path.size - 1) path[i + 1] % GRID_SIZE else GRID_SIZE
            val nextR = if (i < path.size - 1) path[i + 1] / GRID_SIZE else r

            val needed = mutableSetOf<Direction>()
            if (prevC < c) needed.add(Direction.WEST)
            if (prevC > c) needed.add(Direction.EAST)
            if (prevR < r) needed.add(Direction.NORTH)
            if (prevR > r) needed.add(Direction.SOUTH)
            if (nextC > c) needed.add(Direction.EAST)
            if (nextC < c) needed.add(Direction.WEST)
            if (nextR > r) needed.add(Direction.SOUTH)
            if (nextR < r) needed.add(Direction.NORTH)

            val (type, rot) = typeForDirections(needed)
            tiles[idx] = WireTile(type, rot)
        }

        // 3. Fill non-path cells
        val allTypes = listOf(TileType.STRAIGHT, TileType.CURVE, TileType.TEE, TileType.CROSS, TileType.EMPTY)
        for (idx in 0 until GRID_SIZE * GRID_SIZE) {
            if (idx !in pathCells) {
                tiles[idx] = WireTile(allTypes[rng.nextInt(allTypes.size)], rng.nextInt(4))
            }
        }

        // 4. Scramble all rotations (puzzle remains solvable)
        for (idx in 0 until GRID_SIZE * GRID_SIZE) {
            repeat(rng.nextInt(0, 4)) { tiles[idx].rotate() }
        }

        evaluatePower()
    }

    /** Find a [TileType] + rotation whose connections exactly match [needed]. */
    private fun typeForDirections(needed: Set<Direction>): Pair<TileType, Int> {
        if (needed.isEmpty()) return Pair(TileType.EMPTY, 0)
        for (type in TileType.entries) {
            if (type == TileType.EMPTY) continue
            for (rot in 0..3) {
                if (WireTile(type, rot).connections() == needed) return Pair(type, rot)
            }
        }
        return Pair(TileType.CROSS, 0)
    }

    // ── BFS power-flow ──────────────────────────────────────────────────

    /**
     * BFS outward from the start entry. A neighbor becomes powered iff the
     * current tile connects toward it AND the neighbor connects back.
     */
    private fun evaluatePower() {
        powered.clear()

        val startIdx = startRow * GRID_SIZE  // col 0, row = startRow
        if (!tiles[startIdx].hasConnection(Direction.WEST)) {
            isSolved = false
            return
        }

        val queue = LinkedList<Int>()
        powered.add(startIdx)
        queue.add(startIdx)

        while (queue.isNotEmpty()) {
            val idx = queue.poll()
            val row = idx / GRID_SIZE
            val col = idx % GRID_SIZE

            for (dir in Direction.values()) {
                val nRow = row + dir.dy
                val nCol = col + dir.dx
                if (nRow !in 0 until GRID_SIZE || nCol !in 0 until GRID_SIZE) continue

                val nIdx = nRow * GRID_SIZE + nCol
                if (nIdx in powered) continue
                if (!tiles[idx].hasConnection(dir)) continue
                if (!tiles[nIdx].hasConnection(dir.opposite())) continue

                powered.add(nIdx)
                queue.add(nIdx)
            }
        }

        val exitIdx = endRow * GRID_SIZE + (GRID_SIZE - 1)
        val wasSolved = isSolved
        isSolved = exitIdx in powered && tiles[exitIdx].hasConnection(Direction.EAST)
        if (isSolved && !wasSolved) {
            post { onPuzzleSolved?.invoke() }
        }
    }

    // ── Touch ───────────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return false
        if (isSolved) return true

        val col = ((event.x - offsetX) / cellSize).toInt()
        val row = ((event.y - offsetY) / cellSize).toInt()
        if (col !in 0 until GRID_SIZE || row !in 0 until GRID_SIZE) return false

        tiles[row * GRID_SIZE + col].rotate()
        evaluatePower()
        invalidate()
        return true
    }

    // ── Layout ──────────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val totalSize = min(w, h).toFloat() * 0.88f
        cellSize = totalSize / GRID_SIZE
        offsetX = (w - cellSize * GRID_SIZE) / 2f
        offsetY = (h - cellSize * GRID_SIZE) / 2f
    }

    // ── Drawing ─────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cellSize <= 0f) return

        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                val idx = row * GRID_SIZE + col
                val tile = tiles[idx]
                val left = offsetX + col * cellSize
                val top = offsetY + row * cellSize
                val right = left + cellSize
                val bottom = top + cellSize
                val cx = left + cellSize / 2f
                val cy = top + cellSize / 2f
                val pad = cellSize * 0.04f
                val half = cellSize / 2f

                val rect = RectF(left + pad, top + pad, right - pad, bottom - pad)

                // Background
                val bg = if (isSolved || idx in powered) pathBgPaint else cellBgPaint
                canvas.drawRoundRect(rect, 10f, 10f, bg)
                canvas.drawRoundRect(rect, 10f, 10f, cellStrokePaint)

                // Wire lines
                val conns = tile.connections()
                val lineP = when {
                    isSolved -> poweredWirePaint
                    idx in powered -> poweredWirePaint
                    tile.type != TileType.EMPTY -> wirePaint
                    else -> wireDimPaint
                }

                for (dir in conns) {
                    when (dir) {
                        Direction.NORTH -> canvas.drawLine(cx, cy, cx, cy - half, lineP)
                        Direction.SOUTH -> canvas.drawLine(cx, cy, cx, cy + half, lineP)
                        Direction.WEST  -> canvas.drawLine(cx, cy, cx - half, cy, lineP)
                        Direction.EAST  -> canvas.drawLine(cx, cy, cx + half, cy, lineP)
                    }
                }

                // Center dot
                val dotColor = when {
                    isSolved || idx in powered -> poweredWirePaint.color
                    tile.type != TileType.EMPTY -> wirePaint.color
                    else -> wireDimPaint.color
                }
                canvas.drawCircle(cx, cy, cellSize * 0.07f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL; color = dotColor
                })
            }
        }

        // Start node (left edge, middle row)
        val startCY = offsetY + startRow * cellSize + cellSize / 2f
        canvas.drawCircle(offsetX - cellSize * 0.18f, startCY, cellSize * 0.13f, nodeStartPaint)
        canvas.drawLine(
            offsetX - cellSize * 0.35f, startCY, offsetX, startCY,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 5f; color = nodeStartPaint.color
            }
        )

        // End node (right edge)
        val endCY = offsetY + endRow * cellSize + cellSize / 2f
        val endX = offsetX + GRID_SIZE * cellSize
        canvas.drawCircle(endX + cellSize * 0.18f, endCY, cellSize * 0.13f, nodeEndPaint)
        canvas.drawCircle(endX + cellSize * 0.18f, endCY, cellSize * 0.13f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL; color = nodeEndPaint.color; alpha = 45
            }
        )
        canvas.drawLine(
            endX, endCY, endX + cellSize * 0.35f, endCY,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 5f; color = nodeEndPaint.color
            }
        )
    }
}