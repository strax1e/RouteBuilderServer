import server.Server
import utils.JsonMapper
import utils.ObjectMapper
import java.sql.Driver

fun main(args: Array<String>) {
    val mapper: ObjectMapper = JsonMapper()
    val dbDriver: Driver = org.sqlite.JDBC()
    val port = 8888
    val dbUrl = "jdbc:sqlite:${args[0]}"

    Server(port, dbUrl, dbDriver, mapper).use { server ->
        server.start()
    }
}
