package dev.jsinco.recipes

import dev.jsinco.recipes.commands.RecipesCommand
import dev.jsinco.recipes.configuration.GuiConfig
import dev.jsinco.recipes.configuration.RecipesConfig
import dev.jsinco.recipes.configuration.RecipesTranslator
import dev.jsinco.recipes.configuration.SpawnConfig
import dev.jsinco.recipes.configuration.serialize.*
import dev.jsinco.recipes.data.DataManager
import dev.jsinco.recipes.data.storage.StorageImpl
import dev.jsinco.recipes.gui.RecipeGuiItemCache
import dev.jsinco.recipes.integration.BreweryXBrewingIntegration
import dev.jsinco.recipes.integration.BrewingIntegration
import dev.jsinco.recipes.integration.TbpBrewingIntegration
import dev.jsinco.recipes.listeners.*
import dev.jsinco.recipes.recipe.BreweryRecipe
import dev.jsinco.recipes.recipe.RecipeCompletionManager
import dev.jsinco.recipes.recipe.RecipeViewLoreWriter
import dev.jsinco.recipes.recipe.RecipeViewManager
import dev.jsinco.recipes.util.BookUtil
import dev.jsinco.recipes.util.ClassUtil
import eu.okaeri.configs.ConfigManager
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.block.Biome
import org.bukkit.block.BlockType
import org.bukkit.inventory.ItemType
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

// Idea:
// Allow recipes for brews to be collected from randomly generated chests and make some recipes rarer than others
// Has a gui that shows all the recipes the player has collected and how to make them
// Pulls directly from the Brewery plugin's config.yml file
class BreweryRecipes : JavaPlugin() {
    companion object {
        lateinit var instance: BreweryRecipes
        lateinit var recipesConfig: RecipesConfig
        lateinit var guiConfig: GuiConfig
        lateinit var spawnConfig: SpawnConfig
        lateinit var recipeViewManager: RecipeViewManager
        lateinit var completedRecipeManager: RecipeCompletionManager
        lateinit var recipeGuiItemCache: RecipeGuiItemCache
        lateinit var brewingIntegration: BrewingIntegration

        fun key(key: String): NamespacedKey {
            if (key.contains(":")) {
                return NamespacedKey.fromString(key) ?: NamespacedKey("brewery_recipes", key)
            }
            return NamespacedKey("brewery_recipes", key)
        }
    }

    lateinit var storageImpl: StorageImpl
    var translator: RecipesTranslator? = null

    override fun onEnable() {
        recipesConfig = readConfig()
        guiConfig = readGuiConfig()
        spawnConfig = readSpawnConfig()
        storageImpl = DataManager(dataFolder).storageImpl
        recipeViewManager = RecipeViewManager(storageImpl)
        completedRecipeManager = RecipeCompletionManager(storageImpl)
        recipeGuiItemCache = RecipeGuiItemCache()
        brewingIntegration = loadGuiIntegration()
        brewingIntegration.enable(this)
        translator = RecipesTranslator(File(dataFolder, "locale"), recipesConfig.language).also {
            it.reload()
            GlobalTranslator.translator().addSource(it)
        }
        val playerEventListener = PlayerEventListener(recipeViewManager, completedRecipeManager, recipeGuiItemCache)
        Bukkit.getPluginManager().registerEvents(GuiEventListener(), this)
        Bukkit.getPluginManager().registerEvents(RecipeSpawningListener(), this)
        Bukkit.getPluginManager().registerEvents(RecipeListener(), this)
        Bukkit.getPluginManager().registerEvents(MigrationListener(), this)
        Bukkit.getPluginManager().registerEvents(playerEventListener, this)
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
            it.registrar().register(RecipesCommand.command())
        }
        recipesConfig.book.craftingRecipe.register(BookUtil.createBook(), "recipes_book")
        spawnConfig.recipeSpawning
            .forEachIndexed { index, definition -> definition.registerRecipe(index) }
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(
            this,
            { playerEventListener.tick() },
            1,
            20
        )
    }

    private fun loadRecipeProvider(): List<BreweryRecipe>? {
        if (ClassUtil.exists("dev.jsinco.brewery.bukkit.api.TheBrewingProjectApi")) {

        }
        throw IllegalStateException("Expected either BreweryX to be available or TBP")
    }

    private fun loadGuiIntegration(): BrewingIntegration {
        if (ClassUtil.exists("dev.jsinco.brewery.bukkit.api.TheBrewingProjectApi")) {
            return TbpBrewingIntegration
        }
        if (ClassUtil.exists("com.dre.brewery.recipe.BRecipe")) {
            return BreweryXBrewingIntegration
        }
        throw IllegalStateException("Expected either BreweryX to be available or TBP")
    }

    private fun readConfig(): RecipesConfig {
        return ConfigManager.create(RecipesConfig::class.java) {
            it.withConfigurer(YamlBukkitConfigurer(), configSerializers().build())
            it.withBindFile(File(this.dataFolder, "config.yml"))
            it.saveDefaults()
            it.load(true)
            it.save()
        }
    }

    private fun readGuiConfig(): GuiConfig {
        return ConfigManager.create(GuiConfig::class.java) {
            it.withConfigurer(YamlBukkitConfigurer(), configSerializers().build())
            it.withBindFile(File(this.dataFolder, "gui.yml"))
            it.saveDefaults()
            it.load(true)
            it.save()
        }
    }

    private fun configSerializers(): SerdesPackBuilder {
        return SerdesPackBuilder()
            .add(ComponentSerializer)
            .add(KeySerializer)
            .add(ConfigItemSerializer)
            .add(ConfigItemCollectionSerializer)
            .add(LoreSerializer)
            .add(LocaleSerializer)
            .add(ConditionsDefinitionSerializer)
            .add(TriggersDefinitionSerializer)
            .add(KeyedSerializer(RegistryKey.BLOCK, BlockType::class.java))
            .add(KeyedSerializer(RegistryKey.BIOME, Biome::class.java))
            .add(KeyedSerializer(RegistryKey.ITEM, ItemType::class.java))
            .add(SpawnDefinitionSerializer)
    }

    private fun readSpawnConfig(): SpawnConfig {
        return ConfigManager.create(SpawnConfig::class.java) {
            it.withConfigurer(YamlBukkitConfigurer(), configSerializers().build())
            it.withBindFile(File(this.dataFolder, "spawning.yml"))
            it.saveDefaults()
            it.load(true)
            it.save()
        }
    }

    override fun onLoad() {
        instance = this
    }

    fun reload() {
        brewingIntegration.reload()
        recipesConfig = readConfig()
        guiConfig = readGuiConfig()
        spawnConfig = readSpawnConfig()
        translator?.let { GlobalTranslator.translator().removeSource(it) }
        translator = RecipesTranslator(File(dataFolder, "locale"), recipesConfig.language).also {
            it.reload()
            GlobalTranslator.translator().addSource(it)
        }
        recipeGuiItemCache.clearGlobal()
        RecipeViewLoreWriter.bumpVersion()
        recipesConfig.book.craftingRecipe.register(recipesConfig.book.item.generateItem(), "breweryrecipes_book")
        spawnConfig.recipeSpawning
            .forEachIndexed { index, definition -> definition.registerRecipe(index) }
    }
}