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

package no.jckf.dhsupport.core.configuration;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class Configuration
{
    protected Map<String, Object> variables = new HashMap<>();

    public void set(String key, @Nullable Object value)
    {
        this.variables.put(key, value);
    }

    public void unset(String key)
    {
        this.variables.remove(key);
    }

    public @Nullable Object get(String key, @Nullable Object defaultValue)
    {
        return this.variables.getOrDefault(key, defaultValue);
    }

    public @Nullable Object get(String key)
    {
        return this.get(key, null);
    }

    public @Nullable Boolean getBool(String key, @Nullable Boolean defaultValue)
    {
        return (Boolean) this.get(key, defaultValue);
    }

    public @Nullable Boolean getBool(String key)
    {
        return this.getBool(key, null);
    }

    public @Nullable Integer getInt(String key, @Nullable Integer defaultValue)
    {
        return (Integer) this.get(key, defaultValue);
    }

    public @Nullable Integer getInt(String key)
    {
        return this.getInt(key, null);
    }

    public @Nullable String getString(String key, @Nullable String defaultValue)
    {
        return (String) this.get(key, defaultValue);
    }

    public @Nullable String getString(String key)
    {
        return (String) this.get(key, null);
    }
}
