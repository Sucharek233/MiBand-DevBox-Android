package com.sucharek.devbox.websocket

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class WebSocketServerManager(
    private val onMessageReceived: (String) -> Unit,
    private val onLog: (String) -> Unit
) {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private val sessions = Collections.newSetFromMap(ConcurrentHashMap<DefaultWebSocketServerSession, Boolean>())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(port: Int) {
        if (server != null) return

        try {
            server = embeddedServer(Netty, port = port) {
                install(WebSockets)
                routing {
                    webSocket("/") {
                        sessions.add(this)
                        onLog("Client connected: ${call.request.local.remoteHost}")
                        try {
                            for (frame in incoming) {
                                if (frame is Frame.Text) {
                                    val text = frame.readText()
                                    onLog("WS RECV: $text")
                                    onMessageReceived(text)
                                }
                            }
                        } catch (e: Exception) {
                            onLog("WS Session Error: ${e.message}")
                        } finally {
                            sessions.remove(this)
                            onLog("Client disconnected")
                        }
                    }
                }
            }.start(wait = false)
            onLog("WebSocket server started on port $port")
        } catch (e: Exception) {
            onLog("Failed to start server: ${e.message}")
        }
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        onLog("WebSocket server stopped")
    }

    fun broadcast(message: String) {
        scope.launch {
            val sessionsCopy = sessions.toList()
            sessionsCopy.forEach { session ->
                try {
                    if (session.isActive) {
                        session.send(message)
                    }
                } catch (e: Exception) {
                    // Ignore session send errors
                }
            }
        }
    }

    fun isRunning(): Boolean = server != null
}
