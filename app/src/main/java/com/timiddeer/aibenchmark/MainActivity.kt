package com.timiddeer.aibenchmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.timiddeer.aibenchmark.ui.theme.AIBenchmarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIBenchmarkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BenchmarkScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun BenchmarkScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("Tap Run. Results also print to Logcat (tag: ModelBenchmark).") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("On-Device Quantization Benchmark", style = MaterialTheme.typography.headlineSmall)

        Button(
            enabled = !running,
            onClick = {
                running = true
                result = "Running..."
                scope.launch {
                    // runAll blocks (CPU/GPU inference), so run it off the main thread.
                    val table = withContext(Dispatchers.Default) { ModelBenchmark.runAll(context) }
                    result = table
                    running = false
                }
            },
        ) {
            Text(if (running) "Running..." else "Run benchmark")
        }

        if (running) CircularProgressIndicator()

        Text(
            text = result,
            style = MaterialTheme.typography.bodyMedium.merge(TextStyle(fontFamily = FontFamily.Monospace)),
        )
    }
}
