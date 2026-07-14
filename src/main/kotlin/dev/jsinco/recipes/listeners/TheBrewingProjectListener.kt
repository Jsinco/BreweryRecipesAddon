package dev.jsinco.recipes.listeners

import dev.jsinco.brewery.api.brew.Brew
import dev.jsinco.brewery.api.meta.MetaDataType
import dev.jsinco.brewery.bukkit.api.TheBrewingProjectApi
import dev.jsinco.brewery.bukkit.api.event.transaction.BarrelExtractEvent
import dev.jsinco.brewery.bukkit.api.event.transaction.CauldronExtractEvent
import dev.jsinco.brewery.bukkit.api.event.transaction.DistilleryExtractEvent
import dev.jsinco.brewery.bukkit.api.event.transaction.ItemTransactionEvent
import dev.jsinco.brewery.bukkit.api.transaction.ItemSource
import dev.jsinco.recipes.BreweryRecipes
import dev.jsinco.recipes.util.TBPRecipeConverter
import dev.jsinco.recipes.util.metadata.UuidMetaDataType
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

data class TheBrewingProjectListener(val api: TheBrewingProjectApi) : Listener {

    companion object {
        val COMPLETED_RECIPE_KEY: Key = Key.key("recipes", "completed_recipe")
        val COMPLETED_SCORE_KEY: Key = Key.key("recipes", "completed_score")
        val COMPLETED_BY_KEY: Key = Key.key("recipes", "completed_by")
    }

    @EventHandler(ignoreCancelled = true)
    fun onBarrelExtract(event: BarrelExtractEvent) {
        onInventoryExtract(event, event.player)
    }

    @EventHandler(ignoreCancelled = true)
    fun onDistilleryExtract(event: DistilleryExtractEvent) {
        onInventoryExtract(event, event.player)
    }

    @EventHandler(ignoreCancelled = true)
    fun onCauldronExtract(event: CauldronExtractEvent) {
        val brew = actOnBrew(event.brewSource.brew, event.player ?: return) ?: return
        event.setResult(brew)
    }

    private fun actOnBrew(brew: Brew, player: Player): Brew? {
        val recipe = brew.closestRecipe(api.recipeRegistry).orElse(null) ?: return null
        val score = brew.score(recipe)
        if (!score.completed()) {
            return null
        }
        val scoreValue = score.score()
        val recipeKey = recipe.recipeName
        if (!appliesTo(player, brew, scoreValue, recipeKey)) {
            return null
        }
        val brewModified = brew.withMeta(COMPLETED_RECIPE_KEY, MetaDataType.STRING, recipeKey)
            .withMeta(COMPLETED_BY_KEY, UuidMetaDataType, player.uniqueId)
            .withMeta(COMPLETED_SCORE_KEY, MetaDataType.DOUBLE, scoreValue)
        BreweryRecipes.completedRecipeManager.insertOrUpdateRecipeCompletion(
            player.uniqueId,
            TBPRecipeConverter.convert(recipe.recipeName, brew.completedSteps, score = scoreValue)
        )
        return brewModified
    }

    private fun onInventoryExtract(event: ItemTransactionEvent<ItemSource.ItemBasedSource>, player: Player?) {
        player ?: return
        val result = event.transactionSession.result?.itemStack ?: return
        val brew = api.brewManager.fromItem(result).orElse(null) ?: return
        val brewModified = actOnBrew(brew, player) ?: return
        event.transactionSession.result = ItemSource
            .ItemBasedSource(api.brewManager.toItem(brewModified, Brew.State.Other()))
    }

    private fun appliesTo(player: Player, brew: Brew, score: Double, recipeKey: String): Boolean {
        if ((brew.meta(COMPLETED_BY_KEY, UuidMetaDataType)?.let { player.uniqueId != it }) ?: false) {
            return false
        }
        return recipeKey != brew.meta(COMPLETED_RECIPE_KEY, MetaDataType.STRING) ||
                (brew.meta(COMPLETED_SCORE_KEY, MetaDataType.DOUBLE) ?: Double.MIN_VALUE) < score
    }
}
