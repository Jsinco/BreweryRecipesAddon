package dev.jsinco.recipes.recipe

import dev.jsinco.recipes.BreweryRecipes
import dev.jsinco.recipes.data.PersistencyLinkedCache
import dev.jsinco.recipes.data.storage.StorageImpl
import java.util.*
import java.util.Collections.synchronizedMap

class RecipeViewManager(private val storageImpl: StorageImpl) : PersistencyLinkedCache {
    companion object {
        const val CACHE_LIFETIME: Int = 60000 // ms
    }

    val backing: MutableMap<UUID, MutableList<RecipeView>> = synchronizedMap(mutableMapOf())

    override fun initiateCacheFor(playerUuid: UUID) {
        if (backing.contains(playerUuid)) {
            return
        }
        storageImpl.recipeViewSession().selectRecipeViews(playerUuid)
            .thenAccept({
                it?.let {
                    backing[playerUuid] = it.toMutableList()
                }
            })
    }

    fun getViews(playerUuid: UUID): List<RecipeView> {
        return backing[playerUuid] ?: listOf()
    }

    fun contains(playerUuid: UUID, recipeKey: String): Boolean {
        return backing[playerUuid]?.any { it.recipeIdentifier == recipeKey } ?: false
    }

    fun insertOrUpdateView(playerUuid: UUID, recipeView: RecipeView) {
        val recipeViews = backing.computeIfAbsent(playerUuid) {
            mutableListOf()
        }
        recipeViews.removeIf { it.recipeIdentifier == recipeView.recipeIdentifier }
        recipeViews.add(recipeView)
        storageImpl.recipeViewSession().insertOrUpdateRecipeView(playerUuid, recipeView)
        BreweryRecipes.recipeGuiItemCache.invalidate(playerUuid, recipeView.recipeIdentifier)
    }

    fun insertOrMergeView(playerUuid: UUID, incoming: RecipeView) {
        val minimalized = synchronized(backing) {
            val list = backing.computeIfAbsent(playerUuid) { mutableListOf() }
            val idx = list.indexOfFirst { it.recipeIdentifier == incoming.recipeIdentifier }
            if (idx < 0) {
                val minimalized = RecipeViewLoreWriter.clearRedundantFlaws(incoming)
                list.add(minimalized) // No existing view for this recipe yet, add one
                return@synchronized minimalized
            }
            val existing = list[idx]
            val merged = RecipeViewLoreWriter.mergeFlaws(existing, incoming)
            val minimalized = RecipeViewLoreWriter.clearRedundantFlaws(merged)
            return@synchronized minimalized
        }
        storageImpl.recipeViewSession().insertOrUpdateRecipeView(playerUuid, minimalized)
        BreweryRecipes.recipeGuiItemCache.invalidate(playerUuid, minimalized.recipeIdentifier)
    }

    fun removeView(playerUuid: UUID, recipeKey: String) {
        val recipeViews = backing.computeIfAbsent(playerUuid) {
            mutableListOf()
        }
        recipeViews.removeIf { it.recipeIdentifier == recipeKey }
        storageImpl.recipeViewSession().removeRecipeView(playerUuid, recipeKey)
        BreweryRecipes.recipeGuiItemCache.invalidate(playerUuid, recipeKey)
    }

    fun removeAll(playerUuid: UUID) {
        val views = backing.remove(playerUuid)
        views?.forEach {
            storageImpl.recipeViewSession().removeRecipeView(playerUuid, it.recipeIdentifier)
        }
        BreweryRecipes.recipeGuiItemCache.clearAll(playerUuid)
    }

    override fun clearAll(playerUuid: UUID) {
        BreweryRecipes.recipeGuiItemCache.clearAll(playerUuid)
    }
}