package no.jckf.dhsupport.core.message.plugin;

import no.jckf.dhsupport.core.bytestream.Encoder;

public class FullDataPartialUpdateMessage extends PluginMessage
{
    protected String levelKey;

    protected int bufferId;

    protected byte[] beacons;

    public void setLevelKey(String levelKey)
    {
        this.levelKey = levelKey;
    }

    public String getLevelKey()
    {
        return levelKey;
    }

    public void setBufferId(int bufferId)
    {
        this.bufferId = bufferId;
    }

    public int getBufferId()
    {
        return bufferId;
    }

    public void setBeacons(byte[] beacons)
    {
        this.beacons = beacons;
    }

    public byte[] getBeacons()
    {
        return this.beacons;
    }

    @Override
    public void encode(Encoder encoder)
    {
        encoder.writeShortString(this.levelKey);
        encoder.writeInt(this.bufferId);
        encoder.write(this.beacons);
    }
}
