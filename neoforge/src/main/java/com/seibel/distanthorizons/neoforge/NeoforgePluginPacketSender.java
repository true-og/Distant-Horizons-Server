package com.seibel.distanthorizons.neoforge;

import com.seibel.distanthorizons.common.CommonPacketPayload;
import com.seibel.distanthorizons.common.wrappers.misc.ServerPlayerWrapper;
import com.seibel.distanthorizons.common.AbstractPluginPacketSender;
import com.seibel.distanthorizons.core.network.messages.AbstractNetworkMessage;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NeoforgePluginPacketSender extends AbstractPluginPacketSender
{
	private static BiConsumer<IServerPlayerWrapper, AbstractNetworkMessage> packetConsumer;
	
	public static void setPacketHandler(RegisterPayloadHandlersEvent event, Consumer<AbstractNetworkMessage> consumer)
	{ setPacketHandler(event, (player, buffer) -> consumer.accept(buffer)); }
	public static void setPacketHandler(RegisterPayloadHandlersEvent event, BiConsumer<IServerPlayerWrapper, AbstractNetworkMessage> consumer)
	{
		packetConsumer = consumer;
		
		PayloadRegistrar registrar = event.registrar("1").optional();
		registrar.playBidirectional(CommonPacketPayload.TYPE, new CommonPacketPayload.Codec(), (payload, context) ->
		{
			ServerPlayerWrapper serverPlayer = Optional.of(context.player())
					.map(player -> player instanceof ServerPlayer ? (ServerPlayer) player : null)
					.map(ServerPlayerWrapper::getWrapper)
					.orElse(null);
			
			if (payload.message() != null)
			{
				packetConsumer.accept(serverPlayer, payload.message());
			}
		});
	}
	
	@Override
	public void sendToServer(AbstractNetworkMessage message)
	{
		if (Minecraft.getInstance().getConnection() != null) 
		{
			Minecraft.getInstance().getConnection().send(new CommonPacketPayload(message));
		}
	}
	
	@Override
	public void sendToClient(ServerPlayer serverPlayer, AbstractNetworkMessage message)
	{ PacketDistributor.sendToPlayer(serverPlayer, new CommonPacketPayload(message)); }
	
}