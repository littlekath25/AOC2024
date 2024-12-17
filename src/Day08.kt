private val TESTINPUT = readInput("Day08_example")
private val INPUT = readInput("Day08")
private val GRID = convertToGrid(INPUT)
private val XBOUND = GRID[0].size
private val YBOUND = GRID.size
private val FREQUENCIES = GRID.flatten().filterNot { it.first == '.' }.groupBy { it.first }.map { it.key to it.value.map { it.second } }

private fun isWithinBounds(c: Coordinate): Boolean {
    return c.x in 0 until XBOUND && c.y in 0 until YBOUND
}

private fun generateAntinodes(c1: Coordinate, c2: Coordinate): List<Coordinate> {
    val diff1X = c2.x - c1.x
    val diff1Y = c2.y - c1.y

    val diff2X = c1.x - c2.x
    val diff2Y = c1.y - c2.y

    val antinodes = mutableListOf<Coordinate>()

    for (i in 1 until XBOUND) {
        val c1X = c1.x + diff1X * i
        val c1Y = c1.y + diff1Y * i

        val c2X = c2.x + diff2X * i
        val c2Y = c2.y + diff2Y * i

        antinodes.addAll(listOf(Coordinate(c1Y, c1X), Coordinate(c2Y, c2X)).filter { isWithinBounds(it) })
    }

    return antinodes
}

private fun generateTwoAntinodes(c1: Coordinate, c2: Coordinate): List<Coordinate> {
    val diff1X = c2.x - c1.x
    val diff1Y = c2.y - c1.y

    val diff2X = c1.x - c2.x
    val diff2Y = c1.y - c2.y

    return listOf(
        Coordinate(c1.y - diff1Y, c1.x - diff1X),
        Coordinate(c2.y - diff2Y, c2.x - diff2X)
    )
}

fun main() {
    fun part1(): Int {
        val potentialAntinodes = FREQUENCIES.flatMap { (_, values) ->
            val combinations = values.flatMapIndexed { index, element ->
                values.drop(index + 1).map { Pair(element, it) }
            }

            combinations.flatMap { generateTwoAntinodes(it.first, it.second) }
        }.distinct()

        return potentialAntinodes.count { isWithinBounds(it) }
    }

    fun part2(): Int {
        val totalAntinodes = FREQUENCIES.flatMap { (_, values) ->
            val combinations = values.flatMapIndexed { index, element ->
                values.drop(index + 1).map { Pair(element, it) }
            }

            combinations.flatMap { generateAntinodes(it.first, it.second) }
        }.distinct()

        return totalAntinodes.size
    }

    println("Day 00 - Part one: ${part1()}")
    println("Day 00 - Part two: ${part2()}")
}