/*
 * HAVOC Wardrobe - based on Easy Wardrobe by KyNarec (https://github.com/KyNarec/Easy-Wardrobe)
 * Licensed under the GNU General Public License v3.0 or later. See LICENSE.
 *
 * Changes from Easy Wardrobe: rewritten in Java for Minecraft 26.3 (Mojang names), every wardrobe
 * slot is locked and each click type is handled explicitly (no drag/double-click/number-key
 * paths into the menu), the worn set is not kept as a stored copy, Curse of Binding is
 * respected, and optional EconomyCraft costs for extra slots and equipping.
 */
package dev.havoc.wardrobe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * 9x6 chest menu. Rows 0-3: head/chest/legs/feet of each set. Row 4: equip buttons.
 * Row 5: info, close, balance. Vanilla clients see a normal chest.
 */
public final class WardrobeMenu extends AbstractContainerMenu {
    private static final int COLS = 9;
    private static final int ROWS = 6;
    private static final int SIZE = COLS * ROWS;
    private static final int BUTTON_ROW = 4;
    private static final int INFO_SLOT = 45;
    private static final int CLOSE_SLOT = 49;
    private static final int BALANCE_SLOT = 53;

    private static final Item[] PANES = {
            Items.STAINED_GLASS_PANE.pick(DyeColor.RED), Items.STAINED_GLASS_PANE.pick(DyeColor.ORANGE), Items.STAINED_GLASS_PANE.pick(DyeColor.YELLOW),
            Items.STAINED_GLASS_PANE.pick(DyeColor.LIME), Items.STAINED_GLASS_PANE.pick(DyeColor.GREEN), Items.STAINED_GLASS_PANE.pick(DyeColor.CYAN),
            Items.STAINED_GLASS_PANE.pick(DyeColor.LIGHT_BLUE), Items.STAINED_GLASS_PANE.pick(DyeColor.BLUE), Items.STAINED_GLASS_PANE.pick(DyeColor.PURPLE)};
    private static final String[] PIECE = {"Helmet", "Chestplate", "Leggings", "Boots"};

    private final SimpleContainer display = new SimpleContainer(SIZE);
    private final ServerPlayer owner;
    private final MinecraftServer server;
    private final WardrobeData data;

    public WardrobeMenu(int containerId, Inventory playerInv, ServerPlayer owner, MinecraftServer server) {
        super(MenuType.GENERIC_9x6, containerId);
        this.owner = owner;
        this.server = server;
        this.data = WardrobeStorage.load(server, owner.getUUID());

        // Same slot order and positions as a vanilla 6-row chest, so vanilla clients lay it out correctly.
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                addSlot(new LockedSlot(display, col + row * COLS, 8 + col * 18, 18 + row * 18));
            }
        }
        int invTop = 18 + ROWS * 18 + 13;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, invTop + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, invTop + 58));
        }
        render();
    }

    // ---------------------------------------------------------------- helpers

    private WardrobeConfig cfg() {
        return HavocWardrobe.config();
    }

    private int unlockedSlots() {
        if (!Economy.active()) return WardrobeData.SETS;
        return Math.min(WardrobeData.SETS, cfg().freeSlots + data.purchased);
    }

    private boolean isUnlocked(int col) {
        return col < unlockedSlots();
    }

    private void msg(String text) {
        owner.sendSystemMessage(Component.literal(WardrobeConfig.color(cfg().prefix + text)));
    }

    private static ItemStack named(Item item, String name, String... lore) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.CUSTOM_NAME, plain(name));
        if (lore.length > 0) {
            List<Component> lines = new ArrayList<>();
            for (String l : lore) lines.add(plain(l));
            s.set(DataComponents.LORE, new ItemLore(lines));
        }
        return s;
    }

    private static Component plain(String legacy) {
        return Component.literal(WardrobeConfig.color(legacy)).withStyle(st -> st.withItalic(false));
    }

    private boolean wearsBinding() {
        if (owner.isCreative()) return false;
        for (EquipmentSlot slot : WardrobeData.SLOTS) {
            if (EnchantmentHelper.has(owner.getItemBySlot(slot), EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- rendering

    private void render() {
        for (int col = 0; col < COLS; col++) renderColumn(col);
        String cost = cfg().equipCost > 0 && Economy.active()
                ? "&7Equipping costs &e" + Economy.format(server, cfg().equipCost) + "&7."
                : "&7Equipping is free.";
        display.setItem(INFO_SLOT, named(Items.BOOK, "&c&lHAVOC Wardrobe",
                "&7Put armour into a column to save a set.",
                "&7Click the dye under a column to wear it.",
                "&7Click again to take it off into the column.",
                cost));
        display.setItem(CLOSE_SLOT, named(Items.BARRIER, "&cClose"));
        if (Economy.active()) {
            display.setItem(BALANCE_SLOT, named(Items.GOLD_NUGGET, "&6Balance",
                    "&e" + Economy.format(server, Economy.balance(server, owner.getUUID()))));
        } else {
            display.setItem(BALANCE_SLOT, filler());
        }
        for (int s = INFO_SLOT + 1; s < SIZE; s++) {
            if (s != CLOSE_SLOT && s != BALANCE_SLOT) display.setItem(s, filler());
        }
    }

    private static ItemStack filler() {
        return named(Items.STAINED_GLASS_PANE.pick(DyeColor.BLACK), " ");
    }

    private void renderColumn(int col) {
        if (!isUnlocked(col)) {
            for (int row = 0; row < 4; row++) display.setItem(col + row * COLS, named(Items.STAINED_GLASS_PANE.pick(DyeColor.GRAY), "&8Locked"));
            boolean next = col == unlockedSlots();
            display.setItem(col + BUTTON_ROW * COLS, next
                    ? named(Items.GOLD_INGOT, "&6Unlock slot " + (col + 1),
                        "&7Price: &e" + Economy.format(server, cfg().priceForSlot(col)), "&eClick to buy")
                    : named(Items.BARRIER, "&8Locked", "&7Unlock the earlier slots first"));
            return;
        }
        boolean worn = data.active == col;
        for (int row = 0; row < 4; row++) {
            ItemStack shown;
            if (worn) {
                // Display copy of what the player wears right now. Locked slot: cannot be taken.
                ItemStack w = owner.getItemBySlot(WardrobeData.SLOTS[row]);
                shown = w.isEmpty() ? named(PANES[col], "&8(no " + PIECE[row].toLowerCase() + ")") : w.copy();
                if (!w.isEmpty()) shown.set(DataComponents.CUSTOM_NAME, w.getHoverName().copy().withStyle(ChatFormatting.RED));
            } else {
                ItemStack st = data.sets[col][row];
                shown = st.isEmpty() ? named(PANES[col], "&7Empty " + PIECE[row].toLowerCase(), "&8Click with a " + PIECE[row].toLowerCase() + " to store it") : st.copy();
            }
            display.setItem(col + row * COLS, shown);
        }
        ItemStack button;
        if (worn) button = named(Items.DYE.pick(DyeColor.LIME), "&aEquipped", "&7Click to take this set off");
        else if (data.isEmpty(col)) button = named(Items.DYE.pick(DyeColor.GRAY), "&7Empty set " + (col + 1));
        else button = named(Items.DYE.pick(DyeColor.PINK), "&eWear set " + (col + 1), "&7Click to equip");
        display.setItem(col + BUTTON_ROW * COLS, button);
    }

    // ---------------------------------------------------------------- clicks

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId < 0 || slotId >= SIZE) {
            // Player inventory, outside clicks, drags: vanilla behaviour (wardrobe slots refuse items, see below).
            super.clicked(slotId, button, clickType, player);
            return;
        }
        // Wardrobe area: only plain clicks and shift-clicks do anything.
        if (clickType != ContainerInput.PICKUP && clickType != ContainerInput.QUICK_MOVE) return;
        int col = slotId % COLS;
        int row = slotId / COLS;

        if (slotId == CLOSE_SLOT) {
            owner.closeContainer();
            return;
        }
        if (row < 4) {
            if (isUnlocked(col) && data.active != col) clickArmour(col, row, clickType);
        } else if (row == BUTTON_ROW) {
            if (!isUnlocked(col)) buySlot(col);
            else if (data.active == col) takeOff(col);
            else if (!data.isEmpty(col)) wear(col);
        }
        render();
        WardrobeStorage.save(server, owner.getUUID(), data);
    }

    private void clickArmour(int col, int row, ContainerInput type) {
        ItemStack stored = data.sets[col][row];
        if (type == ContainerInput.QUICK_MOVE) {
            if (stored.isEmpty()) return;
            owner.getInventory().add(stored); // shrinks 'stored' by what fitted
            data.sets[col][row] = stored.isEmpty() ? ItemStack.EMPTY : stored;
            return;
        }
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            if (stored.isEmpty()) return;
            setCarried(stored);
            data.sets[col][row] = ItemStack.EMPTY;
            return;
        }
        if (owner.getEquipmentSlotForItem(carried) != WardrobeData.SLOTS[row]) {
            msg("&cThat doesn't go in the " + PIECE[row].toLowerCase() + " slot.");
            return;
        }
        if (stored.isEmpty()) {
            data.sets[col][row] = carried.split(1);
            setCarried(carried);
        } else if (carried.getCount() == 1) {
            data.sets[col][row] = carried;
            setCarried(stored);
        }
    }

    private void wear(int col) {
        if (wearsBinding()) {
            msg("&cYou can't change armour while wearing Curse of Binding.");
            return;
        }
        if (Economy.active() && cfg().equipCost > 0) {
            switch (Economy.charge(server, owner.getUUID(), cfg().equipCost, "equip")) {
                case NOT_ENOUGH -> {
                    msg("&cYou need &e" + Economy.format(server, cfg().equipCost) + " &cto change sets.");
                    return;
                }
                case ERROR -> {
                    msg("&cPayment failed. Try again later.");
                    return;
                }
                default -> {}
            }
        }
        // 1. Whatever is worn now: back into the old set's column, or into the inventory.
        for (int i = 0; i < WardrobeData.SLOTS.length; i++) {
            EquipmentSlot slot = WardrobeData.SLOTS[i];
            ItemStack worn = owner.getItemBySlot(slot);
            owner.setItemSlot(slot, ItemStack.EMPTY);
            if (worn.isEmpty()) continue;
            if (data.active >= 0 && data.sets[data.active][i].isEmpty()) data.sets[data.active][i] = worn;
            else owner.getInventory().placeItemBackInInventory(worn, Prediction.SERVER_ONLY);
        }
        // 2. Move the chosen set onto the player; storage keeps no copy.
        for (int i = 0; i < WardrobeData.SLOTS.length; i++) {
            owner.setItemSlot(WardrobeData.SLOTS[i], data.sets[col][i]);
            data.sets[col][i] = ItemStack.EMPTY;
        }
        data.active = col;
        msg("&aNow wearing set " + (col + 1) + ".");
    }

    private void takeOff(int col) {
        if (wearsBinding()) {
            msg("&cYou can't change armour while wearing Curse of Binding.");
            return;
        }
        for (int i = 0; i < WardrobeData.SLOTS.length; i++) {
            EquipmentSlot slot = WardrobeData.SLOTS[i];
            ItemStack worn = owner.getItemBySlot(slot);
            owner.setItemSlot(slot, ItemStack.EMPTY);
            if (worn.isEmpty()) continue;
            if (data.sets[col][i].isEmpty()) data.sets[col][i] = worn;
            else owner.getInventory().placeItemBackInInventory(worn, Prediction.SERVER_ONLY);
        }
        data.active = -1;
        msg("&7Set " + (col + 1) + " stored.");
    }

    private void buySlot(int col) {
        if (col != unlockedSlots()) return;
        long price = cfg().priceForSlot(col);
        switch (Economy.charge(server, owner.getUUID(), price, "unlock_slot")) {
            case OK -> {
                data.purchased++;
                msg("&aUnlocked wardrobe slot " + (col + 1) + "!");
            }
            case NOT_ENOUGH -> msg("&cYou need &e" + Economy.format(server, price) + " &cto unlock slot " + (col + 1) + ".");
            case ERROR -> msg("&cPayment failed. Try again later.");
        }
    }

    // ---------------------------------------------------------------- vanilla hooks

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Shift-clicking in the player inventory does nothing (no auto-insert into the wardrobe).
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && owner.isAlive();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != display && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.container != display && super.canDragTo(slot);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        WardrobeStorage.save(server, owner.getUUID(), data);
    }

    /** Display-only slot: nothing can be taken out or put in by vanilla logic. */
    private static final class LockedSlot extends Slot {
        LockedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
