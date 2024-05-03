import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

class MainScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun checkElementsVisible() = runComposeUiTest {
        setContent {
            MainScreenContent()
        }

        onNodeWithText("List All Breeds").assertIsDisplayed()
        onNodeWithText("Random Image").assertIsDisplayed()
        onNodeWithText("Breed Images").assertIsDisplayed()
        onNodeWithText("List Sub Breeds").assertIsDisplayed()
    }
}