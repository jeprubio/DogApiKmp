import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class BreedInputTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun checkInputVisible() = runComposeUiTest {
        setContent {
            RandomImageScreenContent(PaddingValues(8.dp), "breed", "subBreed", onBreedChange = { })
        }

        onNodeWithText("Enter a breed").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun checkBreedChange() = runComposeUiTest {
        val breedFlow = MutableStateFlow("breed")
        setContent {
            val breed = remember { breedFlow }
            BreedInput(
                breed.value,
                onValueChange = {
                    breedFlow.value = it
                }
            )
        }

        onNodeWithText("Enter a breed")
            .assertIsDisplayed()
            .performTextClearance()
        onNodeWithText("Enter a breed").performTextInput("newBreed")
        waitUntil(timeoutMillis = DEBOUNCE_TIME + 100) { breedFlow.value == "newBreed" }
        assertEquals("newBreed", breedFlow.value)
    }
}