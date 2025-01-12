package io.github.koalaplot.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import io.github.koalaplot.demo.bar.BarChartDemo

internal fun main(): Unit = application {
    Window(
        title = "Demo-App",
        state = WindowState(
            size = DpSize(400.dp, 800.dp),
            position = WindowPosition.Absolute(10.dp, 10.dp)
        ),
        onCloseRequest = ::exitApplication,
    ) {
        DemoApp()
    }
}

@Composable
private fun DemoApp() {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(it).padding(all = 16.dp).fillMaxSize()
        ) {
            BarChartDemo()
            HorizontalDivider(thickness = 2.dp)
        }
    }
}
