package dev.jsinco.recipes.commands

import com.dre.brewery.BreweryPlugin
import com.dre.brewery.api.addons.AddonCommand
import dev.jsinco.recipes.Util
import dev.jsinco.recipes.commands.subcommands.GiveBook
import dev.jsinco.recipes.commands.subcommands.GiveRecipeItem
import dev.jsinco.recipes.commands.subcommands.GuiCommand
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

class CommandManager(val plugin: BreweryPlugin) : com.dre.brewery.commands.SubCommand, AddonCommand(
    "breweryrecipes",
    "BreweryRecipes command",
    "",
    emptyList(),
) {

    private val commands: Map<String, SubCommand> = mapOf(
        "give" to GiveRecipeItem(),
        "givebook" to GiveBook(),
        "gui" to GuiCommand()
    )


    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
        return tabCompleteSubcommand(args, sender)
    }

    override fun execute(sender: CommandSender, name: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission(permission())) {
            sender.sendMessage("${Util.prefix}You do not have permission to execute this command.")
            return true
        }
        return executeSubcommand(args, sender)
    }

    override fun execute(plugin: BreweryPlugin, sender: CommandSender, label: String, args: Array<out String>) {
        val argsAsMutable = args.toMutableList()
        argsAsMutable.removeAt(0)
        val finalArgs = argsAsMutable.toList().toTypedArray()
        executeSubcommand(finalArgs, sender)
    }

    override fun tabComplete(plugin: BreweryPlugin, sender: CommandSender, label: String, args: Array<out String>): List<String> {
        val argsAsMutable = args.toMutableList()
        argsAsMutable.removeAt(0)
        val finalArgs = argsAsMutable.toList().toTypedArray()
        return tabCompleteSubcommand(finalArgs, sender)
    }

    override fun permission(): String {
        return "brewery.cmd.recipes"
    }

    override fun playerOnly(): Boolean {
        return false
    }


    private fun executeSubcommand(args: Array<out String>, sender: CommandSender): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("${Util.prefix}Unknown subcommand.")
            return true
        }

        val subCommand = commands[args[0]] ?: return false
        if (!sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage("${Util.prefix}You do not have permission to execute this command.")
            return true
        }

        subCommand.execute(plugin, sender, args)
        return true
    }

    private fun tabCompleteSubcommand(args: Array<out String>, sender: CommandSender): MutableList<String> {
        if (args.size == 1) return commands.keys.toMutableList()
        val subCommand = commands[args[0]] ?: return Bukkit.getOnlinePlayers().map { it.name }.toMutableList()
        return subCommand.tabComplete(plugin, sender, args)?.toMutableList() ?: return Bukkit.getOnlinePlayers().map { it.name }.toMutableList()
    }
}