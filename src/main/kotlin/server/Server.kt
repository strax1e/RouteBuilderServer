package server

import db.DbHandler
import db.ResponseGetter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import utils.Logger
import utils.ObjectMapper
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.sql.Driver
import java.sql.SQLException

class Server(
    port: Int,
    dbUrl: String,
    dbDriver: Driver,
    private val mapper: ObjectMapper,
    private var logger: Logger = Logger(System.out)
) : AutoCloseable {

    private val socket by lazy { ServerSocket(port) }
    private val responseGetter by lazy { ResponseGetter(DbHandler(dbUrl, dbDriver, mapper), mapper) }

    fun start() {
        socket.use { serverSocket ->
            responseGetter.use {

                // Main loop which serves clients
                while (true) {
                    val clientSocket = serverSocket.accept() // waiting for connections
                    logger.newLog("${clientSocket}: Client is connected")

                    asyncServeClient(clientSocket)
                }
            }
        }
    }

    private fun asyncServeClient(clientSocket: Socket) {
        CoroutineScope(Dispatchers.IO).launch {
            clientSocket.use {
                serveClient(clientSocket)
            }
        }
    }

    /**
     * Function that serves client: pushes response
     * @param[clientSocket] client socket
     */
    private fun serveClient(clientSocket: Socket) {
        try {
            tryServeClient(clientSocket)
        } catch (e: IOException) {
            logger.newLog("${clientSocket}: Exception: ${e.message}. Closing connection.")
        } finally {
            clientSocket.close()
            logger.newLog("${clientSocket}: The connection is closed")
        }
    }

    private fun tryServeClient(clientSocket: Socket) {
        while (!clientSocket.isClosed) {
            val request = getRequest(clientSocket)

            val response = getResponseFromDb(request)
            sendResponse(response, clientSocket)

            closeConnectionIfItsRequiredByRequest(request, clientSocket)
        }
    }

    private fun getRequest(clientSocket: Socket): String {
        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
        val request = reader.readLine()
        logger.newLog("${clientSocket}: Got: $request")

        return request
    }

    /**
     * Function that gets response from db.
     * @param[request] the request from client
     * @return required data from database.
     * If command is finish returns "finish" and signals to close connection.
     * If command is unknown returns "unknown command"
     */
    private fun getResponseFromDb(request: String): String {
        return try {
            responseGetter.getResponse(request)
        } catch (e: SQLException) {
            logger.newLog("getResponse() exception: ${e.message}")
            "error: no such table or db is unavailable"
        }
    }

    private fun sendResponse(response: String, clientSocket: Socket) {
        val writer = PrintWriter(OutputStreamWriter(clientSocket.getOutputStream()), true)
        writer.println(response)
        logger.newLog("${clientSocket}: Sent: $response")
    }

    private fun closeConnectionIfItsRequiredByRequest(request: String, clientSocket: Socket) {
        if (request == "finish") {
            logger.newLog("${clientSocket}: Closing connection.")
            clientSocket.close()
        }
    }

    /**
     * Closes socket and db connections
     */
    override fun close() {
        this.responseGetter.close()
        this.socket.close()
    }
}
