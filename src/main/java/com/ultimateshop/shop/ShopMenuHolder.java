package com.ultimateshop.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ShopMenuHolder implements InventoryHolder {

    public enum Type {
        MAIN,
        CATEGORY
    }

    private final Type type;
    private final String categoryId;
    private final int page;
    private Inventory inventory;

    public ShopMenuHolder(Type type, String categoryId, int page) {
        this.type = type;
        this.categoryId = categoryId;
        this.page = page;
    }

    public Type getType() {
        return type;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public int getPage() {
        return page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
