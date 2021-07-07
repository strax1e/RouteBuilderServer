import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.sql.SQLException

fun main(args: Array<String>) {
    val dbHandler = DbHandler("jdbc:sqlite:${args[0]}", org.sqlite.JDBC()) // creating a db connection
    val serverSocket = ServerSocket(8888) // creating socket

    // Main loop which serves clients
    while (!serverSocket.isClosed) {
        val clientSocket: Socket = serverSocket.accept() // waiting for connections
        println("${clientSocket.inetAddress}:${clientSocket.port} is connected")

        CoroutineScope(Dispatchers.IO).launch {
            serveClient(clientSocket, dbHandler)
        }
    }

    dbHandler.close()
}

/**
 * Function that serves client: pushes response
 * @param[clientSocket] client socket
 * @param[dbHandler] database handler
 */
fun serveClient(clientSocket: Socket, dbHandler: DbHandler) {
    try {
        val writer = OutputStreamWriter(clientSocket.getOutputStream())
        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

        val request: String = reader.readLine()
        println("Got: $request")

        val response = getResponse(request, dbHandler)
        writer.write(response)
        writer.flush()
        println("Sent: $response")
    } catch (e: Exception) {
        println("Exception: ${e.message}. Closing connection.")
    } finally {
        clientSocket.close()
        println("The connection to ${clientSocket.inetAddress}:${clientSocket.port} is closed")
    }
}

/**
 * Function that gets response from db
 * @param[request] the request from client
 * @param[dbHandler] database handler
 * @return required data from database
 */
fun getResponse(request: String, dbHandler: DbHandler): String {
    return try {
        if (!request.startsWith("select")) throw SQLException("Not a select query")
        dbHandler.getData(request)
    } catch (e: SQLException) {
        println("getResponse() Exception: ${e.message}")
        "err"
    }
}
