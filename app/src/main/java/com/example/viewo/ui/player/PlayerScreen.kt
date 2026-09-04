package com.example.viewo.ui.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.viewo.viewmodel.PlayerState
import com.example.viewo.viewmodel.PlayerViewModel

@Composable
fun PlayerScreen(viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.playerState.collectAsState()
    val pairingCode by viewModel.pairingCode.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val currentState = state) {
            is PlayerState.Unpaired -> PairingScreen(pairingCode)
            is PlayerState.WaitingForCampaign -> WaitingScreen()
            is PlayerState.Downloading -> DownloadingScreen()
            is PlayerState.Playing -> PlayingScreen(
                assignment = currentState.assignment,
                onLogProofOfPlay = viewModel::logProofOfPlay
            )
        }
    }
}
