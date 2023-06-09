package com.kiko.kflashbot.ui.screens

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.kiko.kflashbot.ui.dialogs.PairDialog
import com.kiko.kflashbot.ui.viewmodel.StartViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph

@Composable
@Destination
@RootNavGraph(start=true)
fun StartScreen(startViewModel: StartViewModel = hiltViewModel()){
    if (startViewModel.isPairingDialogVisible.value)
        PairDialog {
            startViewModel.hidePairingDialog()
        }
}