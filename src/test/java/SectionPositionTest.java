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
import no.jckf.dhsupport.core.Utils;
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

    public void testBinary()
    {
        this.sectionPosition = new SectionPosition();

        this.sectionPosition.setDetailLevel(1);
        Encoder detailLevelEncoder = new Encoder();
        this.sectionPosition.encode(detailLevelEncoder);
        byte[] detailLevelBytes = detailLevelEncoder.toByteArray();
        this.sectionPosition.setDetailLevel(0);

        this.sectionPosition.setX(1);
        Encoder xEncoder = new Encoder();
        this.sectionPosition.encode(xEncoder);
        byte[] xBytes = xEncoder.toByteArray();
        this.sectionPosition.setX(0);

        this.sectionPosition.setX(-1);
        Encoder negativeXEncoder = new Encoder();
        this.sectionPosition.encode(negativeXEncoder);
        byte[] negativeXBytes = negativeXEncoder.toByteArray();
        this.sectionPosition.setX(0);

        this.sectionPosition.setZ(1);
        Encoder zEncoder = new Encoder();
        this.sectionPosition.encode(zEncoder);
        byte[] zBytes = zEncoder.toByteArray();
        this.sectionPosition.setZ(0);

        this.sectionPosition.setZ(-1);
        Encoder negativeZEncoder = new Encoder();
        this.sectionPosition.encode(negativeZEncoder);
        byte[] negativeZBytes = negativeZEncoder.toByteArray();
        this.sectionPosition.setZ(0);

        //                                    |                                 Z|                                 X|   Detail|
        assertEquals("Detail level",          "0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0001", Utils.bytesToBin(detailLevelBytes));
        assertEquals("X coordinate",          "0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0001 0000 0000", Utils.bytesToBin(xBytes));
        assertEquals("Negative X coordinate", "0000 0000 0000 0000 0000 0000 0000 1111 1111 1111 1111 1111 1111 1111 0000 0000", Utils.bytesToBin(negativeXBytes));
        assertEquals("Z coordinate",          "0000 0000 0000 0000 0000 0000 0001 0000 0000 0000 0000 0000 0000 0000 0000 0000", Utils.bytesToBin(zBytes));
        assertEquals("Negative Z coordinate", "1111 1111 1111 1111 1111 1111 1111 0000 0000 0000 0000 0000 0000 0000 0000 0000", Utils.bytesToBin(negativeZBytes));
    }
}
