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

package no.jckf.dhsupport.core;

import no.jckf.dhsupport.core.bytestream.Encoder;
import no.jckf.dhsupport.core.configuration.Configurable;
import no.jckf.dhsupport.core.configuration.Configuration;
import no.jckf.dhsupport.core.configuration.DhsConfig;
import no.jckf.dhsupport.core.database.Database;
import no.jckf.dhsupport.core.database.migrations.CreateLodsTable;
import no.jckf.dhsupport.core.database.models.LodModel;
import no.jckf.dhsupport.core.database.repositories.AsyncLodRepository;
import no.jckf.dhsupport.core.dataobject.Beacon;
import no.jckf.dhsupport.core.dataobject.Lod;
import no.jckf.dhsupport.core.dataobject.SectionPosition;
import no.jckf.dhsupport.core.handler.LodHandler;
import no.jckf.dhsupport.core.handler.PlayerConfigHandler;
import no.jckf.dhsupport.core.handler.PluginMessageHandler;
import no.jckf.dhsupport.core.lodbuilders.LodBuilder;
import no.jckf.dhsupport.core.message.plugin.FullDataChunkMessage;
import no.jckf.dhsupport.core.message.plugin.FullDataPartialUpdateMessage;
import no.jckf.dhsupport.core.message.plugin.PluginMessageSender;
import no.jckf.dhsupport.core.scheduling.Scheduler;
import no.jckf.dhsupport.core.world.WorldInterface;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class DhSupport implements Configurable
{
    protected String dataDirectory;

    protected Database database;

    protected AsyncLodRepository lodRepository;

    protected Configuration configuration;

    protected Logger logger;

    protected Scheduler scheduler;

    protected Map<UUID, WorldInterface> worldInterfaces = new HashMap<>();

    protected PluginMessageHandler pluginMessageHandler;

    protected PluginMessageSender pluginMessageSender;

    protected Map<String, CompletableFuture<Lod>> queuedBuilders = new HashMap<>();

    protected Map<String, LodModel> touchedLods = new ConcurrentHashMap<>();

    protected Map<UUID, Configuration> playerConfigurations = new HashMap<>();

    public DhSupport()
    {
        this.configuration = new Configuration();

        this.database = new Database();
        this.lodRepository = new AsyncLodRepository(this.database);

        this.pluginMessageHandler = new PluginMessageHandler(this);
    }

    public void onEnable()
    {
        this.lodRepository.setLogger(this.getLogger());

        try {
            this.database.open(this.getDataDirectory() + "/data.sqlite");

            this.database.addMigration(CreateLodsTable.class);

            this.database.migrate();
        } catch (Exception exception) {
            this.warning("Failed to initialize database: " + exception.getMessage());
            // TODO: Disable the plugin?
        }

        (new PlayerConfigHandler(this, this.pluginMessageHandler)).register();
        (new LodHandler(this, this.pluginMessageHandler)).register();

        this.pluginMessageHandler.onEnable();
    }

    public void onDisable()
    {
        if (this.pluginMessageHandler != null) {
            this.pluginMessageHandler.onDisable();
        }
    }

    public void setDataDirectory(String dataDirectory)
    {
        this.dataDirectory = dataDirectory;
    }

    public String getDataDirectory()
    {
        return this.dataDirectory;
    }

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    public Scheduler getScheduler()
    {
        return this.scheduler;
    }

    public void setWorldInterface(UUID id, @Nullable WorldInterface worldInterface)
    {
        if (worldInterface == null) {
            this.worldInterfaces.remove(id);
            return;
        }

        this.worldInterfaces.put(id, worldInterface);
    }

    @Nullable
    public WorldInterface getWorldInterface(UUID id)
    {
        return this.worldInterfaces.get(id);
    }

    public PluginMessageHandler getPluginMessageHandler()
    {
        return this.pluginMessageHandler;
    }

    public void setPluginMessageSender(PluginMessageSender sender)
    {
        this.pluginMessageSender = sender;
    }

    @Nullable
    public PluginMessageSender getPluginMessageSender()
    {
        return this.pluginMessageSender;
    }

    public AsyncLodRepository getLodRepository()
    {
        return this.lodRepository;
    }

    public Configuration getConfig()
    {
        return this.configuration;
    }

    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }

    @Nullable
    public Logger getLogger()
    {
        return this.logger;
    }

    public void info(String message)
    {
        this.getLogger().info(message);
    }

    public void warning(String message)
    {
        this.getLogger().warning(message);
    }

    public void setPlayerConfiguration(UUID playerId, Configuration playerConfiguration)
    {
        this.playerConfigurations.put(playerId, playerConfiguration);
    }

    public Configuration getPlayerConfiguration(UUID playerId)
    {
        return this.playerConfigurations.get(playerId);
    }

    public void clearPlayerConfiguration(UUID playerId)
    {
        this.playerConfigurations.remove(playerId);
    }

    public LodBuilder getBuilder(WorldInterface world, SectionPosition position)
    {
        String builderType = world.getConfig().getString(DhsConfig.BUILDER_TYPE);

        LodBuilder builder;

        try {
            Class<? extends LodBuilder> builderClass = Class.forName(LodBuilder.class.getPackageName() + "." + builderType).asSubclass(LodBuilder.class);

            Constructor<?> constructor = builderClass.getConstructor(WorldInterface.class, SectionPosition.class);

            builder = (LodBuilder) constructor.newInstance(world, position);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | ClassNotFoundException exception) {
            return null;
        }

        return builder;
    }

    public CompletableFuture<Lod> queueBuilder(UUID worldId, SectionPosition position, LodBuilder builder)
    {
        String key = LodModel.create()
            .setWorldId(worldId)
            .setX(position.getX())
            .setZ(position.getZ())
            .toString();

        if (this.queuedBuilders.containsKey(key)) {
            return this.queuedBuilders.get(key);
        }

        Scheduler scheduler = this.getScheduler();

        CompletableFuture<Lod> queued;

        // TODO: Fetch chunk data on region thread, then move to builder thread.
        if (scheduler.canReadWorldAsync()) {
            queued = this.getScheduler().runOnSeparateThread(
                builder::generate
            );
        } else {
            queued = this.getScheduler().runOnRegionThread(
                worldId,
                Coordinates.sectionToBlock(position.getX()),
                Coordinates.sectionToBlock(position.getZ()),
                builder::generate
            );
        }

        queued = queued.thenApply((lod) -> {
                this.queuedBuilders.remove(key);

                return lod;
            })
            .exceptionally((exception) -> {
                exception.printStackTrace();

                this.queuedBuilders.remove(key);

                return null;
            });

        this.queuedBuilders.put(key, queued);

        return queued;
    }

    public CompletableFuture<LodModel> getLod(UUID worldId, SectionPosition position)
    {
        int worldX = Coordinates.sectionToBlock(position.getX());
        int worldZ = Coordinates.sectionToBlock(position.getZ());

        return this.getLodRepository()
            .loadLodAsync(worldId, position.getX(), position.getZ())
            .thenCompose((modelFromDb) -> {
                // If a LOD was found in the database, return it.
                if (modelFromDb != null) {
                    return CompletableFuture.completedFuture(modelFromDb);
                }

                WorldInterface world = this.getWorldInterface(worldId).newInstance();

                Map<String, CompletableFuture<Boolean>> loads = new HashMap<>();

                // Load all the chunks we need for this request.
                for (int xMultiplier = 0; xMultiplier < 4; xMultiplier++) {
                    for (int zMultiplier = 0; zMultiplier < 4; zMultiplier++) {
                        int chunkX = worldX + 16 * xMultiplier;
                        int chunkZ = worldZ + 16 * zMultiplier;

                        if (world.isChunkLoaded(chunkX, chunkZ)) {
                            loads.put(worldX + "x" + worldZ, world.loadChunkAsync(chunkX, chunkZ));
                        }
                    }
                }

                // Wait for chunk loads, then...
                return CompletableFuture.allOf(loads.values().toArray(new CompletableFuture[loads.size()]))
                    .thenCompose((asd) -> {
                        // No LOD was found. Start building a new one.
                        CompletableFuture<Lod> lodFuture = this.queueBuilder(worldId, position, this.getBuilder(world, position));

                        // Find any beacons that should appear in this LOD.
                        // TODO: Config option to disable beacons?
                        CompletableFuture<Collection<Beacon>> beaconFuture = this.getScheduler().runOnRegionThread(worldId, worldX, worldZ, () -> {
                            Collection<Beacon> accumulator = new ArrayList<>();

                            boolean includeBeacons = world.getConfig().getBool(DhsConfig.INCLUDE_BEACONS, false);

                            if (includeBeacons) {
                                for (int xMultiplier = 0; xMultiplier < 4; xMultiplier++) {
                                    for (int zMultiplier = 0; zMultiplier < 4; zMultiplier++) {
                                        accumulator.addAll(world.getBeaconsInChunk(worldX + 16 * xMultiplier, worldZ + 16 * zMultiplier));
                                    }
                                }
                            }

                            return accumulator;
                        });

                        // Combine the LOD and beacons and save the result in the database.
                        return lodFuture.thenCombine(beaconFuture, (lod, beacons) -> {
                                // Discard the chunks we loaded.
                                this.getScheduler().runOnRegionThread(worldId, worldX, worldZ, () -> {
                                    for (String key : loads.keySet()) {
                                        String[] xz = key.split("x", 2);

                                        world.discardChunk(
                                            Coordinates.chunkToBlock(Integer.parseInt(xz[0])),
                                            Coordinates.chunkToBlock(Integer.parseInt(xz[1]))
                                        );
                                    }

                                    return null;
                                });

                                Encoder lodEncoder = new Encoder();
                                lod.encode(lodEncoder);

                                Encoder beaconEncoder = new Encoder();
                                beaconEncoder.writeCollection(beacons);

                                return this.lodRepository.saveLodAsync(
                                    worldId,
                                    position.getX(),
                                    position.getZ(),
                                    lodEncoder.toByteArray(),
                                    beaconEncoder.toByteArray()
                                );
                            })
                            .thenCompose((f) -> f); // Unwrap the nested future.
                    });
            });
    }

    public void touchLod(UUID worldId, int x, int z)
    {
        int sectionX = Coordinates.blockToSection(x);
        int sectionZ = Coordinates.blockToSection(z);

        LodModel lodModel = LodModel.create()
            .setWorldId(worldId)
            .setX(sectionX)
            .setZ(sectionZ);

        String key = lodModel.toString();

        if (this.touchedLods.containsKey(key)) {
            return;
        }

        this.touchedLods.put(key, lodModel);
    }

    public void updateTouchedLods()
    {
        for (String key : this.touchedLods.keySet()) {
            LodModel lodModelToDelete = this.touchedLods.get(key);

            this.getLodRepository().deleteLodAsync(lodModelToDelete.getWorldId(), lodModelToDelete.getX(), lodModelToDelete.getZ())
                .thenAccept((deleted) -> {
                    if (!deleted) {
                        return;
                    }

                    SectionPosition position = new SectionPosition();
                    position.setDetailLevel(6);
                    position.setX(lodModelToDelete.getX());
                    position.setZ(lodModelToDelete.getZ());

                    this.getLod(lodModelToDelete.getWorldId(), position)
                        .thenAccept((newLodModel) -> {
                            WorldInterface world = this.getWorldInterface(newLodModel.getWorldId());
                            Configuration worldConfig = world.getConfig();

                            // If this is false, then it will be false for all players as well.
                            boolean updatesEnabled = worldConfig.getBool(DhsConfig.REAL_TIME_UPDATES_ENABLED);

                            if (!updatesEnabled) {
                                return;
                            }

                            String levelKeyPrefix = worldConfig.getString(DhsConfig.LEVEL_KEY_PREFIX);
                            String levelKey = world.getName();

                            if (levelKeyPrefix != null) {
                                levelKey = levelKeyPrefix + levelKey;
                            }

                            int lodChunkX = Coordinates.sectionToChunk(lodModelToDelete.getX());
                            int lodChunkZ = Coordinates.sectionToChunk(lodModelToDelete.getZ());

                            // TODO: Don't use Bukkit classes.
                            for (Player player : Bukkit.getWorld(newLodModel.getWorldId()).getPlayers()) {
                                Configuration playerConfig = this.getPlayerConfiguration(player.getUniqueId());

                                // No config for this player? Probably not using DH.
                                if (playerConfig == null) {
                                    continue;
                                }

                                int updatesRadius = playerConfig.getInt(DhsConfig.REAL_TIME_UPDATE_RADIUS);

                                int playerChunkX = Coordinates.blockToChunk(player.getLocation().getBlockX());
                                int playerChunkZ = Coordinates.blockToChunk(player.getLocation().getBlockZ());

                                int distanceX = Math.abs(Math.max(lodChunkX, playerChunkX) - Math.min(lodChunkX, playerChunkX));
                                int distanceZ = Math.abs(Math.max(lodChunkZ, playerChunkZ) - Math.min(lodChunkZ, playerChunkZ));

                                // Update outside of player's range?
                                if (distanceX > updatesRadius || distanceZ > updatesRadius) {
                                    continue;
                                }

                                int myBufferId = playerConfig.getInt("buffer-id", 0) + 1;
                                playerConfig.set("buffer-id", myBufferId);

                                FullDataPartialUpdateMessage partialUpdateMessage = new FullDataPartialUpdateMessage();
                                partialUpdateMessage.setLevelKey(levelKey);
                                partialUpdateMessage.setBufferId(myBufferId);
                                partialUpdateMessage.setBeacons(newLodModel.getBeacons());

                                byte[] data = newLodModel.getData();

                                int chunkCount = (int) Math.ceil((double) data.length / LodHandler.CHUNK_SIZE);

                                for (int chunkNo = 0; chunkNo < chunkCount; chunkNo++) {
                                    FullDataChunkMessage chunkResponse = new FullDataChunkMessage();
                                    chunkResponse.setBufferId(myBufferId);
                                    chunkResponse.setIsFirst(chunkNo == 0);
                                    chunkResponse.setData(Arrays.copyOfRange(
                                            data,
                                            LodHandler.CHUNK_SIZE * chunkNo,
                                            Math.min(LodHandler.CHUNK_SIZE * chunkNo + LodHandler.CHUNK_SIZE, data.length)
                                    ));

                                    this.pluginMessageHandler.sendPluginMessage(player.getUniqueId(), chunkResponse);
                                }

                                this.pluginMessageHandler.sendPluginMessage(player.getUniqueId(), partialUpdateMessage);
                            }
                        });

                    this.touchedLods.remove(key);
                });
        }
    }
}
