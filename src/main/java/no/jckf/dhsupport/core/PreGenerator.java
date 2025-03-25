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
