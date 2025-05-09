package server;

import client.MapleCharacter;
import client.MapleClient;
import client.anticheat.CheatTracker;
import client.anticheat.CheatingOffense;
import handling.channel.ChannelServer;
import java.awt.Point;

import scripting.PortalScriptManager;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.MaplePacketCreator;

public class MaplePortal {

    public static int MAP_PORTAL = 2;
    public static int DOOR_PORTAL = 6;
    private String name;
    private String target;
    private String scriptName;
    private Point position;
    private int targetmap;
    private int type;
    private int id;
    private boolean portalState = true;

    public MaplePortal(int type) {
        this.type = type;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Point getPosition() {
        return this.position;
    }

    public String getTarget() {
        return this.target;
    }

    public int getTargetMapId() {
        return this.targetmap;
    }

    public int getType() {
        return this.type;
    }

    public String getScriptName() {
        return this.scriptName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setTargetMapId(int targetmapid) {
        this.targetmap = targetmapid;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public void enterPortal(MapleClient c) {
        if ((getPosition().distanceSq(c.getPlayer().getPosition()) > 40000.0D) && (!c.getPlayer().isGM())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.USING_FARAWAY_PORTAL);
            return;
        }
        MapleMap currentmap = c.getPlayer().getMap();
        if ((!c.getPlayer().hasBlockedInventory()) && ((this.portalState) || (c.getPlayer().isGM()))) {
            if (getScriptName() != null) {
                c.getPlayer().checkFollow();
                try {
                    PortalScriptManager.getInstance().executePortalScript(this, c);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (getTargetMapId() != 999999999) {
                MapleMap to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(getTargetMapId());
                if (to == null) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                if ((!c.getPlayer().isGM())
                        && (to.getLevelLimit() > 0) && (to.getLevelLimit() > c.getPlayer().getLevel())) {
                    c.getPlayer().dropMessage(-1, "You are too low of a level to enter this place.");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }

                c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()));
            }
        }
        if ((c != null) && (c.getPlayer() != null) && (c.getPlayer().getMap() == currentmap)) {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public boolean getPortalState() {
        return this.portalState;
    }

    public void setPortalState(boolean ps) {
        this.portalState = ps;
    }
}