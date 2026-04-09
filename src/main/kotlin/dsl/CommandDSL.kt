@file:Suppress("DEPRECATION", "unused")

package yo.raidlite.dsl

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

@DslMarker
annotation class CommandDsl

typealias Executor = CommandSender.(args: Array<String>) -> Boolean
typealias Completer = CommandSender.(args: Array<String>) -> List<String>?

@CommandDsl
class CommandDSL(
    val name: String,
    private var executor: Executor = { _ -> false },
    private var completer: Completer? = null,
    private var description: String = "",
    private var usage: String = "",
    private var permission: String? = null,
    private var aliases: List<String> = emptyList(),
    private val subcommands: MutableMap<String, CommandDSL> = mutableMapOf()
) {
    fun executes(block: Executor) { executor = block }
    fun completes(block: Completer) { completer = block }
    fun description(value: String) { description = value }
    fun usage(value: String) { usage = value }
    fun permission(value: String) { permission = value }
    fun aliases(vararg values: String) { aliases = values.toList() }

    fun subcommand(name: String, block: CommandDSL.() -> Unit) {
        subcommands[name] = CommandDSL(name).apply(block)
    }

    fun playerOnly(block: Player.(args: Array<String>) -> Boolean) {
        executor = { args -> (this as? Player)?.block(args) ?: false }
    }

    fun consoleOnly(block: Executor) {
        executor = { args -> if (this !is Player) block(args) else false }
    }

    fun requireArgs(min: Int, block: Executor) {
        executor = { args -> if (args.size >= min) block(args) else false }
    }

    fun completePlayers() {
        completer = { args ->
            server.onlinePlayers
                .map { it.name }
                .filter { it.startsWith(args.lastOrNull() ?: "", ignoreCase = true) }
        }
    }

    fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val sub = args.firstOrNull()?.let { subcommands[it] }
        if (sub != null) return sub.execute(sender, args.drop(1).toTypedArray())
        return sender.executor(args)
    }

    fun complete(sender: CommandSender, args: Array<String>): List<String>? {
        val sub = args.firstOrNull()?.let { subcommands[it] }
        if (sub != null && args.size > 1) return sub.complete(sender, args.drop(1).toTypedArray())
        if (args.size == 1) {
            val partials = subcommands.keys.filter { it.startsWith(args[0], ignoreCase = true) }
            if (partials.isNotEmpty()) return partials
        }
        return completer?.let { sender.it(args) }
    }

    fun register(plugin: JavaPlugin) {
        plugin.getCommand(name)?.let { cmd ->
            if (description.isNotEmpty()) cmd.description = description
            if (usage.isNotEmpty()) cmd.usage = usage
            permission?.let { cmd.permission = it }
            if (aliases.isNotEmpty()) cmd.aliases = aliases
            cmd.setExecutor { sender, _, _, args -> execute(sender, args) }
            cmd.setTabCompleter { sender, _, _, args -> complete(sender, args) ?: emptyList() }
        }
    }
}

fun JavaPlugin.command(name: String, block: CommandDSL.() -> Unit): CommandDSL =
    CommandDSL(name).apply(block).also { it.register(this) }