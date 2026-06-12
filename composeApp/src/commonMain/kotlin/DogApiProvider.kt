import com.rumosoft.librarydogapi.DogApi
import com.rumosoft.librarydogapi.DogApiClient
import com.rumosoft.librarydogapi.DogApiLogger
import io.github.aakira.napier.Napier

/**
 * Creates a [DogApiLogger] that forwards messages to Napier.
 */
fun napierLogger(): DogApiLogger = object : DogApiLogger {
    override fun d(msg: String) {
        Napier.d(msg, tag = "DogApi")
    }

    override fun e(msg: String, throwable: Throwable?) {
        Napier.e(msg, throwable, tag = "DogApi")
    }
}

/**
 * Creates a [DogApiClient] with Napier logging enabled.
 */
fun createDogApiWithLogging(): DogApiClient =
    DogApi.createDefault(logger = napierLogger())

