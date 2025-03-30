package dev.jsinco.recipes.listeners

import com.dre.brewery.BreweryPlugin
import com.dre.brewery.api.events.brew.BrewModifyEvent
import com.dre.brewery.recipe.BRecipe
import com.dre.brewery.utility.Logging
import dev.jsinco.recipes.Recipes
import dev.jsinco.recipes.configuration.RecipesConfig
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

    private val config: RecipesConfig = Recipes.configManager.getConfig(RecipesConfig::class.java)

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
        val bound = config.recipeSpawning.bound
        val chance = config.recipeSpawning.chance
        if (bound <= 0 || chance <= 0) return
        else if (Random.nextInt(bound) > chance) return

        var recipe: Recipe = RecipeUtil.getRandomRecipe()
        while (config.recipeSpawning.blacklistedRecipes.contains(recipe.recipeKey)) {
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
        val recipeObj: Recipe = RecipeUtil.getRecipeFromKey(recipeKey ?: return) ?: return
        event.isCancelled = true

        if (Util.hasRecipePermission(player, recipeKey)) {
            Logging.msg(player, config.messages.alreadyLearned.replace("%recipe%", recipeObj.name))
            return
        }

        event.item!!.amount--

        Recipes.permissionManager.setPermission(config.recipePermissionNode.replace("%recipe%", recipeKey), player, true)
        Logging.msg(player, config.messages.learned.replace("%recipe%", recipeObj.name))
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
    }

    @EventHandler
    fun onBrewModify(event: BrewModifyEvent) {
        val player = event.player ?: return

        if (event.type != BrewModifyEvent.Type.FILL && event.type != BrewModifyEvent.Type.CREATE) {
            return
        }

        val bRecipe: BRecipe? = event.brew.currentRecipe

        if (config.learnRecipeUponCreation && config.requireRecipePermissionToBrew) {
            Logging.errorLog("You have two conflicting options enabled: `learnRecipeUponCreation` and `requireRecipePermissionToBrew`. Please disable one of them.")
            return
        }

        bRecipe?.id?.let {
            if (config.learnRecipeUponCreation) {
                handleLearnUponRecipeCreation(player, it)
            } else if (config.requireRecipePermissionToBrew) {
                handleRequireRecipePermissionToBrew(player, it, event)
            }
        }
    }

    // Honestly, it's a little messy, but I really do little with
    //  this addon anyway.

    private fun handleLearnUponRecipeCreation(player: Player, recipeKey: String) {
        if (Util.hasRecipePermission(player, recipeKey)) {
            return
        }

        Recipes.permissionManager.setPermission(config.recipePermissionNode.replace("%recipe%", recipeKey), player, true)
        Logging.msg(player, config.messages.learned.replace("%recipe%", RecipeUtil.getRecipeFromKey(recipeKey)?.name ?: recipeKey))
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
    }

    private fun handleRequireRecipePermissionToBrew(player: Player, recipeKey: String, event: BrewModifyEvent) {
        if (!Util.hasRecipePermission(player, recipeKey)) {
            event.isCancelled = true
            Logging.msg(player, config.messages.notLearned.replace("%recipe%", RecipeUtil.getRecipeFromKey(recipeKey)?.name ?: recipeKey))
        }
    }
}