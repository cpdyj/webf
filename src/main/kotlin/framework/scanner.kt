package framework

import framework.annotation.BootstrapExecute
import framework.annotation.RestController
import framework.bootstrap.scanAndExecuteBootstraps
import org.apache.logging.log4j.kotlin.logger
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import java.lang.reflect.Method
import kotlin.reflect.jvm.kotlinFunction

interface IRestController

private val logger = logger("bootstrap")

suspend fun main() {
    println("Hello world")
    val logger = logger("main")
    logger.info { "Hello world" }
    startApp(listOf(""))
}

suspend fun startApp(pkgs: List<String>) {
    val logger = logger("bootstrap")
    logger.info("start app.")
    val r = Reflections(
        (listOf("framework") + pkgs).map { ClasspathHelper.forPackage(it) }
            .fold(ConfigurationBuilder()) { acc, urls -> acc.addUrls(urls) }
            .setScanners(TypeAnnotationsScanner(), SubTypesScanner(), MethodAnnotationsScanner())
    )
    logger.debug { "scanner init." }
    scanAndExecuteBootstraps(r)
    logger.info("Controller scanning...")
    val controller = r.getTypesAnnotatedWith(RestController::class.java).toList()
    logger.info { "Scanned controllers: ${controller.size}" }

}

private fun resolve(
    source: Map<String, Method>,
    target: LinkedHashMap<String, Method>,
    name: String,
    method: Method,
    working: LinkedHashSet<String> = linkedSetOf(),
) {
    check(working.size < 100) { "dependence relation too deep." }
    if (target.containsKey(name)) {
        return
    }
    method.getAnnotation(BootstrapExecute::class.java).depends.forEach {
        if (working.contains(it)) {
            // circular
            println("===== DETECTED CIRCULAR DEPENDENCY =====")
            working.toList().subList(working.indexOf(it), working.size).forEach { println(" - $it") }
            println("========================================")
            error("Detected circular dependency.")
        }
        if (!target.containsKey(it)) {
            val m = source[it] ?: error("can't resolve dependency '$it' declare in $name")
            working.add(it)
            resolve(source, target, it, m, working)
            working.remove(working.last())
        }
    }
    target[name] = method
}

private fun scanAndExecuteAllBootstrap(r: Reflections) {
    logger.info("bootstrap scanning...")
    val ms = r.getMethodsAnnotatedWith(BootstrapExecute::class.java)
    ms.forEach {
        it.kotlinFunction?.let { println(it to it.isSuspend) }
    }
    logger.debug { "bootstraps: " + ms.map { it.name } }
    val mm = ms.map { it.name to it!! }.toMap()
    val list = LinkedHashMap<String, Method>(ms.size)
    mm.forEach { (k, v) -> resolve(mm, list, k, v) }
    list.forEach { (_, m) ->
        logger.info { "execute: ${m.name}" }
        runCatching {
            try {
                if (!m.canAccess(null)) {
                    logger.debug("method is inaccessible, try set accessible")
                    runCatching { m.trySetAccessible() }.onFailure {
                        logger.warn("can't set accessible to ${m.name}", it)
                    }
                }
            } catch (e: Exception) {
                logger.warn("Can't check accessible, method should be static and without parameter", e)
            }
            m.invoke(null)
        }.onFailure {
            logger.fatal("fail to execute: ${m.name}", it)
            throw it
        }
    }
    logger.info { "All bootstrap execution done." }
}