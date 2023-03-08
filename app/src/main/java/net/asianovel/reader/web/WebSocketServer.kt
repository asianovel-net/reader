package net.asianovel.reader.web

import fi.iki.elonen.NanoWSD
import net.asianovel.reader.service.WebService
import net.asianovel.reader.web.socket.BookSourceDebugWebSocket
import net.asianovel.reader.web.socket.RssSourceDebugWebSocket

class WebSocketServer(port: Int) : NanoWSD(port) {

    override fun openWebSocket(handshake: IHTTPSession): WebSocket? {
        WebService.serve()
        return when (handshake.uri) {
            "/bookSourceDebug" -> {
                BookSourceDebugWebSocket(handshake)
            }
            "/rssSourceDebug" -> {
                RssSourceDebugWebSocket(handshake)
            }
            else -> null
        }
    }
}
