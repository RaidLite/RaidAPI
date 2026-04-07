package yo.raidlite.enchants

import io.papermc.paper.enchantments.EnchantmentRarity.RARE
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget.ALL
import org.bukkit.inventory.ItemStack

@Suppress("DEPRECATION", "unused")
class RaidEnchantBuilder(private val key: NamespacedKey) {
    private var _maxLevel: Int = 1
    private var _startLevel: Int = 1
    private var _target: RaidEnchantmentTarget = ALL
    private var _rarity: RaidEnchantmentRarity = RARE
    private var _display: (Int) -> Component = { text(key.key) }
    private var _canEnchant: (ItemStack) -> Boolean = { true }
    private var _conflicts: (Enchantment) -> Boolean = { false }

    fun maxLevel(level: Int): RaidEnchantBuilder = apply { this._maxLevel = level }
    fun startLevel(level: Int): RaidEnchantBuilder = apply { this._startLevel = level }
    fun target(target: RaidEnchantmentTarget): RaidEnchantBuilder = apply { this._target = target }
    fun rarity(rarity: RaidEnchantmentRarity): RaidEnchantBuilder = apply { this._rarity = rarity }
    fun displayName(factory: (Int) -> Component): RaidEnchantBuilder = apply { this._display = factory }
    fun canEnchant(predicate: (ItemStack) -> Boolean): RaidEnchantBuilder = apply { this._canEnchant = predicate }
    fun conflicts(predicate: (Enchantment) -> Boolean): RaidEnchantBuilder = apply { this._conflicts = predicate }

    fun build(): RaidEnchantment {
        return object : RaidEnchantment(key) {
            override fun getMaxLevel(): Int = _maxLevel
            override fun getStartLevel(): Int = _startLevel
            override fun getItemTarget(): RaidEnchantmentTarget = _target
            override fun getRarity(): RaidEnchantmentRarity = _rarity
            override fun displayName(level: Int): Component = _display(level)
            override fun canEnchantItem(item: ItemStack): Boolean = _canEnchant(item)
            override fun conflictsWith(other: Enchantment): Boolean = _conflicts(other)
        }
    }
}