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
import no.jckf.dhsupport.core.world.WorldInterface;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
            return false;
        }

        switch (args[0]) {
            case "reload":
                return this.reload(sender);

            case "pregen":
                return this.pregen(sender, Arrays.copyOfRange(args, 1, args.length));
        }

        return false;
    }

    protected boolean reload(CommandSender sender)
    {
        sender.sendMessage("Reloading config...");

        this.plugin.loadDhsConfig();

        sender.sendMessage("Reload complete.");

        return true;
    }

    protected boolean pregen(CommandSender sender, String[] args)
    {
        WorldInterface world;
        Integer centerX;
        Integer centerZ;
        Integer radius;

        if (args.length >= 1) {
            World bukkitWorld = this.plugin.getServer().getWorld(args[0]);

            if (bukkitWorld == null) {
                return false;
            }

            world = this.plugin.getDhSupport().getWorldInterface(bukkitWorld.getUID());
        } else if (sender instanceof Player) {
            world = this.plugin.getDhSupport().getWorldInterface(((Player) sender).getWorld().getUID());
        } else {
            world = null;
        }

        if (world == null) {
            return false;
        }

        if (args.length >= 3) {
            centerX = Integer.parseInt(args[1]);
            centerZ = Integer.parseInt(args[2]);
        } else {
            centerX = world.getWorldBorderX();
            centerZ = world.getWorldBorderZ();
        }

        if (centerX == null || centerZ == null) {
            return false;
        }

        if (args.length >= 4) {
            radius = Integer.parseInt(args[3]);
        } else {
            radius = world.getWorldBorderRadius();
        }

        if (radius == null) {
            return false;
        }

        sender.sendMessage("Generating LODs for view distance of " + radius + " chunks in world " + world.getName() + " starting at center " + centerX + " x " + centerZ + "...");

        this.plugin.getDhSupport().preGenerate(world, centerX, centerZ, radius);

        return true;
    }
}
