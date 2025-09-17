/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020 James Seibel
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

package com.seibel.distanthorizons.fabric.mixins.server;

import com.seibel.distanthorizons.common.wrappers.DependencySetupDoneCheck;
import com.seibel.distanthorizons.core.util.objects.RunOnThisThreadExecutorService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.Util;

import java.util.concurrent.ExecutorService;

#if MC_VER < MC_1_16_5
#elif MC_VER < MC_1_21_3
import java.util.function.Supplier;
#else
#endif

/**
 * This is needed for DH's world gen so we can run
 * world gen on our own threads instead of using MC thread pools.
 *
 * @see MixinTracingExecutor
 * @see RunOnThisThreadExecutorService
 */
@Mixin(Util.class)
public class MixinUtilBackgroundThread
{
	private static boolean isWorldGenThread()
	{ return DependencySetupDoneCheck.isDone && DependencySetupDoneCheck.getIsCurrentThreadDistantGeneratorThread.get(); }
	
	
	#if MC_VER < MC_1_21_3
	@Inject(method = "backgroundExecutor", at = @At("HEAD"), cancellable = true)
	private static void overrideUtil$backgroundExecutor(CallbackInfoReturnable<ExecutorService> ci)
	{
		if (isWorldGenThread())
		{
			// run this task on the current DH thread instead of a new MC thread
			ci.setReturnValue(new RunOnThisThreadExecutorService());
		}
	}
	#else
	// replaced with TracingExecutor in MC 1.21.3+
	#endif
	
	#if MC_VER < MC_1_17_1
	#elif MC_VER < MC_1_21_3
	@Inject(method = "wrapThreadWithTaskName(Ljava/lang/String;Ljava/lang/Runnable;)Ljava/lang/Runnable;",
			at = @At("HEAD"), cancellable = true)
	private static void overrideUtil$wrapThreadWithTaskName(String string, Runnable r, CallbackInfoReturnable<Runnable> ci)
	{
		if (isWorldGenThread())
		{
			//ApiShared.LOGGER.info("util wrapThreadWithTaskName(Runnable) triggered");
			ci.setReturnValue(r);
		}
	}
	#else
	// replaced with TracingExecutor in MC 1.21.3+
	#endif

	#if MC_VER < MC_1_18_2
	#elif MC_VER < MC_1_21_3
	@Inject(method = "wrapThreadWithTaskName(Ljava/lang/String;Ljava/util/function/Supplier;)Ljava/util/function/Supplier;",
			at = @At("HEAD"), cancellable = true)
	private static void overrideUtil$wrapThreadWithTaskNameForSupplier(String string, Supplier<?> r, CallbackInfoReturnable<Supplier<?>> ci)
	{
		if (isWorldGenThread())
		{
			//ApiShared.LOGGER.info("util wrapThreadWithTaskName(Supplier) triggered");
			ci.setReturnValue(r);
		}
	}
	#else
	// replaced with TracingExecutor in MC 1.21.3+
	#endif
	
}
