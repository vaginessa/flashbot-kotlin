package com.kiko.kflashbot.data

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.kiko.kadblib.AdbBase64
import com.kiko.kadblib.AdbChannel
import com.kiko.kadblib.AdbConnection
import com.kiko.kadblib.AdbCrypto
import com.kiko.kadblib.TcpChannel
import com.kiko.kflashbot.data.Consts.TAG
import com.kiko.kflashbot.utils.MyAdbBase64
import java.io.File
import java.net.Socket

class Connector {
    fun connect(address: String, port: String, context: Context){
        val adbCrypto = Crypter().getAdbCrypto(context)

        val adbConnection = AdbConnection.create(channel = TcpChannel(Socket(address, port.toInt())),crypto = adbCrypto) // Соединяем

        try {
            adbConnection.connect()
        }
        catch (error: java.io.IOException){
            //TODO СДЕЛАТЬ ВЫВОД ОШИБКИ ПОДКЛЮЧЕНИЯ
        }

        //TODO: DO NOT DELETE IT, I CAN'T EXPLAIN WHY
        adbConnection.open("shell:exec date")
    }
}