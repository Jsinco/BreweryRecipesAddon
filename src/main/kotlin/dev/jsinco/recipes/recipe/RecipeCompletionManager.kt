package dev.jsinco.recipes.recipe

import dev.jsinco.recipes.BreweryRecipes
import dev.jsinco.recipes.data.PersistencyLinkedCache
import dev.jsinco.recipes.data.storage.StorageImpl
import dev.jsinco.recipes.recipe.process.Step
import dev.jsinco.recipes.recipe.process.steps.AgeStep
import dev.jsinco.recipes.recipe.process.steps.CookStep
import dev.jsinco.recipes.recipe.process.steps.DistillStep
import dev.jsinco.recipes.recipe.process.steps.MixStep
import java.util.*
import java.util.Collections.synchronizedMap

class RecipeCompletionManager(private val storageImpl: StorageImpl) : PersistencyLinkedCache {

    val backing = synchronizedMap(mutableMapOf<UUID, MutableMap<String, BreweryRecipe>>())

    fun insertOrUpdateRecipeCompletion(uuid: UUID, recipe: BreweryRecipe) {
        val existing = backing[uuid]?.get(recipe.identifier)
        if (existing != null) {
            if (existing.score > recipe.score) return
            if (existing.score == recipe.score) {
                val ideal = BreweryRecipes.brewingIntegration.getRecipe(recipe.identifier) ?: return
                val existingMismatches = typeMismatches(existing.steps, ideal.steps)
                val newMismatches = typeMismatches(recipe.steps, ideal.steps)
                if (existingMismatches < newMismatches) return
                if (existingMismatches == newMismatches) {
                    if (stepDeviation(existing.steps, ideal.steps) <= stepDeviation(recipe.steps, ideal.steps)) return
                }
            }
        }
        storageImpl.completedRecipeSession()
            .insertOrUpdateRecipeCompletion(uuid, recipe)
        synchronized(backing) {
            backing.computeIfAbsent(uuid) { mutableMapOf() }[recipe.identifier] = recipe
        }
        BreweryRecipes.recipeGuiItemCache.invalidate(uuid, recipe.identifier)
    }

    fun removeCompletion(uuid: UUID, recipeKey: String) {
        storageImpl.completedRecipeSession()
            .removeRecipeCompletion(uuid, recipeKey)
        backing[uuid]?.remove(recipeKey)
        BreweryRecipes.recipeGuiItemCache.invalidate(uuid, recipeKey)
    }

    fun contains(playerUuid: UUID, recipeKey: String): Boolean {
        return backing[playerUuid]?.containsKey(recipeKey) ?: false
    }

    fun removeAll(playerUuid: UUID) {
        val recipes = backing.remove(playerUuid)
        recipes?.forEach {
            storageImpl.completedRecipeSession().removeRecipeCompletion(playerUuid, it.key)
        }
        BreweryRecipes.recipeGuiItemCache.clearAll(playerUuid)
    }

    override fun clearAll(playerUuid: UUID) {
        backing.remove(playerUuid)
        BreweryRecipes.recipeGuiItemCache.clearAll(playerUuid)
    }

    override fun initiateCacheFor(playerUuid: UUID) {
        storageImpl.completedRecipeSession()
            .selectRecipeCompletions(playerUuid)
            .thenAccept { completed ->
                completed ?: return@thenAccept
                backing[playerUuid] = completed.associateBy { it.recipeKey() }
                    .toMutableMap()
            }
    }

    fun getCompletedRecipes(playerUuid: UUID): Collection<BreweryRecipe> {
        return backing.getOrDefault(playerUuid, mapOf())
            .values
    }

    private fun stepDeviation(actual: List<Step>, ideal: List<Step>): Long {
        return actual.zip(ideal).sumOf { (a, i) ->
            when {
                a is CookStep && i is CookStep -> Math.abs(a.cookingTicks - i.cookingTicks)
                a is MixStep && i is MixStep -> Math.abs(a.mixingTicks - i.mixingTicks)
                a is DistillStep && i is DistillStep -> Math.abs(a.count - i.count)
                a is AgeStep && i is AgeStep -> Math.abs(a.agingTicks - i.agingTicks)
                else -> Long.MAX_VALUE / ideal.size
            }
        }
    }

    private fun typeMismatches(actual: List<Step>, ideal: List<Step>): Int {
        return actual.zip(ideal).count { (a, i) ->
            when {
                a is CookStep && i is CookStep -> a.cauldronType != i.cauldronType
                a is MixStep && i is MixStep -> a.cauldronType != i.cauldronType
                a is AgeStep && i is AgeStep -> i.barrelType != AgeStep.BarrelType.ANY && a.barrelType != i.barrelType
                else -> false
            }
        }
    }
}