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

package com.seibel.distanthorizons.fabric.wrappers.modAccessor;

#if MC_VER >= MC_1_19_4

import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor;

#if MC_VER <= MC_1_20_4
import net.coderbot.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
#elif MC_VER < MC_1_21_3
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
#else
// TODO fabric/iris needs fixing for MC 1.21.3
#endif

public class IrisAccessor implements IIrisAccessor
{
	@Override
	public String getModName()
	{
		// TODO
		#if MC_VER < MC_1_21_3
		return Iris.MODID;
		#else
		return "Iris-Fabric";
		#endif
	}
	
	@Override
	public boolean isShaderPackInUse()
	{
		// TODO
		#if MC_VER < MC_1_21_3
		return IrisApi.getInstance().isShaderPackInUse();
		#else
		return false;
		#endif
	}
	
	@Override
	public boolean isRenderingShadowPass()
	{
		// TODO
		#if MC_VER < MC_1_21_3
		return IrisApi.getInstance().isRenderingShadowPass();
		#else
		return false;
		#endif
	}
}

#endif
