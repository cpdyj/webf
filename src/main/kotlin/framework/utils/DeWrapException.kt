package framework.utils

/**
 * DeWrapException
 *
 * @param[T] wrapper
 * @param[th]
 *
 * @return if [th] is wrapped by [T], return [Throwable.cause] of [th]. otherwise null.
 * @author iseki
 */
inline fun <reified T : Throwable> deWrapException(th: Throwable) =
    if (th is T) {
        th.cause
    } else {
        th
    }