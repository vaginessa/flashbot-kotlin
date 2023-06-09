package com.kiko.kflashbot.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.kiko.kadblib.AdbConnection
import com.kiko.kadblib.DefaultADBCrypto
import com.kiko.kadblib.channel.TcpChannel
import com.kiko.kadblib.states.ConnectionResult
import com.kiko.kadblib.states.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class Connector {
    lateinit var adbConnection: AdbConnection

    fun connect(
        address: String,
        port: String,
        context: Context,
        coroutineScope: CoroutineScope,
        mConnectionState: MutableState<ConnectionState>
    ) {

        val result = object : ConnectionResult {
            override fun changedState(connectionState: ConnectionState) {
                mConnectionState.value = connectionState
            }
            override fun onError(error: Error) {
                error
            }
        }


        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                adbConnection = AdbConnection.create(
                    channel = TcpChannel.create(address, port.toInt(), result),
                    crypto = DefaultADBCrypto.getDefault(context),
                    result
                )
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        adbConnection.connect()
                    }
                }
            }
        }
    }
}