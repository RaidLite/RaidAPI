package yo.raidlite.enchants

import io.papermc.paper.enchantments.EnchantmentRarity
import io.papermc.paper.enchantments.EnchantmentRarity.RARE
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.GRAY
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.enchantments.EnchantmentTarget.ALL
import org.bukkit.entity.EntityCategory
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

@Suppress("DEPRECATION", "unused")
abstract class RaidEnchantment(key: NamespacedKey) : Enchantment(key) {
    abstract override fun getMaxLevel(): Int
    override fun getStartLevel() = 1
    @Deprecated("Deprecated in Java")
    override fun getName(): String = key.key
    override fun displayName(level: Int): Component = text(getName(), GRAY)
    override fun getItemTarget(): EnchantmentTarget = ALL
    override fun isTreasure() = false
    @Deprecated("Deprecated in Java")
    override fun isCursed() = false
    override fun isTradeable() = false
    override fun isDiscoverable() = false
    override fun getRarity(): EnchantmentRarity = RARE
    override fun getDamageIncrease(level: Int, entityCategory: EntityCategory) = 0f
    override fun getActiveSlots(): Set<EquipmentSlot> = emptySet()
    override fun conflictsWith(other: Enchantment) = false
    override fun canEnchantItem(item: ItemStack) = true
}