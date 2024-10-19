package com.seibel.distanthorizons.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

import static com.seibel.distanthorizons.core.network.messages.MessageRegistry.DEBUG_CODEC_CRASH_MESSAGE;

/**
 * Initializes commands of the mod.
 */
public class CommandInitializer
{
	private final CommandDispatcher<CommandSourceStack> commandDispatcher;
	
	/**
	 * Constructs a new instance of this class.
	 *
	 * @param commandDispatcher The dispatcher to use for registering commands.
	 */
	public CommandInitializer(CommandDispatcher<CommandSourceStack> commandDispatcher)
	{
		this.commandDispatcher = commandDispatcher;
	}
	
	
	
	/**
	 * Initializes all available commands.
	 */
	public void initCommands()
	{
		new DhConfigCommand().register(this.commandDispatcher);
		
		if (DEBUG_CODEC_CRASH_MESSAGE)
		{
			new DhCrashCommand().register(this.commandDispatcher);
		}
	}
	
}
