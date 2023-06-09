package com.kiko.kadblib.states

enum class ConnectionState {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    UNREACHABLE,
    TIMEOUT
}

interface ConnectionResult{
    fun changedState(connectionState: ConnectionState)
    fun onError(error: Error)
}