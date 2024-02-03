package dev.jsinco.recipes.commands.subcommands;

import com.dre.brewery.BreweryPlugin;
import dev.jsinco.recipes.Config;
import dev.jsinco.recipes.Util;
import dev.jsinco.recipes.commands.SubCommand;
import dev.jsinco.recipes.recipe.Recipe;
import dev.jsinco.recipes.recipe.RecipeItem;
import dev.jsinco.recipes.recipe.RecipeUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class GiveRecipeItem implements SubCommand {
    @Override
    public void execute(@NotNull BreweryPlugin plugin, @NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Util.colorcode("&cOnly players can use this command (Give yourself a specific recipe item)"));
            return;
        }


        Recipe recipe = RecipeUtil.getRecipeFromKey(args[1]);
        ItemStack recipeItem = new RecipeItem(recipe).getItem();

        Util.giveItem(player, recipeItem);
    }

    @Nullable
    @Override
    public List<String> tabComplete(@NotNull BreweryPlugin plugin, @NotNull CommandSender sender, @NotNull String[] args) {
        return RecipeUtil.getAllRecipeKeys();
    }

    @NotNull
    @Override
    public String getPermission() {
        return Objects.requireNonNullElse(Config.get().getString("command-permissions.give"), "breweryrecipes.give");
    }
}
