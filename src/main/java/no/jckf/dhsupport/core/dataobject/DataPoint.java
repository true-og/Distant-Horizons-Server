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

package no.jckf.dhsupport.core.dataobject;

import no.jckf.dhsupport.core.bytestream.Decoder;
import no.jckf.dhsupport.core.bytestream.Encoder;

public class DataPoint extends DataObject
{
    protected static int MAPPING_ID_OFFSET = 0;
    protected static int MAPPING_ID_BITS = 32;
    protected static int MAPPING_ID_MASK = 0xFFFFFFFF;

    protected static int HEIGHT_OFFSET = MAPPING_ID_OFFSET + MAPPING_ID_BITS;
    protected static int HEIGHT_BITS = 12;
    protected static int HEIGHT_MASK = 0x0FFF;

    protected static int START_Y_OFFSET = HEIGHT_OFFSET + HEIGHT_BITS;
    protected static int START_Y_SIZE = 12;
    protected static int START_Y_MASK = 0x0FFF;

    protected static int SKY_LIGHT_OFFSET = START_Y_OFFSET + START_Y_SIZE;
    protected static int SKY_LIGHT_SIZE = 4;
    protected static int SKY_LIGHT_MASK = 0x0F;

    protected static int BLOCK_LIGHT_OFFSET = SKY_LIGHT_OFFSET + SKY_LIGHT_SIZE;
    protected static int BLOCK_LIGHT_SIZE = 4;
    protected static int BLOCK_LIGHT_MASK = 0x0F;

    protected long data = 0;

    public void setMappingId(int mappingId)
    {
        this.data &= ~((long) MAPPING_ID_MASK << MAPPING_ID_OFFSET);

        this.data |= mappingId & MAPPING_ID_MASK;
    }

    public int getMappingId()
    {
        return (int) ((this.data >> MAPPING_ID_OFFSET) & MAPPING_ID_MASK);
    }

    public void setHeight(int height)
    {
        this.data &= ~((long) HEIGHT_MASK << HEIGHT_OFFSET);

        this.data |= (long) (height & HEIGHT_MASK) << HEIGHT_OFFSET;
    }

    public int getHeight()
    {
        return (int) ((this.data >> HEIGHT_OFFSET) & HEIGHT_MASK);
    }

    public void setStartY(int startY)
    {
        this.data &= ~((long) START_Y_MASK << START_Y_OFFSET);

        this.data |= ((long) startY & START_Y_MASK) << START_Y_OFFSET;
    }


    public int getStartY()
    {
        int raw = (int) ((this.data >> START_Y_OFFSET) & START_Y_MASK);

        if ((raw & 0x0800) != 0) {
            raw |= ~START_Y_MASK;
        }

        return raw;
    }


    public void setSkyLight(byte skyLight)
    {
        this.data &= ~((long) SKY_LIGHT_MASK << SKY_LIGHT_OFFSET);

        this.data |= (long) (skyLight & SKY_LIGHT_MASK) << SKY_LIGHT_OFFSET;
    }

    public byte getSkyLight()
    {
        return (byte) ((this.data >> SKY_LIGHT_OFFSET) & SKY_LIGHT_MASK);
    }

    public void setBlockLight(byte blockLight)
    {
        this.data &= ~((long) BLOCK_LIGHT_MASK << BLOCK_LIGHT_OFFSET);

        this.data |= (long) (blockLight & BLOCK_LIGHT_MASK) << BLOCK_LIGHT_OFFSET;
    }

    public byte getBlockLight()
    {
        return (byte) ((this.data >> BLOCK_LIGHT_OFFSET) & BLOCK_LIGHT_MASK);
    }

    @Override
    public void encode(Encoder encoder)
    {
        encoder.writeLong(data);
    }

    @Override
    public void decode(Decoder decoder)
    {
        this.data = decoder.readLong();
    }
}
