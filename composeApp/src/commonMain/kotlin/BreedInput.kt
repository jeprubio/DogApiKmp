import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.delay

const val DEBOUNCE_TIME = 1_000L

@Composable
fun BreedInput(text: String, onValueChange: (String) -> Unit) {
    var inputText by remember { mutableStateOf(TextFieldValue(text)) }
    OutlinedTextField(
        value = inputText,
        onValueChange = { inputText = it },
        label = { Text("Enter a breed") }
    )
    LaunchedEffect(inputText) {
        delay(DEBOUNCE_TIME)
        onValueChange(inputText.text)
    }
}