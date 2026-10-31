package com.thoen.androidstructure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thoen.androidstructure.presentation.main.MainScreen
import com.thoen.androidstructure.presentation.main.MainViewModel
import com.thoen.androidstructure.presentation.theme.AndroidStructureTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as AndroidStructureApp).container

        setContent {
            AndroidStructureTheme {
                val viewModel: MainViewModel =
                    viewModel(factory = MainViewModel.factory(container.getTodosUseCase))
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
