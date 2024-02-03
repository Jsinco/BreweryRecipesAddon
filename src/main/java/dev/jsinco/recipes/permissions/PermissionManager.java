package dev.jsinco.recipes.permissions;

import org.bukkit.entity.Player;

public interface PermissionManager {

    void setPermission(String permission, Player player, boolean value);

    void removePermission(String permission, Player player);
}
