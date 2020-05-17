package framework.bootstrap

import framework.annotation.BootstrapExecute

@BootstrapExecute(depends = ["registerJacksonKotlinModule"])
internal fun vertxOptionsLoad() {

}