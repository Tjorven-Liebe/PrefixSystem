package de.ellyrion.prefix.util.tablist_scoreboard

import de.ellyrion.prefix.util.ReflectionUtils
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.lang.reflect.Field
import java.lang.reflect.Method


object McTablist {

    val version = Bukkit.getServer().javaClass.`package`.name.replace("org.bukkit.craftbukkit.", "")
    fun load(player:Player, header:String, footer:String) {
        if (equals("19", "18", "17", "16", "15", "14", "13")) {
            player.setPlayerListHeaderFooter(header, footer)
            return
        }
        println("no")

        val iChatBaseComponent = Class.forName("net.minecraft.server.$version.IChatBaseComponent")
        println(iChatBaseComponent.`package`)
        val chatSerializer = iChatBaseComponent.declaredClasses[0]
        val packetOutHeaderFooter =
            Class.forName("net.minecraft.server.$version.PacketPlayOutPlayerListHeaderFooter")
        val a: Method = chatSerializer.getMethod("a", String::class.java)
        val header = ChatColor.translateAlternateColorCodes('&', header)
        val footer = ChatColor.translateAlternateColorCodes('&', footer)
        val chatBaseCompHeader: Any = a.invoke(null, if (header.startsWith("{")) header else "{\"text\":\"$header\"}")
        val chatBaseCompFooter: Any = a.invoke(null, if (footer.startsWith("{")) footer else "{\"text\":\"$footer\"}")
        val headerFooterPacket =
            packetOutHeaderFooter.getDeclaredConstructor(iChatBaseComponent).newInstance(chatBaseCompHeader)
        setDeclaredField(headerFooterPacket, "b", chatBaseCompFooter)
        ReflectionUtils.sendPacket(player, headerFooterPacket)
    }

    fun equals(vararg string:String):Boolean {
        string.forEach {
            if (version.contains(it, true))
                return true
        }
        return false
    }

    fun setDeclaredField(o: Any, name: String?, value: Any?) {
        try {
            val f: Field = o.javaClass.getDeclaredField(name)
            f.setAccessible(true)
            f.set(o, value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}