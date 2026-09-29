package com.developersyndicate.mvikit.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class MviAction(
    val key: String = "",
    val policy: String = "ALLOW", // ALLOW, IGNORE, CANCEL_PREVIOUS, RESTART
    val retries: Int = 0,
    val retryDelayMillis: Long = 0,
    val exponentialBackoff: Boolean = false,
    val loadingKey: String = ""
)
