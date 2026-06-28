import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface AppRoute : NavKey

@Serializable
data object MainRoute : AppRoute

@Serializable
data object ListAllBreedsRoute : AppRoute

@Serializable
data object RandomImageRoute : AppRoute

@Serializable
data object BreedImagesRoute : AppRoute

@Serializable
data object ListSubBreedsRoute : AppRoute

val appRouteConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(MainRoute::class, MainRoute.serializer())
            subclass(ListAllBreedsRoute::class, ListAllBreedsRoute.serializer())
            subclass(RandomImageRoute::class, RandomImageRoute.serializer())
            subclass(BreedImagesRoute::class, BreedImagesRoute.serializer())
            subclass(ListSubBreedsRoute::class, ListSubBreedsRoute.serializer())
        }
    }
}
