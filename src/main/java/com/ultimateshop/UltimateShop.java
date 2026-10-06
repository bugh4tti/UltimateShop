package com.ultimateshop;

import com.ultimateshop.command.ShopCommand;
import com.ultimateshop.listener.MenuListener;
import com.ultimateshop.shop.ShopManager;
import com.ultimateshop.util.Text;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class UltimateShop extends JavaPlugin {

    private static final String[] RESOURCES = {
            "messages.yml",
            "menus/main.yml",
            "categories/madera.yml",
            "categories/bloques.yml",
            "categories/granja.yml",
            "categories/minerales.yml",
            "categories/decoracion.yml",
            "categories/redstone.yml",
            "categories/herramientas.yml",
            "categories/utiles.yml",
            "categories/mobdrops.yml"
    };

    private Economy economy;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        for (String res : RESOURCES) {
            if (!new File(getDataFolder(), res).exists()) {
                try {
                    saveResource(res, false);
                } catch (IllegalArgumentException ignored) {
                    // El recurso todavía no existe dentro del jar
                }
            }
        }

        Text.init(getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"));

        if (!setupEconomy()) {
            getLogger().warning("No se encontró una economía de Vault todavía. Se volverá a buscar al usar la tienda.");
        }

        shopManager = new ShopManager(this);
        shopManager.load();

        ShopCommand command = new ShopCommand(this);
        register("shop", command);
        register("ultimateshop", command);

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getLogger().info("UltimateShop activado correctamente.");
    }

    private void register(String name, ShopCommand executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            return;
        }
        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        if (economy == null) {
            setupEconomy();
        }
        return economy;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public void reloadAll() {
        shopManager.load();
    }
          }
