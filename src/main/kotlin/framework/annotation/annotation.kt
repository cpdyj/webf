package framework.annotation

import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@NeedScan
annotation class RestController

/**
 * Annotation for bootstrap execute.
 *
 * Annotated function will be called on bootstrap
 *
 * @param[depends] function name list of dependencies
 *
 * @author iseki
 */
@NeedScan
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class BootstrapExecute(val depends: Array<String> = [])

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Inherited
annotation class NeedScan

