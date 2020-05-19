package framework.bootstrap

import framework.annotation.BootstrapExecute
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.common.WebEnvironment
import io.vertx.kotlin.core.vertxOptionsOf
import org.apache.logging.log4j.kotlin.logger
import java.io.File

private const val VERTX_OPTIONS_PATH_ENV_VAR_ID = "VERTX_CORE_CONFIG_PATH"

lateinit var vertxCoreOptions: VertxOptions

lateinit var vertx: Vertx

@BootstrapExecute(depends = ["registerJacksonKotlinModule"])
internal fun vertxOptionsLoad() {
    val logger = logger("vertxOptionsLoad")
    val path = System.getenv(VERTX_OPTIONS_PATH_ENV_VAR_ID)?.let { it.trim() }?.takeIf { it.isNotBlank() }
    logger.info { if (path == null) "env: $VERTX_OPTIONS_PATH_ENV_VAR_ID not config." else "load config from: $path" }
    vertxCoreOptions = path?.let(::File)?.readText()?.let { VertxOptions(JsonObject(it)) } ?: vertxOptionsOf()
    logger.info("loaded")
}

@BootstrapExecute(depends = ["vertxOptionsLoad"])
internal fun initVertx() {
    val logger = logger("initVertx")
    vertx = Vertx.vertx(vertxCoreOptions)
    logger.info("init.")
    runCatching { logger.info("WebEnvironment.development: ${WebEnvironment.development()}") }
        .onFailure { logger.warn("Can't detect web environment dev mode, might dependencies not exist?") }
}