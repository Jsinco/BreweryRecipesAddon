package dev.jsinco.recipes;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.api.addons.AddonFileManager;
import com.dre.brewery.api.addons.AddonLogger;
import com.dre.brewery.api.addons.AddonManager;
import com.dre.brewery.api.addons.BreweryAddon;
import dev.jsinco.recipes.commands.CommandManager;
import dev.jsinco.recipes.listeners.Events;
import dev.jsinco.recipes.permissions.*;
import dev.jsinco.recipes.recipe.RecipeUtil;
import org.bukkit.Bukkit;

// Idea:
// Allow recipes for brews to be collected from randomly generated chests and make some recipes rarer than others
// Has a gui that shows all the recipes the player has collected and how to make them
// Pulls directly from the Brewery plugin's config.yml file

public class Recipes extends BreweryAddon {

    private static AddonFileManager addonFileManager;
    private static BreweryPlugin plugin;
    private static PermissionManager permissionManager;
    private static CommandManager commandManager;
    private static AddonLogger logger;
    private static boolean bukkitPersistence;

    public Recipes(BreweryPlugin superPlugin, AddonLogger superLogger) {
        super(superPlugin, logger);
        plugin = superPlugin;
        logger = superLogger;
    }

    @Override
    public void onAddonEnable(AddonFileManager addonFileManager) {
        Recipes.addonFileManager = addonFileManager;


        // Version check for when I add in support for versions lower than 1.17
        // returns something like: 1.18.2-R0.1-SNAPSHOT so we strip it to 1.18.2 then to 18 and check that major version is 17 or higher
        String versionStripped = Bukkit.getBukkitVersion().substring(0, Bukkit.getBukkitVersion().indexOf("-"));
        String[] versionSplit = versionStripped.split("\\.");
        versionStripped = versionSplit[1];
        bukkitPersistence = Integer.parseInt(versionStripped) >= 17;



        commandManager = new CommandManager(plugin);

        BreweryPlugin.getScheduler().runTaskLater(() -> {
            switch (PermissionSetter.valueOf(Config.get().getString("recipe-saving-method").toUpperCase())) {
                case PERMISSION_API -> {
                    switch (PermissionAPI.valueOf(Config.get().getString("permissions-api-plugin").toUpperCase())) {
                        case LUCKPERMS -> {
                            if (PermissionAPI.LUCKPERMS.checkIfPermissionPluginExists()) {
                                permissionManager = new LuckPermsPermission();
                            } else {
                                goToDefaultPermissionMethod();
                            }
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

    public static BreweryPlugin getPlugin() {
        return plugin;
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

    public static boolean isBukkitPersistence() {
        return bukkitPersistence;
    }
}