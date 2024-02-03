package dev.jsinco.recipes.guis

import com.dre.brewery.BreweryPlugin
import dev.jsinco.recipes.Util
import dev.jsinco.recipes.Config
import dev.jsinco.recipes.Recipes
import dev.jsinco.recipes.recipe.Recipe
import dev.jsinco.recipes.recipe.RecipeUtil
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

data class GuiItem(
    val material: Material,
    val slots: List<Int>,
    val name: String,
    val lore: List<String>,
    val glint: Boolean,
    val customModelData: Int
) {
    companion object {
        private val plugin: BreweryPlugin = Recipes.getPlugin()

        fun getAllGuiBorderItems(): List<Pair<List<Int>, ItemStack>> {
            val items = mutableListOf<Pair<List<Int>, ItemStack>>()
            for (key in Config.get().getConfigurationSection("gui.border-items")!!.getKeys(false)) {
                items.add(createGUIItem(getGUIItem("border-items.$key"), GuiItemType.BORDER_ITEM))
            }
            return items
        }

        fun getPageArrowItems(): Pair<Pair<List<Int>, ItemStack>, Pair<List<Int>, ItemStack>> {
            val left = createGUIItem(getGUIItem("items.previous_page"), GuiItemType.PREVIOUS_PAGE)
            val right = createGUIItem(getGUIItem("items.next_page"), GuiItemType.NEXT_PAGE)
            return Pair(left, right)
        }

        fun getTotalRecipesItem(amount: Int, total: Int): Pair<List<Int>, ItemStack> {
            val itemPair = createGUIItem(getGUIItem("items.total_recipes"), GuiItemType.BORDER_ITEM)
            val meta = itemPair.second.itemMeta!!
            meta.lore = meta.lore?.let { Util.colorArrayList(it.map { line -> line.replace("%total_recipes%", "$amount/$total") }) }
            meta.setDisplayName(Util.colorcode(meta.displayName.replace("%total_recipes%", "$amount/$total")))
            itemPair.second.itemMeta = meta
            return itemPair
        }

        fun createRecipeGuiItem(recipe: Recipe): ItemStack {
            val item = ItemStack(Material.PAPER)
            val meta = item.itemMeta!!
            meta.setDisplayName(Util.colorcode("&#F7FFC9${RecipeUtil.parseRecipeName(recipe.name)} &fRecipe"))

            val lore: MutableList<String> = mutableListOf()
            lore.addAll(
                Util.colorArrayList(listOf(
                "&fDifficulty&7: &#F7FFC9${recipe.difficulty}",
                "&fCooking time&7: &#F7FFC9${recipe.cookingTime}m",
                "&fDistill Runs&7: &#F7FFC9${recipe.distillRuns}",
                "&fAge&7: &#F7FFC9${recipe.age}yrs &f(Minecraft days)",
                "", "&fIngredients&7:")
            ))
            for (ingredient in recipe.ingredients) {
                lore.add(Util.colorcode(" &#F7FFC9${ingredient.value}x &f${Util.itemNameFromMaterial(RecipeUtil.parseIngredientsName(ingredient.key))}"))
            }
            meta.addEnchant(Enchantment.LUCK, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            meta.lore = lore
            item.itemMeta = meta
            return item
        }

        fun createGUIItem(guiItem: GuiItem, guiItemType: GuiItemType): Pair<List<Int>, ItemStack> {
            val item = ItemStack(guiItem.material)
            val meta = item.itemMeta!!
            meta.setDisplayName(Util.colorcode(guiItem.name))
            meta.lore = Util.colorArrayList(guiItem.lore)
            if (guiItem.glint) {
                meta.addEnchant(Enchantment.LUCK, 1, true)
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            }
            if (guiItem.customModelData != 0) {
                meta.setCustomModelData(guiItem.customModelData)
            }
            meta.persistentDataContainer.set(NamespacedKey(plugin, "gui-item-type"), PersistentDataType.STRING, guiItemType.name)

            item.itemMeta = meta
            return Pair(guiItem.slots, item)
        }

        fun getGUIItem(string: String): GuiItem {
            return GuiItem(Material.valueOf(Config.get().getString("gui.$string.material") ?: "DIRT"),
                Config.get().getIntegerList("gui.$string.slots"),
                Config.get().getString("gui.$string.display_name") ?: " ",
                Config.get().getStringList("gui.$string.lore"),
                Config.get().getBoolean("gui.$string.glint"),
                Config.get().getInt("gui.$string.custom_model_data")
            )
        }
    }
}
