package com.kiko.kflashbot.ui.screens

import android.os.Environment
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.kiko.kadblib.adbbase.AdbConnection
import com.kiko.kflashbot.data.Pusher
import com.kiko.kflashbot.ui.adbConnection
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
@Destination
fun FileScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}

    val context = LocalContext.current
    val coroutine = rememberCoroutineScope()

    if (adbConnection != null) {
        OutlinedButton(onClick = {
            coroutine.launch {
                withContext(Dispatchers.IO) {
                    Pusher().push(
                        adbConnection, File(
                            "${context.filesDir.path}/test.txt"
                        ), "/data/local/tmp/"
                    )
                }
            }
        }) {
            Text(text = "Push")
        }
    } else {
        Text("Some Error")
    }
}