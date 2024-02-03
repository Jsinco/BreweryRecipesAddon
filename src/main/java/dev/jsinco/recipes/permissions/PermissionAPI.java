package dev.jsinco.recipes.permissions;

import org.bukkit.Bukkit;

public enum PermissionAPI {
    LUCKPERMS("LuckPerms");

    private final String pluginName;

    PermissionAPI(String pluginName) {
        this.pluginName = pluginName;
    }

    public boolean checkIfPermissionPluginExists() {
        return Bukkit.getPluginManager().getPlugin(pluginName) != null;
    }
}
