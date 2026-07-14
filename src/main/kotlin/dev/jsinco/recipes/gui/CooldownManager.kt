package dev.jsinco.recipes.gui

import dev.jsinco.recipes.BreweryRecipes
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.*
import java.util.Collections.synchronizedMap

object CooldownManager {

    private const val TICK_MS = 50L
    private val COOLDOWN_SOUND = Sound.sound(
        org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS,
        Sound.Source.MASTER,
        1.0f,
        1.0f
    )

    private val openCooldowns: MutableMap<UUID, Long> = synchronizedMap(mutableMapOf())
    private val pageCooldowns: MutableMap<UUID, Long> = synchronizedMap(mutableMapOf())
    private val modeSwitchCooldowns: MutableMap<UUID, Long> = synchronizedMap(mutableMapOf())

    fun tryOpen(player: Player): Boolean =
        tryConsume(
            player,
            openCooldowns,
            BreweryRecipes.recipesConfig.openCooldownTicks,
            "breweryrecipes.gui.cooldown.open"
        )

    fun tryPageSwitch(player: Player): Boolean =
        tryConsume(
            player,
            pageCooldowns,
            BreweryRecipes.recipesConfig.pageCooldownTicks,
            "breweryrecipes.gui.cooldown.page"
        )

    fun tryModeSwitch(player: Player): Boolean =
        tryConsume(
            player,
            modeSwitchCooldowns,
            BreweryRecipes.recipesConfig.modeSwitchCooldownTicks,
            "breweryrecipes.gui.cooldown.mode"
        )

    fun clearFor(playerUuid: UUID) {
        openCooldowns.remove(playerUuid)
        pageCooldowns.remove(playerUuid)
        modeSwitchCooldowns.remove(playerUuid)
    }

    private fun tryConsume(
        player: Player,
        store: MutableMap<UUID, Long>,
        cooldownTicks: Long,
        messageKey: String
    ): Boolean {
        if (cooldownTicks <= 0) return true
        val now = System.currentTimeMillis()
        val cooldownMs = cooldownTicks * TICK_MS
        val last = store[player.uniqueId]
        if (last != null && now - last < cooldownMs) {
            player.sendActionBar(Component.translatable(messageKey))
            player.playSound(COOLDOWN_SOUND)
            return false
        }
        store[player.uniqueId] = now
        return true
    }
}
