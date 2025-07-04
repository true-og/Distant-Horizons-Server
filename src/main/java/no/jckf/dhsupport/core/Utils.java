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

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class Utils
{
    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    public static String bytesToHex(byte[] bytes)
    {
        byte[] hexChars = new byte[bytes.length * 2];

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public static String bytesToBin(byte[] bytes)
    {
        StringBuilder builder = new StringBuilder();

        for (byte b : bytes) {
            builder.append(Integer.toBinaryString((b & 0xFF) | 0x0100).substring(1));
        }

        return builder.toString().replaceAll("(.{4})", "$1 ").trim();
    }

    public static String ucFirst(String string, Locale locale)
    {
        if (string.isEmpty()) {
            return "";
        }

        return string.substring(0, 1).toUpperCase(locale) + string.substring(1);
    }

    public static String ucFirst(String string)
    {
        return ucFirst(string, Locale.getDefault());
    }
}
