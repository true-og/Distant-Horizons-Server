/*
 * DH Support, server-side support for Distant Horizons.
 * Copyright (C) 2024 Jim C K Flaten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package no.jckf.dhsupport.bukkit;

import no.jckf.dhsupport.core.Coordinates;
import no.jckf.dhsupport.core.configuration.Configuration;
import no.jckf.dhsupport.core.configuration.DhsConfig;
import no.jckf.dhsupport.core.configuration.WorldConfiguration;
import no.jckf.dhsupport.core.world.WorldInterface;
import org.bukkit.*;
import org.bukkit.block.Beacon;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class BukkitWorldInterface implements WorldInterface
{
    protected static boolean BIOME_KEY_WARNING_SENT = false;

    protected static boolean ASYNC_LOAD_WARNING_SENT = false;

    protected DhSupportBukkitPlugin plugin;

    protected World world;

    protected Configuration config;

    protected WorldConfiguration worldConfig;

    protected Logger logger;

    protected Map<String, ChunkSnapshot> chunks = new HashMap<>();

    protected UnsafeValues unsafeValues;

    @Nullable
    protected Method getCoordinateScale;

    @Nullable
    protected Method getBiomeKey;

    @Nullable
    protected Method getChunkAtAsync;

    public BukkitWorldInterface(DhSupportBukkitPlugin plugin, World world, Configuration config)
    {
        this.plugin = plugin;
        this.world = world;
        this.config = config;
        this.worldConfig = new WorldConfiguration(this, config);
    }

    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }

    public Logger getLogger()
    {
        return this.logger;
    }

    public void doUnsafeThings()
    {
        this.unsafeValues = Bukkit.getUnsafe();

        // Does not exist for 1.16.5.
        try {
            this.getCoordinateScale = this.world.getClass().getMethod("getCoordinateScale");
        } catch (NoSuchMethodException e) {

        }

        // Detect if we're running under Paper (or a Paper fork) that has this patch:
        // https://github.com/PaperMC/Paper/commit/5bf259115c1ce29dd96df5fcf53739c94d39f902
        try {
            Class<?> regionAccessor = Class.forName("org.bukkit.RegionAccessor");

            this.getBiomeKey = this.unsafeValues.getClass().getMethod("getBiomeKey", regionAccessor, int.class, int.class, int.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            if (!BIOME_KEY_WARNING_SENT) {
                this.getLogger().warning("Custom biomes are not supported on this server.");

                BIOME_KEY_WARNING_SENT = true;
            }
        }

        try {
            this.getChunkAtAsync = this.world.getClass().getMethod("getChunkAtAsync", int.class, int.class, boolean.class);
        } catch (NoSuchMethodException exception) {
            if (!ASYNC_LOAD_WARNING_SENT) {
                this.getLogger().warning("Async chunk loading is not supported on this server. Performance will suffer.");

                ASYNC_LOAD_WARNING_SENT = true;
            }
        }
    }

    @Override
    public WorldInterface newInstance()
    {
        BukkitWorldInterface newInstance = new BukkitWorldInterface(this.plugin, this.world, this.config);

        newInstance.setLogger(this.getLogger());

        newInstance.doUnsafeThings();

        return newInstance;
    }

    protected ChunkSnapshot getChunk(int x, int z)
    {
        int chunkX = Coordinates.blockToChunk(x);
        int chunkZ = Coordinates.blockToChunk(z);

        String key = chunkX + "x" + chunkZ;

        if (this.chunks.containsKey(key)) {
            return this.chunks.get(key);
        }

        ChunkSnapshot chunk = this.world.getChunkAt(chunkX, chunkZ).getChunkSnapshot(true, true, false);

        this.chunks.put(key, chunk);

        return chunk;
    }

    @Override
    public UUID getId()
    {
        return this.world.getUID();
    }

    @Override
    public String getName()
    {
        return this.world.getName();
    }

    @Override
    public String getKey()
    {
        return this.getName().replaceAll("[^a-zA-Z0-9_-]", "");
    }

    @Override
    public double getCoordinateScale()
    {
        if (this.getCoordinateScale == null) {
            boolean isNether = this.world.getEnvironment().equals(World.Environment.NETHER);

            return isNether ? 8 : 1;
        }

        try {
            return (double) this.getCoordinateScale.invoke(this.world);
        } catch (InvocationTargetException | IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }

    protected boolean useVanillaWorldBorder()
    {
        return this.worldConfig.getBool(DhsConfig.USE_VANILLA_WORLD_BORDER, true);
    }

    @Override
    public Integer getWorldBorderX()
    {
        if (this.useVanillaWorldBorder()) {
            return this.world.getWorldBorder().getCenter().getBlockX();
        }

        return this.worldConfig.getInt(DhsConfig.BORDER_CENTER_X);
    }

    @Override
    public Integer getWorldBorderZ()
    {
        if (this.useVanillaWorldBorder()) {
            return this.world.getWorldBorder().getCenter().getBlockZ();
        }

        return this.worldConfig.getInt(DhsConfig.BORDER_CENTER_Z);
    }

    @Override
    public Integer getWorldBorderRadius()
    {
        if (!this.useVanillaWorldBorder()) {
            return this.worldConfig.getInt(DhsConfig.BORDER_RADIUS);
        }

        int borderRadius = (int) (this.world.getWorldBorder().getSize() / 2.0);

        if (this.worldConfig.getString(DhsConfig.VANILLA_WORLD_BORDER_EXPANSION, "auto").equals("auto")) {
            borderRadius += Coordinates.chunkToBlock(this.world.getViewDistance());
        } else {
            borderRadius += Coordinates.chunkToBlock(this.worldConfig.getInt(DhsConfig.VANILLA_WORLD_BORDER_EXPANSION, 0));
        }

        return borderRadius;
    }

    @Override
    public boolean chunkExists(int x, int z)
    {
        int chunkX = Coordinates.blockToChunk(x);
        int chunkZ = Coordinates.blockToChunk(z);

        boolean alreadyLoaded = this.world.isChunkLoaded(chunkX, chunkZ);

        if (alreadyLoaded) {
            return true;
        }

        int regionX = Coordinates.chunkToRegion(chunkX);
        int regionZ = Coordinates.chunkToRegion(chunkZ);

        File regionFile = new File(world.getWorldFolder() + "/region/r." + regionX + "." + regionZ + ".mca");

        if (!regionFile.exists()) {
            return false;
        }

        boolean exists = this.world.loadChunk(chunkX, chunkZ, false);

        if (exists) {
            this.world.unloadChunk(chunkX, chunkZ, false);
        }

        return exists;
    }

    @Override
    public boolean isChunkLoaded(int x, int z)
    {
        int chunkX = Coordinates.blockToChunk(x);
        int chunkZ = Coordinates.blockToChunk(z);

        return this.world.isChunkLoaded(chunkX, chunkZ);
    }

    @Override
    public boolean loadChunk(int x, int z)
    {
        int chunkX = Coordinates.blockToChunk(x);
        int chunkZ = Coordinates.blockToChunk(z);

        return this.world.loadChunk(chunkX, chunkZ, false);
    }

    @Override
    public boolean loadOrGenerateChunk(int x, int z)
    {
        int chunkX = Coordinates.blockToChunk(x);
        int chunkZ = Coordinates.blockToChunk(z);

        return this.world.loadChunk(chunkX, chunkZ, true);
    }

    @Override
    public CompletableFuture<Boolean> loadChunkAsync(int x, int z)
    {
        if (this.getChunkAtAsync == null) {
            return this.plugin.getDhSupport().getScheduler().runOnMainThread(() -> this.loadChunk(x, z));
        }

        int chunkX = Coordinates.blockToChunk(x);
        int chunkZ = Coordinates.blockToChunk(z);

        try {
            CompletableFuture<Chunk> chunkFuture = (CompletableFuture<Chunk>) this.getChunkAtAsync.invoke(this.world, chunkX, chunkZ, false);

            return chunkFuture.thenApply(Objects::nonNull);
        } catch (InvocationTargetException | IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public CompletableFuture<Boolean> loadOrGenerateChunkAsync(int x, int z)
    {
        if (this.getChunkAtAsync == null) {
            return this.plugin.getDhSupport().getScheduler().runOnMainThread(() -> this.loadOrGenerateChunk(x, z));
        }

        int chunkX = Coordinates.blockToChunk(x);
        int chunkZ = Coordinates.blockToChunk(z);

        try {
            CompletableFuture<Chunk> chunkFuture = (CompletableFuture<Chunk>) this.getChunkAtAsync.invoke(this.world, chunkX, chunkZ, true);

            return chunkFuture.thenApply(Objects::nonNull);
        } catch (InvocationTargetException | IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public boolean unloadChunk(int x, int z)
    {
        int chunkX = Coordinates.blockToChunk(x);
        int chunkZ = Coordinates.blockToChunk(z);

        return this.world.unloadChunk(chunkX, chunkZ);
    }

    @Override
    public boolean unloadChunkAsync(int x, int z)
    {
        int chunkX = Coordinates.blockToChunk(x);
        int chunkZ = Coordinates.blockToChunk(z);

        return this.world.unloadChunkRequest(chunkX, chunkZ);
    }

    @Override
    public boolean discardChunk(int x, int z)
    {
        int chunkX = Coordinates.blockToChunk(x);
        int chunkZ = Coordinates.blockToChunk(z);

        return this.world.unloadChunk(chunkX, chunkZ, false);
    }

    @Override
    public int getMinY()
    {
        return this.world.getMinHeight();
    }

    @Override
    public int getMaxY()
    {
        return this.world.getMaxHeight();
    }

    @Override
    public int getSeaLevel()
    {
        return Math.min(Math.max(this.getMinY(), this.world.getSeaLevel()), this.getMaxY());
    }

    @Override
    public int getHighestYAt(int x, int z)
    {
        return this.getChunk(x, z).getHighestBlockYAt(Coordinates.blockToChunkRelative(x), Coordinates.blockToChunkRelative(z));
    }

    @Override
    public String getBiomeAt(int x, int z)
    {
        NamespacedKey key = this.getChunk(x, z).getBiome(Coordinates.blockToChunkRelative(x), this.getSeaLevel(), Coordinates.blockToChunkRelative(z)).getKey();

        // If the server just reports "custom" and we have access to getBiomeKey, try to get the correct biome name.
        if (key.toString().equals("minecraft:custom") && this.getBiomeKey != null) {
            try {
                key = (NamespacedKey) this.getBiomeKey.invoke(this.unsafeValues, this.world, x, this.getSeaLevel(), z);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new RuntimeException(exception);
            }
        }

        return key.toString();
    }

    @Override
    public String getMaterialAt(int x, int y, int z)
    {
        return this.getChunk(x, z).getBlockType(Coordinates.blockToChunkRelative(x), y, Coordinates.blockToChunkRelative(z)).getKey().toString();
    }

    @Override
    public String getBlockStateAsStringAt(int x, int y, int z)
    {
        return this.getChunk(x, z).getBlockData(Coordinates.blockToChunkRelative(x), y, Coordinates.blockToChunkRelative(z)).getAsString();
    }

    @Override
    public Map<String, String> getBlockPropertiesAt(int x, int y, int z)
    {
        Map<String, String> properties = new HashMap<>();

        String dataString = this.getBlockStateAsStringAt(x, y, z);

        int kvStart = dataString.indexOf("[");

        if (kvStart == -1) {
            return properties;
        }

        String[] kvStrings = dataString.substring(kvStart + 1, dataString.length() - 1).split(",");

        for (String kvString : kvStrings) {
            String[] kv = kvString.split("=", 2);

            properties.put(kv[0], kv[1]);
        }

        return properties;
    }

    @Override
    public byte getBlockLightAt(int x, int y, int z)
    {
        return (byte) this.getChunk(x, z).getBlockEmittedLight(Coordinates.blockToChunkRelative(x), y, Coordinates.blockToChunkRelative(z));
    }

    @Override
    public byte getSkyLightAt(int x, int y, int z)
    {
        return (byte) this.getChunk(x, z).getBlockSkyLight(Coordinates.blockToChunkRelative(x), y, Coordinates.blockToChunkRelative(z));
    }

    @Override
    public Configuration getConfig()
    {
        return this.worldConfig;
    }

    @Override
    public boolean isBeacon(int x, int y, int z)
    {
        if (!this.getMaterialAt(x, y, z).equals("minecraft:beacon")) {
            return false;
        }

        return this.plugin.getDhSupport().getScheduler()
            .runOnRegionThread(this.world.getUID(), x, z, () ->
                ((Beacon) this.world.getBlockAt(x, y, z).getState()).getTier() > 0
            )
            .join();
    }

    @Override
    public int getBeaconColor(int x, int y, int z)
    {
        int skipped = 0;
        int n = 0;
        int red = 0;
        int green = 0;
        int blue = 0;

        int maxY = this.getMaxY();

        while (y < maxY) {
            y++;

            String material = this.getMaterialAt(x, y, z);

            if (!material.startsWith("minecraft:") || (!material.endsWith("stained_glass") && !material.endsWith("stained_glass_pane"))) {
                skipped++;

                if (skipped >= 5) {
                    break;
                }

                continue;
            }

            n++;

            String colorName = material.split("[:_]")[1].toUpperCase();

            Color color = DyeColor.valueOf(colorName).getColor();

            red += color.getRed();
            green += color.getGreen();
            blue += color.getBlue();

            if (n > 1) {
                red /= 2;
                green /= 2;
                blue /= 2;
            }
        }

        if (n == 0) {
            return Color.WHITE.asRGB();
        }

        return (new java.awt.Color(red, green, blue)).getRGB();
    }
}
