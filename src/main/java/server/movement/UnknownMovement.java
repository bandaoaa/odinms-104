package server.movement;

import java.awt.Point;
import tools.data.MaplePacketLittleEndianWriter;

public class UnknownMovement extends AbstractLifeMovement {

    private Point pixelsPerSecond;
    private int unk;
    private int fh;

    public UnknownMovement(int type, Point position, int duration, int newstate) {
        super(type, position, duration, newstate);
    }

    public Point getPixelsPerSecond() {
        return this.pixelsPerSecond;
    }

    public void setPixelsPerSecond(Point wobble) {
        this.pixelsPerSecond = wobble;
    }

    public int getUnk() {
        return this.unk;
    }

    public void setUnk(int unk) {
        this.unk = unk;
    }

    public int getFH() {
        return this.fh;
    }

    public void setFH(int fh) {
        this.fh = fh;
    }

    @Override
    public void serialize(MaplePacketLittleEndianWriter lew) {
        lew.write(getType());
        lew.writeShort(this.unk);
        lew.writeShort(getPosition().x);
        lew.writeShort(getPosition().y);
        lew.writeShort(this.pixelsPerSecond.x);
        lew.writeShort(this.pixelsPerSecond.y);
        lew.writeShort(this.fh);
        lew.write(getNewstate());
        lew.writeShort(getDuration());
    }
}