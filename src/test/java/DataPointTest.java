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
import no.jckf.dhsupport.core.dataobject.DataPoint;

public class DataPointTest extends TestCase
{
    final DataPoint dataPoint = new DataPoint();

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
}
