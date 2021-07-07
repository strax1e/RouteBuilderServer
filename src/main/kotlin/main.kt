import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.io.BufferedWriter
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.GregorianCalendar

fun main(args: Array<String>) {
    val dbHandler = DbHandler("jdbc:sqlite:${args[0]}", org.sqlite.JDBC()) // creating a db connection
    val serverSocket = ServerSocket(8888) // creating socket

    // Main loop which serves clients
    while (!serverSocket.isClosed) {
        val clientSocket: Socket = serverSocket.accept() // waiting for connections
        newLog("Client is connected", clientSocket)

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
        newLog("Got: $request", clientSocket)

        val response = getResponse(request, dbHandler, clientSocket)
        writer.write(response)
        writer.flush()
        newLog("Sent: $response", clientSocket)
    } catch (e: Exception) {
        newLog("Exception: ${e.message}. Closing connection.", clientSocket)
    } finally {
        clientSocket.close()
        newLog("The connection is closed", clientSocket)
    }
}

/**
 * Function that gets response from db
 * @param[request] the request from client
 * @param[dbHandler] database handler
 * @return required data from database
 */
fun getResponse(request: String, dbHandler: DbHandler, clientSocket: Socket): String {
    return try {
        if (!request.startsWith("select")) throw SQLException("Not a select query")
        dbHandler.getData(request)
    } catch (e: SQLException) {
        newLog("getResponse() exception: ${e.message}", clientSocket)
        "err"
    }
}

/**
 * Makes new log in output stream
 * @param[stream] output stream
 */
fun newLog(message: String, clientSocket: Socket, stream: OutputStream = System.out) {
    val writer = BufferedWriter(OutputStreamWriter(stream))
    writer.write("${getCurrentTime()}: ${clientSocket.remoteSocketAddress}: $message")
    writer.newLine()
    writer.flush()
}

/**
 * Gets a current time for logging
 * @return current time. "dd.MM.yy HH:mm:ss" is a format
 */
fun getCurrentTime(): String = SimpleDateFormat("dd.MM.yy HH:mm:ss").format(GregorianCalendar().time)
