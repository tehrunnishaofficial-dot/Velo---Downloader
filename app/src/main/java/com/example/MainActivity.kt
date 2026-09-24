package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.DownloadRepository
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DownloadViewModel
import com.example.viewmodel.DownloadViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: DownloadViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Repository and ViewModel
        val repository = DownloadRepository(applicationContext)
        val factory = DownloadViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                // Fetch the ViewModel via provider factory
                viewModel = viewModel(factory = factory)

                // Handle shared intent on cold startup
                LaunchedEffectIntentHandler()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareIntent(intent)
    }

    @androidx.compose.runtime.Composable
    private fun LaunchedEffectIntentHandler() {
        androidx.compose.runtime.LaunchedEffect(intent) {
            handleIncomingShareIntent(intent)
        }
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrEmpty()) {
                    if (::viewModel.isInitialized) {
                        viewModel.handleSharedUrl(sharedText)
                    }
                }
            }
        }
    }
}
