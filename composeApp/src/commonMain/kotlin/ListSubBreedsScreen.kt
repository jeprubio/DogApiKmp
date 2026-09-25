import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rumosoft.librarydogapi.DogApiClient
import com.rumosoft.librarydogapi.DogApiError
import dogapikmp.composeapp.generated.resources.Res
import dogapikmp.composeapp.generated.resources.ic_arrow_back
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSubBreedsScreen(
    onBack: () -> Unit = {},
    dogApi: DogApiClient = createDogApiWithLogging(),
    modifier: Modifier = Modifier
) {
    var breed by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("Please enter a breed") }
    LaunchedEffect(breed) {
        val query = breed.trim()
        text = if (query.isEmpty()) {
            "Please enter a breed"
        } else {
            text = "Loading..."
            try {
                val subBreeds = dogApi.listSubBreeds(query)
                Napier.d("result: $subBreeds")
                if (subBreeds.isEmpty()) {
                    "No sub-breeds found for '$query'"
                } else {
                    subBreeds.joinToString(", ")
                }
            } catch (error: DogApiError) {
                error.message ?: "error"
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("List Sub Breeds") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
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
            onBreedChange = { breed = it }
        )
    }
}
