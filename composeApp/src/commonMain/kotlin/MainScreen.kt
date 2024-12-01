import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator

class MainScreen(private val modifier: Modifier = Modifier) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Dog Api")
                    },
                )
            },
            modifier = modifier
        ) { paddingValues ->
            MainScreenContent(
                padding = paddingValues,
                onNewScreenSelected = { navigator?.push(it) }
            )
        }
    }
}

@Composable
internal fun MainScreenContent(
    padding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    onNewScreenSelected: (Screen) -> Unit = {}
) {
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier.fillMaxSize().padding(padding)
    ) {
        item {
            Button(onClick = { onNewScreenSelected(ListAllBreedsScreen()) }) {
                Text(text = "List All Breeds")
            }
        }
        item {
            Button(onClick = { onNewScreenSelected(RandomImageScreen()) }) {
                Text(text = "Random Image")
            }
        }
        item {
            Button(onClick = { onNewScreenSelected(BreedImagesScreen()) }) {
                Text(text = "Breed Images")
            }
        }
        item {
            Button(onClick = { onNewScreenSelected(ListSubBreedsScreen()) }) {
                Text(text = "List Sub Breeds")
            }
        }
    }
}
