import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.rumosoft.librarydogapi.DogApi
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch

class BreedImagesScreen(val modifier: Modifier = Modifier) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        var breed by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        var text by remember { mutableStateOf("Loading") }
        LaunchedEffect(breed) {
            scope.launch {
                text = try {
                    if (breed.isNotEmpty()) {
                        val result = DogApi.createDefault().breedImages(breed)
                        Napier.d("JEP - result: $result")
                        result.toString()
                    } else {
                        "Please enter a breed"
                    }
                } catch (e: Exception) {
                    e.message ?: "error"
                }
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Breed Images Screen") },
                    navigationIcon = {
                        IconButton(onClick = { navigator?.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    modifier = Modifier.statusBarsPadding(),
                )
            }
        ) { padding ->
            FilterWithResult(breed, text, Modifier.padding(padding), onBreedChange = { breed = it })
        }
    }
}
