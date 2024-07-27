/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.common.wrappers.block.cache;

import com.seibel.distanthorizons.common.wrappers.block.BiomeWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.pos.DhBlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ConcurrentHashMap;

public class ClientBlockDetailMap
{
	private final ConcurrentHashMap<BlockState, ClientBlockStateCache> blockCache = new ConcurrentHashMap<>();
	private final ClientLevelWrapper level;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public ClientBlockDetailMap(ClientLevelWrapper level) { this.level = level; }
	
	
	
	//=========//
	// methods //
	//=========//
	
	public ClientBlockStateCache getBlockStateData(BlockState blockState)
	{ return this.blockCache.computeIfAbsent(blockState, (block) -> new ClientBlockStateCache(block, this.level)); }
	
	public int getColor(BlockState state, BiomeWrapper biome, DhBlockPos pos)
	{ return this.getBlockStateData(state).getAndResolveFaceColor(biome, pos); }
	
	public void clear() { this.blockCache.clear(); }
	
	
}
