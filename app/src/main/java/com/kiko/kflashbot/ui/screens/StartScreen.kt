package com.kiko.kflashbot.ui.screens

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kiko.kadblib.states.ConnectionState
import com.kiko.kflashbot.data.Connector
import com.kiko.kflashbot.ui.components.TextCard
import com.kiko.kflashbot.ui.viewmodel.StartViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph

@Composable
@Destination
@RootNavGraph(start = true)
fun StartScreen(startViewModel: StartViewModel = hiltViewModel()) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val context = LocalContext.current
        Card(modifier = Modifier.animateContentSize()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = startViewModel.ipAddressTextFieldValue.value,
                    onValueChange = { startViewModel.ipAddressTextFieldValue.value = it },
                    label = {
                        Text(
                            text = "Ip адрес устройства",
                            color = MaterialTheme.colorScheme.onPrimary.copy(0.6f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onPrimary.copy(0.8f),
                        focusedIndicatorColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(20.dp)
                )

                OutlinedTextField(
                    value = startViewModel.portTextFieldValue.value,
                    onValueChange = { startViewModel.portTextFieldValue.value = it },
                    label = {
                        Text(
                            text = "Порт",
                            color = MaterialTheme.colorScheme.onPrimary.copy(0.6f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onPrimary.copy(0.8f),
                        focusedIndicatorColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(20.dp)
                )

                val composeScope = rememberCoroutineScope()

                Log.d("PairDialog", "STATE = ${startViewModel.connectionState.value}")
                when (startViewModel.connectionState.value) {
                    ConnectionState.DISCONNECTED -> {
                        OutlinedButton(
                            onClick = {
                                startViewModel.connectToDevice(context, composeScope)
                            }, border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Text(
                                text = "Подключиться",
                                color = MaterialTheme.colorScheme.onPrimary.copy(0.8f)
                            )
                        }
                    }

                    ConnectionState.UNREACHABLE -> {
                        OutlinedButton(
                            onClick = {
                                startViewModel.connectToDevice(context, composeScope)
                            }, border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Text(
                                text = "Подключиться",
                                color = MaterialTheme.colorScheme.onPrimary.copy(0.8f)
                            )
                        }
                        TextCard(
                            textColor = MaterialTheme.colorScheme.onPrimary,
                            text = "Ошибка, устройство недостижимо"
                        )
                    }

                    ConnectionState.CONNECTING -> {
                        LinearProgressIndicator(
                            trackColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    ConnectionState.CONNECTED -> {
                        Text(text = "Conntected")
                    }

                    ConnectionState.TIMEOUT ->{
                        Text(text = "Timeout")
                    }
                }
            }
        }
    }
}
