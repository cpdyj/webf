package framework.bootstrap

import framework.annotation.BootstrapExecute
import framework.utils.Graph
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.logger
import org.reflections.Reflections
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

private val logger = logger("bootstrap")
suspend fun scanAndExecuteBootstraps(r: Reflections) {
    // wrap to suspend()->Unit
    val list = r.getMethodsAnnotatedWith(BootstrapExecute::class.java).map {
        object {
            val callable = it.kotlinFunction?.let {
                it.takeIf { it.isSuspend }?.let {
                    logger.debug { "method '${it.name}' is suspend, wrap it." }
                    suspend { it.callSuspend() }
                } ?: run {
                    logger.debug { "method '${it.name}' is not suspend, wrap it." }
                    suspend { it.call() }
                }
            } ?: run {
                logger.debug { "can't get kotlin function with method '${it.name}', wrap it." }
                suspend { it.invoke(null) }
            }
            val name = it.name
            val annotation = it.getAnnotation(BootstrapExecute::class.java)!!
        }
    }

    val a = list.toTypedArray()
    val map = a.mapIndexed { index, p -> p.name to index }.toMap()
    val g = Graph(a.size)
    a.forEach {
        val begin = map[it.name]!!
        it.annotation.depends.forEach {
            g.addEdge(begin, map[it]!!)
        }
    }
    val priList = g.kahn()?.reversed()
    if (priList == null) {
        // found circle, fail
        println(g.circleDetect())
        val circle = g.circleDetect()
        val o = buildString {
            appendln()
            appendln("===== CIRCULAR DEPENDENCY DETECTED =====")
            circle?.forEach {
                appendln(" - " + a[it].name)
            }
            appendln("========================================")
        }
        println(o)
        logger.fatal("detected circular dependency.")
        logger.fatal(o)
        error("detected circular dependency")
    }
    val result = priList.map { it.map(a::get) }
    logger.debug { "execute priority: " + result.map { it.map { it.name } } }
    val scope = CoroutineScope(SupervisorJob())
    try {
        result.forEach {
            logger.debug { "concurrent execute: ${it.map { it.name }}" }
            it.map { task ->
                withContext(Dispatchers.Default) {
                    scope.async {
                        runCatching {
                            task.callable.invoke()
                        }.onFailure {
                            val rootCause = deWrapException<InvocationTargetException>(it) ?: it
                            val errmsg =
                                "[${task.name}] -> ${rootCause::class.qualifiedName}: ${rootCause.message}"
                            throw BootstrapMethodFailException(msg = errmsg, cause = rootCause)
                        }
                    }
                }.also { logger.debug { "begin execute: ${task.name}" } }
            }.let { awaitAll(*it.toTypedArray()) }
            logger.debug { "done." }
        }
    } catch (e: Throwable) {
        logger.fatal("Something went wrong.", e)
        throw e
    }
}

internal class BootstrapMethodFailException(msg: String, cause: Throwable?) : RuntimeException(msg, cause)

inline fun <reified T : Throwable> deWrapException(th: Throwable) =
    if (th is T) {
        th.cause
    } else {
        th
    }
