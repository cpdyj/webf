package framework.bootstrap

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import framework.annotation.BootstrapExecute

/**
 * Register Jackson Kotlin module for Vert.x
 *
 * @author iseki
 */
@BootstrapExecute
internal fun registerJacksonKotlinModule() {
    io.vertx.core.json.jackson.DatabindCodec.mapper().registerKotlinModule()
    io.vertx.core.json.jackson.DatabindCodec.prettyMapper().registerKotlinModule()
    error("AWSL")
}

