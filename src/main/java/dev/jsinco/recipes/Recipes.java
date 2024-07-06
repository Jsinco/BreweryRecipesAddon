package dev.jsinco.recipes;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.api.addons.AddonFileManager;
import com.dre.brewery.api.addons.AddonLogger;
import com.dre.brewery.api.addons.AddonManager;
import com.dre.brewery.api.addons.BreweryAddon;
import com.dre.brewery.utility.MinecraftVersion;
import dev.jsinco.recipes.commands.CommandManager;
import dev.jsinco.recipes.listeners.Events;
import dev.jsinco.recipes.permissions.CommandPermission;
import dev.jsinco.recipes.permissions.LuckPermsPermission;
import dev.jsinco.recipes.permissions.PermissionAPI;
import dev.jsinco.recipes.permissions.PermissionManager;
import dev.jsinco.recipes.permissions.PermissionSetter;
import dev.jsinco.recipes.recipe.RecipeUtil;
import org.bukkit.Bukkit;

// Idea:
// Allow recipes for brews to be collected from randomly generated chests and make some recipes rarer than others
// Has a gui that shows all the recipes the player has collected and how to make them
// Pulls directly from the Brewery plugin's config.yml file

public class Recipes extends BreweryAddon {

    private static AddonFileManager addonFileManager;
    private static PermissionManager permissionManager;
    private static CommandManager commandManager;
    private static AddonLogger logger;
    //private static boolean bukkitPersistence;

    public Recipes(BreweryPlugin superPlugin, AddonLogger superLogger) {
        super(superPlugin, logger);
        logger = superLogger;
    }

    @Override
    public void onAddonEnable(AddonFileManager superAddonFileManager) {
        addonFileManager = superAddonFileManager;


        MinecraftVersion mcV = BreweryPlugin.getMCVersion();
        if (mcV.isOrEarlier(MinecraftVersion.V1_13)) {
            logger.info("This addon uses PersistentDataContainers (PDC) which were added in API version 1.14.1. This addon is not compatible with your server version: &7(" + mcV.getVersion() + ")");
        }


        commandManager = new CommandManager(plugin);

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
        Bukkit.getPluginManager().registerEvents(new Events(plugin), plugin);
        registerCommand(true);

        getAddonLogger().info("Loaded &a" + RecipeUtil.getAllRecipeKeys().size() + " &rrecipes from Brewery!");

        LazyConfigUpdater configUpdater = new LazyConfigUpdater();
        if (configUpdater.isConfigOutdated()) {
            configUpdater.createNewConfigFile();
        }
    }

    @Override
    public void onBreweryReload() {
        Config.reload();
        Util.reloadPrefix();
        registerCommand(false);
        getAddonLogger().info("Loaded &a" + RecipeUtil.getAllRecipeKeys().size() + " &rrecipes from Brewery!");
    }

    public static AddonFileManager getAddonFileManager() {
        return addonFileManager;
    }

    public static PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public static AddonLogger getAddonLogger() {
        return logger;
    }


    private void registerCommand(boolean startup) { // edit this probably
        if (!startup) {
            com.dre.brewery.commands.CommandManager.removeSubCommand("recipes");
            AddonManager.unRegisterAddonCommand(commandManager);
        }

        if (Config.get().getBoolean("use-brewery-subcommand")) {
            com.dre.brewery.commands.CommandManager.addSubCommand("recipes", commandManager);
        } else {
            AddonManager.registerAddonCommand(commandManager);
        }
    }

    private void goToDefaultPermissionMethod() {
        Config.get().set("recipe-saving-method", "Command");
        Config.save();
        permissionManager = new CommandPermission();
    }

    //public static boolean isBukkitPersistence() {
    //    return bukkitPersistence;
    //}
}