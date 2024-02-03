package dev.jsinco.recipes.recipe

data class Recipe (
    val recipeKey: String,

    val name: String,
    val difficulty: Int,
    val cookingTime: Int,
    val distillRuns: Int,
    val distillTime: Int,
    val age: Int,

    val ingredients: Map<String, Int>,

    val rarityWeight: Int
)