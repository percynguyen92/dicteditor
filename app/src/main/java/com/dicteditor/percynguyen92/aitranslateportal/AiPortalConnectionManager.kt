package com.dicteditor.percynguyen92.aitranslateportal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

class AiPortalConnectionManager(private val context: Context) {

    private val applicationContext = context.applicationContext
    private var aiPortalService: IAiPortalService? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val activeRequests = ConcurrentHashMap<String, (AiSuggestionParcel?, String?) -> Unit>()

    private val callback = object : IAiPortalCallback.Stub() {
        override fun onSuccess(requestId: String, result: AiSuggestionParcel) {
            val handler = activeRequests.remove(requestId)
            handler?.invoke(result, null)
        }

        override fun onError(requestId: String, errorCode: Int, message: String) {
            val handler = activeRequests.remove(requestId)
            handler?.invoke(null, message)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            aiPortalService = IAiPortalService.Stub.asInterface(service)
            _isConnected.value = true
            _connectionError.value = null
            try {
                val isAlive = aiPortalService?.ping()
                Log.d("AiPortalConnection", "Connected. Ping: $isAlive")
            } catch (e: Exception) {
                Log.e("AiPortalConnection", "Ping failed", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            aiPortalService = null
            _isConnected.value = false
            _connectionError.value = "Service disconnected (bị đóng hoặc crash)"
            activeRequests.forEach { (_, handler) ->
                handler.invoke(null, "Service disconnected")
            }
            activeRequests.clear()
        }
    }

    fun bindService() {
        if (_isConnected.value) return
        _connectionError.value = null
        val intent = Intent("com.dicteditor.percynguyen92.aitranslateportal.AI_SERVICE").apply {
            setPackage("com.aistudio.translateportal.kwmzqa") 
        }

        val resolveInfo = applicationContext.packageManager.resolveService(intent, 0)
        if (resolveInfo == null) {
            _connectionError.value = "Không tìm thấy ứng dụng AI Translate Portal (com.aistudio.translateportal.kwmzqa) hoặc service không được expose."
            return
        }

        try {
            val bound = applicationContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (!bound) {
                _connectionError.value = "bindService trả về false (Không thể kết nối. Có thể thiếu quyền hoặc service bị vô hiệu hoá)."
            }
        } catch (e: SecurityException) {
            _connectionError.value = "Lỗi quyền (SecurityException): ${e.message}"
            Log.e("AiPortalConnection", "SecurityException to bind", e)
        } catch (e: Exception) {
            _connectionError.value = "Lỗi không xác định khi bind service: ${e.message}"
            Log.e("AiPortalConnection", "Failed to bind", e)
        }
    }

    fun unbindService() {
        if (_isConnected.value) {
            try {
                applicationContext.unbindService(serviceConnection)
            } catch (e: Exception) {
                Log.e("AiPortalConnection", "Failed to unbind", e)
            }
            _isConnected.value = false
            aiPortalService = null
        }
        activeRequests.forEach { (_, handler) ->
            handler.invoke(null, "Service unbound (kết nối bị đóng)")
        }
        activeRequests.clear()
    }

    suspend fun getSuggestion(chinese: String): Result<AiSuggestionParcel> = suspendCancellableCoroutine { continuation ->
        val service = aiPortalService
        if (service == null) {
            continuation.resume(Result.failure(Exception("AI Translate Portal service is not connected.")))
            return@suspendCancellableCoroutine
        }

        val requestId = UUID.randomUUID().toString()
        activeRequests[requestId] = { result, error ->
            if (continuation.isActive) {
                try {
                    if (error != null) {
                        Log.e("AiPortalConnection", "Translation Error: $error")
                        continuation.resume(Result.failure(Exception(error)))
                    } else if (result != null) {
                        continuation.resume(Result.success(result))
                    } else {
                        continuation.resume(Result.failure(Exception("Unknown error: Result is null")))
                    }
                } catch (e: Exception) {
                    // Ignore if already resumed
                }
            }
        }

        continuation.invokeOnCancellation {
            try {
                service.cancelRequest(requestId)
            } catch (e: Exception) {
                // Ignore
            }
            activeRequests.remove(requestId)
        }

        try {
            service.getSuggestion(requestId, chinese, callback)
        } catch (e: Exception) {
            Log.e("AiPortalConnection", "Failed to call getSuggestion", e)
            activeRequests.remove(requestId)
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }
}
