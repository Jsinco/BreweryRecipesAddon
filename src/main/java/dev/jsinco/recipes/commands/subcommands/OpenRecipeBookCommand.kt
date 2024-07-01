package dev.jsinco.recipes.commands.subcommands

import com.dre.brewery.BreweryPlugin
import dev.jsinco.recipes.Config
import dev.jsinco.recipes.Util.colorcode
import dev.jsinco.recipes.commands.SubCommand
import dev.jsinco.recipes.guis.RecipeGui
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

class OpenRecipeBookCommand : SubCommand {
    override fun execute(plugin: BreweryPlugin, sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(colorcode("&cUsage: /breweryrecipes openrecipebook <player>"))
            return
        }

        val player = Bukkit.getPlayerExact(args[1])
        if (player == null) {
            sender.sendMessage(colorcode("&cPlayer not found"))
            return
        }

        RecipeGui(player).openRecipeGui(player)
    }

    override fun tabComplete(plugin: BreweryPlugin, sender: CommandSender, args: Array<out String>): List<String>? {
        return null
    }

    override fun getPermission(): String {
        return Config.get().getString("permissions.openrecipebook") ?: "breweryrecipes.openrecipebook"
    }
}