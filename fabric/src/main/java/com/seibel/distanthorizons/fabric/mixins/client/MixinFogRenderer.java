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

package com.seibel.distanthorizons.fabric.mixins.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Camera;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

#if MC_VER < MC_1_17_1
import net.minecraft.world.level.material.FluidState;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
#elif MC_VER < MC_1_21_3
import net.minecraft.world.level.material.FogType;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
#elif MC_VER < MC_1_21_6
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.client.renderer.FogParameters;
import org.joml.Vector4f;
#else
import net.minecraft.world.level.material.FogType;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Vector4f;
#endif

@Mixin(FogRenderer.class)
public class MixinFogRenderer
{
	// Using this instead of Float.MAX_VALUE because Sodium don't like it.
	@Unique
	private static final float A_REALLY_REALLY_BIG_VALUE = 420694206942069.F;
	@Unique
	private static final float A_EVEN_LARGER_VALUE = 42069420694206942069.F;
	
	
	
	#if MC_VER < MC_1_19_2
	@Inject(at = @At("RETURN"), method = "setupFog")
	private static void disableSetupFog(Camera camera, FogMode fogMode, float f, boolean bl, CallbackInfo callback)
	#elif MC_VER < MC_1_21_3
	@Inject(at = @At("RETURN"), method = "setupFog")
	private static void disableSetupFog(Camera camera, FogMode fogMode, float f, boolean bl, float g, CallbackInfo callback)
	#elif MC_VER < MC_1_21_6
	@Inject(at = @At("RETURN"), method = "setupFog", cancellable = true)
	private static void disableSetupFog(Camera camera, FogMode fogMode, Vector4f vector4f, float f, boolean bl, float g, CallbackInfoReturnable<FogParameters> callback)
	#else
	@ModifyReturnValue(at = @At("RETURN"), method = "computeFogColor")
	private static Vector4f disableSetupFog(Vector4f fogReturnValue, Camera camera, float f, ClientLevel clientLevel, int i, float g, boolean bl)
	#endif
	{
		#if MC_VER < MC_1_21_6
		boolean cancelFog = cancelFog(camera, fogMode);
		#else
		boolean cancelFog = cancelFog(camera);
		#endif
		
		if (cancelFog)
		{
			#if MC_VER < MC_1_17_1
			RenderSystem.fogStart(A_REALLY_REALLY_BIG_VALUE);
			RenderSystem.fogEnd(A_EVEN_LARGER_VALUE);
			#elif MC_VER < MC_1_21_3
			RenderSystem.setShaderFogStart(A_REALLY_REALLY_BIG_VALUE);
			RenderSystem.setShaderFogEnd(A_EVEN_LARGER_VALUE);
			#elif MC_VER < MC_1_21_6
			callback.setReturnValue(FogParameters.NO_FOG);
			#else
			// set the fog color to invisible
			fogReturnValue.x = 0;
			fogReturnValue.y = 0;
			fogReturnValue.z = 0;
			fogReturnValue.w = 0;
			#endif
		}
		
		#if MC_VER < MC_1_21_6
		// no return value needed
		#else
		return fogReturnValue;
		#endif
	}
	
	
	
	@Unique
	#if MC_VER < MC_1_21_6
	private static boolean cancelFog(Camera camera, FogMode fogMode)
	#else
	private static boolean cancelFog(Camera camera)
	#endif
	{
		boolean cameraNotInFluid = cameraNotInFluid(camera);
		
		Entity entity = camera.getEntity();
		boolean isSpecialFog = (entity instanceof LivingEntity) && ((LivingEntity) entity).hasEffect(MobEffects.BLINDNESS);
		
		boolean cancelFog = !isSpecialFog;
		cancelFog = cancelFog && cameraNotInFluid;
		#if MC_VER < MC_1_21_6
		cancelFog = cancelFog && (fogMode == FogMode.FOG_TERRAIN);
		#endif
		cancelFog = cancelFog && !SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class).isFogStateSpecial();
		cancelFog = cancelFog && !Config.Client.Advanced.Graphics.Fog.enableVanillaFog.get();
		
		return cancelFog;
	}
	
	@Unique
	private static boolean cameraNotInFluid(Camera camera)
	{
		#if MC_VER < MC_1_17_1
		FluidState fluidState = camera.getFluidInCamera();
		boolean cameraNotInFluid = fluidState.isEmpty();
		#else
		FogType fogTypes = camera.getFluidInCamera();
		boolean cameraNotInFluid = fogTypes == FogType.NONE;
		#endif
		
		return cameraNotInFluid;
	}
	
	
	
}
