package dev.jsinco.recipes;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.api.addons.AddonFileManager;
import com.dre.brewery.api.addons.AddonLogger;
import com.dre.brewery.api.addons.AddonManager;
import com.dre.brewery.api.addons.BreweryAddon;
import dev.jsinco.recipes.commands.CommandManager;
import dev.jsinco.recipes.listeners.Events;
import dev.jsinco.recipes.permissions.*;
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

    public Recipes(BreweryPlugin superPlugin, AddonLogger logger) {
        super(superPlugin, logger);
        plugin = superPlugin;
    }

    @Override
    public void onAddonEnable(AddonFileManager addonFileManager) {
        Recipes.addonFileManager = addonFileManager;
        commandManager = new CommandManager(plugin);

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
        Bukkit.getPluginManager().registerEvents(new Events(plugin), plugin);
        registerCommand(true);
        getLogger().info("Enabled!");
    }

    @Override
    public void onBreweryReload() {
        Config.reload();
        Util.reloadPrefix();
        registerCommand(false);
        getLogger().info("Reloaded!");
    }

    @Override
    public void onAddonDisable() {
        getLogger().info("Disabled!");
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

    @Override
    public AddonLogger getLogger() {
        return super.getLogger();
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
}