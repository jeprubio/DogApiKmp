import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigate: (AppRoute) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Dog Api") },
            )
        },
        modifier = modifier
    ) { paddingValues ->
        MainScreenContent(
            padding = paddingValues,
            onNewScreenSelected = onNavigate
        )
    }
}

@Composable
internal fun MainScreenContent(
    padding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    onNewScreenSelected: (AppRoute) -> Unit = {}
) {
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier.fillMaxSize().padding(padding)
    ) {
        item {
            Button(onClick = { onNewScreenSelected(ListAllBreedsRoute) }) {
                Text(text = "List All Breeds")
            }
        }
        item {
            Button(onClick = { onNewScreenSelected(RandomImageRoute) }) {
                Text(text = "Random Image")
            }
        }
        item {
            Button(onClick = { onNewScreenSelected(BreedImagesRoute) }) {
                Text(text = "Breed Images")
            }
        }
        item {
            Button(onClick = { onNewScreenSelected(ListSubBreedsRoute) }) {
                Text(text = "List Sub Breeds")
            }
        }
    }
}
