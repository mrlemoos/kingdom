package dev.mrlemoos.kingdom.helpers;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.persistence.PersistentDataType;

public final class ItemBuilder {
  // #region immutable props
  private final ItemStack itemStack;
  // #endregion

  // #region mutable props
  private int amount = 1;
  private int durability = 0;
  private Set<EnchantmentBuilder> enchantments = new HashSet<>();
  private List<String> lore = new ArrayList<>();
  private String displayName = null;
  private ItemRarity rarity;
  private Set<ItemFlag> flags = new HashSet<>();
  private ToolComponent toolComponent;
  private FoodComponent foodComponent;
  private final List<PdcEntry<?, ?>> pdcEntries = new ArrayList<>();
  private String bookTitle;
  private String bookAuthor;
  private List<String> bookPages;
  private UUID skullOwnerId;
  // #endregion

  // #region constructors
  public ItemBuilder(Material material) {
    this.itemStack = new ItemStack(material);
  }

  public ItemBuilder(Material material, int amount) {
    this.itemStack = new ItemStack(material, amount);
    this.amount = amount;
  }
  // #endregion

  // #region static factories
  public static ItemStack labelled(Material material, String name, String loreLine) {
    return new ItemBuilder(material).displayAs(name).lore(c("&7" + loreLine)).build();
  }

  public static ItemStack fillerPane(Material pane) {
    return new ItemBuilder(pane).displayAs(" ").build();
  }
  // #endregion

  // #region methods
  public ItemBuilder displayAs(String displayName) {
    this.displayName = displayName;
    return this;
  }

  public ItemBuilder lore(String lore) {
    this.lore.add(lore);
    return this;
  }

  public ItemBuilder lore(List<String> lore) {
    this.lore.addAll(lore);
    return this;
  }

  public ItemBuilder lore(String... lore) {
    this.lore.addAll(Arrays.asList(lore));
    return this;
  }

  public List<String> lore() {
    return lore;
  }

  public ItemBuilder amount(int a) {
    this.amount = a;
    return this;
  }

  public int amount() {
    return amount;
  }

  public ItemBuilder durability(int d) {
    this.durability = d;
    return this;
  }

  public int durability() {
    return durability;
  }

  public ItemBuilder type(final Material m) {
    this.itemStack.setType(m);
    return this;
  }

  public Material type() {
    return itemStack.getType();
  }

  public ItemBuilder rarity(final ItemRarity r) {
    this.rarity = r;
    return this;
  }

  public ItemRarity rarity() {
    return rarity;
  }

  public ItemBuilder flags(final ItemFlag... flags) {
    this.flags.addAll(Arrays.asList(flags));
    return this;
  }

  public ItemBuilder flags(final Set<ItemFlag> flags) {
    this.flags.addAll(flags);
    return this;
  }

  public Set<ItemFlag> flags() {
    return flags;
  }

  public ItemBuilder tool(ToolComponent t) {
    toolComponent = t;
    return this;
  }

  public ToolComponent tool() {
    return toolComponent;
  }

  public ItemBuilder food(FoodComponent f) {
    foodComponent = f;
    return this;
  }

  public FoodComponent food() {
    return foodComponent;
  }

  public ItemBuilder enchant(EnchantmentBuilder... enchantmentBuilders) {
    this.enchantments.addAll(Arrays.asList(enchantmentBuilders));
    return this;
  }

  public <T, Z> ItemBuilder pdc(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
    pdcEntries.add(new PdcEntry<>(key, type, value));
    return this;
  }

  public ItemBuilder book(String title, String author, List<String> pages) {
    this.bookTitle = title;
    this.bookAuthor = author;
    this.bookPages = List.copyOf(pages);
    return this;
  }

  public ItemBuilder skullOwner(UUID uuid) {
    this.skullOwnerId = uuid;
    return this;
  }
  // #endregion

  // #region build
  public ItemStack build() {
    itemStack.setAmount(amount);
    itemStack.setDurability((short) durability);

    ItemMeta itemMeta = itemStack.getItemMeta();
    if (itemMeta == null) {
      throw new IllegalStateException("Item meta is null");
    }

    if (displayName != null) {
      itemMeta.setDisplayName(displayName);
    }
    if (!lore.isEmpty()) {
      itemMeta.setLore(lore);
    }
    if (!enchantments.isEmpty()) {
      for (EnchantmentBuilder enchantment : enchantments) {
        enchantment.inject(itemMeta);
      }
    }

    if (rarity != null) {
      itemMeta.setRarity(rarity);
    }

    if (!flags.isEmpty()) {
      itemMeta.addItemFlags(flags.toArray(new ItemFlag[0]));
    }

    if (toolComponent != null) {
      itemMeta.setTool(toolComponent);
    }

    if (foodComponent != null) {
      itemMeta.setFood(foodComponent);
    }

    for (PdcEntry<?, ?> entry : pdcEntries) {
      entry.apply(itemMeta);
    }

    if (bookTitle != null || bookAuthor != null || bookPages != null) {
      if (!(itemMeta instanceof BookMeta bookMeta)) {
        throw new IllegalStateException("Book meta required for written book items");
      }
      if (bookTitle != null) {
        bookMeta.setTitle(bookTitle);
      }
      if (bookAuthor != null) {
        bookMeta.setAuthor(bookAuthor);
      }
      if (bookPages != null) {
        bookMeta.setPages(bookPages);
      }
      itemMeta = bookMeta;
    }

    if (skullOwnerId != null) {
      if (!(itemMeta instanceof SkullMeta skullMeta)) {
        throw new IllegalStateException("Skull meta required for player head items");
      }
      skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(skullOwnerId));
      itemMeta = skullMeta;
    }

    itemStack.setItemMeta(itemMeta);
    return itemStack;
  }
  // #endregion

  private record PdcEntry<T, Z>(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
    void apply(ItemMeta meta) {
      meta.getPersistentDataContainer().set(key, type, value);
    }
  }
}
