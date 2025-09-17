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

#if MC_VER < MC_1_19_4
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import org.lwjgl.opengl.GL32;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
#elif MC_VER < MC_1_21_6
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
#else
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
#endif


import com.seibel.distanthorizons.common.wrappers.McObjectConverter;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.fabric.FabricClientProxy;
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import net.minecraft.client.Minecraft;
import com.seibel.distanthorizons.core.config.Config;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.apache.logging.log4j.Logger;


@Mixin(LevelRenderer.class)
public class MixinLevelRenderer
{
    @Shadow
    private ClientLevel level;
	
	@Unique
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	
	#if MC_VER < MC_1_17_1
    @Inject(at = @At("HEAD"),
			method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDD)V",
			cancellable = true)
	private void renderChunkLayer(RenderType renderType, PoseStack matrixStackIn, double xIn, double yIn, double zIn, CallbackInfo callback)
	#elif MC_VER < MC_1_19_4
    @Inject(at = @At("HEAD"),
            method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLcom/mojang/math/Matrix4f;)V",
            cancellable = true)
    private void renderChunkLayer(RenderType renderType, PoseStack modelViewMatrixStack, double cameraXBlockPos, double cameraYBlockPos, double cameraZBlockPos, Matrix4f projectionMatrix, CallbackInfo callback)
	#elif MC_VER < MC_1_20_2
    @Inject(at = @At("HEAD"),
            method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V",
            cancellable = true)
    private void renderChunkLayer(RenderType renderType, PoseStack modelViewMatrixStack, double cameraXBlockPos, double cameraYBlockPos, double cameraZBlockPos, Matrix4f projectionMatrix, CallbackInfo callback)
    #elif MC_VER < MC_1_20_6
	@Inject(at = @At("HEAD"),
			method = "Lnet/minecraft/client/renderer/LevelRenderer;renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V",
			cancellable = true)
	private void renderChunkLayer(RenderType renderType, PoseStack modelViewMatrixStack, double camX, double camY, double camZ, Matrix4f projectionMatrix, CallbackInfo callback)
	#elif MC_VER < MC_1_21_6
	@Inject(at = @At("HEAD"),
			method = "Lnet/minecraft/client/renderer/LevelRenderer;renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
			cancellable = true)
	private void renderChunkLayer(RenderType renderType, double x, double y, double z, Matrix4f projectionMatrix, Matrix4f frustumMatrix, CallbackInfo callback)
	#else
	@Inject(at = @At("HEAD"), method = "prepareChunkRenders", cancellable = true)
	private void prepareChunkRenders(Matrix4fc projectionMatrix, double d, double e, double f, CallbackInfoReturnable<ChunkSectionsToRender> callback)
    #endif
    {
		#if MC_VER == MC_1_16_5
	    // get the matrices from the OpenGL fixed pipeline
	    float[] mcProjMatrixRaw = new float[16];
	    GL32.glGetFloatv(GL32.GL_PROJECTION_MATRIX, mcProjMatrixRaw);
	    ClientApi.RENDER_STATE.mcProjectionMatrix = new Mat4f(mcProjMatrixRaw);
	    ClientApi.RENDER_STATE.mcProjectionMatrix.transpose();
	    
	    ClientApi.RENDER_STATE.mcModelViewMatrix = McObjectConverter.Convert(matrixStackIn.last().pose());
		
		#elif MC_VER <= MC_1_20_4
		// get the matrices directly from MC
		ClientApi.RENDER_STATE.mcModelViewMatrix = McObjectConverter.Convert(modelViewMatrixStack.last().pose());
		ClientApi.RENDER_STATE.mcProjectionMatrix = McObjectConverter.Convert(projectionMatrix);
		#else
	    // MC combined the model view and projection matricies
	    ClientApi.RENDER_STATE.mcModelViewMatrix = McObjectConverter.Convert(projectionMatrix);
	    ClientApi.RENDER_STATE.mcProjectionMatrix = new Mat4f();
	    ClientApi.RENDER_STATE.mcProjectionMatrix.setIdentity();
		#endif
	    
		// TODO move this into a common place
		#if MC_VER < MC_1_21_1
		ClientApi.RENDER_STATE.frameTime = Minecraft.getInstance().getFrameTime();
		#elif MC_VER < MC_1_21_3
		ClientApi.RENDER_STATE.frameTime = Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
		#else
	    ClientApi.RENDER_STATE.frameTime = Minecraft.getInstance().deltaTracker.getRealtimeDeltaTicks();
		#endif
	    
	    
	    //LOGGER.info("\n\n" +
		//	    "Level Mixin\n" +
		//	    "Mc MVM: \n" + mcModelViewMatrix.toString() + "\n" +
		//	    "Mc Proj: \n" + mcProjectionMatrix.toString()
	    //);
	    
	    
	    #if MC_VER < MC_1_21_6
	    if (renderType.equals(RenderType.translucent())) 
		{
		    ClientApi.INSTANCE.renderDeferredLodsForShaders(ClientLevelWrapper.getWrapper(this.level),
				    ClientApi.RENDER_STATE.mcModelViewMatrix,
				    ClientApi.RENDER_STATE.mcProjectionMatrix,
				    ClientApi.RENDER_STATE.frameTime
				    );
	    }
		#else
		// rendering handled via Fabric Api render event
	    #endif
    }
	
	
	
}
