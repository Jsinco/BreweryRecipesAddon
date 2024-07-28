package dev.jsinco.recipes.listeners

import com.dre.brewery.BreweryPlugin
import dev.jsinco.recipes.Config
import dev.jsinco.recipes.Recipes
import dev.jsinco.recipes.Util
import dev.jsinco.recipes.guis.GuiItemType
import dev.jsinco.recipes.guis.PaginatedGui
import dev.jsinco.recipes.guis.RecipeGui
import dev.jsinco.recipes.recipe.Recipe
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

    private val BOOK_KEY = NamespacedKey(plugin, "recipe-book")
    private val LEGACY_BOOK_KEY = NamespacedKey("brewery", "recipe-book")

    private val RECIPE_KEY = NamespacedKey(plugin, "recipe-key")
    private val LEGACY_RECIPE_KEY = NamespacedKey("brewery", "recipe-key")

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

        var recipe: Recipe = RecipeUtil.getRandomRecipe()
        while (Config.get().getStringList("recipe-spawning.blacklisted-recipes").contains(recipe.recipeKey)) {
            recipe = RecipeUtil.getRandomRecipe()
        }
        event.loot.add(RecipeItem(recipe).item)
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK  && event.action != Action.RIGHT_CLICK_AIR) return
        val meta = event.item?.itemMeta ?: return
        val player = event.player
        if (meta.persistentDataContainer.has(BOOK_KEY, PersistentDataType.INTEGER) || meta.persistentDataContainer.has(LEGACY_BOOK_KEY, PersistentDataType.INTEGER)) {
            RecipeGui(player).openRecipeGui(player)
            event.isCancelled = true
            return
        }

        var recipeKey: String? = meta.persistentDataContainer.get(RECIPE_KEY, PersistentDataType.STRING)
        if (recipeKey == null) {
            recipeKey = meta.persistentDataContainer.get(LEGACY_RECIPE_KEY, PersistentDataType.STRING)
        }
        if (recipeKey == null) return
        event.isCancelled = true

        val msgs = Config.get().getConfigurationSection("messages")

        if (Util.checkForRecipePermission(player, recipeKey)) {
            player.sendMessage(Util.colorcode(Util.prefix + msgs?.getString("already-learned")))
            return
        }
        event.item!!.amount--
        player.sendMessage(Util.colorcode(Util.prefix + msgs?.getString("learned")?.let { String.format(it, event.item?.itemMeta?.displayName) }))
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)


        Recipes.getPermissionManager().setPermission(Config.get().getString("recipe-permission-node")?.replace("%recipe%", recipeKey), player, true)
    }
}