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

package no.jckf.dhsupport.bukkit.handler;

import no.jckf.dhsupport.bukkit.BukkitWorldInterface;
import no.jckf.dhsupport.bukkit.DhSupportBukkitPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldHandler implements Listener
{
    protected DhSupportBukkitPlugin plugin;

    public WorldHandler(DhSupportBukkitPlugin plugin)
    {
        this.plugin = plugin;

        this.plugin.getServer().getWorlds().forEach(this::addWorldInterface);
    }

    protected void addWorldInterface(World world)
    {
        BukkitWorldInterface worldInterface = new BukkitWorldInterface(this.plugin, world, this.plugin.getDhSupport().getConfig());

        worldInterface.setLogger(this.plugin.getDhSupport().getLogger());

        worldInterface.doUnsafeThings();

        this.plugin.getDhSupport().setWorldInterface(world.getUID(), worldInterface);
    }

    protected void removeWorldInterface(World world)
    {
        this.plugin.getDhSupport().setWorldInterface(world.getUID(), null);
    }

    protected void touchLod(Location location)
    {
        this.plugin.getDhSupport().touchLod(location.getWorld().getUID(), location.getBlockX(), location.getBlockZ());
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent worldLoad)
    {
        this.addWorldInterface(worldLoad.getWorld());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent worldUnload)
    {
        this.removeWorldInterface(worldUnload.getWorld());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent blockPlace)
    {
        this.touchLod(blockPlace.getBlock().getLocation());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent blockBreak)
    {
        this.touchLod(blockBreak.getBlock().getLocation());
    }

// FIXME messes with 1.16.5 compilation
//    @EventHandler
//    public void onTNTPrime(TNTPrimeEvent blockBreak)
//    {
//        this.touchLod(blockBreak.getBlock().getLocation());
//    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent blockExplode)
    {
        for (var block : blockExplode.blockList()) {
            this.touchLod(block.getLocation());
        }
    }


    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent blockIgnite)
    {
        this.touchLod(blockIgnite.getBlock().getLocation());
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent blockBurn)
    {
        this.touchLod(blockBurn.getBlock().getLocation());
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent leavesDecay)
    {
        this.touchLod(leavesDecay.getBlock().getLocation());
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent blockFade)
    {
        this.touchLod(blockFade.getBlock().getLocation());
    }


    @EventHandler
    public void onBlockGrow(BlockGrowEvent blockGrow)
    {
        this.touchLod(blockGrow.getBlock().getLocation());
    }

    @EventHandler
    public void onMoistureChange(MoistureChangeEvent moistureChange)
    {
        this.touchLod(moistureChange.getBlock().getLocation());
    }


    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent furnaceBurn)
    {
        this.touchLod(furnaceBurn.getBlock().getLocation());
    }


    @EventHandler
    public void onBlockRedstone(BlockRedstoneEvent blockRedstone)
    {
        this.touchLod(blockRedstone.getBlock().getLocation());
    }

    @EventHandler
    public void onBlockPistonExtend(BlockPistonExtendEvent blockPistonExtend)
    {
        for (var block : blockPistonExtend.getBlocks()) {
            this.touchLod(block.getLocation());
        }
    }

    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent blockPistonRetract)
    {
        for (var block : blockPistonRetract.getBlocks()) {
            this.touchLod(block.getLocation());
        }
    }


    @EventHandler
    public void onStructureGrow(StructureGrowEvent structureGrow)
    {
        for (var blockState : structureGrow.getBlocks()) {
            this.touchLod(blockState.getLocation());
        }
    }

    @EventHandler
    public void onSpongeAbsorb(SpongeAbsorbEvent spongeAbsorb)
    {
        for (var blockState : spongeAbsorb.getBlocks()) {
            this.touchLod(blockState.getLocation());
        }
    }

}
