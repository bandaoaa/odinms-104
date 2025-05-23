package server.movement;

import java.awt.Point;
import tools.data.MaplePacketLittleEndianWriter;

public class AranMovement extends AbstractLifeMovement {

    public AranMovement(int type, Point position, int duration, int newstate) {
        super(type, position, duration, newstate);
    }

    public void serialize(MaplePacketLittleEndianWriter lew) {
        lew.write(getType());

        lew.write(getNewstate());
        lew.writeShort(getDuration());
    }
}