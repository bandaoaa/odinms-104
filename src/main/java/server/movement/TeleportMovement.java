package server.movement;

import java.awt.Point;
import tools.data.MaplePacketLittleEndianWriter;

public class TeleportMovement extends AbsoluteLifeMovement {

    public TeleportMovement(int type, Point position, int newstate) {
        super(type, position, 0, newstate);
    }

    @Override
    public void serialize(MaplePacketLittleEndianWriter lew) {
        lew.write(getType());
        lew.writeShort(getPosition().x);
        lew.writeShort(getPosition().y);
        lew.writeShort(getPixelsPerSecond().x);
        lew.writeShort(getPixelsPerSecond().y);
        lew.write(getNewstate());
    }
}