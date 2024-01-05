import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PrintStringInfo(text: String, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        item {
            Text(text = text)
        }
    }
}