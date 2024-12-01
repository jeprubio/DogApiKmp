import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun FilterWithResult(
    breed: String,
    text: String,
    modifier: Modifier = Modifier,
    onBreedChange: (String) -> Unit = {}
) {
    val bottomSpace = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues()
    Box(modifier = modifier.fillMaxWidth()) {
        var columnHeightPx by remember { mutableStateOf(0) }
        LazyColumn(modifier = Modifier.fillMaxSize()
            .padding(horizontal = 16.dp)
        ) {
            item {
                val columnHeightDp = with(LocalDensity.current) { columnHeightPx.toDp() }
                Spacer(modifier = Modifier.height(columnHeightDp + 16.dp))
            }
            item {
                Text(text)
                Spacer(modifier = Modifier.height(bottomSpace.calculateBottomPadding()))
            }
        }
        Column(modifier = Modifier.fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.90f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = columnHeightPx.toFloat() + 16,
                )
            )
            .onGloballyPositioned { coordinates ->
                columnHeightPx = coordinates.size.height
            }, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            BreedInput(breed, onValueChange = onBreedChange)
        }
    }
}
