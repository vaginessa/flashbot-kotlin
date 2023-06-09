package com.kiko.kflashbot.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.kiko.kadblib.states.ConnectionState
import com.kiko.kflashbot.data.Connector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor() : ViewModel() {
    private val _isPairingDialogVisible = mutableStateOf(true)
    val isPairingDialogVisible: State<Boolean> = _isPairingDialogVisible

    val connectionState = mutableStateOf(ConnectionState.DISCONNECTED)

    val ipAddressTextFieldValue =
        mutableStateOf(TextFieldValue("192.168.0.106"))

    val portTextFieldValue =
        mutableStateOf(TextFieldValue("5555"))

    fun showPairingDialog() {
        _isPairingDialogVisible.value = true
    }

    fun hidePairingDialog() {
        _isPairingDialogVisible.value = false
    }

    fun connectToDevice(context: Context, composeScope: CoroutineScope) {
        connectionState.value = ConnectionState.CONNECTING

        Connector().connect(
            ipAddressTextFieldValue.value.text,
            portTextFieldValue.value.text,
            context,
            composeScope,
            connectionState
        )
    }
}