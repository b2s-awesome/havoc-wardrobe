/*
 * HAVOC Wardrobe - based on Easy Wardrobe by KyNarec (https://github.com/KyNarec/Easy-Wardrobe)
 * Licensed under the GNU General Public License v3.0 or later. See LICENSE.
 */
package dev.havoc.wardrobe;

import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

/**
 * Optional EconomyCraft link. This class never references EconomyCraft types itself;
 * {@link EconomyHook} does, and it is only loaded when EconomyCraft is installed.
 */
public final class Economy {
    private Economy() {}

    public enum Charge { OK, NOT_ENOUGH, ERROR }

    private static final boolean INSTALLED = FabricLoader.getInstance().isModLoaded("economycraft");

    public static boolean active() {
        return INSTALLED && HavocWardrobe.config().economyEnabled;
    }

    public static String format(MinecraftServer server, long amount) {
        if (!active()) return Long.toString(amount);
        try {
            return EconomyHook.format(server, amount);
        } catch (Throwable t) {
            return Long.toString(amount);
        }
    }

    public static long balance(MinecraftServer server, UUID player) {
        if (!active()) return 0L;
        try {
            return EconomyHook.balance(server, player);
        } catch (Throwable t) {
            return 0L;
        }
    }

    /** Takes money. Free (amount <= 0) or economy off always returns OK. */
    public static Charge charge(MinecraftServer server, UUID player, long amount, String reason) {
        if (!active() || amount <= 0) return Charge.OK;
        try {
            return EconomyHook.charge(server, player, amount, reason);
        } catch (Throwable t) {
            HavocWardrobe.LOGGER.error("EconomyCraft charge failed", t);
            return Charge.ERROR;
        }
    }
}
