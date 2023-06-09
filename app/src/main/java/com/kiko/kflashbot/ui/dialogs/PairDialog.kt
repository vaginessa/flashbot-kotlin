package com.kiko.kflashbot.ui.dialogs

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.kiko.kflashbot.data.Connector


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairDialog(hide: () -> Unit) {
    AlertDialog(onDismissRequest = {
    }) {
        val context = LocalContext.current
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val ipAddressTextFieldValue = remember {
                    mutableStateOf(TextFieldValue("192.168.0.106"))
                }
                val portTextFieldValue = remember {
                    mutableStateOf(TextFieldValue("5555"))
                }

                OutlinedTextField(
                    value = ipAddressTextFieldValue.value,
                    onValueChange = { ipAddressTextFieldValue.value = it },
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
                    value = portTextFieldValue.value,
                    onValueChange = { portTextFieldValue.value = it },
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

                OutlinedButton(
                    onClick = {
                        Thread {
                            //Do some Network Request
                            Connector().connect(
                                ipAddressTextFieldValue.value.text,
                                portTextFieldValue.value.text,
                                context
                            )
                        }.start()

                    }, border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text(
                        text = "Подключиться",
                        color = MaterialTheme.colorScheme.onPrimary.copy(0.8f)
                    )
                }
            }
        }
    }
}