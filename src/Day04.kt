data class Coordinate(val y: Int, val x: Int )

typealias LetterGrid = List<List<Pair<Char, Coordinate>>>

private const val xBound = 139
private const val yBound = 139

private val INPUT = readInput("Day04")
private val TEST_INPUT = readInput("Day04_example")
private val LETTER_GRID = letterGrid(INPUT)

private fun getAllCoordsForOneLetter(letter: Char): List<Pair<Char, Coordinate>> {
    return LETTER_GRID.flatMap { line ->
        line.filter { it.first == letter }
    }
}

private fun isInBounds(coordinates: List<Coordinate>): Boolean {
    return coordinates.all { coordinate ->
        coordinate.x in 0..xBound && coordinate.y in 0..yBound
    }
}

private fun letterGrid(input: List<String>): LetterGrid{
    return input.map { line ->
        line.mapIndexed { index, char ->
            Pair(char, Coordinate(input.indexOf(line), index))
        }
    }
}

private fun getLettersForCoordinates(coordinates: List<Coordinate>): List<Char> {
    return coordinates.map { coordinate ->
        LETTER_GRID[coordinate.y][coordinate.x].first
    }
}

private fun generateNeighborCoordinates(coordinates: Coordinate): List<List<Coordinate>> {
    val rangeTo = 1.rangeTo(3)

    return listOf(
        rangeTo.map { Coordinate(coordinates.y - it, coordinates.x) },
        rangeTo.map { Coordinate(coordinates.y + it, coordinates.x) },
        rangeTo.map { Coordinate(coordinates.y, coordinates.x - it) },
        rangeTo.map { Coordinate(coordinates.y, coordinates.x + it) },
        rangeTo.map { Coordinate(coordinates.y - it, coordinates.x - it) },
        rangeTo.map { Coordinate(coordinates.y - it, coordinates.x + it) },
        rangeTo.map { Coordinate(coordinates.y + it, coordinates.x - it) },
        rangeTo.map { Coordinate(coordinates.y + it, coordinates.x + it) }
    )
}

private fun generateXCoordinates(coordinates: Coordinate): List<List<Coordinate>> {
    return listOf(
        listOf(Coordinate(coordinates.y - 1, coordinates.x - 1), Coordinate(coordinates.y + 1, coordinates.x + 1)),
        listOf(Coordinate(coordinates.y - 1, coordinates.x + 1), Coordinate(coordinates.y + 1, coordinates.x - 1)),
    )
}

private fun countXmas(coordinates: List<List<Coordinate>>, sort: Boolean, condition: (neighbors: List<String>) -> Int): Int {
    val neighbors = coordinates.filter { neighbor ->
        isInBounds(neighbor)
    }
    val neighborLetters = neighbors.map { neighbor ->
        val letters = getLettersForCoordinates(neighbor)
        if (sort) letters.sorted().joinToString("") else letters.joinToString("")
    }

    return condition(neighborLetters)
}

fun main() {
    fun part1(): Int {
        val allLetterXs = getAllCoordsForOneLetter('X')

        return allLetterXs.fold(0) { acc, letterX ->
            val coordinates = generateNeighborCoordinates(letterX.second)
            acc + countXmas(coordinates, false) { neighbors -> (neighbors.count { it == "MAS"}) }
        }
    }

    fun part2(): Int {
        val allLetterAs = getAllCoordsForOneLetter('A')

        return allLetterAs.fold(0) { acc, letterX ->
            val coordinates = generateXCoordinates(letterX.second)
            acc + countXmas(coordinates, true) { neighbors -> (if (neighbors.sorted().count { it == "MS" } == 2) 1 else 0) }
        }
    }

    println("Day 04 - Part one: ${part1()}")
    println("Day 04 - Part two: ${part2()}")
}