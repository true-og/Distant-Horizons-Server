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

public class SectionPosition extends DataObject
{
    public static final int DETAIL_LEVEL_OFFSET = 0;
    public static final int DETAIL_LEVEL_BITS = 8;
    public static final long DETAIL_LEVEL_MASK = 0xFF;

    public static final int X_OFFSET = DETAIL_LEVEL_OFFSET + DETAIL_LEVEL_BITS;
    public static final int X_BITS = 28;
    public static final int X_MASK = 0x0FFFFFFF;

    public static final int Z_OFFSET = X_OFFSET + X_BITS;
    public static final int Z_BITS = 28;
    public static final int Z_MASK = 0x0FFFFFFF;

    protected long data = 0;

    public void setDetailLevel(int detailLevel)
    {
        this.data &= ~((long) DETAIL_LEVEL_MASK << DETAIL_LEVEL_OFFSET);

        this.data |= (detailLevel & DETAIL_LEVEL_MASK) << DETAIL_LEVEL_OFFSET;
    }

    public int getDetailLevel()
    {
        return (int) (data & DETAIL_LEVEL_MASK);
    }

    public void setX(int x)
    {
        this.data &= ~((long) X_MASK << X_OFFSET);

        this.data |= (long) (x & X_MASK) << X_OFFSET;
    }

    public int getX()
    {
        int raw = (int) ((data >> X_OFFSET) & X_MASK);

        if ((raw & (1 << 27)) != 0) {
            raw |= ~X_MASK;
        }

        return raw;
    }

    public void setZ(int z)
    {
        this.data &= ~((long) Z_MASK << Z_OFFSET);

        this.data |= (long) (z & Z_MASK) << Z_OFFSET;
    }

    public int getZ()
    {
        int raw = (int) ((data >> Z_OFFSET) & Z_MASK);

        if ((raw & (1 << 27)) != 0) {
            raw |= ~Z_MASK;
        }

        return raw;
    }

    @Override
    public void encode(Encoder encoder)
    {
        encoder.writeLong(this.data);
    }

    @Override
    public void decode(Decoder decoder)
    {
        this.data = decoder.readLong();
    }
}
