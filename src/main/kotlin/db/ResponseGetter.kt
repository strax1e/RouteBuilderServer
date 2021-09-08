package db

import utils.ObjectMapper
import java.util.regex.Pattern

class ResponseGetter(
    private val dbHandler: DbHandler,
    private val mapper: ObjectMapper
) : AutoCloseable {

    private val commandGetCountriesRegex = Pattern.compile("^get countries$")
    private val commandGetRoadsRegex = Pattern.compile("^get roads \\d+$")
    private val commandGetTownsRegex = Pattern.compile("^get towns \\d+$")
    private val commandFinishRegex = Pattern.compile("^finish$")

    fun getResponse(request: String): String {
        return when {
            commandGetCountriesRegex.matcher(request).matches() -> {
                mapper.write(dbHandler.getCountries())
            }

            commandGetRoadsRegex.matcher(request).matches() -> {
                val countryId = request.substringAfterLast("get roads ").toShort()
                mapper.write(dbHandler.getRoadsByCountryId(countryId))
            }

            commandGetTownsRegex.matcher(request).matches() -> {
                val countryId = request.substringAfterLast("get towns ").toShort()
                mapper.write(dbHandler.getTownsByCountryId(countryId))
            }

            commandFinishRegex.matcher(request).matches() -> "finish"

            else -> "unknown command"
        }
    }

    override fun close() {
        dbHandler.close()
    }
}
