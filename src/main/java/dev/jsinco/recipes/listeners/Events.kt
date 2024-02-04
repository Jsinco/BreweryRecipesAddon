package dev.jsinco.recipes.listeners

import com.dre.brewery.BreweryPlugin
import dev.jsinco.recipes.Config
import dev.jsinco.recipes.Recipes
import dev.jsinco.recipes.Util
import dev.jsinco.recipes.guis.GuiItemType
import dev.jsinco.recipes.guis.PaginatedGui
import dev.jsinco.recipes.guis.RecipeGui
import dev.jsinco.recipes.recipe.RecipeItem
import dev.jsinco.recipes.recipe.RecipeUtil
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.LootGenerateEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.random.Random


class Events(private val plugin: BreweryPlugin) : Listener {

    @EventHandler
    fun onGuiClick(event: InventoryClickEvent) {
        if (event.inventory.holder !is RecipeGui) return
        event.isCancelled = true
        val paginatedGUI: PaginatedGui = (event.inventory.holder as RecipeGui).paginatedGui

        val player: Player = event.whoClicked as Player
        val clickedItem: ItemStack = event.currentItem ?: return

        val guiItemType: GuiItemType = GuiItemType.valueOf(clickedItem.itemMeta?.persistentDataContainer?.get(
            NamespacedKey(plugin,"gui-item-type"), PersistentDataType.STRING) ?: return)

        when (guiItemType) {
            GuiItemType.PREVIOUS_PAGE -> {
                val currentPage = paginatedGUI.indexOf(event.inventory)
                if (currentPage == 0) return
                player.openInventory(paginatedGUI.getPage(currentPage - 1))
            }
            GuiItemType.NEXT_PAGE -> {
                val currentPage = paginatedGUI.indexOf(event.inventory)
                if (currentPage == paginatedGUI.size - 1) return
                player.openInventory(paginatedGUI.getPage(currentPage + 1))
            }
            else -> {}
        }
    }

    @EventHandler
    fun onLootGenerate(event: LootGenerateEvent) {
        if (Random.nextInt(Config.get().getInt("recipe-spawning.bound")) > Config.get().getInt("recipe-spawning.chance")) return
        event.loot.add(RecipeItem(RecipeUtil.getRandomRecipe()).item)
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK  && event.action != Action.RIGHT_CLICK_AIR) return
        val meta = event.item?.itemMeta ?: return
        val player = event.player
        if (meta.persistentDataContainer.has(NamespacedKey(plugin, "recipe-book"), PersistentDataType.INTEGER)) {
            RecipeGui(player).openRecipeGui(player)
            event.isCancelled = true
            return
        }

        val recipeKey: String = meta.persistentDataContainer.get(NamespacedKey(plugin, "recipe-key"), PersistentDataType.STRING) ?: return
        event.isCancelled = true

        if (Util.checkForRecipePermission(player, recipeKey)) {
            player.sendMessage(Util.colorcode("${Util.prefix}You already know this recipe!"))
            return
        }

        player.sendMessage(Util.colorcode("${Util.prefix}You have learned the '${event.item?.itemMeta!!.displayName}&#E2E2E2'!"))
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        event.item!!.amount--

        Recipes.getPermissionManager().setPermission(recipeKey, player, true)
    }
}