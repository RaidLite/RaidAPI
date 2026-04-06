package yo.raidlite.enchants

import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentOffer

@Suppress("unused")
class RaidEnchantmentOffer(
    enchantment: Enchantment,
    level: Int,
    cost: Int
): EnchantmentOffer(
    enchantment,
    level,
    cost
)