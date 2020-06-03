package framework.bootstrap

import framework.annotation.Bootstrap
import org.apache.logging.log4j.kotlin.logger
import org.reflections.Reflections
import java.lang.reflect.Method
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

fun main() {

}

private val logger = logger("BootstrapScanner")
fun bootstrapScanner(r: Reflections) {
    val list = r.getMethodsAnnotatedWith(Bootstrap::class.java).toList()
        .map { method: Method ->
            object {
                val name = method.name.trim()
                val className = method.declaringClass.name.trim()
                val fullName = "$className:$name"
                val method = method
                val deps = method.getAnnotation(Bootstrap::class.java).deps.toList()
                val callable = wrapMethod(method)
            }
        }
    val conflictKeySet =
        list.asSequence().map { it.name }.groupBy { it }.filter { it.value.size > 1 }.map { it.key }.toSet()
    logger.debug { "conflictKeySet: $conflictKeySet" }
    val map = list.associateByMulti {
        if (conflictKeySet.contains(it.name)) listOf(it.fullName) else listOf(it.name, it.fullName)
    }

    fun getFromMap(name: String) =
        if (conflictKeySet.contains(name)) error("Name: $name is conflict")
        else map[name] ?: error("Can't resolve $name")

    val sorted = mutableListOf<List<String>>()
    //koah
    val inDegree = list.map {
        it.fullName to object {
            var i = 0
        }
    }.toMap().toMutableMap()
    list.forEach { it.deps.forEach { inDegree[getFromMap(it).fullName]!!.i++ } }
    var f = inDegree.filter { it.value.i == 0 }
    while (f.isNotEmpty()) {
        f.forEach { (k, _) ->
            inDegree.remove(k)
            map[k]!!.deps.forEach { inDegree[it]!!.i-- }
        }
        f = inDegree.filter { it.value.i == 0 }
    }
}

private fun <K, V> List<V>.associateByMulti(keySelector: (V) -> Collection<K>): Map<K, V> {
    val map = mutableMapOf<K, V>()
    this.forEach { v -> keySelector.invoke(v).forEach { map[it] = v } }
    return map
}

private fun wrapMethod(method: Method): suspend () -> Any? {
    val name = method.name
    val className = method.declaringClass?.name
    return method.kotlinFunction?.let {
        if (it.isSuspend) {
            logger.debug { "kotlin suspend function: $name[$className]" }
            suspend { it.callSuspend() }
        } else {
            logger.debug { "kotlin simple function: $name[$className]" }
            suspend { it.call() }
        }
    } ?: {
        logger.debug { "java simple function: $name[$className]" }
        suspend { method.invoke(null) }
    }
}