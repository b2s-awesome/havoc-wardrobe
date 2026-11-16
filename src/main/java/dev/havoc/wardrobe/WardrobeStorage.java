/*
 * HAVOC Wardrobe - based on Easy Wardrobe by KyNarec (https://github.com/KyNarec/Easy-Wardrobe)
 * Licensed under the GNU General Public License v3.0 or later. See LICENSE.
 */
package dev.havoc.wardrobe;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Stores each player's wardrobe in <world>/havoc_wardrobe/<uuid>.dat (gzipped NBT). */
public final class WardrobeStorage {
    private WardrobeStorage() {}

    private static Path dir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("havoc_wardrobe");
    }

    public static WardrobeData load(MinecraftServer server, UUID player) {
        Path file = dir(server).resolve(player + ".dat");
        if (!Files.exists(file)) return new WardrobeData();
        try {
            CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            RegistryOps<Tag> ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            return WardrobeData.CODEC.parse(ops, tag)
                    .resultOrPartial(err -> HavocWardrobe.LOGGER.error("Wardrobe data for {} partly unreadable: {}", player, err))
                    .orElseGet(WardrobeData::new);
        } catch (Exception e) {
            // Keep the broken file so staff can recover it; never overwrite it silently.
            HavocWardrobe.LOGGER.error("Could not read wardrobe for {}", player, e);
            try {
                Files.copy(file, file.resolveSibling(player + ".broken-" + System.currentTimeMillis() + ".dat"));
            } catch (Exception ignored) {
            }
            return new WardrobeData();
        }
    }

    public static void save(MinecraftServer server, UUID player, WardrobeData data) {
        try {
            Path d = dir(server);
            Files.createDirectories(d);
            RegistryOps<Tag> ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            Tag tag = WardrobeData.CODEC.encodeStart(ops, data)
                    .resultOrPartial(err -> HavocWardrobe.LOGGER.error("Could not encode wardrobe for {}: {}", player, err))
                    .orElse(null);
            if (!(tag instanceof CompoundTag compound)) return;
            Path tmp = d.resolve(player + ".dat.tmp");
            NbtIo.writeCompressed(compound, tmp);
            Files.move(tmp, d.resolve(player + ".dat"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            HavocWardrobe.LOGGER.error("Could not save wardrobe for {}", player, e);
        }
    }
}
