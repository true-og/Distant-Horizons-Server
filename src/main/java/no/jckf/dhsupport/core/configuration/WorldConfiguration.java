package no.jckf.dhsupport.core.configuration;

import no.jckf.dhsupport.core.world.WorldInterface;
import org.checkerframework.checker.nullness.qual.PolyNull;

import javax.annotation.Nullable;

public class WorldConfiguration extends Configuration
{
    protected static String WORLD_PREFIX = "worlds.%s.";

    protected WorldInterface world;

    protected Configuration config;

    public WorldConfiguration(WorldInterface world, Configuration config)
    {
        this.world = world;
        this.config = config;
    }

    @Override
    public void set(String key, @Nullable Object value)
    {
        this.config.set(WORLD_PREFIX.formatted(this.world.getName()) + key, value);
    }

    @Override
    public void unset(String key)
    {
        this.config.unset(WORLD_PREFIX.formatted(this.world.getName()) + key);
    }

    @Override
    public @PolyNull Object get(String key, @PolyNull Object defaultValue)
    {
        Object specific = this.config.get(WORLD_PREFIX.formatted(this.world.getName()) + key);

        return specific == null ? this.config.get(key, defaultValue) : specific;
    }
}
