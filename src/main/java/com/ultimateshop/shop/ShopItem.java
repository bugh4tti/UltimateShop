package com.ultimateshop.shop;

import org.bukkit.Material;

/**
 * Un ítem de la tienda. buy/sell = precio por unidad (-1 = no disponible).
 * name puede ser null: en ese caso el cliente muestra el nombre original en su idioma.
 */
public record ShopItem(String id, Material material, String name, int slot, int page, double buy, double sell) {
                       }
