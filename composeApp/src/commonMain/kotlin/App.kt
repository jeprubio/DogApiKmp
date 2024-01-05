import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rumosoft.librarydogapi.DogApi
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch

@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            val scope = rememberCoroutineScope()
            var text by remember { mutableStateOf("Loading") }
            LaunchedEffect(true) {
                scope.launch {
                    text = try {
                        val result = DogApi.createDefault().breeds()
                        Napier.d("JEP - result: $result")
                        result.toString()
                    } catch (e: Exception) {
                        e.message ?: "error"
                    }
                }
            }
            MainView(text)
        }
    }
}

@Composable
fun MainView(text: String) {
    LazyColumn {
        item {
            Text(text = text)
        }
    }
}
