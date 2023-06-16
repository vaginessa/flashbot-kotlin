package com.kiko.kadblib.results

import com.kiko.kadblib.states.ConnectionState

interface InstallResult{
    fun changedProgress(connectionState: ConnectionState)
    fun changedState(connectionState: ConnectionState)
    fun onError(error: Error)
}