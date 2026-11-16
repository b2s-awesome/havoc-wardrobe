/*
 * HAVOC Wardrobe - based on Easy Wardrobe by KyNarec (https://github.com/KyNarec/Easy-Wardrobe)
 * Licensed under the GNU General Public License v3.0 or later. See LICENSE.
 */
package dev.havoc.wardrobe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/** Loaded from config/havoc_wardrobe.json. '&' colour codes are allowed in text fields. */
public final class WardrobeConfig {
    /** Title of the chest menu. */
    public String title = "&8&lHAVOC &7» &0Wardrobe";
    /** Prefix for chat messages. */
    public String prefix = "&8[&c&lHAVOC&8] &7";
    /** Extra command names that open the wardrobe besides /wardrobe. Avoid "w": it is vanilla whisper. */
    public List<String> aliases = new ArrayList<>(List.of("wd"));

    /** Economy (only used when EconomyCraft is installed). */
    public boolean economyEnabled = true;
    /** Wardrobe slots every player gets for free (1-9). */
    public int freeSlots = 3;
    /** Price of each extra slot, in order (slot 4, slot 5, ...). Missing entries reuse the last price. */
    public List<Long> slotPrices = new ArrayList<>(List.of(25_000L, 50_000L, 100_000L, 250_000L, 500_000L, 1_000_000L));
    /** Charged every time a player equips a set. 0 = free. */
    public long equipCost = 0L;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static WardrobeConfig load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("havoc_wardrobe.json");
        WardrobeConfig cfg = null;
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                cfg = GSON.fromJson(r, WardrobeConfig.class);
            } catch (Exception e) {
                HavocWardrobe.LOGGER.error("Could not read {}, using defaults", file, e);
            }
        }
        if (cfg == null) cfg = new WardrobeConfig();
        cfg.sanitize();
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(cfg, w); // writes defaults and any new fields back to disk
        } catch (IOException e) {
            HavocWardrobe.LOGGER.error("Could not write {}", file, e);
        }
        return cfg;
    }

    private void sanitize() {
        if (title == null) title = "Wardrobe";
        if (prefix == null) prefix = "";
        if (aliases == null) aliases = new ArrayList<>();
        aliases.removeIf(a -> a == null || a.isBlank() || a.equalsIgnoreCase("wardrobe"));
        freeSlots = Math.max(1, Math.min(WardrobeData.SETS, freeSlots));
        if (slotPrices == null || slotPrices.isEmpty()) slotPrices = new ArrayList<>(List.of(0L));
        slotPrices.replaceAll(p -> p == null || p < 0 ? 0L : p);
        equipCost = Math.max(0L, equipCost);
    }

    /** Price to unlock the given slot index (0-based), which must be >= freeSlots. */
    public long priceForSlot(int slotIndex) {
        int i = Math.max(0, slotIndex - freeSlots);
        return slotPrices.get(Math.min(i, slotPrices.size() - 1));
    }

    /** Converts '&' colour codes to the section sign used by Minecraft's legacy formatting. */
    public static String color(String s) {
        return s.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }
}
