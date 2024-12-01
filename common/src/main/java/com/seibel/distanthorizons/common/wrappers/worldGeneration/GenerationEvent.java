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

package com.seibel.distanthorizons.common.wrappers.worldGeneration;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodQuadBuilder;
import com.seibel.distanthorizons.core.util.objects.UncheckedInterruptedException;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.util.objects.EventTimer;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;

import org.apache.logging.log4j.Logger;

public final class GenerationEvent
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	private static int generationFutureDebugIDs = 0;
	
	public final int id;
	public final ThreadedParameters threadedParam;
	public final DhChunkPos minPos;
	/** the number of chunks wide this event is */
	public final int size;
	public final EDhApiWorldGenerationStep targetGenerationStep;
	public EventTimer timer = null;
	public long inQueueTime;
	public long timeoutTime = -1;
	public CompletableFuture<Void> future = null;
	public final Consumer<IChunkWrapper> resultConsumer;
	
	
	
	public GenerationEvent(
			DhChunkPos minPos, int size, BatchGenerationEnvironment generationGroup,
			EDhApiWorldGenerationStep targetGenerationStep, Consumer<IChunkWrapper> resultConsumer)
	{
		this.inQueueTime = System.nanoTime();
		this.id = generationFutureDebugIDs++;
		this.minPos = minPos;
		this.size = size;
		this.targetGenerationStep = targetGenerationStep;
		this.threadedParam = ThreadedParameters.getOrMake(generationGroup.params);
		this.resultConsumer = resultConsumer;
	}
	
	
	
	public static GenerationEvent startEvent(
			DhChunkPos minPos, int size, BatchGenerationEnvironment genEnvironment,
			EDhApiWorldGenerationStep target, Consumer<IChunkWrapper> resultConsumer,
			ExecutorService worldGeneratorThreadPool)
	{
		GenerationEvent generationEvent = new GenerationEvent(minPos, size, genEnvironment, target, resultConsumer);
		generationEvent.future = CompletableFuture.supplyAsync(() ->
		{
			long runStartTime = System.nanoTime();
			generationEvent.timeoutTime = runStartTime;
			generationEvent.inQueueTime = runStartTime - generationEvent.inQueueTime;
			generationEvent.timer = new EventTimer("setup");
			
			BatchGenerationEnvironment.isDistantGeneratorThread.set(true);
			try
			{
				return genEnvironment.generateLodFromListAsync(generationEvent, (runnable) -> 
				{
					worldGeneratorThreadPool.execute(() ->
					{
						boolean alreadyMarked = BatchGenerationEnvironment.isCurrentThreadDistantGeneratorThread();
						if (!alreadyMarked)
						{
							BatchGenerationEnvironment.isDistantGeneratorThread.set(true);
						}
						
						try
						{
							runnable.run();
						}
						finally
						{
							if (!alreadyMarked)
							{
								BatchGenerationEnvironment.isDistantGeneratorThread.set(false);
							}
						}
					});
				}).exceptionallyCompose(throwable -> 
				{
					while (throwable instanceof CompletionException completionException)
					{
						throwable = completionException.getCause();
					}
					
					if (throwable instanceof InterruptedException 
						|| throwable instanceof UncheckedInterruptedException
						|| throwable instanceof RejectedExecutionException)
					{
						return CompletableFuture.completedFuture(null);
					}
					else
					{
						return CompletableFuture.failedFuture(throwable);
					}
				});
			}
			finally
			{
				BatchGenerationEnvironment.isDistantGeneratorThread.remove();
			}
		}, worldGeneratorThreadPool)
			// un-wrap future so we can go from CompletableFuture<CompletableFuture<Void>> -> CompletableFuture<Void>
			// TODO can we remove this double future wrapping?
			.thenCompose(Function.identity());
		return generationEvent;
	}
	
	public boolean isComplete() { return this.future.isDone(); }
	
	public boolean hasTimeout(int duration, TimeUnit unit)
	{
		if (this.timeoutTime == -1)
		{
			return false;
		}
		
		long currentTime = System.nanoTime();
		long delta = currentTime - this.timeoutTime;
		return (delta > TimeUnit.NANOSECONDS.convert(duration, unit));
	}
	
	public boolean terminate()
	{
		LOGGER.info("======================DUMPING ALL THREADS FOR WORLD GEN=======================");
		ThreadPoolUtil.WORLD_GEN_THREAD_FACTORY.dumpAllThreadStacks();
		this.future.cancel(true);
		return this.future.isCancelled();
	}
	
	public void refreshTimeout()
	{
		this.timeoutTime = System.nanoTime();
		UncheckedInterruptedException.throwIfInterrupted();
	}
	
	@Override
	public String toString() { return this.id + ":" + this.size + "@" + this.minPos + "(" + this.targetGenerationStep + ")"; }
	
}