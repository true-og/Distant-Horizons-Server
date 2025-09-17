package com.seibel.distanthorizons.fabric.testing;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiLevelLoadEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import net.minecraft.server.level.ServerLevel;
import org.apache.logging.log4j.Logger;

// TODO add to API example once Builderb0y has given the all-clear
public class TestWorldGenBindingEvent extends DhApiLevelLoadEvent
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	@Override
	public void onLevelLoad(DhApiEventParam<DhApiLevelLoadEvent.EventParam> event)
	{
		LOGGER.info("DH Level: ["+event.value.levelWrapper.getDimensionType()+"] loaded.");
		
		try
		{
			// Note: whenever you use a wrapper method on a new Minecraft version it is recommended that you
			// call wrapper.getClass() to determine which object the API will return before you try casting it.
			ServerLevel level = (ServerLevel) event.value.levelWrapper.getWrappedMcObject();
			
			// override the core DH world generator for this level
			//IDhApiWorldGenerator exampleWorldGen = new TestChunkWorldGenerator(level); // TODO biomes are broken for some reason
			IDhApiWorldGenerator exampleWorldGen = new TestGenericWorldGenerator(event.value.levelWrapper);
			DhApi.worldGenOverrides.registerWorldGeneratorOverride(event.value.levelWrapper, exampleWorldGen);
		}
		catch (ClassCastException e)
		{
			LOGGER.warn("Unable to add world generator to level wrapper ["+event.value.levelWrapper.getClass()+"] - ["+event.value.levelWrapper.getDimensionType()+"].");
		}
	}
}
