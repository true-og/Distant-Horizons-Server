package com.seibel.distanthorizons.common;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiAfterDhInitEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeDhInitEvent;
import com.seibel.distanthorizons.common.wrappers.DependencySetup;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftServerWrapper;
import com.seibel.distanthorizons.common.wrappers.misc.ServerPlayerWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.config.ConfigBase;
import com.seibel.distanthorizons.core.config.eventHandlers.presets.ThreadPresetConfigEventHandler;
import com.seibel.distanthorizons.core.config.types.AbstractConfigType;
import com.seibel.distanthorizons.core.config.types.ConfigEntry;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.jar.ModJarInfo;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.network.messages.base.CodecCrashMessage;
import com.seibel.distanthorizons.core.util.objects.Pair;
import com.seibel.distanthorizons.core.world.DhServerWorld;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IModAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IModChecker;
import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;
import com.seibel.distanthorizons.coreapi.ModInfo;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.seibel.distanthorizons.core.network.messages.MessageRegistry.DEBUG_CODEC_CRASH_MESSAGE;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

#if MC_VER >= MC_1_19_2
import net.minecraft.network.chat.Component;
#else // < 1.19.2
import net.minecraft.network.chat.TranslatableComponent;
#endif

/**
 * Base for all mod loader initializers 
 * and handles most setup. 
 */
public abstract class AbstractModInitializer
{
	protected static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	
	private CommandDispatcher<CommandSourceStack> commandDispatcher;
	
	
	
	//==================//
	// abstract methods //
	//==================//
	
	protected abstract void createInitialBindings();
	protected abstract IEventProxy createClientProxy();
	protected abstract IEventProxy createServerProxy(boolean isDedicated);
	protected abstract void initializeModCompat();
	
	protected abstract void subscribeRegisterCommandsEvent(Consumer<CommandDispatcher<CommandSourceStack>> eventHandler);
	
	protected abstract void subscribeClientStartedEvent(Runnable eventHandler);
	protected abstract void subscribeServerStartingEvent(Consumer<MinecraftServer> eventHandler);
	protected abstract void runDelayedSetup();
	
	
	
	//===================//
	// initialize events //
	//===================//
	
	public void onInitializeClient()
	{
		DependencySetup.createClientBindings();
		
		LOGGER.info("Initializing " + ModInfo.READABLE_NAME + " client.");
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeDhInitEvent.class, null);
		
		this.startup();
		this.logBuildInfo();
		
		this.createClientProxy().registerEvents();
		this.createServerProxy(false).registerEvents();
		
		this.initializeModCompat();
		
		LOGGER.info(ModInfo.READABLE_NAME + " client Initialized.");
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiAfterDhInitEvent.class, null);
		
		// Client uses config for auto-updater, so it's initialized here instead of post-init stage
		this.initConfig();
		
		this.subscribeClientStartedEvent(this::postInit);
	}
	
	public void onInitializeServer()
	{
		DependencySetup.createServerBindings();
		
		LOGGER.info("Initializing " + ModInfo.READABLE_NAME + " server.");
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeDhInitEvent.class, null);
		
		this.startup();
		this.logBuildInfo();
		
		// This prevents returning uninitialized Config values,
		// resulting from a circular reference mid-initialization in a static class
		// noinspection ResultOfMethodCallIgnored
		ThreadPresetConfigEventHandler.INSTANCE.toString();
		
		this.createServerProxy(true).registerEvents();
		
		this.initializeModCompat();
		
		LOGGER.info(ModInfo.READABLE_NAME + " server Initialized.");
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiAfterDhInitEvent.class, null);
		
		this.subscribeRegisterCommandsEvent(dispatcher -> { this.commandDispatcher = dispatcher; });
		
		this.subscribeServerStartingEvent(server -> 
		{
			MinecraftServerWrapper.INSTANCE.dedicatedServer = (DedicatedServer)server;
			
			this.initConfig();
			this.postInit();
			this.initCommands();
			
			LOGGER.info("Dedicated server initialized at " + server.getServerDirectory());
		});
	}
	
	
	
	//===========================//
	// inner initializer methods //
	//===========================//
	
	private void startup()
	{
		DependencySetup.createSharedBindings();
		SharedApi.init();
		this.createInitialBindings();
		logModIncompatibilityWarnings();
	}
	
	private void logBuildInfo()
	{
		LOGGER.info(ModInfo.READABLE_NAME + ", Version: " + ModInfo.VERSION);
		
		// if the build is stable the branch/commit/etc shouldn't be needed
		if (ModInfo.IS_DEV_BUILD)
		{
			LOGGER.info("DH Branch: " + ModJarInfo.Git_Branch);
			LOGGER.info("DH Commit: " + ModJarInfo.Git_Commit);
			LOGGER.info("DH Jar Build Source: " + ModJarInfo.Build_Source);
		}
	}
	
	protected <T extends IModAccessor> void tryCreateModCompatAccessor(String modId, Class<? super T> accessorClass, Supplier<T> accessorConstructor)
	{
		IModChecker modChecker = SingletonInjector.INSTANCE.get(IModChecker.class);
		if (modChecker.isModLoaded(modId))
		{
			//noinspection unchecked
			ModAccessorInjector.INSTANCE.bind((Class<? extends IModAccessor>) accessorClass, accessorConstructor.get());
		}
	}
	
	private void initConfig()
	{
		ConfigBase.INSTANCE = new ConfigBase(ModInfo.ID, ModInfo.NAME, Config.class, ModInfo.CONFIG_FILE_VERSION);
		Config.completeDelayedSetup();
	}
	
	private void postInit()
	{
		LOGGER.info("Post-Initializing Mod");
		this.runDelayedSetup();
		LOGGER.info("Mod Post-Initialized");
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void initCommands()
	{
		LiteralArgumentBuilder<CommandSourceStack> builder = literal("dhconfig")
				.requires(source -> source.hasPermission(4));
		
		for (AbstractConfigType<?, ?> type : ConfigBase.INSTANCE.entries)
		{
			if (!(type instanceof ConfigEntry))
			{
				continue;
			}
			//noinspection PatternVariableCanBeUsed
			ConfigEntry configEntry = (ConfigEntry) type;
			if (configEntry.getServersideShortName() == null)
			{
				continue;
			}
			
			Function<
					Function<CommandContext<CommandSourceStack>, Object>,
					Command<CommandSourceStack>
					> makeConfigUpdater = (getter) -> (commandContext) -> {
				Object value = getter.apply(commandContext);
				
				commandContext.getSource().sendSuccess(
						#if MC_VER >= MC_1_20_1
						() -> Component.literal("Changed the value of "+configEntry.getServersideShortName()+" to "+value),
						#elif MC_VER >= MC_1_19_2
						Component.literal("Changed the value of "+configEntry.getServersideShortName()+" to "+value),
						#else
						new TranslatableComponent("Changed the value of "+configEntry.getServersideShortName()+" to "+value),
						#endif
						true);
				configEntry.set(value);
				return 1;
			};
			
			LiteralArgumentBuilder<CommandSourceStack> subcommand = literal(configEntry.getServersideShortName())
					.executes((commandContext) -> {
						#if MC_VER >= MC_1_20_1
						commandContext.getSource().sendSuccess(() -> Component.literal("Current value of "+configEntry.getServersideShortName()+" is "+configEntry.get()), true);
						#elif MC_VER >= MC_1_19_2
						commandContext.getSource().sendSuccess(Component.literal("Current value of "+configEntry.getServersideShortName()+" is "+configEntry.get()), true);
						#else // < 1.19.2
						commandContext.getSource().sendSuccess(new TranslatableComponent("Current value of "+configEntry.getServersideShortName()+" is "+configEntry.get()), true);
						#endif
						return 1;
					});
			
			if (Enum.class.isAssignableFrom(configEntry.getType()))
			{
				for (Object choice : configEntry.getType().getEnumConstants())
				{
					subcommand.then(
							literal(choice.toString())
									.executes(makeConfigUpdater.apply(c -> choice))
					);
				}
			}
			else
			{
				boolean setterAdded = false;
				
				for (java.util.Map.Entry<Class<?>, Pair<Supplier<ArgumentType<?>>, BiFunction<CommandContext<?>, String, ?>>> pair : new HashMap<
						Class<?>,
						Pair<
								Supplier<ArgumentType<?>>,
								BiFunction<CommandContext<?>, String, ?>>
						>() {{
					this.put(Integer.class, new Pair<>(() -> integer((int) configEntry.getMin(), (int) configEntry.getMax()), IntegerArgumentType::getInteger));
					this.put(Double.class, new Pair<>(() -> doubleArg((double) configEntry.getMin(), (double) configEntry.getMax()), DoubleArgumentType::getDouble));
					this.put(Boolean.class, new Pair<>(BoolArgumentType::bool, BoolArgumentType::getBool));
					this.put(String.class, new Pair<>(StringArgumentType::string, StringArgumentType::getString));
				}}.entrySet())
				{
					if (!pair.getKey().isAssignableFrom(configEntry.getType()))
					{
						continue;
					}
					
					subcommand.then(argument("value", pair.getValue().first.get())
							.executes(makeConfigUpdater.apply(c -> pair.getValue().second.apply(c, "value"))));
					
					setterAdded = true;
					break;
				}
				
				if (!setterAdded)
				{
					throw new RuntimeException("Config type of "+type.getName()+" is not supported: "+configEntry.getType().getSimpleName());
				}
			}
			
			builder.then(subcommand);
		}
		
		this.commandDispatcher.register(builder);
		
		if (DEBUG_CODEC_CRASH_MESSAGE)
		{
			LiteralArgumentBuilder<CommandSourceStack> dhcrash = literal("dhcrash")
					.requires(source -> source.hasPermission(4))
					.then(literal("encode")
							.executes(c -> {
								assert SharedApi.getIDhServerWorld() != null;
								((DhServerWorld) SharedApi.getIDhServerWorld()).getServerPlayerStateManager()
										#if MC_VER >= MC_1_19_2
										.getConnectedPlayer(ServerPlayerWrapper.getWrapper(Objects.requireNonNull(c.getSource().getPlayer())))
										#else
										.getConnectedPlayer(ServerPlayerWrapper.getWrapper(Objects.requireNonNull(c.getSource().getPlayerOrException())))
										#endif
										.networkSession.sendMessage(new CodecCrashMessage(CodecCrashMessage.ECrashPhase.ENCODE));
								return 1;
							}))
					.then(literal("decode")
							.executes(c -> {
								assert SharedApi.getIDhServerWorld() != null;
								((DhServerWorld) SharedApi.getIDhServerWorld()).getServerPlayerStateManager()
										#if MC_VER >= MC_1_19_2
										.getConnectedPlayer(ServerPlayerWrapper.getWrapper(Objects.requireNonNull(c.getSource().getPlayer())))
										#else
										.getConnectedPlayer(ServerPlayerWrapper.getWrapper(Objects.requireNonNull(c.getSource().getPlayerOrException())))
										#endif
										.networkSession.sendMessage(new CodecCrashMessage(CodecCrashMessage.ECrashPhase.DECODE));
								return 1;
							}));
			this.commandDispatcher.register(dhcrash);
		}
	}
	
	
	
	//==================================//
	// mod partial compatibility checks //
	//==================================//
	
	/** 
	 * Some mods will work with a few tweaks
	 * or will partially work but have some known issues we can't solve.
	 * This method will log (and display to chat if enabled)
	 * these warnings and potential fixes.
	 */
	private static void logModIncompatibilityWarnings()
	{
		boolean showChatWarnings = Config.Common.Logging.Warning.showModCompatibilityWarningsOnStartup.get();
		IModChecker modChecker = SingletonInjector.INSTANCE.get(IModChecker.class);
		
		String startingString = "Partially Incompatible Distant Horizons mod detected: ";
		
		
		
		// Alex's caves
		if (modChecker.isModLoaded("alexscaves"))
		{
			// There've been a few reports about this mod breaking DH at a few different points in time
			// the fixes for said breakage changes depending on the version so unfortunately
			// all we can do is log a warning so the user can handle it.
			
			if (showChatWarnings)
			{
				String message =
						// orange text
						"\u00A76" + "Distant Horizons: Alex's Cave detected." + "\u00A7r\n" +
								"You may have to change Alex's config for DH to render. ";
				ClientApi.INSTANCE.showChatMessageNextFrame(message);
			}
			
			LOGGER.warn(startingString + "[Alex's Caves] may require some config changes in order to render Distant Horizons correctly.");
		}
		
		// William Wythers' Overhauled Overworld (WWOO)
		if (modChecker.isModLoaded("wwoo"))
		{
			// WWOO has a bug with it's world gen that can't be fixed by DH or WWOO
			// (at least that is what James learned after talking with WWOO)
			// WWOO will cause grid lines to appear in the world when DH generates the chunks
			// this might be due to how WWOO uses features for everything when generating
			// and said features don't always get to the edge of said chunks.
			
			String wwooWarning = "LODs generated by DH may have grid lines between sections. Disabling either WWOO or DH's distant generator will fix the problem.";
			
			if (showChatWarnings)
			{
				String message =
						// orange text
						"\u00A76" + "Distant Horizons: WWOO detected." + "\u00A7r\n" +
								wwooWarning;
				ClientApi.INSTANCE.showChatMessageNextFrame(message);
			}
			
			LOGGER.warn(startingString + "[WWOO] "+ wwooWarning);
		}
		
		// Chunky
		boolean chunkyPresent = false;
		try
		{
			Class.forName("org.popcraft.chunky.api.ChunkyAPI");
			chunkyPresent = true;
		}
		catch (ClassNotFoundException ignore) { }
		
		if (chunkyPresent)
		{
			// Chunky can generate chunks faster than DH can process them,
			// causing holes in the LODs.
			// Generally it's better and faster to use DH's world generator.
			
			String chunkyWarning = "Chunky can cause DH LODs to have holes " +
					"since Chunky can generate chunks faster than DH can process them. \n" +
					"Using DH's distant generator instead of chunky or increasing DH's CPU thread count can resolve the issue.";
			
			if (showChatWarnings)
			{
				String message =
						// orange text
						"\u00A76" + "Distant Horizons: Chunky detected." + "\u00A7r\n" +
								chunkyWarning;
				ClientApi.INSTANCE.showChatMessageNextFrame(message);
			}
			
			LOGGER.warn(startingString + "[Chunky] "+ chunkyWarning);
		}
		
	}
	
	
	
	//================//
	// helper classes //
	//================//
	
	public interface IEventProxy
	{
		void registerEvents();
	}
	
}
