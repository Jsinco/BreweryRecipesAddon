package dev.jsinco.recipes.recipe

import com.dre.brewery.recipe.PotionColor

// We're not using BreweryX's BRecipe class because it has a bunch of extra stuff that we don't need
data class Recipe (
    val recipeKey: String,

    val name: String,
    val difficulty: Int,
    val cookingTime: Int,
    val distillRuns: Int,
    val distillTime: Int,
    val age: Int,
    val woodType: BarrelWoodTypes,

    val ingredients: Map<String, Int>,

    val potionColor: PotionColor?,
    val customModelData: Int,
    val rarityWeight: Int
) {
    enum class BarrelWoodTypes(val woodNumber: Int) {
        ANY(0),
        BIRCH(1),
        OAK(2),
        JUNGLE(3),
        SPRUCE(4),
        ACACIA(5),
        DARK_OAK(6),
        CRIMSON(7),
        WARPED(8),
        MANGROVE(9),
        CHERRY(10),
        BAMBOO(11),
    }
}