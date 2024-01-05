import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import com.rumosoft.librarydogapi.DogApi
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch

class ListAllBreedsScreen(val modifier: Modifier = Modifier) : Screen {
    @Composable
    override fun Content() {

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
            PrintStringInfo(text, modifier = modifier)
    }
}

