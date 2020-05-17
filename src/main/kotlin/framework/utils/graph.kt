package framework.utils

/**
 * Graph
 *
 * @author iseki
 */
class Graph(val size: Int) {

    private val inDegree = IntArray(size)
    private val outDegree = IntArray(size)
    private val table = Array<MutableSet<Int>>(size) { mutableSetOf() }
    fun addEdge(from: Int, to: Int) {
        check(from < size && to < size) { "outofbound" }
        inDegree[to]++
        outDegree[from]++
        table[from].takeIf { !it.contains(to) }?.add(to) ?: error("edge exists.")
    }

    fun kahn(): List<List<Int>>? {
        val ind = inDegree.copyOf()
        val nodes = (0 until size).toMutableSet()
        val output = mutableListOf<List<Int>>()
        while (true) {
            nodes.filter { ind[it] == 0 }.takeIf { it.isNotEmpty() }?.also(output::add)?.forEach { n ->
                nodes.remove(n)
                table[n].forEach {
                    ind[it]--
                }
            } ?: break
        }
        return output.takeIf { nodes.isEmpty() }
    }

    fun circleDetect(): List<Int>? {
        table.forEachIndexed { index, _ ->
            val l = LinkedHashSet<Int>()
            l.add(index)
            val r = aa(l, index)
            if (r != null) return r
        }
        return null
    }

    private fun aa(working: LinkedHashSet<Int>, n: Int): List<Int>? {
        table[n].forEach {
            val i = working.indexOf(it)
            if (i > -1) {
                // circle
                return working.toList().slice(i until working.size)
            }
            working.add(n)
            val t = aa(working, it)
            assert(working.remove(n))
            if (t != null) {
                return t
            }
        }
        return null
    }
}