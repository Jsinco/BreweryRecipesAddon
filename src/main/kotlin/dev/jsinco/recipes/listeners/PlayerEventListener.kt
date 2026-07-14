package dev.jsinco.recipes.listeners

import com.destroystokyo.paper.profile.PlayerProfile
import dev.jsinco.recipes.data.PersistencyLinkedCache
import dev.jsinco.recipes.recipe.RecipeViewManager.Companion.CACHE_LIFETIME
import io.papermc.paper.connection.PlayerConfigurationConnection
import io.papermc.paper.connection.PlayerLoginConnection
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.Collections.synchronizedMap

class PlayerEventListener(
    private vararg val persistencyLinkedCaches: PersistencyLinkedCache
) : Listener {
    val forRemoval: MutableMap<UUID, Long> = synchronizedMap(mutableMapOf())

    @EventHandler
    fun onPlayerPlayerConnectionValidateLogin(event: PlayerConnectionValidateLoginEvent) {
        val profile: PlayerProfile? =
            when (val connection = event.getConnection()) {
                is PlayerLoginConnection -> {
                    connection.getAuthenticatedProfile()
                }

                is PlayerConfigurationConnection -> {
                    connection.getProfile()
                }

                else -> null
            }
        profile?.id?.let { playerUuid ->
            persistencyLinkedCaches.forEach { it.initiateCacheFor(playerUuid) }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        forRemoval[event.player.uniqueId] = System.currentTimeMillis() + CACHE_LIFETIME
    }

    fun tick() {
        val toRemove = forRemoval.filter { it.value < System.currentTimeMillis() }
            .map { it.key }
        toRemove.forEach(forRemoval::remove)
        persistencyLinkedCaches.forEach {
            toRemove.forEach(it::clearAll)
        }
    }
}