package de.ellyrion.prefix

import de.ellyrion.ellyrionapi.util.AbstractCommand
import de.ellyrion.prefix.command.PrefixSetup
import de.ellyrion.prefix.util.Util
import org.bukkit.plugin.java.JavaPlugin

class PrefixSystem : JavaPlugin() {

    companion object {
        lateinit var instance:JavaPlugin;
        fun instance():JavaPlugin {
            return instance;
        }
    }

    override fun onEnable() {
        instance = this;
        Util.reload()
        register("prefixsetup", PrefixSetup())
        super.onEnable()
    }

    fun register(command:String, abstractCommand: AbstractCommand) {
        getCommand(command)?.setExecutor(abstractCommand)
        getCommand(command)?.setTabCompleter(abstractCommand)
    }
}