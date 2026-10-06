package com.ultimateshop.shop;

import com.ultimateshop.UltimateShop;
import com.ultimateshop.util.Text;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShopManager {

    private final UltimateShop plugin;
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final Map<Integer, String> mainActions = new HashMap<>();

    private FileConfiguration messages;
    private FileConfiguration mainMenu;
    private DecimalFormat df;
    private String priceFormat = "$%s";

    public ShopManager(UltimateShop plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // Carga
    // ------------------------------------------------------------------

    public void load() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();
        priceFormat = cfg.getString("price-format", "$%s");
        df = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
        mainMenu = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "menus/main.yml"));
        loadMainActions();
        loadCategories();
    }

    private void loadMainActions() {
        mainActions.clear();
        ConfigurationSection items = mainMenu.getConfigurationSection("items");
        if (items == null) {
            return;
        }
        for (String key : items.getKeys(false)) {
            ConfigurationSection s = items.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            String category = s.getString("category");
            String action = category != null ? "category:" + category.toLowerCase() : s.getString("action", "none");
            mainActions.put(s.getInt("slot"), action);
        }
    }

    private void loadCategories() {
        categories.clear();
        File dir = new File(plugin.getDataFolder(), "categories");
        File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".yml"));
        if (files == null) {
            return;
        }
        Arrays.sort(files);
        for (File file : files) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase();
            List<ShopItem> list = new ArrayList<>();
            int maxPages = 1;

            ConfigurationSection sec = yml.getConfigurationSection("items");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    ConfigurationSection s = sec.getConfigurationSection(key);
                    if (s == null) {
                        continue;
                    }
                    Material material = Material.matchMaterial(s.getString("material", key));
                    if (material == null || !material.isItem()) {
                        plugin.getLogger().warning("Material inválido en categories/" + file.getName() + " -> " + key);
                        continue;
                    }
                    int page = Math.max(1, s.getInt("page", 1));
                    maxPages = Math.max(maxPages, page);
                    list.add(new ShopItem(key, material, s.getString("name"), s.getInt("slot"), page,
                            s.getDouble("buy", -1), s.getDouble("sell", -1)));
                }
            }

            int rows = Math.max(1, Math.min(6, yml.getInt("rows", 6)));
            categories.put(id, new Category(id, yml.getString("name", id),
                    yml.getString("title", "&8Mercado | " + id), rows, list, maxPages));
        }
    }

    public boolean hasCategory(String id) {
        return id != null && categories.containsKey(id.toLowerCase());
    }

    public List<String> getCategoryIds() {
        return new ArrayList<>(categories.keySet());
    }

    // ------------------------------------------------------------------
    // Menús
    // ------------------------------------------------------------------

    public void openMain(Player p) {
        int rows = Math.max(1, Math.min(6, mainMenu.getInt("rows", 6)));
        Map<String, String> vars = baseVars(p);

        ShopMenuHolder holder = new ShopMenuHolder(ShopMenuHolder.Type.MAIN, null, 1);
        Inventory inv = Bukkit.createInventory(holder, rows * 9,
                Text.apply(p, mainMenu.getString("title", "&8Mercado"), vars));
        holder.setInventory(inv);
        fill(inv);

        ConfigurationSection items = mainMenu.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection s = items.getConfigurationSection(key);
                if (s == null) {
                    continue;
                }
                int slot = s.getInt("slot");
                if (slot < 0 || slot >= inv.getSize()) {
                    continue;
                }
                Material material = Material.matchMaterial(s.getString("material", "STONE"));
                if (material == null) {
                    material = Material.STONE;
                }
                Map<String, String> v = new HashMap<>(vars);
                String catId = s.getString("category");
                if (catId != null) {
                    Category c = categories.get(catId.toLowerCase());
                    if (c != null) {
                        v.put("category", c.name());
                        v.put("items", String.valueOf(c.items().size()));
                    }
                }
                inv.setItem(slot, build(p, material, s.getString("name", " "), s.getStringList("lore"), v));
            }
        }

        inv.setItem(inv.getSize() - 1, info(p));
        p.openInventory(inv);
        sound(p, "sounds.open");
    }

    public void openCategory(Player p, String id, int page) {
        Category c = id == null ? null : categories.get(id.toLowerCase());
        if (c == null) {
            msg(p, "unknown-category", Map.of("category", String.valueOf(id)));
            return;
        }
        page = Math.max(1, Math.min(page, c.maxPages()));

        Map<String, String> vars = baseVars(p);
        vars.put("category", c.name());
        vars.put("page", String.valueOf(page));
        vars.put("max_pages", String.valueOf(c.maxPages()));

        ShopMenuHolder holder = new ShopMenuHolder(ShopMenuHolder.Type.CATEGORY, c.id(), page);
        Inventory inv = Bukkit.createInventory(holder, c.rows() * 9, Text.apply(p, c.title(), vars));
        holder.setInventory(inv);
        fill(inv);

        for (ShopItem it : c.items()) {
            if (it.page() != page || it.slot() < 0 || it.slot() >= inv.getSize()) {
                continue;
            }
            inv.setItem(it.slot(), shopItemStack(p, it, vars));
        }

        int size = inv.getSize();
        inv.setItem(size - 5, navItem(p, "nav.back", vars));
        if (page > 1) {
            inv.setItem(size - 6, navItem(p, "nav.previous", vars));
        }
        if (page < c.maxPages()) {
            inv.setItem(size - 4, navItem(p, "nav.next", vars));
        }
        inv.setItem(size - 1, info(p));

        p.openInventory(inv);
        sound(p, "sounds.open");
    }

    // ------------------------------------------------------------------
    // Clicks
    // ------------------------------------------------------------------

    public void handleMainClick(Player p, int slot) {
        String action = mainActions.get(slot);
        if (action == null) {
            return;
        }
        if (action.equals("close")) {
            p.closeInventory();
            return;
        }
        if (action.startsWith("category:")) {
            sound(p, "sounds.click");
            openCategory(p, action.substring("category:".length()), 1);
        }
    }

    public void handleCategoryClick(Player p, ShopMenuHolder holder, int slot, ClickType click) {
        Category c = categories.get(holder.getCategoryId());
        if (c == null) {
            p.closeInventory();
            return;
        }
        int size = holder.getInventory().getSize();

        if (slot == size - 5) {
            sound(p, "sounds.click");
            openMain(p);
            return;
        }
        if (slot == size - 6 && holder.getPage() > 1) {
            sound(p, "sounds.click");
            openCategory(p, c.id(), holder.getPage() - 1);
            return;
        }
        if (slot == size - 4 && holder.getPage() < c.maxPages()) {
            sound(p, "sounds.click");
            openCategory(p, c.id(), holder.getPage() + 1);
            return;
        }

        for (ShopItem it : c.items()) {
            if (it.page() != holder.getPage() || it.slot() != slot) {
                continue;
            }
            switch (click) {
                case LEFT -> buy(p, holder, it, false);
                case SHIFT_LEFT -> buy(p, holder, it, true);
                case RIGHT -> sell(p, holder, it, false);
                case SHIFT_RIGHT -> sell(p, holder, it, true);
                default -> {
                }
            }
            return;
        }
    }

    // ------------------------------------------------------------------
    // Compra y venta
    // ------------------------------------------------------------------

    private void buy(Player p, ShopMenuHolder holder, ShopItem it, boolean stack) {
        Map<String, String> v = itemVars(p, it);
        if (it.buy() < 0) {
            msg(p, "not-buyable", v);
            sound(p, "sounds.error");
            return;
        }
        Economy eco = plugin.getEconomy();
        if (eco == null) {
            msg(p, "economy-error", v);
            sound(p, "sounds.error");
            return;
        }

        int qty = stack ? it.material().getMaxStackSize() : 1;
        double total = it.buy() * qty;
        v.put("amount", String.valueOf(qty));
        v.put("price", fmt(total));

        if (!hasSpace(p, it.material(), qty)) {
            msg(p, "no-space", v);
            sound(p, "sounds.error");
            return;
        }
        if (!eco.has(p, total)) {
            msg(p, "no-money", v);
            sound(p, "sounds.error");
            return;
        }
        EconomyResponse response = eco.withdrawPlayer(p, total);
        if (!response.transactionSuccess()) {
            msg(p, "economy-error", v);
            sound(p, "sounds.error");
            return;
        }

        int max = it.material().getMaxStackSize();
        int left = qty;
        while (left > 0) {
            int n = Math.min(left, max);
            p.getInventory().addItem(new ItemStack(it.material(), n));
            left -= n;
        }

        v.put("balance", fmt(eco.getBalance(p)));
        msg(p, "bought", v);
        sound(p, "sounds.success");
        refreshInfo(p, holder);
    }

    private void sell(Player p, ShopMenuHolder holder, ShopItem it, boolean all) {
        Map<String, String> v = itemVars(p, it);
        if (it.sell() < 0) {
            msg(p, "not-sellable", v);
            sound(p, "sounds.error");
            return;
        }
        Economy eco = plugin.getEconomy();
        if (eco == null) {
            msg(p, "economy-error", v);
            sound(p, "sounds.error");
            return;
        }

        int have = count(p, it.material());
        if (have <= 0) {
            msg(p, "no-items", v);
            sound(p, "sounds.error");
            return;
        }

        int qty = all ? have : 1;
        double total = it.sell() * qty;
        remove(p, it.material(), qty);
        eco.depositPlayer(p, total);

        v.put("amount", String.valueOf(qty));
        v.put("price", fmt(total));
        v.put("balance", fmt(eco.getBalance(p)));
        msg(p, "sold", v);
        sound(p, "sounds.success");
        refreshInfo(p, holder);
    }

    private boolean hasSpace(Player p, Material material, int qty) {
        int max = material.getMaxStackSize();
        ItemStack base = new ItemStack(material);
        int free = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) {
            if (s == null || s.getType() == Material.AIR) {
                free += max;
            } else if (s.isSimilar(base)) {
                free += max - s.getAmount();
            }
            if (free >= qty) {
                return true;
            }
        }
        return free >= qty;
    }

    private int count(Player p, Material material) {
        ItemStack base = new ItemStack(material);
        int total = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) {
            if (s != null && s.isSimilar(base)) {
                total += s.getAmount();
            }
        }
        return total;
    }

    private void remove(Player p, Material material, int qty) {
        ItemStack base = new ItemStack(material);
        ItemStack[] contents = p.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && qty > 0; i++) {
            ItemStack s = contents[i];
            if (s == null || !s.isSimilar(base)) {
                continue;
            }
            int take = Math.min(qty, s.getAmount());
            if (take >= s.getAmount()) {
                contents[i] = null;
            } else {
                s.setAmount(s.getAmount() - take);
            }
            qty -= take;
        }
        p.getInventory().setStorageContents(contents);
    }

    private void refreshInfo(Player p, ShopMenuHolder holder) {
        Inventory inv = holder.getInventory();
        inv.setItem(inv.getSize() - 1, info(p));
    }

    // ------------------------------------------------------------------
    // Construcción de ítems
    // ------------------------------------------------------------------

    private ItemStack shopItemStack(Player p, ShopItem it, Map<String, String> baseVars) {
        Map<String, String> v = new HashMap<>(baseVars);
        v.putAll(itemVars(p, it));
        return build(p, it.material(), it.name(), plugin.getConfig().getStringList("item-lore"), v);
    }

    private ItemStack navItem(Player p, String path, Map<String, String> vars) {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection(path);
        if (s == null) {
            return new ItemStack(Material.BARRIER);
        }
        Material material = Material.matchMaterial(s.getString("material", "ARROW"));
        if (material == null) {
            material = Material.ARROW;
        }
        return build(p, material, s.getString("name", " "), s.getStringList("lore"), vars);
    }

    private ItemStack info(Player p) {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("info");
        if (s == null) {
            return new ItemStack(Material.GOLD_INGOT);
        }
        Material material = Material.matchMaterial(s.getString("material", "GOLD_INGOT"));
        if (material == null) {
            material = Material.GOLD_INGOT;
        }
        return build(p, material, s.getString("name", " "), s.getStringList("lore"), baseVars(p));
    }

    private ItemStack build(Player p, Material material, String name, List<String> lore, Map<String, String> vars) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(Text.apply(p, name, vars));
            }
            meta.setLore(Text.applyList(p, lore, vars));
            meta.addItemFlags(ItemFlag.values());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void fill(Inventory inv) {
        Material material = Material.matchMaterial(
                plugin.getConfig().getString("filler-material", "GRAY_STAINED_GLASS_PANE"));
        if (material == null) {
            material = Material.GRAY_STAINED_GLASS_PANE;
        }
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane);
        }
    }

    // ------------------------------------------------------------------
    // Variables, mensajes y sonidos
    // ------------------------------------------------------------------

    private Map<String, String> baseVars(Player p) {
        Map<String, String> v = new HashMap<>();
        v.put("player", p.getName());
        Economy eco = plugin.getEconomy();
        v.put("balance", fmt(eco != null ? eco.getBalance(p) : 0));
        return v;
    }

    private Map<String, String> itemVars(Player p, ShopItem it) {
        Map<String, String> v = baseVars(p);
        String unavailable = plugin.getConfig().getString("unavailable", "&cNo disponible");
        String itemName = it.name() != null
                ? ChatColor.stripColor(Text.color(it.name()))
                : pretty(it.material());
        v.put("item", itemName);
        v.put("buy_price", it.buy() >= 0 ? fmt(it.buy()) : unavailable);
        v.put("sell_price", it.sell() >= 0 ? fmt(it.sell()) : unavailable);
        v.put("stack", String.valueOf(it.material().getMaxStackSize()));
        return v;
    }

    private String fmt(double value) {
        return String.format(priceFormat, df.format(value));
    }

    private String pretty(Material material) {
        StringBuilder sb = new StringBuilder();
        for (String part : material.name().toLowerCase().split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    public void msg(CommandSender sender, String key, Map<String, String> vars) {
        String raw = messages.getString(key);
        if (raw == null || raw.isEmpty()) {
            return;
        }
        Player player = sender instanceof Player ? (Player) sender : null;
        sender.sendMessage(Text.apply(player, messages.getString("prefix", "") + raw, vars));
    }

    public void sendList(CommandSender sender, String key) {
        Player player = sender instanceof Player ? (Player) sender : null;
        for (String line : messages.getStringList(key)) {
            sender.sendMessage(Text.apply(player, line, null));
        }
    }

    private void sound(Player p, String path) {
        String sound = plugin.getConfig().getString(path);
        if (sound == null || sound.isEmpty()) {
            return;
        }
        p.playSound(p.getLocation(), sound, 1f, 1f);
    }
  }
