@file:Suppress("DEPRECATION", "unused")

package yo.raidlite.dsl

import org.bukkit.event.EventPriority
import org.bukkit.event.EventPriority.NORMAL
import org.bukkit.event.HandlerList.unregisterAll
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.event.Event as BukkitEvent

class EventListener : Listener
typealias EventHandler<T> = T.() -> Unit
fun Plugin.events(block: EventBuilder.() -> Unit): Listener = EventBuilder(this).apply(block).build()
fun Plugin.unregisterAll() = unregisterAll(this)

class EventBuilder(private val plugin: Plugin) {

    @PublishedApi
    internal val registrations = mutableListOf<EventRegistration<*>>()

    inline fun <reified T : BukkitEvent> on(
        priority: EventPriority = NORMAL,
        ignoreCancelled: Boolean = false,
        noinline handler: EventHandler<T>
    ) {
        registrations += EventRegistration(T::class.java, priority, ignoreCancelled, handler)
    }

    fun build(): Listener {
        val listener = EventListener()
        registrations.forEach { reg -> reg.register(plugin, listener) }
        return listener
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : BukkitEvent> EventRegistration<T>.register(plugin: Plugin, listener: Listener) {
        plugin.server.pluginManager.registerEvent(
            eventClass,
            listener,
            priority,
            { _, event -> if (eventClass.isInstance(event)) handler(event as T) },
            plugin,
            ignoreCancelled
        )
    }
}

data class EventRegistration<T : BukkitEvent>(
    val eventClass: Class<T>,
    val priority: EventPriority = NORMAL,
    val ignoreCancelled: Boolean = false,
    val handler: EventHandler<T>
)