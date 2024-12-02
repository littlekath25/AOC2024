import kotlin.math.abs

private fun listOfNumbers(input: List<String>): List<List<Int>> {
    return input.map { line -> line.split(' ').map { it.toInt() }}
}

private fun isSafeList(list: List<Int>): Boolean {
    val windowedList = list.windowed(2, 1)

    val allInOrder = windowedList.all { it.first() > it.last() } || windowedList.all { it.first() < it.last() }
    val allSafeDistance = windowedList.all { abs(it.first() - it.last()) in 1..3 }

    return allInOrder && allSafeDistance
}

fun main() {
    fun part1(input: List<List<Int>>): Int {
        return input.count { isSafeList(it) }
    }

    fun part2(input: List<List<Int>>): Int {
        val totalSafeLists = input.count { isSafeList(it) }
        val unSafeLists = input.filter { !isSafeList(it) }

        val totalPotentialSafeLists = unSafeLists.count { list ->
            val filteredLists = list.indices.map { index ->
                isSafeList(list.filterIndexed { nIndex, _ -> nIndex != index })
            }

            filteredLists.contains(true)
        }

        return totalSafeLists + totalPotentialSafeLists
    }

    val testInput = readInput("Day02_example")
//    check(part1(testInput) == 1)

    val input = readInput("Day02")
    println("Day 02 - Part one: ${part1(listOfNumbers(input))}")
    println("Day 02 - Part two: ${part2(listOfNumbers(input))}")
}