package handling.login.handler;

import client.MapleClient;
import tools.data.LittleEndianAccessor;
import tools.packet.LoginPacket;

public class SetGenderHandler {

    public static void handlePacket(LittleEndianAccessor slea, MapleClient c) {
        byte gender = slea.readByte();
        String username = slea.readMapleAsciiString();
        if (c.getAccountName().equals(username)) {
            c.setGender(gender);
            c.getSession().write(LoginPacket.genderChanged(c));
            c.getSession().write(LoginPacket.getLoginFailed(22));
        } else {
            c.getSession().close();
        }
    }
}
