package ru.inaga228.meteorevent.utils;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class LootGenerator {

    public static void fillChest(Inventory inventory, FileConfiguration config) {
        List<Map<?, ?>> items = config.getMapList("loot.items");
        int minItems = config.getInt("loot.min-items", 3);
        int maxItems = config.getInt("loot.max-items", 6);
        Random random = new Random();

        List<ItemStack> lootPool = new ArrayList<>();

        for (Map<?, ?> item : items) {
            String typeStr = (String) item.get("type");
            int amount = item.get("amount") != null ? ((Number) item.get("amount")).intValue() : 1;
            int chance = item.get("chance") != null ? ((Number) item.get("chance")).intValue() : 50;

            if (random.nextInt(100) < chance) {
                try {
                    Material mat = Material.valueOf(typeStr);
                    lootPool.add(new ItemStack(mat, amount));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (lootPool.isEmpty()) {
            inventory.addItem(new ItemStack(Material.GOLD_INGOT, 3));
            return;
        }

        Collections.shuffle(lootPool);
        int count = Math.min(lootPool.size(), minItems + random.nextInt(maxItems - minItems + 1));

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) slots.add(i);
        Collections.shuffle(slots);

        for (int i = 0; i < count && i < slots.size(); i++) {
            inventory.setItem(slots.get(i), lootPool.get(i));
        }
    }
}
