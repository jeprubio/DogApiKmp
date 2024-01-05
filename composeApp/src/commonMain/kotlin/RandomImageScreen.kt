import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.rumosoft.librarydogapi.DogApi
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch

class RandomImageScreen(val modifier: Modifier = Modifier) : Screen {
    @Composable
    override fun Content() {
            var breed by remember { mutableStateOf("") }
            val scope = rememberCoroutineScope()
            var text by remember { mutableStateOf("Loading") }
            LaunchedEffect(breed) {
                scope.launch {
                    text = try {
                        val dogApi = DogApi.createDefault()
                        val result = if (breed.isEmpty()) dogApi.randomImage() else dogApi.randomImage(breed)
                        Napier.d("JEP - result: $result")
                        result.toString()
                    } catch (e: Exception) {
                        e.message ?: "error"
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = modifier.fillMaxSize().padding(16.dp),
            ) {
                BreedInput(breed, onValueChange = { breed = it })
                PrintStringInfo(text)
            }
    }
}

