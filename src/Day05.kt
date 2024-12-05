
private val INPUT = readInput("Day05")
private val RULES = getRules(INPUT)
private val RULES_DUO = getRulesDuo(INPUT)
private val UPDATES = getUpdates(INPUT)

class Number(val value: String): Comparable<Number> {
    override fun compareTo(other: Number): Int {
        return if (RULES_DUO.contains(listOf(this.value, other.value))) {
            -1
        } else if (RULES_DUO.contains(listOf(other.value, this.value))) {
            1
        } else {
            0
        }
    }
}

private fun getRulesDuo(input: List<String>): List<List<String>> {
    val (rules, _) = input.partition { it.contains("|") }

    return rules
        .map { it.split("|") }
        .map { listOf(it.first(), it.last()) }
}

private fun getUpdates(input: List<String>): List<List<String>> {
    val (_, updates) = input.partition { it.contains("|") }

    return updates
        .filterNot { it.isBlank() }
        .map { it.split(',') }
}

private fun getRules(input: List<String>): Map<String, List<String>> {
    val (rules, _) = input.partition { it.contains("|") }

    return rules
        .map { it.split("|") }
        .groupBy { it.first() }
        .map { it.key to it.value.map { it.last() } }
        .toMap()
}

private fun isInOrder(update: List<String>): Boolean {
    update.indices.reversed().forEach { index ->
        if (index == 0) {
            return true
        }

        val number = update[index]
        val rulesForNumber = RULES[number]
        val otherNumbers = update.subList(0, index)

        rulesForNumber?.let {
            if (rulesForNumber.intersect(otherNumbers.toSet()).isNotEmpty()) {
                return false
            }
        }
    }

    return false
}

fun main() {
    fun part1(): Int {
        return UPDATES.filter { isInOrder(it) }.sumOf { it[it.size / 2].toInt() }
    }

    fun part2(): Int {
        val invalidLists = UPDATES.filterNot { isInOrder(it) }

        val total = invalidLists.map { update ->
            update.map { Number(it) }.sorted().map { it.value }
        }.sumOf { it[it.size / 2].toInt() }

        return total
    }


    println("Day 05 - Part one: ${part1()}")
    println("Day 05 - Part two: ${part2()}")
}