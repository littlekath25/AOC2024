private val multRegex = Regex("mul[(]\\d+,\\d+[)]")
private val instrMultRegex = Regex("mul[(]\\d+,\\d+[)]|do[(][)]|don't[(][)]")
private val digitRegex = Regex("\\D+")

fun main() {
    fun part1(input: String): Int {
        return multRegex.findAll(input).sumOf { matchResult ->
            val (first, last) = matchResult.value
                .split(digitRegex)
                .filter { it.isNotBlank() }
                .map { it.toInt() }

            first * last
        }
    }

    fun part2(input: String): Int {
        val total = instrMultRegex.findAll(input).map { it.value }.fold(Pair(0, true)) { acc, instruction ->
            val (sum, isTurnedOn) = acc

            when {
                instruction.startsWith("mul") -> {
                    if (isTurnedOn) {
                        val (first, last) = instruction
                            .split(digitRegex)
                            .filter { it.isNotBlank() }
                            .map { it.toInt() }

                        Pair(sum + (first * last), true)
                    } else {
                        Pair(sum, false)
                    }
                }
                instruction.startsWith("don't") -> Pair(sum, false)
                instruction.startsWith("do") -> Pair(sum, true)
                else -> throw IllegalArgumentException("Unknown instruction")
            }
        }

        return total.first
    }

    val testInput = readInput("Day03_example")
//    check(part1(testInput) == 1)

    val input = readInput("Day03")
    println("Day 03 - Part one: ${part1(input.toString())}")
    println("Day 03 - Part two: ${part2(input.toString())}")
}