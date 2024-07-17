package dev.jsinco.recipes.permissions;

import com.dre.brewery.BreweryPlugin;
import dev.jsinco.recipes.Config;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandPermission implements PermissionManager {
    @Override
    public void setPermission(String permission, Player player, boolean value) {
        BreweryPlugin.getScheduler().runTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Config.get().getString("permission-command")
                .replace("%player%", player.getName()).replace("%permission%", permission)
                .replace("%boolean%", String.valueOf(value))));
    }

    @Override
    public void removePermission(String permission, Player player) {
        BreweryPlugin.getScheduler().runTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Config.get().getString("permission-unset-command")
                .replace("%player%", player.getName()).replace("%permission%", permission)
                .replace("%boolean%", "false")));
    }
}
