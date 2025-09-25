package com.pave.driversapp.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope

/**
 * Safe LaunchedEffect that handles Compose crashes gracefully
 */
@Composable
fun SafeLaunchedEffect(
    key1: Any? = null,
    key2: Any? = null,
    key3: Any? = null,
    block: suspend CoroutineScope.() -> Unit
) {
    val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        if (exception.message?.contains("ACTION_HOVER_EXIT") == true) {
            Log.w("ComposeUtils", "Caught ACTION_HOVER_EXIT in LaunchedEffect, ignoring")
        } else {
            Log.e("ComposeUtils", "Error in LaunchedEffect: ${exception.message}", exception)
        }
    }
    
    LaunchedEffect(key1, key2, key3) {
        try {
            block()
        } catch (e: Exception) {
            exceptionHandler.handleException(coroutineContext, e)
        }
    }
}
