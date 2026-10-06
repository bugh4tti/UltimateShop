package com.ultimateshop.shop;

import java.util.List;

public record Category(String id, String name, String title, int rows, List<ShopItem> items, int maxPages) {
}
