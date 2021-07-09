import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import db.DbHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.sql.Driver
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * Class of server
 */
class Server(port: Int, dbUrl: String, dbDriver: Driver) : AutoCloseable {

    /**
     * Starts server
     */
    fun start() {
        this.socket.use {
            this.dbHandler.use {
                // Main loop which serves clients
                while (true) {
                    val clientSocket = this.socket.accept() // waiting for connections
                    newLog("Client is connected", clientSocket)

                    CoroutineScope(Dispatchers.IO).launch {
                        clientSocket.use {
                            serveClient(clientSocket)
                        }
                    }
                }
            }
        }
    }

    /**
     * Function that serves client: pushes response
     * @param[clientSocket] client socket
     */
    private fun serveClient(clientSocket: Socket) {
        try {
            val writer = PrintWriter(OutputStreamWriter(clientSocket.getOutputStream()), true)
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            while (!clientSocket.isClosed) {
                val request: String = reader.readLine()
                newLog("Got: $request", clientSocket)

                val response = getResponse(request, clientSocket)
                writer.println(response)
                newLog("Sent: $response", clientSocket)

                if (response == "finish") {
                    newLog("Closing connection.", clientSocket)
                    clientSocket.close()
                }
            }
        } catch (e: Exception) {
            newLog("Exception: ${e.message}. Closing connection.", clientSocket)
        } finally {
            clientSocket.close()
            newLog("The connection is closed", clientSocket)
        }
    }

    /**
     * Function that gets response from db.
     * @param[request] the request from client
     * @return required data from database.
     * If command is finish returns "finish" and signals to close connection.
     * If command is unknown returns "unknown command"
     */
    private fun getResponse(request: String, clientSocket: Socket): String {
        return try {
            when {
                this.getCountriesRegex.matcher(request)
                    .matches() -> this.jsonMapper.writeValueAsString(this.dbHandler.getCountries())
                this.getRoadsRegex.matcher(request).matches() -> {
                    val countryId = request.substringAfterLast("get roads ").toShort()
                    this.jsonMapper.writeValueAsString(this.dbHandler.getRoadsByCountryId(countryId))
                }
                this.getTownsRegex.matcher(request).matches() -> {
                    val countryId = request.substringAfterLast("get towns ").toShort()
                    this.jsonMapper.writeValueAsString(this.dbHandler.getTownsByCountryId(countryId))
                }
                this.finishRegex.matcher(request).matches() -> "finish"
                else -> "unknown command"
            }
        } catch (e: SQLException) {
            newLog("getResponse() exception: ${e.message}", clientSocket)
            "error: no such table or db is unavailable"
        }
    }

    /**
     * Makes new log in output stream
     * @param[stream] output stream
     */
    private fun newLog(message: String, clientSocket: Socket, stream: OutputStream = System.out) {
        val writer = BufferedWriter(OutputStreamWriter(stream))
        writer.write("${getCurrentTime()}: ${clientSocket.remoteSocketAddress}: $message")
        writer.newLine()
        writer.flush()
    }

    /**
     * Gets a current time for logging
     * @return current time. "dd.MM.yy HH:mm:ss" is a format
     */
    private fun getCurrentTime(): String = SimpleDateFormat("dd.MM.yy HH:mm:ss").format(GregorianCalendar().time)

    /**
     * Closes socket and db connections
     */
    override fun close() {
        this.dbHandler.close()
        this.socket.close()
    }

    // Database handler
    private val dbHandler by lazy { DbHandler(dbUrl, dbDriver) }

    // Server socket
    private val socket by lazy { ServerSocket(port) }

    private val jsonMapper = jacksonObjectMapper()

    // regexes for commands
    private val getCountriesRegex = Pattern.compile("^get countries$")
    private val getRoadsRegex = Pattern.compile("^get roads \\d+$")
    private val getTownsRegex = Pattern.compile("^get towns \\d+$")
    private val finishRegex = Pattern.compile("^finish$")
}
