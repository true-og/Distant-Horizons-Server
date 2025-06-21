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
import net.minecraft.client.Minecraft;
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
import com.mojang.blaze3d.buffers.Std140Builder;
import net.minecraft.world.level.material.FogType;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
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
	@Inject(at = @At("RETURN"), method = "updateBuffer")
	private static void disableSetupFog(ByteBuffer fogDataBuffer, int bufferStartPos, Vector4f fogColor, float environmentalStart, float environmentalEnd, float renderDistanceStart, float renderDistanceEnd, float skyEnd, float cloudEnd, CallbackInfo callback)
	#endif
	{
		#if MC_VER < MC_1_21_6
		boolean cancelFog = cancelFog(camera, fogMode);
		#elif MC_VER < MC_1_21_6
		boolean cancelFog = cancelFog(camera);
		#else
		// don't try disabling the "Empty fog" buffer, that one is already empty and
		// will be called before the renderer is set up which can cause some weird null-pointer issues
		if (fogColor.x == 0.0f // Red
			&& fogColor.y == 0.0f // Green
			&& fogColor.z == 0.0f // Blue
			&& fogColor.w == 0.0f // Alpha
			)
		{
			return;
		}
			
		boolean cancelFog = cancelFog();
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
			// replace the fog data with our own info
			fogDataBuffer.position(bufferStartPos);
			Std140Builder.intoBuffer(fogDataBuffer)
					.putVec4(fogColor)
					//environmentalStart/End
					.putFloat(A_REALLY_REALLY_BIG_VALUE).putFloat(A_EVEN_LARGER_VALUE)
					//renderDistanceStart/End
					.putFloat(A_REALLY_REALLY_BIG_VALUE).putFloat(A_EVEN_LARGER_VALUE)
					
					// sky and cloud should be unaffected otherwise the distant sky will render incorrectly
					// (IE have a large fog-colored block in the sky)
					.putFloat(skyEnd)
					.putFloat(cloudEnd);
			#endif
		}
		
	}
	
	
	
	@Unique
	#if MC_VER < MC_1_21_6
	private static boolean cancelFog(Camera camera, FogMode fogMode)
	#else
	private static boolean cancelFog()
	#endif
	{
		#if MC_VER < MC_1_21_6
		Entity entity = camera.getEntity();
		#else
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		Entity entity = camera.getEntity();	
		#endif
		
		
		boolean cameraNotInFluid = cameraNotInFluid(camera);
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
