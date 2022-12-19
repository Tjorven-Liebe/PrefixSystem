package de.ellyrion.prefix.command

import de.ellyrion.ellyrionapi.util.AbstractCommand
import de.ellyrion.prefix.PrefixSystem
import java.io.InputStreamReader
import java.util.regex.Pattern

class PrefixSetup : AbstractCommand(
    "plugins/Prefix/messages.yml", "prefixsetup.prefix.set",
    InputStreamReader(PrefixSystem.instance().getResource("messages.yml"))
) {
    override fun run(label: String?, vararg args: String?): Boolean {
        if (asPlayer.hasPermission("prefixsetup.use")) {
            asPlayer.sendMessage(noPerm)
            return true
        }
        if (args.isNotEmpty())
            when (args[0]?.lowercase()) {
                "reload" -> {
                    TODO("Reload configs and sql")
                }
                "group" -> {
                    if (args.size > 1) {
                        if (args[1].equals("list", true)) {
                            TODO("List all groups from sql")
                        }
                        if (args.size > 2) {
                            val groupName = args[2]?.lowercase()
                            when (args[3]?.lowercase()) {
                                "info" -> {
                                    TODO("List all info of the group")
                                }

                                "set" -> {
                                    when (args[4]?.lowercase()) {
                                        "prefix" -> {
                                            TODO("Set prefix of the group")
                                        }

                                        "suffix" -> {
                                            TODO("Set suffix of the group")
                                        }
                                    }
                                }

                                "get" -> {
                                    when (args[4]?.lowercase()) {
                                        "prefix" -> {
                                            TODO("Get prefix of the group -> Send player")
                                        }
                                        "suffix" -> {
                                            TODO("Get suffix of the group -> Send player")
                                        }
                                    }
                                }

                                "remove" -> {
                                    TODO("remove the group")
                                }
                            }
                        }
                    }
                }

                "regex" -> {
                    if (args.size > 1) {
                        when (args[1]?.lowercase()) {
                            "chat" -> {
                                TODO("Regex for chat")
                            }

                            "tab" -> {
                                TODO("Regex for Tab")
                            }

                            "header" -> {
                                TODO("Text in header")
                            }

                            "footer" -> {
                                TODO("Text in footer")
                            }
                        }
                    }
                }
            }
        helpMessage(
            "prefixsetup reload",
            "prefixsetup group list",
            "prefixsetup group <name> info",
            "prefixsetup group <name> set <prefix|suffix> <value>",
            "prefixsetup group <name> get <prefix|suffix>",
            "prefixsetup group <name> remove",
            "§8§m                                            ",
            "/n wird zu Zeilenumbruch",
            "%player% wird zum Spielernamen",
            "%prefix% wird zum Prefix vom Spieler",
            "%suffix% wird zum Suffix vom Spieler",
            "PlaceholderApi-Support ist eingebaut",
            "\"\" erlaubt es Leerzeichen zu schreiben (Multi-Regex)",
            "prefixsetup regex <chat|tab|header|footer> <value>"
        )
        return false
    }

    override fun complete(label: String?, vararg args: String?): MutableList<String> {
        return arrayListOf()
    }

    private fun helpMessage(vararg messages: String) {
        asPlayer.sendMessage("§8§m                   §r§8[ §6Help §8]§m                   ")
        messages.forEach {
            asPlayer.sendMessage("§b $it")
        }
        asPlayer.sendMessage("§8§m                   §r§8[ §6Help §8]§m                   ")
    }

    fun generateStringFromRegex(vararg args: String?): Array<String?> {
        val text = StringBuilder()
        args.forEach {
            text.append(it).append(" ")
        }
        text.delete(text.length - 1, text.length)
        val pattern = Pattern.compile("(\".*?\")")
        val matcher = pattern.matcher(text)
        val argsNew = arrayOfNulls<String>(2)
        var i = 0
        while (matcher.find()) {
            argsNew[i] = matcher.group(i).replace("\"", "")
            i++
        }
        return argsNew
    }
}