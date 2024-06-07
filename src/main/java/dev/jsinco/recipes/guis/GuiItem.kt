package dev.jsinco.recipes.guis

import com.dre.brewery.BreweryPlugin
import com.dre.brewery.utility.BUtil
import dev.jsinco.recipes.Config
import dev.jsinco.recipes.Util
import dev.jsinco.recipes.recipe.Recipe
import dev.jsinco.recipes.recipe.RecipeUtil
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
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
        private val plugin: BreweryPlugin = BreweryPlugin.getInstance()

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
            val configSec = Config.get().getConfigurationSection("gui.items.recipe-gui-item")
            val item = ItemStack(BUtil.getMaterialSafely(configSec?.getString("material") ?: "PAPER"))
            val meta = item.itemMeta ?: return item

            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_POTION_EFFECTS)
            if (item.type == Material.POTION) { // if it's a potion, set the color
                meta as PotionMeta
                meta.color = recipe.potionColor?.color
            }
            meta.setDisplayName(Util.colorcode( // display name
                recipeItemStringHelper(configSec?.getString("display_name"), recipe) ?: "&#F7FFC9${RecipeUtil.parseRecipeName(recipe.name)} &fRecipe")
            )
            if (configSec?.getBoolean("glint") == true) { // glint
                meta.addEnchant(Enchantment.LUCK, 1, true)
            }
            if (recipe.customModelData != 0) { // custom model data
                meta.setCustomModelData(recipe.customModelData)
            }


            // lore/ingredients
            val ingredients: MutableList<String> = mutableListOf()
            for (ingredient in recipe.ingredients) {
                ingredients.add(Util.colorcode(
                    configSec?.getString("ingredient-format")?.replace("%amount%", ingredient.value.toString())
                        ?.replace("%ingredient%", Util.itemNameFromMaterial(RecipeUtil.parseIngredientsName(ingredient.key)))
                        ?: " &#F7FFC9${ingredient.value}x &f${Util.itemNameFromMaterial(RecipeUtil.parseIngredientsName(ingredient.key))}"))
            }
            val lore: MutableList<String> = configSec?.getStringList("lore")
                ?.map { Util.colorcode(recipeItemStringHelper(it, recipe) ?: "")}?.toMutableList()
                ?: mutableListOf()
            val ingredientsPlaceHolderIndexes: List<Int> = lore.mapIndexedNotNull { index, line -> if (line.contains("%ingredients%")) index else null }
            for (index in ingredientsPlaceHolderIndexes) {
                lore.removeAt(index)
                lore.addAll(index, ingredients)
            }

            meta.lore = lore
            item.itemMeta = meta
            return item
        }
        
        private fun recipeItemStringHelper(string: String?, recipe: Recipe): String? {
            if (string == null) return null
            return string
                .replace("%recipe%", RecipeUtil.parseRecipeName(recipe.name))
                .replace("%difficulty%", recipe.difficulty.toString())
                .replace("%cooking_time%", recipe.cookingTime.toString())
                .replace("%distill_runs%", recipe.distillRuns.toString())
                .replace("%age%", recipe.age.toString())
                .replace("%barrel_type%", Util.itemNameFromMaterial(recipe.woodType.name))
        }

        private fun createGUIItem(guiItem: GuiItem, guiItemType: GuiItemType): Pair<List<Int>, ItemStack> {
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

        private fun getGUIItem(string: String): GuiItem {
            return GuiItem(BUtil.getMaterialSafely(Config.get().getString("gui.$string.material") ?: "MAP"),
                Config.get().getIntegerList("gui.$string.slots"),
                Config.get().getString("gui.$string.display_name") ?: " ",
                Config.get().getStringList("gui.$string.lore"),
                Config.get().getBoolean("gui.$string.glint"),
                Config.get().getInt("gui.$string.custom_model_data")
            )
        }
    }
}
