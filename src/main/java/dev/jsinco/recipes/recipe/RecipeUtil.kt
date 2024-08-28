package dev.jsinco.recipes.recipe

import com.dre.brewery.BreweryPlugin
import com.dre.brewery.recipe.PotionColor
import dev.jsinco.recipes.Recipes
import org.bukkit.configuration.ConfigurationSection

object RecipeUtil {
    private val plugin: BreweryPlugin = BreweryPlugin.getInstance()
    val loadedRecipes: MutableList<Recipe> = mutableListOf()

    @JvmStatic
    fun loadAllRecipes() {
        if (loadedRecipes.isNotEmpty()) {
            loadedRecipes.clear()
            Recipes.getAddonInstance().addonLogger.info("Refreshing loaded recipes!")
        }
        val configurationSection = plugin.config.getConfigurationSection("recipes") ?: throw RuntimeException("Config section 'recipes' in null in BreweryX's config.yml!")
        for (recipe in configurationSection.getKeys(false)) {
            loadedRecipes.add(getRecipeFromKey(recipe))
        }
    }

    @JvmStatic
    fun getAllRecipes(): List<Recipe> {
        return loadedRecipes
    }

    @JvmStatic
    fun getAllRecipeKeys(): List<String> {
        val configurationSection = plugin.config.getConfigurationSection("recipes") ?: return emptyList()
        return configurationSection.getKeys(false).toList()
    }

    @JvmStatic
    fun getRecipeFromKey(recipe: String): Recipe {
        val configurationSection: ConfigurationSection = plugin.config.getConfigurationSection("recipes.$recipe") ?: return Recipe(
            recipe,
            "Unknown Recipe",
            0,
            0,
            0,
            0,
            0,
            Recipe.BarrelWoodTypes.ANY,
            mutableMapOf(),
            null,
            0,
            0
        )

        val ingredientsRaw = configurationSection.getStringList("ingredients")
        val ingredientsMap: MutableMap<String, Int> = mutableMapOf()
        for (ingredientRaw in ingredientsRaw) {
            ingredientsMap[ingredientRaw.substringBefore("/")] = ingredientRaw.substringAfter("/").toInt()
        }

        var woodType: Recipe.BarrelWoodTypes = Recipe.BarrelWoodTypes.ANY
        if (configurationSection.contains("wood")) {
            for (woodNum in Recipe.BarrelWoodTypes.entries.map { it.woodNumber }) {
                if (configurationSection.getInt("wood") == woodNum) {
                    woodType = Recipe.BarrelWoodTypes.entries.first { it.woodNumber == woodNum }
                    break
                }
            }
        }


        return Recipe(
            recipe,

            configurationSection.getString("name") ?: "Unnamed Recipe",
            configurationSection.getInt("difficulty"),
            configurationSection.getInt("cookingtime"),
            configurationSection.getInt("distillruns"),
            if (configurationSection.contains("distilltime")) configurationSection.getInt("distilltime") else 40,
            configurationSection.getInt("age"),
            woodType,

            ingredientsMap,

            if (configurationSection.contains("color")) PotionColor.fromString(configurationSection.getString("color") ?: "PINK") else null,
            parseRecipeName(configurationSection.getString("customModelData") ?: "0").toIntOrNull() ?: 0,
            if (configurationSection.contains("rarity_weight")) configurationSection.getInt("rarity_weight") else configurationSection.getInt("difficulty")
        )
    }


    fun getRandomRecipe(): Recipe {
        return getAllRecipes().random()
    }



    // We need the one in the middle!
    // recipeName/recipeName2/recipeName3
    // recipeName/recipeName2

    fun parseRecipeName(recipeName: String): String {
        if (!recipeName.contains("/")) {
             return recipeName
        }
        val newString = recipeName.substringAfter("/")
        if (newString.contains("/")) {
            return newString.substring(0, newString.indexOf("/"))
        }
        return newString
    }

    fun parseIngredientsName(string: String): String {
        if (string.contains(":")) {
            return string.substringAfter(":")
        }
        return string
    }
}