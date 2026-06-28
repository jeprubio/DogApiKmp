import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay

@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val backStack = rememberNavBackStack(appRouteConfig, MainRoute)
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<MainRoute> {
                        MainScreen(onNavigate = { route -> backStack.add(route) })
                    }
                    entry<ListAllBreedsRoute> {
                        ListAllBreedsScreen(onBack = { backStack.removeLastOrNull() })
                    }
                    entry<RandomImageRoute> {
                        RandomImageScreen(onBack = { backStack.removeLastOrNull() })
                    }
                    entry<BreedImagesRoute> {
                        BreedImagesScreen(onBack = { backStack.removeLastOrNull() })
                    }
                    entry<ListSubBreedsRoute> {
                        ListSubBreedsScreen(onBack = { backStack.removeLastOrNull() })
                    }
                }
            )
        }
    }
}
