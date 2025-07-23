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

import com.tcoded.folialib.FoliaLib;
import no.jckf.dhsupport.core.Utils;
import no.jckf.dhsupport.core.configuration.DhsConfig;
import no.jckf.dhsupport.core.scheduling.Scheduler;
import org.bukkit.Location;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class BukkitScheduler implements Scheduler
{
    protected DhSupportBukkitPlugin plugin;

    protected FoliaLib foliaLib;

    protected ExecutorService executor;

    public BukkitScheduler(DhSupportBukkitPlugin plugin)
    {
        this.plugin = plugin;

        this.foliaLib = new FoliaLib(this.plugin);

        int threadCount = this.plugin.getDhSupport().getConfig().getInt(DhsConfig.SCHEDULER_THREADS);

        this.executor = new ThreadPoolExecutor(
            threadCount, threadCount,
            60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
        );

        ((ThreadPoolExecutor) this.executor).allowCoreThreadTimeOut(true);

        this.plugin.getDhSupport().info("Using " + Utils.ucFirst(this.foliaLib.getImplType().name().toLowerCase().replace('_', ' ')) + " scheduler.");
    }

    @Override
    public boolean canReadWorldAsync()
    {
        return !this.foliaLib.isFolia();
    }

    @Override
    public <U> CompletableFuture<U> runOnMainThread(Supplier<U> supplier)
    {
        CompletableFuture<U> future = new CompletableFuture<>();

        if (this.foliaLib.getScheduler().isGlobalTickThread()) {
            future.complete(supplier.get());
        } else {
            this.foliaLib.getScheduler().runNextTick((task) -> {
                try {
                    future.complete(supplier.get());
                } catch (Exception exception) {
                    future.completeExceptionally(exception);
                }
            });
        }

        return future;
    }

    @Override
    public <U> CompletableFuture<U> runOnRegionThread(UUID worldId, int x, int z, Supplier<U> supplier)
    {
        CompletableFuture<U> future = new CompletableFuture<>();

        Location location = new Location(
            this.plugin.getServer().getWorld(worldId),
            x,
            0,
            z
        );

        if (this.foliaLib.getScheduler().isOwnedByCurrentRegion(location)) {
            future.complete(supplier.get());
        } else {
            this.foliaLib.getScheduler().runAtLocation(
                    location,
                    (task) -> {
                        try {
                            future.complete(supplier.get());
                        } catch (Exception exception) {
                            future.completeExceptionally(exception);
                        }
                    }
            );
        }

        return future;
    }

    @Override
    public <U> CompletableFuture<U> runOnSeparateThread(Supplier<U> supplier)
    {
        CompletableFuture<U> future = new CompletableFuture<>();

        this.executor.execute(() -> {
            try {
                future.complete(supplier.get());
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        });

        return future;
    }

    @Override
    public void cancelTasks()
    {
        this.executor.shutdown();
        this.foliaLib.getScheduler().cancelAllTasks();
    }

    @Override
    public ExecutorService getExecutor()
    {
        return this.executor;
    }

    public void runTimer(Runnable runnable, long initialDelay, long interval)
    {
        this.foliaLib.getScheduler().runTimer(runnable, initialDelay, interval);
    }
}
