private val INPUT = readInput("Day06")
private val GRID = convertToGrid(INPUT)
private val OBSTACLES = GRID.map { line -> line.filter { it.first == '#' }.map { it.second } }.flatten()
private val NONE_OBSTACLES = GRID.map { line -> line.filter { it.first == '.' }.map { it.second } }.flatten()
private val START_POSITION = GRID.firstNotNullOf { line -> line.find { it.first == '^' } }.second
private val VISITED = mutableSetOf<Coordinate>()

private val VISITED_OBSTACLES = mutableListOf<Coordinate>()

private fun searchObstacle(start: Coordinate, direction: DIRECTION, obstacles: List<Coordinate>? = null): Coordinate? {
    val correctObstacles = obstacles ?: OBSTACLES

    return when (direction) {
        DIRECTION.UP -> correctObstacles.filter { it.x == start.x && it.y < start.y }.maxByOrNull { it.y }
        DIRECTION.DOWN -> correctObstacles.filter { it.x == start.x && it.y > start.y }.minByOrNull { it.y }
        DIRECTION.LEFT -> correctObstacles.filter { it.y == start.y && it.x < start.x }.maxByOrNull { it.x }
        DIRECTION.RIGHT -> correctObstacles.filter { it.y == start.y && it.x > start.x }.minByOrNull { it.x }
    }
}

private fun turn(direction: DIRECTION): DIRECTION {
    return when (direction) {
        DIRECTION.UP -> DIRECTION.RIGHT
        DIRECTION.DOWN -> DIRECTION.LEFT
        DIRECTION.LEFT -> DIRECTION.UP
        DIRECTION.RIGHT -> DIRECTION.DOWN
    }
}

private fun addVisitedCoordinates(start: Coordinate, end: Coordinate) {
    val xRange = if (start.x <= end.x) start.x..end.x else start.x downTo end.x
    val yRange = if (start.y <= end.y) start.y..end.y else start.y downTo end.y

    for (i in xRange) {
        for (j in yRange) {
            VISITED.add(Coordinate(j, i))
        }
    }
}

private fun moveUntilObstacle(start: Coordinate, obstacle: Coordinate): Coordinate {
    return when {
        start.x == obstacle.x -> if (start.y > obstacle.y) {
            val end = Coordinate(obstacle.y + 1, obstacle.x)
            addVisitedCoordinates(start, end)
            end
        } else {
            val end = Coordinate(obstacle.y - 1, obstacle.x)
            addVisitedCoordinates(start, end)
            end
        }
        start.y == obstacle.y -> if (start.x > obstacle.x) {
            val end = Coordinate(obstacle.y, obstacle.x + 1)
            addVisitedCoordinates(start, end)
            end
        } else {
            val end = Coordinate(obstacle.y, obstacle.x - 1)
            addVisitedCoordinates(start, end)
            end
        }
        else -> throw IllegalArgumentException("Obstacle is not in a straight line from the start")
    }
}

private fun endOfMapCoordinate(direction: DIRECTION): Coordinate {
    return when (direction) {
        DIRECTION.UP -> Coordinate(0, 0)
        DIRECTION.DOWN -> Coordinate(GRID.size - 1, 0)
        DIRECTION.LEFT -> Coordinate(0, 0)
        DIRECTION.RIGHT -> Coordinate(0, GRID[0].size - 1)
    }
}

private fun turnAndContinue(start: Coordinate, direction: DIRECTION): Int {
    val obstacle = searchObstacle(start, direction)

    return if (obstacle == null) {
        val end = endOfMapCoordinate(direction)
        addVisitedCoordinates(start, end)
        VISITED.size
    } else {
        val newStartingPoint = moveUntilObstacle(start, obstacle)
        turnAndContinue(newStartingPoint , turn(direction))
    }
}

private fun checkIfItLoops(): Boolean {
    val lastFour = VISITED_OBSTACLES.takeLast(4)
    return countSublistOccurrences(lastFour, VISITED_OBSTACLES) > 2
}

private fun causesALoop(obstacles: List<Coordinate>, start: Coordinate, direction: DIRECTION): Boolean {
    val obstacle = searchObstacle(start, direction, obstacles)

    if (checkIfItLoops()) return true

    return if (obstacle == null) {
        false
    } else {
        VISITED_OBSTACLES.add(obstacle)
        val newStartingPoint = moveUntilObstacle(start, obstacle)
        causesALoop(obstacles, newStartingPoint , turn(direction))
    }
}

fun main() {
    fun part1(): Int {
        return turnAndContinue(START_POSITION, DIRECTION.UP)
    }

    fun part2(): Int {
        val total = NONE_OBSTACLES.fold(0) { acc, coordinate ->
            val obstacles = OBSTACLES + listOf(coordinate)

            VISITED_OBSTACLES.clear()
            if (causesALoop(obstacles, START_POSITION, DIRECTION.UP)) {
                acc + 1
            } else {
                acc
            }
        }

        return total
    }

    println("Day 06 - Part one: ${part1()}")
    println("Day 06 - Part two: ${part2()}")
}