package dev.jsinco.recipes.commands

import com.dre.brewery.BreweryPlugin
import org.bukkit.command.CommandSender

interface SubCommand {
    fun execute(plugin: BreweryPlugin, sender: CommandSender, args: Array<out String>)

    fun tabComplete(plugin: BreweryPlugin, sender: CommandSender, args: Array<out String>): List<String>?

    fun getPermission(): String

}