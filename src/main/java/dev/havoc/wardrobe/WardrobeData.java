/*
 * HAVOC Wardrobe - based on Easy Wardrobe by KyNarec (https://github.com/KyNarec/Easy-Wardrobe)
 * Licensed under the GNU General Public License v3.0 or later. See LICENSE.
 */
package dev.havoc.wardrobe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * One player's wardrobe: 9 sets of 4 armour pieces (head, chest, legs, feet).
 * The set that is currently worn is stored EMPTY here: its items live on the player,
 * so there is never a second copy in storage.
 */
public final class WardrobeData {
    public static final int SETS = 9;
    public static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public final ItemStack[][] sets = new ItemStack[SETS][SLOTS.length];
    /** Column of the worn set, or -1. */
    public int active = -1;
    /** Extra slots bought with money (on top of the free slots). */
    public int purchased = 0;

    public WardrobeData() {
        for (ItemStack[] set : sets) java.util.Arrays.fill(set, ItemStack.EMPTY);
    }

    public boolean isEmpty(int col) {
        for (ItemStack s : sets[col]) if (!s.isEmpty()) return false;
        return true;
    }

    public static final Codec<WardrobeData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.listOf().listOf().fieldOf("sets").forGetter(WardrobeData::setsAsLists),
            Codec.INT.optionalFieldOf("active", -1).forGetter(d -> d.active),
            Codec.INT.optionalFieldOf("purchased", 0).forGetter(d -> d.purchased)
    ).apply(i, WardrobeData::fromLists));

    private List<List<ItemStack>> setsAsLists() {
        List<List<ItemStack>> out = new ArrayList<>(SETS);
        for (ItemStack[] set : sets) out.add(List.of(set));
        return out;
    }

    private static WardrobeData fromLists(List<List<ItemStack>> lists, int active, int purchased) {
        WardrobeData d = new WardrobeData();
        for (int c = 0; c < Math.min(SETS, lists.size()); c++) {
            List<ItemStack> set = lists.get(c);
            for (int r = 0; r < Math.min(SLOTS.length, set.size()); r++) {
                ItemStack s = set.get(r);
                d.sets[c][r] = s == null ? ItemStack.EMPTY : s;
            }
        }
        d.active = active >= -1 && active < SETS ? active : -1;
        d.purchased = Math.max(0, Math.min(SETS, purchased));
        return d;
    }
}
