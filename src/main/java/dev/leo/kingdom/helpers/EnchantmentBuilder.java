package dev.leo.kingdom.helpers;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;

public class EnchantmentBuilder {
  // #region immutable props
  private final Enchantment enchantment;
  // #endregion

  // #region mutable props
  private int level = 1;
  private boolean ignoreLevelRestriction = false;
  private boolean ignoreCurse = false;
  private boolean ignoreMending = false;
  // #endregion

  // #region constructors
  public EnchantmentBuilder(Enchantment enchantment) {
    this.enchantment = enchantment;
  }

  public EnchantmentBuilder(Enchantment enchantment, int level) {
    this.enchantment = enchantment;
    this.level = level;
  }

  public EnchantmentBuilder(Enchantment enchantment, int level, boolean ignoreLevelRestriction) {
    this.enchantment = enchantment;
    this.level = level;
    this.ignoreLevelRestriction = ignoreLevelRestriction;
  }

  // #endregion

  // #region methods

  public EnchantmentBuilder level(int level) {
    this.level = level;
    return this;
  }

  public EnchantmentBuilder ignoreLevelRestriction(boolean ignoreLevelRestriction) {
    this.ignoreLevelRestriction = ignoreLevelRestriction;
    return this;
  }

  public EnchantmentBuilder ignoreCurse(boolean ignoreCurse) {
    this.ignoreCurse = ignoreCurse;
    return this;
  }

  public EnchantmentBuilder ignoreMending(boolean ignoreMending) {
    this.ignoreMending = ignoreMending;
    return this;
  }

  public ItemMeta inject(final ItemMeta m) {
    m.addEnchant(enchantment, level, ignoreLevelRestriction);
    if (ignoreCurse) {
      m.addEnchant(Enchantment.BINDING_CURSE, 1, true);
    }
    if (ignoreMending) {
      m.addEnchant(Enchantment.MENDING, 1, true);
    }
    return m;
  }

  // #endregion
}
