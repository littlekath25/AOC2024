data class Equation(val total: Long, val numbers: List<Long>)
enum class Operation {
    ADD,
    MULTIPLY,
    CONCATENATE
}

private fun generatePermutations(length: Int, elements: List<Operation> = Operation.entries): List<List<Operation>> {
    if (length == 1) return elements.map { listOf(it) }

    return elements.flatMap { element ->
        generatePermutations(length - 1, elements).map { subPermutation ->
            listOf(element) + subPermutation
        }
    }
}

private fun calculate(numbers: List<Long>, operations: List<Operation>, total: Long): Long {
    if (numbers.isEmpty()) return total

    val number = numbers.first()
    val remainingNumbers = numbers.drop(1)
    val remainingOperations = operations.drop(1)

    return when (operations.first()) {
        Operation.ADD -> calculate(remainingNumbers, remainingOperations, total + number)
        Operation.MULTIPLY -> calculate(remainingNumbers, remainingOperations, total * number)
    }
}

fun main() {
    fun part1(input: List<String>): Long {
        val equations = input.map { line ->
            val splitted = line.split(':', ' ').filter { it.isNotBlank() }
            Equation(splitted[0].toLong(), splitted.subList(1, splitted.size).map { it.toLong() })
        }

        val answer = equations.mapNotNull { equation ->
            val operations = generatePermutations(equation.numbers.size - 1)
            if (operations.any { operation ->
                    calculate(equation.numbers.subList(1, equation.numbers.size), operation, equation.numbers.first()) == equation.total
                }) equation.total else null
        }.sum()

        return answer
    }

    fun part2(input: List<String>): Int {
        return input.size
    }

    val testInput = readInput("Day07_example")

    val input = readInput("Day07")
    println("Day 07 - Part one: ${part1(testInput)}")
//    println("Day 07 - Part two: ${part2(input)}")
}