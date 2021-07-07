import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager

/**
 * Class which contains connection to db
 * @property[connection] the connection to db
 */
class DbHandler(url: String, driver: Driver) {

    /**
     * Gets data from database
     * @param[request] query from user
     * @return required data
     */
    fun getData(request: String): String {
        val state = this.connection.createStatement()
        var data = ""
        val result = state.executeQuery(request)
        while (result.next()) {
            data += result.getString("nodeA") + '&' + result.getString("nodeB") + '&' + result.getString("distance") + '#'
        }
        result.close()
        state.close()
        return data
    }

    fun close() = this.connection.close()

    private val connection: Connection

    init {
        DriverManager.registerDriver(driver)
        this.connection = DriverManager.getConnection(url)
    }
}
