package com.kiko.kadblib.results

import com.kiko.kadblib.states.ConnectionState

interface ConnectionResult{
    fun changedState(connectionState: ConnectionState)
    fun onError(error: Error)
}