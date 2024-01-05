import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator

class MainScreen(private val modifier: Modifier = Modifier) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Dog Api")
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                contentPadding = PaddingValues(16.dp),
                modifier = modifier.fillMaxSize().padding(padding).padding(16.dp)
            ) {
                item {
                    Button(onClick = { navigator?.push(ListAllBreedsScreen()) }) {
                        Text(text = "List All Breeds")
                    }
                }
                item {
                    Button(onClick = { navigator?.push(RandomImageScreen()) }) {
                        Text(text = "Random Image")
                    }
                }
                item {
                    Button(onClick = { navigator?.push(BreedImagesScreen()) }) {
                        Text(text = "Breed Images")
                    }
                }
                item {
                    Button(onClick = { navigator?.push(ListSubBreedsScreen()) }) {
                        Text(text = "List Sub Breeds")
                    }
                }
            }
        }
    }
}

