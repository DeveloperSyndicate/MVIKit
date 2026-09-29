package com.developersyndicate.mvikit.core.intent

/**
 * Strategy defining how dispatched [Intent]s are executed in a ViewModel.
 */
enum class IntentExecutionPolicy {
    /**
     * Intents are executed concurrently in separate coroutines.
     * Offers maximum throughput, but execution order is not guaranteed.
     */
    CONCURRENT,

    /**
     * Intents are queued in a Channel and processed sequentially in a single coroutine loop.
     * Prevents overlapping intent executions and guarantees race-free sequential execution.
     */
    SEQUENTIAL
}
