package de.ellyrion.prefix.util

import de.ellyrion.ellyrionapi.util.EllyrionConfiguration
import de.ellyrion.prefix.util.tablist_scoreboard.McTablist
import org.bukkit.Bukkit
import org.jetbrains.exposed.sql.Database

object Util {

    fun reload() {
        Bukkit.getOnlinePlayers().forEach {
            McTablist.load(it, "Header", "Footer")
        }
        SqlLoader.load()
    }
}

object SqlLoader {

    private val config = EllyrionConfiguration.loadConfiguration("plugins/Prefix/sql.yml")

    fun load() {
        loadConfig()

        Database.connect(
            "jdbc:mysql://${config.getString("sql.ipv4")}:${config.getInt("sql.port")}/${config.getString("sql.database")}?autoReconnect=true",
            driver = "com.mysql.jdbc.Driver",
            user = config.getString("sql.user") + "",
            password = config.getString("sql.password") + ""
        )
    }

    fun loadConfig() {
        config.addDefault("sql.user", "root")
        config.addDefault("sql.port", 3306)
        config.addDefault("sql.ipv4", "localhost")
        config.addDefault("sql.database", "database")
        config.addDefault("sql.password", "")
        config.options().copyDefaults(true);
        config.save()
    }

}