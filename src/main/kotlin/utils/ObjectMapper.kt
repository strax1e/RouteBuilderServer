package utils

abstract class ObjectMapper {

    abstract fun write(obj: Any): String

    abstract fun <T> read(string: String, aClass: Class<T>): T
}
