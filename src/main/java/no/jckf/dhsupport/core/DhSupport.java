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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class DhSupport implements Configurable
{
    protected String pluginVersion;

    protected String dataDirectory;

    protected Database database;

    protected AsyncLodRepository lodRepository;

    protected Configuration configuration;

    protected Logger logger;

    protected Scheduler scheduler;

    protected UpdateChecker updateChecker;

    @Nullable
    protected CompletableFuture<?> pause;

    protected PerformanceTracker generationTracker = new PerformanceTracker();

    protected Map<UUID, WorldInterface> worldInterfaces = new HashMap<>();

    protected PluginMessageHandler pluginMessageHandler;

    protected PluginMessageSender pluginMessageSender;

    protected Map<String, CompletableFuture<Lod>> queuedBuilders = new ConcurrentHashMap<>();

    protected Map<String, LodModel> touchedLods = new ConcurrentHashMap<>();

    protected Map<UUID, Configuration> playerConfigurations = new HashMap<>();

    protected Map<UUID, PreGenerator> preGenerators = new HashMap<>();

    public DhSupport(String pluginVersion)
    {
        this.pluginVersion = pluginVersion;

        this.configuration = new Configuration();

        this.database = new Database();
        this.lodRepository = new AsyncLodRepository(this.database);

        this.pluginMessageHandler = new PluginMessageHandler(this);

        this.updateChecker = new UpdateChecker(62013887);
    }

    public void onEnable()
    {
        this.lodRepository.setLogger(this.getLogger());

        try {
            this.database.open(
                this.getConfig().getString(DhsConfig.DATABASE_PATH)
                    .replace("{datadir}", this.getDataDirectory())
            );

            this.database.addMigration(CreateLodsTable.class);

            this.database.migrate();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to initialize database!", exception);
        }

        (new PlayerConfigHandler(this, this.pluginMessageHandler)).register();
        (new LodHandler(this, this.pluginMessageHandler)).register();

        this.pluginMessageHandler.onEnable();

        if (this.getConfig().getBool(DhsConfig.GENERATE_NEW_CHUNKS, true) && this.getConfig().getBool(DhsConfig.GENERATE_NEW_CHUNKS_WARNING, true)) {
            this.warning("Chunk generation is enabled. New chunks will be generated as needed to complete LOD generation. This could significantly increase the size of your world.");
            this.warning("If you understand what this means and would like to disable this warning, set " + DhsConfig.GENERATE_NEW_CHUNKS_WARNING + " to false in your config.");
        }

        if (this.getConfig().getBool(DhsConfig.CHECK_FOR_UPDATES, true)) {
            this.getScheduler().runOnSeparateThread(() -> {
                this.checkUpdates();
                return null;
            });
        }
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

    public boolean pause()
    {
        if (this.pause != null) {
            return false;
        }

        this.pause = new CompletableFuture<>();

        return true;
    }

    public boolean unpause()
    {
        if (this.pause == null) {
            return false;
        }

        this.pause.complete(null);
        this.pause = null;

        return true;
    }

    public boolean isPaused()
    {
        return this.pause != null;
    }

    public void joinPauseState()
    {
        if (this.pause != null) {
            this.pause.join();
        }
    }

    public PerformanceTracker getGenerationTracker()
    {
        return this.generationTracker;
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

    public void debug(String message)
    {
        //this.getLogger().info("[DEBUG] " + message);
    }

    public void checkUpdates()
    {
        if (this.updateChecker.isLatestVersion(this.pluginVersion)) {
            return;
        }

        this.warning("A newer version of the plugin is available.");
    }

    public Map<UUID, Configuration> getPlayerConfigurations()
    {
        return this.playerConfigurations;
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
        return this.getLodRepository()
            .loadLodAsync(worldId, position.getX(), position.getZ())
            .thenComposeAsync((modelFromDb) -> {
                // If an LOD was found in the database, return it.
                if (modelFromDb != null) {
                    return CompletableFuture.completedFuture(modelFromDb);
                }

                // Otherwise generate a new one.
                return this.generateLod(worldId, position);
            });
    }

    protected CompletableFuture<LodModel> generateLod(UUID worldId, SectionPosition position)
    {
        this.joinPauseState();

        int worldX = Coordinates.sectionToBlock(position.getX());
        int worldZ = Coordinates.sectionToBlock(position.getZ());

        WorldInterface world = this.getWorldInterface(worldId).newInstance();

        boolean generateNewChunks = world.getConfig().getBool(DhsConfig.GENERATE_NEW_CHUNKS, true);

        Map<String, CompletableFuture<Boolean>> loads = new HashMap<>();

        // Load all the chunks we need for this request.
        for (int xMultiplier = 0; xMultiplier < 4; xMultiplier++) {
            for (int zMultiplier = 0; zMultiplier < 4; zMultiplier++) {
                int chunkX = worldX + 16 * xMultiplier;
                int chunkZ = worldZ + 16 * zMultiplier;

                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    if (generateNewChunks) {
                        loads.put(worldX + "x" + worldZ, world.loadOrGenerateChunkAsync(chunkX, chunkZ));
                    } else {
                        loads.put(worldX + "x" + worldZ, world.loadChunkAsync(chunkX, chunkZ));
                    }
                }
            }
        }

        // Wait for chunk loads, then...
        return CompletableFuture.allOf(loads.values().toArray(new CompletableFuture[0]))
            .thenComposeAsync((asd) -> {
                boolean loadRejected = loads
                    .values()
                    .stream()
                    .map(loadRequest -> {
                        try {
                            return loadRequest.get();
                        } catch (InterruptedException | ExecutionException exception) {
                            return false;
                        }
                    })
                    .anyMatch(Predicate.isEqual(false));

                if (loadRejected) {
                    // Discard the chunks we loaded.
                    this.getScheduler().runOnRegionThread(worldId, worldX, worldZ, () -> {
                        for (String key : loads.keySet()) {
                            String[] xz = key.split("x", 2);

                            world.discardChunk(
                                Integer.parseInt(xz[0]),
                                Integer.parseInt(xz[1])
                            );
                        }

                        return null;
                    });

                    return CompletableFuture.completedFuture(null);
                }

                // No LOD was found. Start building a new one.
                CompletableFuture<Lod> lodFuture = this.queueBuilder(worldId, position, this.getBuilder(world, position));

                // Combine the LOD and beacons and save the result in the database.
                return lodFuture.thenApply((lod) -> {
                    // Discard the chunks we loaded.
                    this.getScheduler().runOnRegionThread(worldId, worldX, worldZ, () -> {
                        for (String key : loads.keySet()) {
                            String[] xz = key.split("x", 2);

                            world.discardChunk(
                                Integer.parseInt(xz[0]),
                                Integer.parseInt(xz[1])
                            );
                        }

                        return null;
                    });

                    Encoder lodEncoder = new Encoder();
                    lod.encode(lodEncoder);

                    Encoder beaconEncoder = new Encoder();
                    beaconEncoder.writeCollection(lod.getBeacons());

                    this.generationTracker.ping();

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
        if (this.isPaused()) {
            return;
        }

        for (String key : this.touchedLods.keySet()) {
            LodModel lodModelToDelete = this.touchedLods.get(key);
            this.touchedLods.remove(key);

            WorldInterface world = this.getWorldInterface(lodModelToDelete.getWorldId());

            this.debug("Changes detected in " + world.getName() + " " + lodModelToDelete.getX() + "x" + lodModelToDelete.getZ() + ".");

            this.getLodRepository().lodExistsAsync(lodModelToDelete.getWorldId(), lodModelToDelete.getX(), lodModelToDelete.getZ()).thenAccept((exists) -> {
                if (!exists) {
                    return;
                }

                this.getLodRepository().deleteLodAsync(lodModelToDelete.getWorldId(), lodModelToDelete.getX(), lodModelToDelete.getZ())
                    .thenAccept((deleted) -> {
                        if (!deleted) {
                            this.warning("Could not delete LOD " + world.getName() + " " + lodModelToDelete.getX() + "x" + lodModelToDelete.getZ() + ".");
                            return;
                        }

                        SectionPosition position = new SectionPosition();
                        position.setDetailLevel(6);
                        position.setX(lodModelToDelete.getX());
                        position.setZ(lodModelToDelete.getZ());

                        this.getLod(lodModelToDelete.getWorldId(), position)
                            .thenAcceptAsync((newLodModel) -> {
                                Configuration worldConfig = world.getConfig();

                                // If this is false, then it will be false for all players as well.
                                boolean updatesEnabled = worldConfig.getBool(DhsConfig.REAL_TIME_UPDATES_ENABLED);

                                if (!updatesEnabled) {
                                    this.debug("New LOD " + world.getName() + " " + lodModelToDelete.getX() + "x" + lodModelToDelete.getZ() + " generated, but real-time updates are disabled.");
                                    return;
                                }

                                String levelKeyPrefix = worldConfig.getString(DhsConfig.LEVEL_KEY_PREFIX);
                                String levelKey = world.getKey();

                                if (levelKeyPrefix != null) {
                                    levelKey = levelKeyPrefix + levelKey;
                                }

                                int lodChunkX = Coordinates.sectionToChunk(lodModelToDelete.getX());
                                int lodChunkZ = Coordinates.sectionToChunk(lodModelToDelete.getZ());

                                int playersInRangeCount = 0;
                                int playersOutOfRangeCount = 0;
                                int playersWithoutDhCount = 0;

                                // TODO: Don't use Bukkit classes.
                                for (Player player : Bukkit.getWorld(newLodModel.getWorldId()).getPlayers()) {
                                    Configuration playerConfig = this.getPlayerConfiguration(player.getUniqueId());

                                    // No config for this player? Probably not using DH.
                                    if (playerConfig == null) {
                                        playersWithoutDhCount++;
                                        continue;
                                    }

                                    if (!playerConfig.getBool(DhsConfig.DISTANT_GENERATION_ENABLED) || !playerConfig.getBool(DhsConfig.REAL_TIME_UPDATES_ENABLED)) {
                                        continue;
                                    }

                                    int updatesRadius = playerConfig.getInt(DhsConfig.REAL_TIME_UPDATE_RADIUS);

                                    int playerChunkX = Coordinates.blockToChunk(player.getLocation().getBlockX());
                                    int playerChunkZ = Coordinates.blockToChunk(player.getLocation().getBlockZ());

                                    int distanceX = Math.abs(Math.max(lodChunkX, playerChunkX) - Math.min(lodChunkX, playerChunkX));
                                    int distanceZ = Math.abs(Math.max(lodChunkZ, playerChunkZ) - Math.min(lodChunkZ, playerChunkZ));

                                    // Update outside of player's range?
                                    if (distanceX > updatesRadius || distanceZ > updatesRadius) {
                                        playersOutOfRangeCount++;
                                        continue;
                                    }

                                    playersInRangeCount++;

                                    int myBufferId = playerConfig.increment("buffer-id");

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

                                this.debug("Updated LOD " + world.getName() + " " + lodModelToDelete.getX() + "x" + lodModelToDelete.getZ() + " sent to " + playersInRangeCount + " players. Found " + playersOutOfRangeCount + " players out of range, and " + playersWithoutDhCount + " players without DH.");
                            });
                    });
            });
        }
    }

    public boolean isPreGenerating(WorldInterface world)
    {
        return this.preGenerators.containsKey(world.getId()) && this.getPreGenerator(world).isRunning();
    }

    public void preGenerate(WorldInterface world, int centerX, int centerZ, int radius, boolean force)
    {
        if (this.isPreGenerating(world)) {
            this.info("Cannot run multiple pre-generators in the same world. Stopping current task for " + world.getName() + "...");

            this.stopPreGenerator(world);
        }

        this.preGenerators.put(world.getId(), new PreGenerator(this, world, centerX, centerZ, radius, force));

        this.getScheduler().runOnSeparateThread(() -> {
            this.preGenerators.get(world.getId()).run();

            return null;
        });
    }

    public @Nullable PreGenerator getPreGenerator(WorldInterface world)
    {
        return this.preGenerators.get(world.getId());
    }

    public void stopPreGenerator(WorldInterface world)
    {
        if (this.isPreGenerating(world)) {
            this.getPreGenerator(world).stop();

            this.preGenerators.remove(world.getId());
        }
    }

    public CompletableFuture<Integer> trim(WorldInterface world, int centerX, int centerZ, int radius)
    {
        return this.getLodRepository().trimLodsAsync(
            world.getId(),
            Coordinates.blockToSection(centerX - radius),
            Coordinates.blockToSection(centerZ - radius),
            Coordinates.blockToSection(centerX + radius),
            Coordinates.blockToSection(centerZ + radius)
        );
    }
}
