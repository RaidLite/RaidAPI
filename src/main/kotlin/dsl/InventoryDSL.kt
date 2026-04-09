@file:Suppress("DEPRECATION", "unused")

package yo.raidlite.dsl

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.*
import kotlin.math.min

@DslMarker
annotation class GUIDsl

typealias ClickHandler = (InventoryClickEvent) -> Unit
typealias CloseHandler = (InventoryCloseEvent) -> Unit

class StateKey<T>(val name: String)

object GUIRegistry {
    private val storage = WeakHashMap<Inventory, GUI>()

    fun register(gui: GUI) { storage[gui.inventory] = gui }
    fun get(inventory: Inventory): GUI? = storage[inventory]
    fun remove(inventory: Inventory) { storage.remove(inventory) }
}

class GUIListener : Listener {

    @EventHandler
    fun click(e: InventoryClickEvent) {
        val gui = GUIRegistry.get(e.inventory) ?: return
        if (e.clickedInventory != e.view.topInventory) return
        if (gui.cancelAll || gui.cancelPredicate?.invoke(e) == true) e.isCancelled = true
        gui.clickMap[e.slot]?.invoke(e)
    }

    @EventHandler
    fun close(e: InventoryCloseEvent) {
        val gui = GUIRegistry.get(e.inventory) ?: return
        gui.onClose?.invoke(e)
        GUIRegistry.remove(e.inventory)
    }
}

class GUI(
    val inventory: Inventory,
    internal val clickMap: MutableMap<Int, ClickHandler>
) {
    internal val state = mutableMapOf<StateKey<*>, Any>()
    internal var cancelAll = false
    internal var cancelPredicate: ((InventoryClickEvent) -> Boolean)? = null
    internal var onClose: CloseHandler? = null

    private var renderer: (InventoryDSL.() -> Unit)? = null

    fun open(player: Player) { player.openInventory(inventory) }

    fun rerender() {
        val r = renderer ?: return
        inventory.clear()
        clickMap.clear()
        InventoryDSL(inventory, clickMap, this).apply(r)
    }

    fun setRenderer(block: InventoryDSL.() -> Unit) { renderer = block }
}

@GUIDsl
@Suppress("UNCHECKED_CAST")
class InventoryDSL(
    private val inventory: Inventory,
    private val clickMap: MutableMap<Int, ClickHandler>,
    private val gui: GUI
) {
    fun protectAll() { gui.cancelAll = true }
    fun protectIf(predicate: (InventoryClickEvent) -> Boolean) { gui.cancelPredicate = predicate }
    fun onClose(block: CloseHandler) { gui.onClose = block }

    fun <T> state(key: StateKey<T>, default: T): T =
        gui.state.getOrPut(key) { default as Any } as T

    fun <T> setState(key: StateKey<T>, value: T) { gui.state[key] = value as Any }

    fun slot(index: Int, item: ItemStack?, onClick: ClickHandler? = null) {
        inventory.setItem(index, item)
        if (onClick != null) clickMap[index] = onClick
    }

    fun slot(
        index: Int,
        material: Material,
        amount: Int = 1,
        block: (ItemBuilder.() -> Unit)? = null,
        onClick: ClickHandler? = null
    ) = slot(index, item(material, amount, block), onClick)

    fun pagination(
        items: List<ItemStack>,
        key: StateKey<Int>,
        pageSize: Int = inventory.size,
        block: PageDSL.() -> Unit = {}
    ) {
        val page = state(key, 0)
        val controller = PaginationController(items, page, pageSize)
        PageDSL(inventory, clickMap, gui, controller, key).apply {
            render()
            block()
        }
    }

    fun build(): GUI = gui.also { GUIRegistry.register(it) }
}

@GUIDsl
class PageDSL(
    private val inventory: Inventory,
    private val clickMap: MutableMap<Int, ClickHandler>,
    private val gui: GUI,
    private val controller: PaginationController,
    private val key: StateKey<Int>
) {
    fun render() {
        controller.pageItems.forEachIndexed { i, item -> inventory.setItem(i, item) }
    }

    fun next(slot: Int, item: ItemStack) {
        if (!controller.hasNext) return
        inventory.setItem(slot, item)
        clickMap[slot] = {
            gui.state[key] = controller.page + 1
            gui.rerender()
        }
    }

    fun previous(slot: Int, item: ItemStack) {
        if (!controller.hasPrevious) return
        inventory.setItem(slot, item)
        clickMap[slot] = {
            gui.state[key] = controller.page - 1
            gui.rerender()
        }
    }
}

class PaginationController(
    val items: List<ItemStack>,
    val page: Int,
    val pageSize: Int
) {
    private val from = page * pageSize
    private val to = min(from + pageSize, items.size)

    val pageItems: List<ItemStack> =
        if (from >= items.size) emptyList() else items.subList(from, to)

    val hasNext = to < items.size
    val hasPrevious = page > 0
}

@GUIDsl
class ItemBuilder(material: Material, amount: Int) {
    private val item = ItemStack(material, amount)
    private var meta: ItemMeta? = item.itemMeta
    private var plugin: Plugin? = null

    fun plugin(plugin: Plugin) { this.plugin = plugin }
    fun amount(value: Int) { item.amount = value }
    fun name(value: String) { meta?.setDisplayName(color(value)) }
    fun lore(vararg lines: String) { meta?.lore = lines.map { color(it) } }

    fun pdc(key: String, value: String) {
        val p = plugin ?: return
        meta?.persistentDataContainer?.set(NamespacedKey(p, key), PersistentDataType.STRING, value)
    }

    private fun color(text: String) = text.replace("&", "§")

    fun build(): ItemStack = item.also { it.itemMeta = meta }
}

fun item(material: Material, amount: Int = 1, block: (ItemBuilder.() -> Unit)? = null): ItemStack =
    ItemBuilder(material, amount).also { block?.invoke(it) }.build()

fun Inventory.gui(block: InventoryDSL.() -> Unit): GUI {
    val gui = GUI(this, mutableMapOf())
    val dsl = InventoryDSL(this, gui.clickMap, gui)
    gui.setRenderer(block)
    dsl.apply(block)
    return dsl.build()
}