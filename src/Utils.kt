import kotlin.io.path.Path
import kotlin.io.path.readLines

enum class DIRECTION {
    UP, DOWN, LEFT, RIGHT
}

typealias LetterGrid = List<List<Pair<Char, Coordinate>>>
data class Coordinate(val y: Int, val x: Int )

/**
 * Reads lines from the given input txt file.
 */
fun readInput(name: String) = Path("src/resources/$name.txt").readLines()

/**
 * The cleaner shorthand for printing output.
 */
fun Any?.println() = println(this)


/**
 * Converts a map to a list of char and coordinate.
 */
fun convertToGrid(input: List<String>): LetterGrid {
    return input.mapIndexed { y, line ->
        line.mapIndexed { x, char ->
            Pair(char, Coordinate(y, x))
        }
    }
}

/**
 * Counts the number of times a sublist appears in a list.
 */
fun <T> countSublistOccurrences(subList: List<T>, mainList: List<T>): Int {
    if (subList.isEmpty() || mainList.size < subList.size) return 0

    var count = 0
    for (i in 0..mainList.size - subList.size) {
        if (mainList.subList(i, i + subList.size) == subList) {
            count++
        }
    }
    return count
}