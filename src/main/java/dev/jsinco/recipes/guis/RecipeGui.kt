package dev.jsinco.recipes.guis

import dev.jsinco.recipes.Config
import dev.jsinco.recipes.Util
import dev.jsinco.recipes.recipe.Recipe
import dev.jsinco.recipes.recipe.RecipeUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class RecipeGui(player: Player) : InventoryHolder {

    private val inv: Inventory = Bukkit.createInventory(this, Config.get().getInt("gui.size"), Config.get().getString("gui.title") ?: "Recipes")
    private val recipeGuiItems: MutableList<ItemStack> = mutableListOf()

    init {
        val borderItems: List<Pair<List<Int>, ItemStack>> = GuiItem.getAllGuiBorderItems()
        for (guiItem in borderItems) {
            for (slot in guiItem.first) {
                inv.setItem(slot, guiItem.second)
            }
        }

        val recipes: List<Recipe> = RecipeUtil.getAllRecipes()
        var totalRecipes = 0

        for (recipe in recipes) {
            if (!Util.checkForRecipePermission(player, recipe.recipeKey)) continue
            recipeGuiItems.add(GuiItem.createRecipeGuiItem(recipe))
            totalRecipes++
        }

        val totalRecipesItem = GuiItem.getTotalRecipesItem(totalRecipes, recipes.size)
        for (slot in totalRecipesItem.first) {
            inv.setItem(slot, totalRecipesItem.second)
        }
    }
    //
    val paginatedGui: PaginatedGui = PaginatedGui(Config.get().getString("gui.title") ?: "Recipes", inv, recipeGuiItems, Config.get().getIntegerList("gui.items.recipe-gui-item.slots"))

    init {
        val arrowItems = GuiItem.getPageArrowItems()
        for (page in paginatedGui.pages) {
            if (paginatedGui.indexOf(page) != 0) {
                for (slot in arrowItems.first.first) {
                    page.setItem(slot, arrowItems.first.second)
                }

            }
            if (paginatedGui.indexOf(page) != paginatedGui.size - 1) {
                for (slot in arrowItems.second.first) {
                    page.setItem(slot, arrowItems.second.second)
                }
            }
        }
    }

    fun openRecipeGui(viewer: Player) {
        viewer.openInventory(paginatedGui.getPage(0))
    }



    override fun getInventory(): Inventory {
        return inv
    }

}