package dev.jsinco.recipes.recipe

import dev.jsinco.recipes.BreweryRecipes
import dev.jsinco.recipes.integration.BrewingIntegration
import dev.jsinco.recipes.recipe.flaws.Flaw
import dev.jsinco.recipes.recipe.flaws.FlawExtent
import dev.jsinco.recipes.recipe.flaws.FlawTextModificationWriter
import dev.jsinco.recipes.recipe.flaws.FlawTextModifications
import dev.jsinco.recipes.recipe.flaws.type.FlawType
import dev.jsinco.recipes.recipe.process.IngredientStep
import dev.jsinco.recipes.recipe.process.Step
import dev.jsinco.recipes.recipe.process.steps.AgeStep
import dev.jsinco.recipes.recipe.process.steps.CookStep
import dev.jsinco.recipes.recipe.process.steps.MixStep
import dev.jsinco.recipes.util.ItemColorUtil
import dev.jsinco.recipes.util.TranslationUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.translation.Argument
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.util.*

object RecipeViewLoreWriter {

    var cookingMinuteTicks = 20L * 60L
    var agingYearTicks = 20L * 60L * 20L

    // no touchy
    var version: Int = 0
    fun bumpVersion() {
        version++
    }

    fun writeLore(
        recipeView: RecipeView,
        brewingIntegration: BrewingIntegration,
        stepsOverride: List<Step>? = null,
        isBrewNote: Boolean = false
    ): List<Component>? {
        cookingMinuteTicks = brewingIntegration.cookingMinuteTicks()
        agingYearTicks = brewingIntegration.agingYearTicks()
        val recipe = BreweryRecipes.brewingIntegration.getRecipe(recipeView.recipeIdentifier) ?: return null
        val stepsToRender = stepsOverride ?: recipe.steps
        val ordinals = listOf("①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩")
        val loreConfig = BreweryRecipes.guiConfig.recipes.lore
        val result = mutableListOf<Component>()
        val noIndentIndices = mutableSetOf<Int>()

        if (if (isBrewNote) loreConfig.showDifficultyInBrewNotes else loreConfig.showBrewDifficulty) {
            if (loreConfig.emptyLineAboveBrewDifficulty) result.add(Component.empty())
            val difficulty = recipe.difficulty
            if (!loreConfig.applyIndentationToBrewDifficulty) noIndentIndices.add(result.size)
            result.add(
                TranslationUtil.render(
                    Component.translatable(
                        "breweryrecipes.gui.recipes.lore.difficulty",
                        Argument.tagResolver(
                            TagResolver.resolver("difficultycolor", Tag.styling(difficultyColor(difficulty))),
                            Placeholder.unparsed("difficulty", formatDifficulty(difficulty))
                        )
                    ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                )
            )
        }

        if (loreConfig.emptyLineAboveSteps) result.add(Component.empty())

        stepsToRender.forEachIndexed { index, step ->
            val stepComponent = renderStep(step, index, recipeView.flaws, recipeView.invertedReveals, isBrewNote)
                .colorIfAbsent(NamedTextColor.GRAY)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            val rendered = TranslationUtil.render(stepComponent)
            val ordinal = ordinals.getOrElse(index) { "${index + 1}." }

            result.add(
                TranslationUtil.render(
                    Component.translatable(
                        "breweryrecipes.gui.recipes.lore.step.header",
                        Argument.tagResolver(
                            Placeholder.unparsed("ordinal", ordinal),
                            Placeholder.component("step", rendered)
                        )
                    ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                )
            )

            if (step is IngredientStep) {
                for ((ingredient, amount) in step.ingredients()) {
                    val itemColorTag = ItemColorUtil.getHex(ingredient.key)
                        ?.let { TextColor.fromHexString(it) }
                        ?.let { Tag.styling(it) }
                        ?: Tag.selfClosingInserting(Component.empty())
                    val brewColorTag = if (ingredient.key.startsWith("brewery:"))
                        BreweryRecipes.brewingIntegration.brewIngredientColor(ingredient.key)
                            ?.let { TextColor.color(it.asRGB()) }
                            ?.let { Tag.styling(it) }
                            ?: Tag.selfClosingInserting(Component.empty())
                    else Tag.selfClosingInserting(Component.empty())
                    val ingredientComp = Component.translatable(
                        "breweryrecipes.gui.recipes.lore.step.ingredient",
                        Argument.tagResolver(
                            Formatter.number("count", amount),
                            Placeholder.component("name", ingredient.displayName),
                            TagResolver.resolver("itemcolor", itemColorTag),
                            TagResolver.resolver("brewcolor", brewColorTag)
                        )
                    ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                    result.add(
                        TranslationUtil.render(
                            applyFlaws(ingredientComp, index, recipeView.flaws, recipeView.invertedReveals)
                        )
                    )
                }
            }

            when (step) {
                is CookStep -> step.cauldronType?.let {
                    result.add(
                        TranslationUtil.render(
                            applyFlaws(
                                buildTypeLine(
                                    "breweryrecipes.gui.recipes.lore.step.cauldron",
                                    "breweryrecipes.gui.recipes.lore.step.cauldron.type.${it.name.lowercase(Locale.ROOT)}",
                                    "cauldron_type"
                                ), index, recipeView.flaws, recipeView.invertedReveals
                            )
                        )
                    )
                }

                is MixStep -> step.cauldronType?.let {
                    result.add(
                        TranslationUtil.render(
                            applyFlaws(
                                buildTypeLine(
                                    "breweryrecipes.gui.recipes.lore.step.cauldron",
                                    "breweryrecipes.gui.recipes.lore.step.cauldron.type.${it.name.lowercase(Locale.ROOT)}",
                                    "cauldron_type"
                                ), index, recipeView.flaws, recipeView.invertedReveals
                            )
                        )
                    )
                }

                is AgeStep -> {
                    result.add(
                        TranslationUtil.render(
                            applyFlaws(
                                buildTypeLine(
                                    "breweryrecipes.gui.recipes.lore.step.barrel",
                                    "breweryrecipes.gui.recipes.lore.step.barrel.type.${
                                        step.barrelType.name.lowercase(
                                            Locale.ROOT
                                        )
                                    }",
                                    "barrel_type"
                                ), index, recipeView.flaws, recipeView.invertedReveals
                            )
                        )
                    )
                }
            }

            if (loreConfig.emptyLineBetweenSteps && index < stepsToRender.size - 1) {
                result.add(Component.empty())
            }
        }

        if (loreConfig.emptyLineBelowSteps) result.add(Component.empty())

        val prefix = if (loreConfig.indentation > 0) Component.text(" ".repeat(loreConfig.indentation)) else null
        val suffix = if (loreConfig.trailingSpaces > 0) Component.text(" ".repeat(loreConfig.trailingSpaces)) else null
        if (prefix != null || suffix != null) {
            return result.mapIndexed { index, line ->
                if (index in noIndentIndices) return@mapIndexed line
                var out = line
                if (prefix != null) out = prefix.append(out)
                if (suffix != null) out = out.append(suffix)
                out
            }
        }
        return result
    }

    private fun formatDifficulty(difficulty: Double): String =
        "%.2f".format(difficulty).trimEnd('0').trimEnd('.')

    private fun difficultyColor(difficulty: Double): TextColor {
        val green = TextColor.color(0x55FF55)
        val yellow = TextColor.color(0xFFFF55)
        val red = TextColor.color(0xFF5555)
        val darkRed = TextColor.color(0xCC2222)

        return if (difficulty <= 10.0) {
            val normalized = (difficulty / 10.0).coerceIn(0.0, 1.0).toFloat()
            when {
                normalized < 0.5f -> TextColor.lerp(normalized / 0.5f, green, yellow)
                else -> TextColor.lerp((normalized - 0.5f) / 0.5f, yellow, red)
            }
        } else {
            val normalized = ((difficulty - 10.0) / 10.0).coerceIn(0.0, 1.0).toFloat()
            TextColor.lerp(normalized, red, darkRed)
        }
    }

    private fun buildTypeLine(lineKey: String, typeKey: String, typePlaceholder: String): Component {
        return Component.translatable(
            lineKey,
            Argument.tagResolver(Placeholder.component(typePlaceholder, Component.translatable(typeKey)))
        ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
    }


    private fun buildBaseStep(step: Step, isBrewNote: Boolean = false): Component {
        return TranslationUtil.render(if (isBrewNote) step.displayBrewNote() else step.display())
    }

    private fun applyFlaws(
        component: Component,
        stepIndex: Int,
        flaws: List<Flaw>,
        reveals: List<Set<Int>>
    ): Component {
        if (flaws.isEmpty()) return component
        val base = resolveTranslatablesForMutation(component)
        val textModifications = compileTextModifications(base, stepIndex, flaws)
            .map { it.key to it.value.withMatching { idx -> reveals.isEmpty() || reveals[stepIndex].contains(idx) } }
            .toMap()
        var output = base
        var offsets = mapOf<Int, Int>()
        for (flaw in flaws) {
            val textModification = textModifications[flaw] ?: continue
            output = FlawTextModificationWriter.process(output, textModification, flaw, offsets)
            offsets = textModification.offsets(offsets)
        }
        return output
    }

    private fun renderStep(
        step: Step,
        stepIndex: Int,
        flaws: List<Flaw>,
        reveals: List<Set<Int>>,
        isBrewNote: Boolean = false
    ): Component {
        return applyFlaws(buildBaseStep(step, isBrewNote), stepIndex, flaws, reveals)
    }

    private fun compileTextModifications(
        step: Component,
        stepIndex: Int,
        flaws: List<Flaw>
    ): Map<Flaw, FlawTextModifications> {
        val allTextModifications = mutableMapOf<Flaw, FlawTextModifications>()
        if (flaws.isEmpty()) {
            return allTextModifications
        }
        val flawPositions = mutableListOf<Int>()
        for (flaw in flaws) {
            if (flawApplies(stepIndex, flaw)) {
                val session = FlawType.ModificationFindSession(stepIndex, flaw.config) {
                    !flawPositions.contains(it)
                }
                val textModifications = flaw.type.findFlawModifications(step, session)
                flawPositions.addAll(
                    textModifications.modifiedPoints
                        .keys
                )
                allTextModifications[flaw] = textModifications
            }
        }
        return allTextModifications
            .filter { !it.value.modifiedPoints.isEmpty() && !it.value.modifiedPoints.all { entry -> entry.value is FlawTextModifications.NoModification } }
    }

    fun estimateFragmentation(recipeView: RecipeView): Double {
        val recipe = BreweryRecipes.brewingIntegration.getRecipe(recipeView.recipeIdentifier) ?: return 100.0
        if (recipe.steps.isEmpty()) return 0.0

        var fragmentation = 0.0

        recipe.steps.forEachIndexed { idx, step ->
            val base = resolveTranslatablesForMutation(buildBaseStep(step))
            val approxBaseLength = PlainTextComponentSerializer.plainText().serialize(base).length
            val modifications = compileTextModifications(base, idx, recipeView.flaws)
                .map {
                    it.key to it.value.withMatching { pos ->
                        recipeView.invertedReveals.isEmpty() || recipeView.invertedReveals[idx].contains(pos)
                    }
                }.toMap()
            if (modifications.isEmpty()) {
                return@forEachIndexed
            }
            fragmentation += modifications.values.sumOf { it.intensity(approxBaseLength) }
        }

        return fragmentation / recipe.steps.size * 100.0
    }

    fun clearRedundantFlaws(view: RecipeView, thresholdPercent: Double = 15.0): RecipeView {
        val applicableFlaws = mutableSetOf<Flaw>()
        val recipe = BreweryRecipes.brewingIntegration.getRecipe(view.recipeIdentifier) ?: return view
        recipe.steps.forEachIndexed { index, step ->
            compileTextModifications(resolveTranslatablesForMutation(buildBaseStep(step)), index, view.flaws)
                .keys.forEach { applicableFlaws.add(it) }
        }

        val newFlaws = view.flaws.filter { applicableFlaws.contains(it) }
        val pct = estimateFragmentation(view)
        return if (pct < thresholdPercent) {
            RecipeView(view.recipeIdentifier, emptyList(), emptyList())
        } else {
            RecipeView(view.recipeIdentifier, newFlaws, view.invertedReveals)
        }
    }

    fun mergeFlaws(base: RecipeView, toSubtract: RecipeView): RecipeView {
        val recipe = BreweryRecipes.brewingIntegration.getRecipe(base.recipeIdentifier) ?: return base
        val flawPositions = mutableListOf<MutableSet<Int>>()
        for (i in 0..<recipe.steps.size) {
            val step = recipe.steps[i]
            val positions = mutableSetOf<Int>()
            for (flaw in toSubtract.flaws) {
                if (flawApplies(i, flaw)) {
                    val session = FlawType.ModificationFindSession(i, flaw.config) {
                        !positions.contains(it)
                    }
                    val textModifications = flaw.type.findFlawModifications(buildBaseStep(step), session)
                    positions.addAll(
                        textModifications.modifiedPoints
                            .keys
                    )
                }
            }
            flawPositions.add(positions)
        }
        val invertedReveals = if (base.invertedReveals.isEmpty()) {
            flawPositions
        } else {
            base.invertedReveals
                .mapIndexed { idx, stepReveals ->
                    stepReveals.filter { andMask(it, flawPositions[idx]) }
                        .toSet()
                }
        }
        return RecipeView(
            base.recipeIdentifier, base.flaws, invertedReveals
        )
    }

    private fun andMask(i: Int, ints: Set<Int>, radius: Int = 1): Boolean {
        for (idx in (i - radius)..<(i + radius)) {
            if (!ints.contains(idx)) {
                return false
            }
        }
        return true
    }

    private fun flawApplies(stepIndex: Int, flaw: Flaw): Boolean {
        return when (flaw.config.extent) {
            is FlawExtent.Everywhere -> true
            is FlawExtent.WholeStep -> stepIndex == flaw.config.extent.stepIndex
            is FlawExtent.StepRange -> stepIndex == flaw.config.extent.stepIndex
            is FlawExtent.AfterPoint -> stepIndex >= flaw.config.extent.stepIndex
            else -> false
        }
    }

    private fun resolveTranslatablesForMutation(node: Component): Component {
        val mappedChildren = node.children().map { resolveTranslatablesForMutation(it) }
        val withChildren = node.children(mappedChildren)

        return when (withChildren) {
            is TranslatableComponent -> {
                val rendered = TranslationUtil.render(withChildren)
                if (rendered !is TranslatableComponent) {
                    resolveTranslatablesForMutation(rendered).style(withChildren.style())
                } else {
                    Component.text(
                        BreweryRecipes.instance.translator?.findClientSideTranslation(rendered.key()) ?: rendered.key()
                    )
                }
            }

            else -> withChildren
        }
    }

}