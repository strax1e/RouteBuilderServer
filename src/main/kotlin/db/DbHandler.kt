package db

import db.entities.Road
import utils.ObjectMapper
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager

/**
 * Class which contains connection to db
 * @property[connection] the connection to db
 */
class DbHandler(
    url: String, driver: Driver,
    private val mapper: ObjectMapper
) : AutoCloseable {

    private val connection: Connection

    /**
     * Gets the roads of the selected country from db
     * @param[countryId] id of country whose roads are required
     * @return required data: collection of roads
     */
    fun getRoadsByCountryId(countryId: Short): Collection<Road> {
        connection.createStatement().use { state ->
            state.executeQuery("SELECT road FROM roads WHERE countryId = $countryId").use { resultSet ->
                val roads = ArrayList<Road>()
                while (resultSet.next()) roads += mapper.read(resultSet.getString("road"), Road::class.java)
                return roads
            }
        }
    }

    /**
     * Gets the towns of the selected country from db
     * @param[countryId] id of country whose towns are required
     * @return required data: map of id and name of towns
     */
    fun getTownsByCountryId(countryId: Short): Map<Short, String> {
        connection.createStatement().use { state ->
            state.executeQuery("SELECT * FROM towns WHERE countryId = $countryId").use { resultSet ->
                val towns = HashMap<Short, String>()
                while (resultSet.next()) towns[resultSet.getShort("id")] = resultSet.getString("name")
                return towns
            }
        }
    }

    /**
     * Gets map (id:name) of countries from db
     * @return required data: map of id and name of countries
     */
    fun getCountries(): Map<Short, String> {
        this.connection.createStatement().use { state ->
            state.executeQuery("SELECT * FROM countries").use { result ->
                val towns = HashMap<Short, String>()
                while (result.next()) towns[result.getShort("id")] = result.getString("name")
                return towns
            }
        }
    }

    /**
     * Inserts a [road] to db
     * @param[road] the road to be inserted to db
     */
    fun insert(road: Road) {
        connection.createStatement().use { state ->
            val mappedEntity = mapper.write(road)
            state.executeUpdate("INSERT INTO roads(countryId, road) VALUES(${road.country}, \'${mappedEntity}\')")
        }
    }

    /**
     * Inserts a [town] to db
     * @param[town] the town to be inserted to db
     * @param[countryId] id of country of town
     */
    fun insert(town: String, countryId: Short) {
        this.connection.createStatement().use { state ->
            state.executeUpdate("INSERT INTO towns(countryId, name) VALUES($countryId, \"$town\")")
        }
    }

    /**
     * Inserts a [country] to db
     * @param[country] the country to be inserted to db
     */
    fun insert(country: String) {
        connection.createStatement().use { state ->
            state.executeUpdate("INSERT INTO countries(name) VALUES($country)")
        }
    }

    /**
     * Closes connection to db
     */
    override fun close() = connection.close()

    init {
        DriverManager.registerDriver(driver)
        connection = DriverManager.getConnection(url)
    }
}
