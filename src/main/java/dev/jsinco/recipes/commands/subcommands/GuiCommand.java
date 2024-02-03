package dev.jsinco.recipes.commands.subcommands;

import com.dre.brewery.BreweryPlugin;
import dev.jsinco.recipes.Config;
import dev.jsinco.recipes.Util;
import dev.jsinco.recipes.commands.SubCommand;
import dev.jsinco.recipes.guis.RecipeGui;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class GuiCommand implements SubCommand {

    @Override
    public void execute(@NotNull BreweryPlugin plugin, @NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.colorcode("&cUsage: /breweryrecipes gui <player> (Open the recipe GUI of a player)"));
            return;
        }
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(Util.colorcode("&cOnly players can use this command (Open the recipe GUI of a player)"));
            return;
        }

        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null) {
            sender.sendMessage(Util.colorcode("&cPlayer not found"));
            return;
        }

        RecipeGui recipeGui = new RecipeGui(player);
        recipeGui.openRecipeGui(viewer);
    }

    @Nullable
    @Override
    public List<String> tabComplete(@NotNull BreweryPlugin plugin, @NotNull CommandSender sender, @NotNull String[] args) {
        return null;
    }

    @NotNull
    @Override
    public String getPermission() {
        return Objects.requireNonNullElse(Config.get().getString("command-permissions.gui"), "breweryrecipes.gui");
    }
}
