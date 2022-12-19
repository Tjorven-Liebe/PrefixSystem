package de.ellyrion.prefix.util

import org.bukkit.entity.Player
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture


object ReflectionUtils {
    /**
     * We use reflection mainly to avoid writing a new class for version barrier.
     * The version barrier is for NMS that uses the Minecraft version as the main package name.
     *
     *
     * E.g. EntityPlayer in 1.15 is in the class `net.minecraft.server.v1_15_R1`
     * but in 1.14 it's in `net.minecraft.server.v1_14_R1`
     * In order to maintain cross-version compatibility we cannot import these classes.
     *
     *
     * Performance is not a concern for these specific statically initialized values.
     */
    var VERSION: String? = null

    init { // This needs to be right below VERSION because of initialization order.
        // This package loop is used to avoid implementation-dependant strings like Bukkit.getVersion() or Bukkit.getBukkitVersion()
        // which allows easier testing as well.
        var found: String? = null
        for (pack in Package.getPackages()) {
            val name = pack.name

            // .v because there are other packages.
            if (name.startsWith("org.bukkit.craftbukkit.v")) {
                found = pack.name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[3]

                // Just a final guard to make sure it finds this important class.
                // As a protection for forge+bukkit implementation that tend to mix versions.
                // The real CraftPlayer should exist in the package.
                // Note: Doesn't seem to function properly. Will need to separate the version
                // handler for NMS and CraftBukkit for softwares like catmc.
                found = try {
                    Class.forName("org.bukkit.craftbukkit.$found.entity.CraftPlayer")
                    break
                } catch (e: ClassNotFoundException) {
                    null
                }
            }
        }
        requireNotNull(found) { "Failed to parse server version. Could not find any package starting with name: 'org.bukkit.craftbukkit.v'" }
        VERSION = found
    }

    /**
     * The raw minor version number.
     * E.g. `v1_17_R1` to `17`
     *
     * @since 4.0.0
     */
    val VER = VERSION!!.substring(1).split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].toInt()

    /**
     * Mojang remapped their NMS in 1.17 https://www.spigotmc.org/threads/spigot-bungeecord-1-17.510208/#post-4184317
     */
    val CRAFTBUKKIT = "org.bukkit.craftbukkit." + VERSION + '.'
    val NMS = v(17, "net.minecraft.").orElse("net.minecraft.server." + VERSION + '.')

    /**
     * A nullable public accessible field only available in `EntityPlayer`.
     * This can be null if the player is offline.
     */
    private var PLAYER_CONNECTION: MethodHandle? = null

    /**
     * Responsible for getting the NMS handler `EntityPlayer` object for the player.
     * `CraftPlayer` is simply a wrapper for `EntityPlayer`.
     * Used mainly for handling packet related operations.
     *
     *
     * This is also where the famous player `ping` field comes from!
     */
    private var GET_HANDLE: MethodHandle? = null

    /**
     * Sends a packet to the player's client through a `NetworkManager` which
     * is where `ProtocolLib` controls packets by injecting channels!
     */
    private var SEND_PACKET: MethodHandle? = null

    init {
        val entityPlayer = getNMSClass("server.level", "EntityPlayer")
        val craftPlayer = getCraftClass("entity.CraftPlayer")
        val playerConnection = getNMSClass("server.network", "PlayerConnection")
        val lookup = MethodHandles.lookup()
        var sendPacket: MethodHandle? = null
        var getHandle: MethodHandle? = null
        var connection: MethodHandle? = null
        try {
            connection = lookup.findGetter(
                entityPlayer,
                v(17, "b").orElse("playerConnection"), playerConnection
            )
            getHandle = lookup.findVirtual(craftPlayer, "getHandle", MethodType.methodType(entityPlayer))
            sendPacket = lookup.findVirtual(
                playerConnection,
                v(18, "a").orElse("sendPacket"),
                MethodType.methodType(Void.TYPE, getNMSClass("network.protocol", "Packet"))
            )
        } catch (ex: NoSuchMethodException) {
            ex.printStackTrace()
        } catch (ex: NoSuchFieldException) {
            ex.printStackTrace()
        } catch (ex: IllegalAccessException) {
            ex.printStackTrace()
        }
        PLAYER_CONNECTION = connection
        SEND_PACKET = sendPacket
        GET_HANDLE = getHandle
    }

    /**
     * This method is purely for readability.
     * No performance is gained.
     *
     * @since 5.0.0
     */
    fun <T> v(version: Int, handle: T): VersionHandler<T> {
        return VersionHandler(version, handle)
    }

    fun <T> v(version: Int, handle: Callable<T>): CallableVersionHandler<T> {
        return CallableVersionHandler(version, handle)
    }

    /**
     * Checks whether the server version is equal or greater than the given version.
     *
     * @param version the version to compare the server version with.
     *
     * @return true if the version is equal or newer, otherwise false.
     * @since 4.0.0
     */
    fun supports(version: Int): Boolean {
        return VER >= version
    }

    /**
     * Get a NMS (net.minecraft.server) class which accepts a package for 1.17 compatibility.
     *
     * @param newPackage the 1.17 package name.
     * @param name       the name of the class.
     *
     * @return the NMS class or null if not found.
     * @since 4.0.0
     */
    fun getNMSClass(newPackage: String, name: String): Class<*>? {
        var name = name
        if (supports(17)) name = "$newPackage.$name"
        return getNMSClass(name)
    }

    /**
     * Get a NMS (net.minecraft.server) class.
     *
     * @param name the name of the class.
     *
     * @return the NMS class or null if not found.
     * @since 1.0.0
     */
    fun getNMSClass(name: String): Class<*>? {
        return try {
            Class.forName(NMS + name)
        } catch (ex: ClassNotFoundException) {
            ex.printStackTrace()
            null
        }
    }

    /**
     * Sends a packet to the player asynchronously if they're online.
     * Packets are thread-safe.
     *
     * @param player  the player to send the packet to.
     * @param packets the packets to send.
     *
     * @return the async thread handling the packet.
     * @see .sendPacketSync
     * @since 1.0.0
     */
    fun sendPacket(player: Player?, vararg packets: Any?
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            sendPacketSync(
                player,
                *packets
            )
        }
            .exceptionally { ex: Throwable ->
                ex.printStackTrace()
                null
            }
    }

    /**
     * Sends a packet to the player synchronously if they're online.
     *
     * @param player  the player to send the packet to.
     * @param packets the packets to send.
     *
     * @see .sendPacket
     * @since 2.0.0
     */
    fun sendPacketSync(player: Player?, vararg packets: Any?) {
        try {
            val handle = GET_HANDLE!!.invoke(player)
            val connection = PLAYER_CONNECTION!!.invoke(handle)

            // Checking if the connection is not null is enough. There is no need to check if the player is online.
            if (connection != null) {
                for (packet in packets) SEND_PACKET!!.invoke(connection, packet)
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
    }

    fun getHandle(player: Player?): Any? {
        Objects.requireNonNull(player, "Cannot get handle of null player")
        return try {
            GET_HANDLE!!.invoke(player)
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            null
        }
    }

    fun getConnection(player: Player?): Any? {
        Objects.requireNonNull(player, "Cannot get connection of null player")
        return try {
            val handle = GET_HANDLE!!.invoke(player)
            PLAYER_CONNECTION!!.invoke(handle)
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            null
        }
    }

    /**
     * Get a CraftBukkit (org.bukkit.craftbukkit) class.
     *
     * @param name the name of the class to load.
     *
     * @return the CraftBukkit class or null if not found.
     * @since 1.0.0
     */
    fun getCraftClass(name: String): Class<*>? {
        return try {
            Class.forName(CRAFTBUKKIT + name)
        } catch (ex: ClassNotFoundException) {
            ex.printStackTrace()
            null
        }
    }

    fun getArrayClass(clazz: String, nms: Boolean): Class<*>? {
        var clazz = clazz
        clazz = "[L" + (if (nms) NMS else CRAFTBUKKIT) + clazz + ';'
        return try {
            Class.forName(clazz)
        } catch (ex: ClassNotFoundException) {
            ex.printStackTrace()
            null
        }
    }

    fun toArrayClass(clazz: Class<*>): Class<*>? {
        return try {
            Class.forName("[L" + clazz.name + ';')
        } catch (ex: ClassNotFoundException) {
            ex.printStackTrace()
            null
        }
    }

    class VersionHandler<T>(version: Int, handle: T) {
        private var version = 0
        private var handle: T? = null

        init {
            if (supports(version)) {
                this.version = version
                this.handle = handle
            }
        }

        fun v(version: Int, handle: T): VersionHandler<T> {
            require(version != this.version) { "Cannot have duplicate version handles for version: $version" }
            if (version > this.version && supports(version)) {
                this.version = version
                this.handle = handle
            }
            return this
        }

        fun orElse(handle: T): T? {
            return if (version == 0) handle else this.handle
        }
    }

    class CallableVersionHandler<T>(version: Int, handle: Callable<T>) {
        private var version = 0
        private var handle: Callable<T>? = null

        init {
            if (supports(version)) {
                this.version = version
                this.handle = handle
            }
        }

        fun v(version: Int, handle: Callable<T>): CallableVersionHandler<T> {
            require(version != this.version) { "Cannot have duplicate version handles for version: $version" }
            if (version > this.version && supports(version)) {
                this.version = version
                this.handle = handle
            }
            return this
        }

        fun orElse(handle: Callable<T>): T? {
            return try {
                (if (version == 0) handle else this.handle)?.call()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}