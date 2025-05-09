package handling.channel.handler;

import client.MapleClient;
import handling.world.World;
import handling.world.guild.MapleBBSThread;
import java.util.List;
import tools.data.LittleEndianAccessor;
import tools.packet.GuildPacket;

public class BBSHandler {

    private static String correctLength(String in, int maxSize) {
        if (in.length() > maxSize) {
            return in.substring(0, maxSize);
        }
        return in;
    }

    public static void BBSOperation(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }
        int localthreadid = 0;
        byte action = slea.readByte();
        String text;
        switch (action) {
            case 0:
                if (!c.getPlayer().getCheatTracker().canBBS()) {
                    c.getPlayer().dropMessage(1, "You may only start a new thread every 60 seconds.");
                    return;
                }
                boolean bEdit = slea.readByte() > 0;
                if (bEdit) {
                    localthreadid = slea.readInt();
                }
                boolean bNotice = slea.readByte() > 0;
                String title = correctLength(slea.readMapleAsciiString(), 25);
                text = correctLength(slea.readMapleAsciiString(), 600);
                int icon = slea.readInt();
                if ((icon >= 100) && (icon <= 106)) {
                    if (!c.getPlayer().haveItem(5290000 + icon - 100, 1, false, true)) {
                        return;
                    }
                } else if ((icon < 0) || (icon > 2)) {
                    return;
                }
                if (!bEdit) {
                    newBBSThread(c, title, text, icon, bNotice);
                } else {
                    editBBSThread(c, title, text, icon, localthreadid);
                }
                break;
            case 1:
                localthreadid = slea.readInt();
                deleteBBSThread(c, localthreadid);
                break;
            case 2:
                int start = slea.readInt();
                listBBSThreads(c, start * 10);
                break;
            case 3:
                localthreadid = slea.readInt();
                displayThread(c, localthreadid);
                break;
            case 4:
                if (!c.getPlayer().getCheatTracker().canBBS()) {
                    c.getPlayer().dropMessage(1, "You may only start a new reply every 60 seconds.");
                    return;
                }
                localthreadid = slea.readInt();
                text = correctLength(slea.readMapleAsciiString(), 25);
                newBBSReply(c, localthreadid, text);
                break;
            case 5:
                localthreadid = slea.readInt();
                int replyid = slea.readInt();
                deleteBBSReply(c, localthreadid, replyid);
        }
    }

    private static void listBBSThreads(MapleClient c, int start) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }
        c.getSession().write(GuildPacket.BBSThreadList(World.Guild.getBBS(c.getPlayer().getGuildId()), start));
    }

    private static void newBBSReply(MapleClient c, int localthreadid, String text) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }
        World.Guild.addBBSReply(c.getPlayer().getGuildId(), localthreadid, text, c.getPlayer().getId());
        displayThread(c, localthreadid);
    }

    private static void editBBSThread(MapleClient c, String title, String text, int icon, int localthreadid) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }
        World.Guild.editBBSThread(c.getPlayer().getGuildId(), localthreadid, title, text, icon, c.getPlayer().getId(), c.getPlayer().getGuildRank());
        displayThread(c, localthreadid);
    }

    private static void newBBSThread(MapleClient c, String title, String text, int icon, boolean bNotice) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }
        displayThread(c, World.Guild.addBBSThread(c.getPlayer().getGuildId(), title, text, icon, bNotice, c.getPlayer().getId()));
        listBBSThreads(c, 0);
    }

    private static void deleteBBSThread(MapleClient c, int localthreadid) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }
        World.Guild.deleteBBSThread(c.getPlayer().getGuildId(), localthreadid, c.getPlayer().getId(), c.getPlayer().getGuildRank());
    }

    private static void deleteBBSReply(MapleClient c, int localthreadid, int replyid) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }

        World.Guild.deleteBBSReply(c.getPlayer().getGuildId(), localthreadid, replyid, c.getPlayer().getId(), c.getPlayer().getGuildRank());
        displayThread(c, localthreadid);
    }

    private static void displayThread(MapleClient c, int localthreadid) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }
        List<MapleBBSThread> bbsList = World.Guild.getBBS(c.getPlayer().getGuildId());
        if (bbsList != null) {
            for (MapleBBSThread t : bbsList) {
                if ((t != null) && (t.localthreadID == localthreadid)) {
                    c.getSession().write(GuildPacket.showThread(t));
                }
            }
        }
    }
}
