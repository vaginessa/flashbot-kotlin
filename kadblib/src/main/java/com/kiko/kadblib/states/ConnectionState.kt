package com.kiko.kadblib.states

enum class ConnectionState(val state: Int) {
    CONNECTED(0),
    CONNECTING(1),
    DISCONNECTED(2),
    UNREACHABLE(3)
}

interface ConnectionResult{
    fun changedState(connectionState: ConnectionState)
    fun onError(error: Error)
}