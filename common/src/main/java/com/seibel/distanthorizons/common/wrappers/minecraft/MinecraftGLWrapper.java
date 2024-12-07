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

package com.seibel.distanthorizons.common.wrappers.minecraft;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.seibel.distanthorizons.common.wrappers.WrapperFactory;
import com.seibel.distanthorizons.common.wrappers.misc.LightMapWrapper;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.util.ColorUtil;
import com.seibel.distanthorizons.core.util.math.Vec3d;
import com.seibel.distanthorizons.core.util.math.Vec3f;
import com.seibel.distanthorizons.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftGLWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.ILightMapWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.AbstractOptifineAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IOptifineAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.Logger;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.NativeType;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A singleton that contains everything
 * related to rendering in Minecraft.
 *
 * @author James Seibel
 * @version 12-12-2021
 */
//@Environment(EnvType.CLIENT)
public class MinecraftGLWrapper implements IMinecraftGLWrapper
{
	public static final MinecraftGLWrapper INSTANCE = new MinecraftGLWrapper();
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	
	
	/*
    private static final StencilState STENCIL;
	 */
	
	
	// scissor //
	
	/** @see GL32#GL_SCISSOR_TEST */
	@Override
	public void enableScissorTest() { GlStateManager._enableScissorTest(); }
	/** @see GL32#GL_SCISSOR_TEST */
	@Override
	public void disableScissorTest() { GlStateManager._disableScissorTest(); }
	
	
	// stencil //
//	
//	/** @see GL32#GL_SCISSOR_TEST */
//	public void enableScissorTest() { GlStateManager._stencilFunc(); }
//	/** @see GL32#GL_SCISSOR_TEST */
//	public void disableScissorTest() { GlStateManager._disableScissorTest(); }
	
	
	// depth //
	
	/** @see GL32#GL_DEPTH_TEST */
	@Override
	public void enableDepthTest() { GlStateManager._enableDepthTest(); }
	/** @see GL32#GL_DEPTH_TEST */
	@Override
	public void disableDepthTest() { GlStateManager._disableDepthTest(); }
	
	/** @see GL32#glDepthFunc(int)  */
	@Override
	public void glDepthFunc(int func) { GlStateManager._depthFunc(func); }
	
	/** @see GL32#glDepthMask(boolean) */
	@Override
	public void enableDepthMask() { GlStateManager._depthMask(true); }
	/** @see GL32#glDepthMask(boolean) */
	@Override
	public void disableDepthMask() { GlStateManager._depthMask(false); }
	
	
	// blending //
	
	/** @see GL32#GL_BLEND */
	@Override
	public void enableBlend() { GlStateManager._enableBlend(); }
	/** @see GL32#GL_BLEND */
	@Override
	public void disableBlend() { GlStateManager._disableBlend(); }
	
	/** @see GL32#glBlendFunc */
	@Override
	public void glBlendFunc(int sfactor, int dfactor) { GlStateManager._blendFunc(sfactor, dfactor); }
	/** @see GL32#glBlendFuncSeparate */
	@Override
	public void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) 
	{ GlStateManager._blendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha); }
	
	
	// frame buffers //
	
	/** @see GL32#glBindFramebuffer */
	@Override
	public void glBindFramebuffer(int target, int framebuffer) 
	{ GlStateManager._glBindFramebuffer(target, framebuffer); }
	
	
	// buffers //
	
	/** @see GL32#glGenBuffers() */
	@Override
	public int glGenBuffers()
	{ return GlStateManager._glGenBuffers(); }
	
	/** @see GL32#glDeleteBuffers(int)  */
	@Override
	public void glDeleteBuffers(int buffer)
	{ GlStateManager._glDeleteBuffers(buffer); }
	
	
	// culling //
	
	/** @see GL32#GL_CULL_FACE */
	@Override
	public void enableFaceCulling() { GlStateManager._enableCull(); }
	/** @see GL32#GL_CULL_FACE */
	@Override
	public void disableFaceCulling() { GlStateManager._disableCull(); }
	
	
	// textures //
	
	/** @see GL32#glGenTextures() */
	@Override
	public int glGenTextures() { return GlStateManager._genTexture(); }
	/** @see GL32#glDeleteTextures(int) */
	@Override
	public void glDeleteTextures(int texture) { GlStateManager._deleteTexture(texture); }
	
	/** @see GL32#glActiveTexture(int) */
	@Override
	public void glActiveTexture(int textureId) { GlStateManager._activeTexture(textureId); }
	/** only works for textures bound via this system or MC's {@link GlStateManager} */
	@Override
	public int getActiveTexture() { return GlStateManager._getActiveTexture(); }
	
	/**
	 * Always binds to {@link GL32#GL_TEXTURE_2D}
	 * @see GL32#glBindTexture(int, int)
	 */
	@Override
	public void glBindTexture(int texture) { GlStateManager._bindTexture(texture); }
	
	
	
	
}
