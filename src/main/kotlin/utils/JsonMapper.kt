package utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class JsonMapper : ObjectMapper() {

    private val mapper = jacksonObjectMapper()

    override fun <T> read(string: String, aClass: Class<T>): T {
        return mapper.readValue(string, aClass)
    }

    override fun write(obj: Any): String {
        return mapper.writeValueAsString(obj)
    }
}
