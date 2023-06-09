package com.kiko.kflashbot.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
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
    fun connect(
        address: String,
        port: String,
        context: Context,
        coroutineScope: CoroutineScope,
        mConnectionState: MutableState<ConnectionState>
    ) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val result = object : ConnectionResult {
                    override fun changedState(connectionState: ConnectionState) {
                        when (connectionState) {
                            ConnectionState.UNREACHABLE -> {
                                mConnectionState.value = connectionState
                            }

                            else -> {

                            }
                        }
                    }

                    override fun onError(error: Error) {
                        TODO("Not yet implemented")
                    }

                }


                val adbConnection = AdbConnection.create(
                    channel = TcpChannel.create(address, port.toInt(), result),
                    crypto = DefaultADBCrypto.getDefault(context),
                    result
                )

                try {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            adbConnection.connect()
                        }
                    }
                } catch (error: java.io.IOException) {
                    //TODO СДЕЛАТЬ ВЫВОД ОШИБКИ ПОДКЛЮЧЕНИЯ
                }
            }
        }
    }
}