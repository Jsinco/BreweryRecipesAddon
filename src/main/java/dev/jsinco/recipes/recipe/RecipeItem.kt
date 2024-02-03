package dev.jsinco.recipes.recipe

import com.dre.brewery.BreweryPlugin
import dev.jsinco.recipes.Config
import dev.jsinco.recipes.Recipes
import dev.jsinco.recipes.Util
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class RecipeItem (recipe: Recipe) {

    companion object {
        private val plugin: BreweryPlugin = Recipes.getPlugin()
    }

    val item = ItemStack(Material.valueOf(Config.get().getString("recipe-item.material")?.uppercase() ?: "PAPER"))

    init {
        val meta = item.itemMeta!!

        meta.setDisplayName(Util.colorcode(Config.get().getString("recipe-item.name")?.replace("%recipe%", RecipeUtil.parseRecipeName(recipe.name))
            ?: "&#F7FFC9${RecipeUtil.parseRecipeName(recipe.name)} &fRecipe"))
        meta.lore = Util.colorArrayList(Config.get().getStringList("recipe-item.lore").map { it.replace("%recipe%", RecipeUtil.parseRecipeName(recipe.name)) })
        meta.persistentDataContainer.set(NamespacedKey(plugin, "recipe-key"), PersistentDataType.STRING, recipe.recipeKey)
        if (Config.get().getBoolean("recipe-item.glint")) {
            meta.addEnchant(Enchantment.LUCK, 1, true)
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES)
        item.itemMeta = meta
    }



}