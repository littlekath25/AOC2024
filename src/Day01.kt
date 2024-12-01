import kotlin.math.abs

private fun listOfNumbers(input: List<String>): List<List<Int>> {
    return input.map { line ->
        line.split(" ")
            .filter { it.isNotBlank() }
            .map { it.toInt() }
    }
}

fun main() {
    fun part1(parsedPairsOfNumbers: List<List<Int>>): Int {
        val sortedList = parsedPairsOfNumbers
            .map { it.first() }
            .sorted()
            .zip(parsedPairsOfNumbers.map { it.last() }.sorted())

        return sortedList.sumOf { abs(it.first - it.second) }
    }

    fun part2(parsedPairsOfNumbers: List<List<Int>>): Int {
        val firstList = parsedPairsOfNumbers.map { it.first() }
        val secondList = parsedPairsOfNumbers.map { it.last() }

        return firstList.fold(0) { acc, number ->
            acc + (number * secondList.count { it == number })
        }
    }

    val testInput = readInput("Day01_example")
    val input = readInput("Day01")

//    check(part1(parsedPairsOfNumbers) == 11)

    println("Day 01 - Part one: ${part1(listOfNumbers(input))}")
    println("Day 01 - Part two: ${part2(listOfNumbers(input))}")
}