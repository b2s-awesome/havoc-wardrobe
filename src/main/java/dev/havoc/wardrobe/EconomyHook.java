/*
 * HAVOC Wardrobe - based on Easy Wardrobe by KyNarec (https://github.com/KyNarec/Easy-Wardrobe)
 * Licensed under the GNU General Public License v3.0 or later. See LICENSE.
 */
package dev.havoc.wardrobe;

import com.reazip.economycraft.api.v1.BalanceMutationResult;
import com.reazip.economycraft.api.v1.BalanceMutationStatus;
import com.reazip.economycraft.api.v1.EconomyCraftApi;
import com.reazip.economycraft.api.v1.MutationSource;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/** Only loaded when EconomyCraft is installed (see {@link Economy}). Server thread only. */
final class EconomyHook {
    private EconomyHook() {}

    static String format(MinecraftServer server, long amount) {
        return EconomyCraftApi.get(server).formatMoney(amount);
    }

    static long balance(MinecraftServer server, UUID player) {
        return EconomyCraftApi.get(server).balances().getBalance(player);
    }

    static Economy.Charge charge(MinecraftServer server, UUID player, long amount, String reason) {
        BalanceMutationResult r = EconomyCraftApi.get(server).balances()
                .removeMoney(player, amount, MutationSource.of("havoc_wardrobe:" + reason));
        if (r.status() == BalanceMutationStatus.SUCCESS) return Economy.Charge.OK;
        return r.status() == BalanceMutationStatus.INSUFFICIENT_FUNDS ? Economy.Charge.NOT_ENOUGH : Economy.Charge.ERROR;
    }
}
