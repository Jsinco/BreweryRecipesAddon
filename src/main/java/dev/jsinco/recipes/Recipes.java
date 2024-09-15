package dev.jsinco.recipes;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.api.addons.AddonFileManager;
import com.dre.brewery.api.addons.AddonInfo;
import com.dre.brewery.api.addons.AddonManager;
import com.dre.brewery.api.addons.BreweryAddon;
import com.dre.brewery.commands.CommandManager;
import com.dre.brewery.utility.MinecraftVersion;
import dev.jsinco.recipes.commands.AddonCommandManager;
import dev.jsinco.recipes.listeners.Events;
import dev.jsinco.recipes.permissions.CommandPermission;
import dev.jsinco.recipes.permissions.LuckPermsPermission;
import dev.jsinco.recipes.permissions.PermissionAPI;
import dev.jsinco.recipes.permissions.PermissionManager;
import dev.jsinco.recipes.permissions.PermissionSetter;
import dev.jsinco.recipes.recipe.RecipeUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

// Idea:
// Allow recipes for brews to be collected from randomly generated chests and make some recipes rarer than others
// Has a gui that shows all the recipes the player has collected and how to make them
// Pulls directly from the Brewery plugin's config.yml file

@AddonInfo(
        author = "Jsinco",
        version = "1.14",
        description = "A unique way to collect and view recipes for brews",
        name = "Recipes"
)
public class Recipes extends BreweryAddon {

    private static PermissionManager permissionManager;
    private static AddonFileManager fileManager;
    private static Events events;
    private static Recipes addonInstance;


    @Override
    public void onAddonEnable() {
        addonInstance = this;
        fileManager = getAddonFileManager();


        MinecraftVersion mcV = BreweryPlugin.getMCVersion();
        if (mcV.isOrEarlier(MinecraftVersion.V1_13)) {
            getAddonLogger().info("This addon uses PersistentDataContainers (PDC) which were added in API version 1.14.1. This addon is not compatible with your server version: &7(" + mcV.getVersion() + ")");
        }


        BreweryPlugin.getScheduler().runTaskLater(() -> {
            switch (PermissionSetter.valueOf(Config.get().getString("recipe-saving-method").toUpperCase())) {
                case PERMISSION_API -> {
                    if (PermissionAPI.valueOf(Config.get().getString("permissions-api-plugin").toUpperCase()) == PermissionAPI.LUCKPERMS) {
                        if (PermissionAPI.LUCKPERMS.checkIfPermissionPluginExists()) {
                            permissionManager = new LuckPermsPermission();
                        } else {
                            goToDefaultPermissionMethod();
                        }
                    }

                }
                case COMMAND -> permissionManager = new CommandPermission();
            }
        }, 1L);

        events = new Events(getBreweryPlugin());
        Bukkit.getPluginManager().registerEvents(events, getBreweryPlugin());
        com.dre.brewery.commands.CommandManager.addSubCommand("recipes", new AddonCommandManager(getBreweryPlugin()));

        RecipeUtil.loadAllRecipes();
        getAddonLogger().info("Loaded &a" + RecipeUtil.getAllRecipeKeys().size() + " &rrecipes from Brewery!");

        LazyConfigUpdater configUpdater = new LazyConfigUpdater();
        if (configUpdater.isConfigOutdated()) {
            configUpdater.createNewConfigFile();
        }
    }

    @Override
    public void onAddonDisable() {
        if (events != null) {
            HandlerList.unregisterAll(events);
        }
        com.dre.brewery.commands.CommandManager.removeSubCommand("recipes");
        getAddonLogger().info("Recipes addon disabled.");
    }

    @Override
    public void onBreweryReload() {
        Config.reload();
        RecipeUtil.loadAllRecipes();
        Util.reloadPrefix();
        getAddonLogger().info("Loaded &a" + RecipeUtil.getAllRecipeKeys().size() + " &rrecipes from Brewery!");
    }

    public static AddonFileManager fileManager() {
        return fileManager;
    }

    public static PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public static Recipes getAddonInstance() {
        return addonInstance;
    }



    private void goToDefaultPermissionMethod() {
        Config.get().set("recipe-saving-method", "Command");
        Config.save();
        permissionManager = new CommandPermission();
    }
}