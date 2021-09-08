package utils

import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

class Logger(private val outputStream: OutputStream) {

    fun newLog(message: String) {
        val writer = PrintWriter(BufferedWriter(OutputStreamWriter(outputStream)), true)
        writer.println("${getCurrentTime()}: $message")
    }

    private fun getCurrentTime(): String = SimpleDateFormat("dd.MM.yy HH:mm:ss").format(GregorianCalendar().time)
}
