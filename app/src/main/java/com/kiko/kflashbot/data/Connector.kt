package com.kiko.kflashbot.data

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.kiko.kadblib.adbbase.AdbConnection
import com.kiko.kadblib.adbbase.DefaultADBCrypto
import com.kiko.kadblib.channel.TcpChannel
import com.kiko.kadblib.results.ConnectionResult
import com.kiko.kadblib.states.ConnectionState
import com.kiko.kflashbot.ui.adbConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Connector {
    fun connect(
        address: String,
        port: String,
        context: Context,
        coroutineScope: CoroutineScope,
        mConnectionState: MutableState<ConnectionState>
    ) {
        lateinit var mAdbConnection: AdbConnection

        val result = object : ConnectionResult {
            override fun changedState(connectionState: ConnectionState) {
                mConnectionState.value = connectionState
                if(connectionState == ConnectionState.CONNECTED){
                    adbConnection = mAdbConnection
                }
            }
            override fun onError(error: Error) {
                error
            }
        }


        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                mAdbConnection = AdbConnection.create(
                    channel = TcpChannel.create(address, port.toInt(), result),
                    crypto = DefaultADBCrypto.getDefault(context),
                    result
                )
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        mAdbConnection.connect()
                    }
                }
            }
        }
    }
}