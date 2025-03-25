/*
 * DH Support, server-side support for Distant Horizons.
 * Copyright (C) 2024 Jim C K Flaten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package no.jckf.dhsupport.core;

import no.jckf.dhsupport.core.configuration.DhsConfig;
import no.jckf.dhsupport.core.database.models.LodModel;
import no.jckf.dhsupport.core.dataobject.SectionPosition;
import no.jckf.dhsupport.core.world.WorldInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PreGenerator implements Runnable
{
    protected DhSupport dhSupport;

    protected WorldInterface world;

    protected int radius;

    protected int centerX;

    protected int centerZ;

    protected int inFlight = 0;

    public PreGenerator(DhSupport dhSupport, WorldInterface world, int centerX, int centerZ, int radius)
    {
        this.dhSupport = dhSupport;
        this.world = world;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
    }

    public void run()
    {
        int rateLimit = this.world.getConfig().getInt(DhsConfig.FULL_DATA_REQUEST_CONCURRENCY_LIMIT);

        int currentX = this.centerX;
        int currentZ = this.centerZ;

        int totalSteps = this.radius * 4;
        int stepsSoFar = 0;

        int[][] directions = {{ 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 }};
        int dirIndex = 0;

        List<CompletableFuture<LodModel>> requests = new ArrayList<>();

        // Vibe code :>
        OUTER: for (int step = 1; stepsSoFar < totalSteps; step++) {
            for (int directionChanges = 0; directionChanges < 2; directionChanges++) {
                for (int stepsOnThisSide = 0; stepsOnThisSide < step; stepsOnThisSide++) {
                    currentX += directions[dirIndex][0];
                    currentZ += directions[dirIndex][1];

                    stepsSoFar++;

                    if (stepsSoFar == totalSteps) {
                        break OUTER;
                    }

                    SectionPosition position = new SectionPosition();
                    position.setX(currentX);
                    position.setZ(currentZ);
                    position.setDetailLevel(6);

                    CompletableFuture<LodModel> request = this.dhSupport.getLod(this.world.getId(), position);

                    this.inFlight++;
                    request.thenAccept((LodModel) -> this.inFlight--);

                    requests.add(request);

                    while (this.inFlight >= rateLimit) {
                        CompletableFuture.anyOf(requests.toArray(new CompletableFuture[0])).join();

                        requests.removeIf(CompletableFuture::isDone);
                    }
                }

                dirIndex = (dirIndex + 1) % 4; // Change direction
            }
        }
    }
}
