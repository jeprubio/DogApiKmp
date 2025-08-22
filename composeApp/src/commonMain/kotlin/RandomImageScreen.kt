import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
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
import kotlin.coroutines.cancellation.CancellationException

class RandomImageScreen(val modifier: Modifier = Modifier) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        var breed by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        var text by remember { mutableStateOf("Loading") }
        LaunchedEffect(breed) {
            scope.launch {
                text = try {
                    val dogApi = DogApi.createDefault()
                    val result =
                        if (breed.isEmpty()) dogApi.randomImage() else dogApi.randomImage(breed)
                    Napier.d("JEP - result: $result")
                    result.toString()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e.message ?: "error"
                }
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Random Image") },
                    navigationIcon = {
                        IconButton(onClick = { navigator?.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    modifier = Modifier.statusBarsPadding(),
                )
            },
            modifier = modifier,
        ) { padding ->
            FilterWithResult(
                breed = breed,
                text = text,
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
                onBreedChange = { breed = it })
        }
    }
}
