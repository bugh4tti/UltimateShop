package com.ultimateshop.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Text {

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static boolean papi = false;

    private Text() {
    }

    public static void init(boolean papiEnabled) {
        papi = papiEnabled;
    }

    /** Convierte &c, &l y también &#RRGGBB (hex). */
    public static String color(String text) {
        if (text == null) {
            return "";
        }
        Matcher matcher = HEX.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            StringBuilder repl = new StringBuilder();
            repl.append(ChatColor.COLOR_CHAR).append('x');
            for (char c : matcher.group(1).toCharArray()) {
                repl.append(ChatColor.COLOR_CHAR).append(c);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(repl.toString()));
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    /** Reemplaza variables propias, luego PlaceholderAPI (si existe) y por último los colores. */
    public static String apply(Player player, String text, Map<String, String> vars) {
        if (text == null) {
            return "";
        }
        if (vars != null) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                text = text.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        if (papi && player != null) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }
        return color(text);
    }

    public static List<String> applyList(Player player, List<String> lines, Map<String, String> vars) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            out.add(apply(player, line, vars));
        }
        return out;
    }
}
