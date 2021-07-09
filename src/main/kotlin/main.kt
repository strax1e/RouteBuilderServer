fun main(args: Array<String>) {
    Server(8888, "jdbc:sqlite:${args[0]}", org.sqlite.JDBC()).use { server ->
        server.start()
    }
}
