import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator

class MainScreen(private val modifier: Modifier = Modifier) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier.fillMaxSize()
        ) {
            item {
                Button(onClick = { navigator?.push(ListAllBreedsScreen())}) {
                    Text(text = "List All Breeds")
                }
            }
        }
    }
}

