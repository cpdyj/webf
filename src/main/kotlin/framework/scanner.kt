package framework

import framework.annotation.RestController
import framework.bootstrap.scanAndExecuteBootstraps
import org.apache.logging.log4j.kotlin.logger
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder

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

