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
import no.jckf.dhsupport.core.dataobject.SectionPosition;

public class SectionPositionTest extends TestCase
{
    protected SectionPosition sectionPosition = new SectionPosition();

    public void testDetailLevel()
    {
        int randomNumber = (int) Math.ceil(Math.random() * 6);

        this.sectionPosition.setDetailLevel(randomNumber);

        assertEquals(randomNumber, this.sectionPosition.getDetailLevel());
    }

    public void testDetailLevelAgain()
    {
        this.testDetailLevel();
    }

    public void testPositiveX()
    {
        int randomNumber = (int) Math.ceil(Math.random() * 100000);

        this.sectionPosition.setX(randomNumber);

        assertEquals(randomNumber, this.sectionPosition.getX());
    }

    public void testNegativeX()
    {
        int randomNumber = (int) Math.ceil(0 - Math.random() * 100000);

        this.sectionPosition.setX(randomNumber);

        assertEquals(randomNumber, this.sectionPosition.getX());
    }

    public void testXAgain()
    {
        this.testPositiveX();
    }

    public void testPositiveZ()
    {
        int randomNumber = (int) Math.ceil(Math.random() * 100000);

        this.sectionPosition.setZ(randomNumber);

        assertEquals(randomNumber, this.sectionPosition.getZ());
    }

    public void testNegativeZ()
    {
        int randomNumber = (int) Math.ceil(0 - Math.random() * 100000);

        this.sectionPosition.setZ(randomNumber);

        assertEquals(randomNumber, this.sectionPosition.getZ());
    }

    public void testZAgain()
    {
        this.testPositiveZ();
    }

    public void testEncodeDecode()
    {
        int detailLevel = (int) Math.ceil(Math.random() * 6);
        int x = (int) Math.ceil(Math.random() * 100000);
        int z = (int) Math.ceil(Math.random() * 100000);

        this.sectionPosition.setDetailLevel(detailLevel);
        this.sectionPosition.setX(x);
        this.sectionPosition.setZ(z);

        Encoder encoder = new Encoder();

        this.sectionPosition.encode(encoder);

        this.sectionPosition = new SectionPosition();

        Decoder decoder = new Decoder(encoder.toByteArray());

        this.sectionPosition.decode(decoder);

        assertEquals("Detail level", detailLevel, this.sectionPosition.getDetailLevel());
        assertEquals("X coordinate", x, this.sectionPosition.getX());
        assertEquals("Z coordinate", z, this.sectionPosition.getZ());
    }
}
