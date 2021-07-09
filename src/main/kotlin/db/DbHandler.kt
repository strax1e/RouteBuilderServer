package db

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import db.entities.Road
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager

/**
 * Class which contains connection to db
 * @property[connection] the connection to db
 */
class DbHandler(url: String, driver: Driver) : AutoCloseable {

    /**
     * Gets the roads of the selected country from db
     * @param[countryId] id of country whose roads are required
     * @return required data: collection of roads
     */
    fun getRoadsByCountryId(countryId: Short): Collection<Road> {
        this.connection.createStatement().use { state ->
            val mapper = jacksonObjectMapper()
            state.executeQuery("select road from roads where countryId = $countryId").use { resultSet ->
                val roads = ArrayList<Road>()
                while (resultSet.next()) roads += mapper.readValue<Road>(resultSet.getString("road"))
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
        this.connection.createStatement().use { state ->
            state.executeQuery("select * from towns where countryId = $countryId").use { resultSet ->
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
            state.executeQuery("select * from countries").use { result ->
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
        this.connection.createStatement().use { state ->
            val mapper = jacksonObjectMapper()
            val json = mapper.writeValueAsString(road)
            state.executeUpdate("insert into roads(countryId, road) values(${road.country}, \'${json}\')")
        }
    }

    /**
     * Inserts a [town] to db
     * @param[town] the town to be inserted to db
     * @param[countryId] id of country of town
     */
    fun insert(town: String, countryId: Short) {
        this.connection.createStatement().use { state ->
            state.executeUpdate("insert into towns(countryId, name) values($countryId, \"$town\")")
        }
    }

    /**
     * Inserts a [country] to db
     * @param[country] the country to be inserted to db
     */
    fun insert(country: String) {
        this.connection.createStatement().use { state ->
            state.executeUpdate("insert into countries(name) values(\"$country\")")
        }
    }

    /**
     * Closes connection to db
     */
    override fun close() = this.connection.close()

    private val connection: Connection

    init {
        DriverManager.registerDriver(driver)
        this.connection = DriverManager.getConnection(url)
    }
}
