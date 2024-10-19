package com.seibel.distanthorizons.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.multiplayer.server.ServerPlayerState;
import com.seibel.distanthorizons.core.network.messages.base.CodecCrashMessage;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.literal;

public class DhCrashCommand extends AbstractCommand
{
	public void register(CommandDispatcher<CommandSourceStack> commandDispatcher)
	{
		LiteralArgumentBuilder<CommandSourceStack> dhcrash = literal("dhcrash")
				.requires(source -> this.isPlayerSource(source) && source.hasPermission(4))
				.then(literal("encode")
						.executes(c -> {
							assert SharedApi.getIDhServerWorld() != null;
							
							ServerPlayerState serverPlayerState = SharedApi.getIDhServerWorld().getServerPlayerStateManager()
									.getConnectedPlayer(this.getSourcePlayer(c));
							if (serverPlayerState != null)
							{
								serverPlayerState.networkSession.sendMessage(new CodecCrashMessage(CodecCrashMessage.ECrashPhase.ENCODE));
							}
							return 1;
						}))
				.then(literal("decode")
						.executes(c -> {
							assert SharedApi.getIDhServerWorld() != null;
							
							ServerPlayerState serverPlayerState = SharedApi.getIDhServerWorld().getServerPlayerStateManager()
									.getConnectedPlayer(this.getSourcePlayer(c));
							if (serverPlayerState != null)
							{
								serverPlayerState.networkSession.sendMessage(new CodecCrashMessage(CodecCrashMessage.ECrashPhase.DECODE));
							}
							return 1;
						}));
		commandDispatcher.register(dhcrash);
	}
	
}
