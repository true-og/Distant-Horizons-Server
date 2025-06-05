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

package no.jckf.dhsupport.bukkit.commands;

import no.jckf.dhsupport.bukkit.DhSupportBukkitPlugin;
import no.jckf.dhsupport.core.Coordinates;
import no.jckf.dhsupport.core.PreGenerator;
import no.jckf.dhsupport.core.Utils;
import no.jckf.dhsupport.core.world.WorldInterface;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Arrays;

public class DhsCommand implements CommandExecutor
{
    protected DhSupportBukkitPlugin plugin;

    public DhsCommand(DhSupportBukkitPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
    {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Missing sub-command.");
            return true;
        }

        switch (args[0]) {
            case "reload":
                return this.reload(sender);

            case "worlds":
                return this.worlds(sender);

            case "pregen":
                return this.pregen(sender, Arrays.copyOfRange(args, 1, args.length));

            case "pause":
                return this.pause(sender);

            case "unpause":
                return this.unpause(sender);

            case "trim":
                return this.trim(sender, Arrays.copyOfRange(args, 1, args.length));
        }

        sender.sendMessage(ChatColor.RED + "Unknown sub-command.");

        return true;
    }

    protected boolean reload(CommandSender sender)
    {
        sender.sendMessage(ChatColor.YELLOW + "Reloading config...");

        this.plugin.loadDhsConfig();

        sender.sendMessage(ChatColor.GREEN + "Reload complete.");

        return true;
    }

    protected boolean worlds(CommandSender sender)
    {
        sender.sendMessage(ChatColor.GREEN + "World list:");

        for (World bukkitWorld : this.plugin.getServer().getWorlds()) {
            sender.sendMessage(ChatColor.YELLOW + " - " + ChatColor.GREEN + bukkitWorld.getName() + " " + ChatColor.GRAY + "(" + bukkitWorld.getUID() + ")");
        }

        return true;
    }

    protected boolean pregen(CommandSender sender, String[] args)
    {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Missing action.");
            return true;
        }

        switch (args[0]) {
            case "start":
                return this.pregenStart(sender, Arrays.copyOfRange(args, 1, args.length));

            case "stop":
                return this.pregenStop(sender, Arrays.copyOfRange(args, 1, args.length));

            case "status":
                return this.pregenStatus(sender, Arrays.copyOfRange(args, 1, args.length));
        }

        sender.sendMessage(ChatColor.RED + "Unknown action.");

        return true;
    }

    protected boolean pregenStart(CommandSender sender, String[] args)
    {
        WorldInterface world;
        Integer centerX;
        Integer centerZ;
        Integer radius;

        if (args.length >= 1) {
            World bukkitWorld = this.plugin.getWorld(args[0]);

            if (bukkitWorld == null) {
                sender.sendMessage(ChatColor.RED + "Unknown world.");
                return true;
            }

            world = this.plugin.getDhSupport().getWorldInterface(bukkitWorld.getUID());
        } else if (sender instanceof Player) {
            world = this.plugin.getDhSupport().getWorldInterface(((Player) sender).getWorld().getUID());
        } else {
            world = null;
        }

        if (world == null) {
            sender.sendMessage(ChatColor.RED + "No world specified.");
            return true;
        }

        if (args.length >= 3) {
            centerX = Integer.parseInt(args[1]);
            centerZ = Integer.parseInt(args[2]);
        } else {
            centerX = world.getWorldBorderX();
            centerZ = world.getWorldBorderZ();
        }

        if (centerX == null || centerZ == null) {
            sender.sendMessage(ChatColor.RED + "No center coordinates specified.");
            return true;
        }

        if (args.length >= 4) {
            radius = Coordinates.chunkToBlock(Integer.parseInt(args[3]));
        } else {
            radius = world.getWorldBorderRadius();
        }

        if (radius == null) {
            sender.sendMessage(ChatColor.RED + "No radius specified.");
            return true;
        }

        boolean force = args.length >= 5 && args[4].equals("force");

        sender.sendMessage(ChatColor.YELLOW + "Generating LODs for view distance of " + ChatColor.GREEN + Coordinates.blockToChunk(radius) + ChatColor.YELLOW + " chunks in world " + ChatColor.GREEN + world.getName() + ChatColor.YELLOW + " starting at center " + ChatColor.GREEN + centerX + " x " + centerZ + ChatColor.YELLOW + "...");

        if (force) {
            sender.sendMessage(ChatColor.YELLOW + "All existing LODs in this area will be re-generated.");
        }

        this.plugin.getDhSupport().preGenerate(world, centerX, centerZ, radius, force);

        return true;
    }

    protected boolean pregenStop(CommandSender sender, String[] args)
    {
        WorldInterface world;

        if (args.length >= 1) {
            World bukkitWorld = this.plugin.getWorld(args[0]);

            if (bukkitWorld == null) {
                sender.sendMessage(ChatColor.RED + "Unknown world.");
                return true;
            }

            world = this.plugin.getDhSupport().getWorldInterface(bukkitWorld.getUID());
        } else if (sender instanceof Player) {
            world = this.plugin.getDhSupport().getWorldInterface(((Player) sender).getWorld().getUID());
        } else {
            world = null;
        }

        if (world == null) {
            sender.sendMessage(ChatColor.RED + "No world specified.");
            return true;
        }

        if (!this.plugin.getDhSupport().isPreGenerating(world)) {
            sender.sendMessage(ChatColor.RED + "No pre-generator running in world " + ChatColor.YELLOW + world.getName() + ChatColor.RED + ".");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Stopping pre-generation in world " + ChatColor.GREEN + world.getName() + ChatColor.YELLOW + "...");

        this.plugin.getDhSupport().stopPreGenerator(world);

        return true;
    }

    protected boolean pregenStatus(CommandSender sender, String[] args)
    {
        WorldInterface world;

        if (args.length >= 1) {
            World bukkitWorld = this.plugin.getWorld(args[0]);

            if (bukkitWorld == null) {
                sender.sendMessage(ChatColor.RED + "Unknown world.");
                return true;
            }

            world = this.plugin.getDhSupport().getWorldInterface(bukkitWorld.getUID());
        } else if (sender instanceof Player) {
            world = this.plugin.getDhSupport().getWorldInterface(((Player) sender).getWorld().getUID());
        } else {
            world = null;
        }

        if (world == null) {
            sender.sendMessage(ChatColor.RED + "No world specified.");
            return true;
        }

        PreGenerator generator = this.plugin.getDhSupport().getPreGenerator(world);

        if (generator == null) {
            sender.sendMessage(ChatColor.RED + "No pre-generator running in world " + ChatColor.YELLOW + world.getName() + ChatColor.RED + ".");
            return true;
        }

        Duration elapsedTime = generator.getElapsedTime();

        sender.sendMessage(ChatColor.GREEN + "Generation progress: " + ChatColor.YELLOW + String.format("%.2f", generator.getProgress() * 100f) + "%");
        sender.sendMessage(ChatColor.GREEN + "Processed LODs: " + ChatColor.YELLOW + generator.getCompletedRequests() + ChatColor.GREEN + " / " + ChatColor.YELLOW + generator.getTargetRequests() + ChatColor.GREEN + " (" + ChatColor.YELLOW + String.format("%.2f", (generator.getCompletedRequests() * 16F) / (float) elapsedTime.toSeconds()) + ChatColor.GREEN + " CPS)");
        sender.sendMessage(ChatColor.GREEN + "Time elapsed: " + ChatColor.YELLOW + Utils.humanReadableDuration(elapsedTime));

        if (generator.isRunning()) {
            sender.sendMessage(ChatColor.GREEN + "Time remaining: " + ChatColor.YELLOW + Utils.humanReadableDuration(Duration.ofSeconds((long) ((generator.getTargetRequests() - generator.getCompletedRequests()) / this.plugin.getDhSupport().getGenerationPerSecondStat()))));
        } else {
            sender.sendMessage(ChatColor.GREEN + "Generation is complete.");
        }

        return true;
    }

    protected boolean pause(CommandSender sender)
    {
        if (this.plugin.getDhSupport().isPaused()) {
            sender.sendMessage(ChatColor.RED + "Already paused.");
            return true;
        }

        if (!this.plugin.getDhSupport().pause()) {
            sender.sendMessage(ChatColor.RED + "Could not pause. Check server log for error messages.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "DHS has been paused.");

        return true;
    }

    protected boolean unpause(CommandSender sender)
    {
        if (!this.plugin.getDhSupport().isPaused()) {
            sender.sendMessage(ChatColor.RED + "Not paused.");
            return true;
        }

        if (!this.plugin.getDhSupport().unpause()) {
            sender.sendMessage(ChatColor.RED + "Could not unpause. Check server log for error messages.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "DHS has been unpaused.");

        return true;
    }

    protected boolean trim(CommandSender sender, String[] args)
    {
        WorldInterface world;
        Integer centerX;
        Integer centerZ;
        Integer radius;

        if (args.length >= 1) {
            World bukkitWorld = this.plugin.getWorld(args[0]);

            if (bukkitWorld == null) {
                sender.sendMessage(ChatColor.RED + "Unknown world.");
                return true;
            }

            world = this.plugin.getDhSupport().getWorldInterface(bukkitWorld.getUID());
        } else if (sender instanceof Player) {
            world = this.plugin.getDhSupport().getWorldInterface(((Player) sender).getWorld().getUID());
        } else {
            world = null;
        }

        if (world == null) {
            sender.sendMessage(ChatColor.RED + "No world specified.");
            return true;
        }

        if (args.length >= 3) {
            centerX = Integer.parseInt(args[1]);
            centerZ = Integer.parseInt(args[2]);
        } else {
            centerX = world.getWorldBorderX();
            centerZ = world.getWorldBorderZ();
        }

        if (centerX == null || centerZ == null) {
            sender.sendMessage(ChatColor.RED + "No center coordinates specified.");
            return true;
        }

        if (args.length >= 4) {
            radius = Integer.parseInt(args[3]);
        } else {
            radius = world.getWorldBorderRadius();
        }

        if (radius == null) {
            sender.sendMessage(ChatColor.RED + "No radius specified.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Trimming LODs outside view distance of " + ChatColor.GREEN + radius + ChatColor.YELLOW + " blocks in world " + ChatColor.GREEN + world.getName() + ChatColor.YELLOW + " centered at " + ChatColor.GREEN + centerX + " x " + centerZ + ChatColor.YELLOW + "...");

        this.plugin.getDhSupport().trim(world, centerX, centerZ, radius)
            .thenAccept((trimmedCount) -> {
                sender.sendMessage(ChatColor.GREEN + "Trimming of " + ChatColor.YELLOW + trimmedCount + ChatColor.GREEN + " LODs in " + ChatColor.YELLOW + world.getName() + ChatColor.GREEN + " completed.");
            });

        return true;
    }
}
