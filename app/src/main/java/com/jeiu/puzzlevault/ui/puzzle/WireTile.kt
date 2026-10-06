package com.jeiu.puzzlevault.ui.puzzle

/**
 * Cardinal directions for tile connections.
 */
enum class Direction(val dx: Int, val dy: Int) {
    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0);

    fun opposite(): Direction = when (this) {
        NORTH -> SOUTH
        EAST  -> WEST
        SOUTH -> NORTH
        WEST  -> EAST
    }
}

/**
 * Tile types and their canonical connection sets at rotation 0.
 */
enum class TileType(val canonical: Set<Direction>) {
    STRAIGHT(setOf(Direction.NORTH, Direction.SOUTH)),
    CURVE(setOf(Direction.NORTH, Direction.EAST)),
    TEE(setOf(Direction.NORTH, Direction.EAST, Direction.WEST)),
    CROSS(setOf(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)),
    EMPTY(emptySet())
}

/**
 * A single rotatable tile on a wire-circuit grid.
 *
 * @param type  The tile shape.
 * @param rotation  Clockwise rotations applied (0=base, 1=+90°, 2=+180°, 3=+270°).
 */
data class WireTile(
    val type: TileType,
    var rotation: Int = 0
) {
    init {
        require(rotation in 0..3) { "rotation must be 0–3, got $rotation" }
    }

    /** Active connection directions after applying [rotation]. */
    fun connections(): Set<Direction> {
        if (type == TileType.EMPTY) return emptySet()
        return type.canonical.map { rotateDirection(it, rotation) }.toSet()
    }

    /** True if this tile has an open connection in [dir]. */
    fun hasConnection(dir: Direction): Boolean = dir in connections()

    /** Rotate 90° clockwise. */
    fun rotate() {
        rotation = (rotation + 1) % 4
    }

    companion object {
        fun rotateDirection(dir: Direction, steps: Int): Direction {
            val values = Direction.values()
            return values[(values.indexOf(dir) + steps) % 4]
        }
    }
}