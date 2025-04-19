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

import junit.framework.TestCase;
import no.jckf.dhsupport.core.bytestream.Decoder;
import no.jckf.dhsupport.core.bytestream.Encoder;
import no.jckf.dhsupport.core.dataobject.DataPoint;

public class DataPointTest extends TestCase
{
    private DataPoint dataPoint = new DataPoint();

    public void testMappingId()
    {
        int randomNumber = (int) Math.floor(Math.random() * 100);

        this.dataPoint.setMappingId(randomNumber);

        assertEquals(randomNumber, this.dataPoint.getMappingId());
    }

    public void testMappingIdAgain()
    {
        this.testMappingId();
    }

    public void testHeight()
    {
        int randomNumber = (int) Math.floor(Math.random() * 10);

        this.dataPoint.setHeight(randomNumber);

        assertEquals(randomNumber, this.dataPoint.getHeight());
    }

    public void testHeightAgain()
    {
        this.testHeight();
    }

    public void testPositiveStartY()
    {
        int randomNumber = (int) Math.floor(Math.random() * 100);

        this.dataPoint.setStartY(randomNumber);

        assertEquals(randomNumber, this.dataPoint.getStartY());
    }

    public void testNegativeStartY()
    {
        int randomNumber = (int) Math.floor(0 - Math.random() * 100);

        this.dataPoint.setStartY(randomNumber);

        assertEquals(randomNumber, this.dataPoint.getStartY());
    }

    public void testStartYAgain()
    {
        this.testPositiveStartY();
    }

    public void testSkyLight()
    {
        byte randomNumber = (byte) Math.floor(Math.random() * 15);

        this.dataPoint.setSkyLight(randomNumber);

        assertEquals(randomNumber, this.dataPoint.getSkyLight());
    }

    public void testSkyLightAgain()
    {
        this.testSkyLight();
    }

    public void testBlockLight()
    {
        byte randomNumber = (byte) Math.floor(Math.random() * 15);

        this.dataPoint.setBlockLight(randomNumber);

        assertEquals(randomNumber, this.dataPoint.getBlockLight());
    }

    public void testBlockLightAgain()
    {
        this.testBlockLight();
    }

    public void testEncodeDecode()
    {
        int mappingId = (int) Math.floor(Math.random() * 100);
        int height = (int) Math.floor(Math.random() * 10);
        int startY = (int) Math.floor(Math.random() * 100);
        byte skyLight = (byte) Math.floor(Math.random() * 15);
        byte blockLight = (byte) Math.floor(Math.random() * 15);

        this.dataPoint.setMappingId(mappingId);
        this.dataPoint.setHeight(height);
        this.dataPoint.setStartY(startY);
        this.dataPoint.setSkyLight(skyLight);
        this.dataPoint.setBlockLight(blockLight);

        Encoder encoder = new Encoder();

        this.dataPoint.encode(encoder);

        this.dataPoint = new DataPoint();

        Decoder decoder = new Decoder(encoder.toByteArray());

        this.dataPoint.decode(decoder);

        assertEquals("Mapping ID", mappingId, this.dataPoint.getMappingId());
        assertEquals("Height", height, this.dataPoint.getHeight());
        assertEquals("Start Y", startY, this.dataPoint.getStartY());
        assertEquals("Sky light", skyLight, this.dataPoint.getSkyLight());
        assertEquals("Block light", blockLight, this.dataPoint.getBlockLight());
    }
}
