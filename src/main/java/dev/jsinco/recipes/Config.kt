package dev.jsinco.recipes

import com.dre.brewery.api.addons.AddonFileManager
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object Config {

    private lateinit var addonFileManager: AddonFileManager
    private var configFile: File? = null
    private var config: YamlConfiguration? = null

    @JvmStatic
    fun get(): YamlConfiguration {
        if (configFile == null || config == null) {
            addonFileManager = Recipes.getAddonFileManager()
            addonFileManager.generateFile("recipesConfig.yml")
            configFile = addonFileManager.getFile("recipesConfig.yml")
            config = YamlConfiguration.loadConfiguration(configFile!!)
        }
        return config!!
    }

    @JvmStatic
    fun reload() {
        addonFileManager = Recipes.getAddonFileManager()
        addonFileManager.generateFile("recipesConfig.yml")
        configFile = addonFileManager.getFile("recipesConfig.yml")
        config = YamlConfiguration.loadConfiguration(configFile!!)
    }

    @JvmStatic
    fun save() {
        config?.save(configFile!!)
    }
}