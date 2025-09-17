package com.seibel.distanthorizons.fabric;

import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiLevelLoadEvent;
import com.seibel.distanthorizons.common.AbstractModInitializer;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.misc.ServerPlayerWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.distanthorizons.core.api.internal.ServerApi;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.common.AbstractPluginPacketSender;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IPluginPacketSender;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.fabric.testing.TestWorldGenBindingEvent;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Logger;

#if MC_VER >= MC_1_20_6
import com.seibel.distanthorizons.common.CommonPacketPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
#else
import com.seibel.distanthorizons.core.network.messages.AbstractNetworkMessage;
#endif

import java.util.function.Supplier;

/**
 * This handles all events sent to the server,
 * and is the starting point for most of the mod.
 *
 * @author Ran
 * @author Tomlee
 * @version 5-11-2022
 */
public class FabricServerProxy implements AbstractModInitializer.IEventProxy
{
	private static final ServerApi SERVER_API = ServerApi.INSTANCE;
	@SuppressWarnings("unused")
	private static final AbstractPluginPacketSender PACKET_SENDER = (AbstractPluginPacketSender) SingletonInjector.INSTANCE.get(IPluginPacketSender.class);
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	private final boolean isDedicatedServer;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public FabricServerProxy(boolean isDedicatedServer)
	{
		this.isDedicatedServer = isDedicatedServer;
	}
	
	
	
	// TODO rename
	private boolean isValidTime()
	{
		if (this.isDedicatedServer)
		{
			return true;
		}
		
		//FIXME: This may cause init issue...
		return !(Minecraft.getInstance().screen instanceof TitleScreen);
	}
	
	private IClientLevelWrapper getClientLevelWrapper(ClientLevel level) { return ClientLevelWrapper.getWrapper(level); }
	private ServerLevelWrapper getServerLevelWrapper(ServerLevel level) { return ServerLevelWrapper.getWrapper(level); }
	private ServerPlayerWrapper getServerPlayerWrapper(ServerPlayer player) { return ServerPlayerWrapper.getWrapper(player); }
	
	/** Registers Fabric Events */
	@Override
	public void registerEvents()
	{
		LOGGER.info("Registering Fabric Server Events");
		
		/* Register the mod needed event callbacks */
		
		// ServerTickEvent
		ServerTickEvents.END_SERVER_TICK.register((server) -> SERVER_API.serverTickEvent());
		
		
		// can be enabled to test world gen overrides without having to build a separate API project 
		if (false)
		{
			DhApiEventRegister.on(DhApiLevelLoadEvent.class, new TestWorldGenBindingEvent());
		}
		
		
		// ServerWorldLoadEvent
		//TODO: Check if both of these use the correct timed events. (i.e. is it 'ed' or 'ing' one?)
		ServerLifecycleEvents.SERVER_STARTING.register((server) ->
		{
			if (this.isValidTime())
			{
				ServerApi.INSTANCE.serverLoadEvent(this.isDedicatedServer);
			}
		});
		// ServerWorldUnloadEvent
		ServerLifecycleEvents.SERVER_STOPPED.register((server) ->
		{
			if (this.isValidTime())
			{
				ServerApi.INSTANCE.serverUnloadEvent();
			}
		});
		
		// ServerLevelLoadEvent
		ServerWorldEvents.LOAD.register((server, level) ->
		{
			if (this.isValidTime())
			{
				ServerApi.INSTANCE.serverLevelLoadEvent(this.getServerLevelWrapper(level));
			}
		});
		// ServerLevelUnloadEvent
		ServerWorldEvents.UNLOAD.register((server, level) ->
		{
			if (this.isValidTime())
			{
				ServerApi.INSTANCE.serverLevelUnloadEvent(this.getServerLevelWrapper(level));
			}
		});
		
		// ServerChunkLoadEvent
		ServerChunkEvents.CHUNK_LOAD.register((server, chunk) ->
		{
			ILevelWrapper level = this.getServerLevelWrapper((ServerLevel) chunk.getLevel());
			if (this.isValidTime())
			{
				ServerApi.INSTANCE.serverChunkLoadEvent(
						new ChunkWrapper(chunk, level),
						level);
			}
		});
		// ServerChunkSaveEvent - Done in MixinChunkMap
		
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
		{
			if (this.isValidTime())
			{
				ServerApi.INSTANCE.serverPlayerJoinEvent(this.getServerPlayerWrapper(handler.player));
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
		{
			if (this.isValidTime())
			{
				ServerApi.INSTANCE.serverPlayerDisconnectEvent(this.getServerPlayerWrapper(handler.player));
			}
		});
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, originLevel, destinationLevel) ->
		{
			if (this.isValidTime())
			{
				ServerApi.INSTANCE.serverPlayerLevelChangeEvent(
						this.getServerPlayerWrapper(player),
						this.getServerLevelWrapper(originLevel),
						this.getServerLevelWrapper(destinationLevel)
				);
			}
		});
		
		#if MC_VER >= MC_1_20_6
		PayloadTypeRegistry.playC2S().register(CommonPacketPayload.TYPE, new CommonPacketPayload.Codec());
		if (this.isDedicatedServer)
		{
			PayloadTypeRegistry.playS2C().register(CommonPacketPayload.TYPE, new CommonPacketPayload.Codec());
		}
		
		ServerPlayNetworking.registerGlobalReceiver(CommonPacketPayload.TYPE, (payload, context) ->
		{
			if (payload.message() == null)
			{
				return;
			}
			ServerApi.INSTANCE.pluginMessageReceived(ServerPlayerWrapper.getWrapper(context.player()), payload.message());
		});
		#else
		ServerPlayNetworking.registerGlobalReceiver(AbstractPluginPacketSender.WRAPPER_PACKET_RESOURCE, (server, serverPlayer, handler, buffer, packetSender) ->
		{
			AbstractNetworkMessage message = PACKET_SENDER.decodeMessage(buffer);
			if (message != null)
			{
				ServerApi.INSTANCE.pluginMessageReceived(ServerPlayerWrapper.getWrapper(serverPlayer), message);
			}
		});
		#endif
	}
	
}
