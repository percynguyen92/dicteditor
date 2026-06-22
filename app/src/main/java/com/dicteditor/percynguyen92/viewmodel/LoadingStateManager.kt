package com.dicteditor.percynguyen92.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

class LoadingStateManager {

    private val count = AtomicInteger(0)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun increment() {
        if (count.incrementAndGet() == 1) _isLoading.value = true
    }

    fun decrement() {
        val remaining = count.decrementAndGet()
        if (remaining <= 0) {
            count.set(0)
            _isLoading.value = false
        }
    }

    /** Convenience: wraps a suspend block, automatically managing increment and decrement */
    suspend fun <T> tracked(block: suspend () -> T): T {
        increment()
        return try {
            block()
        } finally {
            decrement()
        }
    }
}
