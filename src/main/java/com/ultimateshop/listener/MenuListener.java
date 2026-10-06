package com.ultimateshop.listener;

import com.ultimateshop.UltimateShop;
import com.ultimateshop.shop.ShopManager;
import com.ultimateshop.shop.ShopMenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class MenuListener implements Listener {

    private final UltimateShop plugin;

    public MenuListener(UltimateShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getInventory();
        if (!(top.getHolder() instanceof ShopMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory clicked = event.getClickedInventory();
        if (clicked == null || !clicked.equals(top)) {
            return;
        }

        ShopManager manager = plugin.getShopManager();
        if (holder.getType() == ShopMenuHolder.Type.MAIN) {
            manager.handleMainClick(player, event.getSlot());
        } else {
            manager.handleCategoryClick(player, holder, event.getSlot(), event.getClick());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ShopMenuHolder) {
            event.setCancelled(true);
        }
    }
  }
