package com.kiko.kflashbot.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor() : ViewModel() {
    private val _isPairingDialogVisible = mutableStateOf(true)
    val isPairingDialogVisible : State<Boolean> = _isPairingDialogVisible

    fun showPairingDialog(){
        _isPairingDialogVisible.value = true
    }

    fun hidePairingDialog(){
        _isPairingDialogVisible.value = false
    }
}