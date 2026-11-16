/*
 * HAVOC Wardrobe - based on Easy Wardrobe by KyNarec (https://github.com/KyNarec/Easy-Wardrobe)
 * Licensed under the GNU General Public License v3.0 or later. See LICENSE.
 */
package dev.havoc.wardrobe;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HavocWardrobe implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("havoc_wardrobe");
    private static WardrobeConfig config;

    public static WardrobeConfig config() {
        return config;
    }

    @Override
    public void onInitialize() {
        config = WardrobeConfig.load();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
        LOGGER.info("HAVOC Wardrobe loaded (economy {})", Economy.active() ? "on" : "off");
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(
                Commands.literal("wardrobe").executes(HavocWardrobe::open));
        for (String alias : config.aliases) {
            dispatcher.register(Commands.literal(alias).executes(HavocWardrobe::open).redirect(root));
        }
    }

    private static int open(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can open the wardrobe."));
            return 0;
        }
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new WardrobeMenu(id, inv, player, source.getServer()),
                Component.literal(WardrobeConfig.color(config.title))));
        return 1;
    }
}
