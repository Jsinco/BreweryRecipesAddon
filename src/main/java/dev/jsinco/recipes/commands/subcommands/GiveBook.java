package dev.jsinco.recipes.commands.subcommands;

import com.dre.brewery.BreweryPlugin;
import dev.jsinco.recipes.Config;
import dev.jsinco.recipes.Util;
import dev.jsinco.recipes.commands.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class GiveBook implements SubCommand {

    @Override
    public void execute(@NotNull BreweryPlugin plugin, @NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Util.colorcode("&cUsage: /breweryrecipes givebook <player> (Give a player the recipe book)"));
            return;
        }

        Player player = Bukkit.getPlayerExact(args[1]);
        if (player != null) {
            Util.giveItem(player, Util.getRecipeBookItem());
        }
    }

    @Nullable
    @Override
    public List<String> tabComplete(@NotNull BreweryPlugin plugin, @NotNull CommandSender sender, @NotNull String[] args) {
        return null;
    }

    @NotNull
    @Override
    public String getPermission() {
        return Objects.requireNonNullElse(Config.get().getString("command-permissions.givebook"), "breweryrecipes.givebook");
    }
}
