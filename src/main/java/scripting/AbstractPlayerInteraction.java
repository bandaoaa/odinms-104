package scripting;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.MapleTrait;
import client.Skill;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.World.Broadcast;
import handling.world.World.Guild;
import handling.world.guild.MapleGuild;
import java.awt.Point;
import java.util.List;
import org.apache.log4j.Logger;
import server.MapleCarnivalChallenge;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.Event_DojoAgent;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleReactor;
import server.maps.SavedLocationType;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.StringUtil;
import tools.packet.PetPacket;
import tools.packet.UIPacket;

public abstract class AbstractPlayerInteraction {

    private static final Logger log = Logger.getLogger(AbstractPlayerInteraction.class);
    protected MapleClient c;
    protected int id;
    protected int id2;
    protected int id3;

    public AbstractPlayerInteraction(MapleClient c, int id, int id2, int id3) {
        this.c = c;
        this.id = id;
        this.id2 = id2;
        this.id3 = id3;
    }

    public MapleClient getClient() {
        return this.c;
    }

    public MapleClient getC() {
        return this.c;
    }

    public MapleCharacter getChar() {
        return this.c.getPlayer();
    }

    public MapleCharacter getPlayer() {
        return this.c.getPlayer();
    }

    public ChannelServer getChannelServer() {
        return this.c.getChannelServer();
    }

    public EventManager getEventManager(String event) {
        return this.c.getChannelServer().getEventSM().getEventManager(event);
    }

    public EventInstanceManager getEventInstance() {
        return this.c.getPlayer().getEventInstance();
    }

    public void warp(int map) {
        MapleMap mapz = getWarpMap(map);
        try {
            this.c.getPlayer().changeMap(mapz, mapz.getPortal(Randomizer.nextInt(mapz.getPortals().size())));
        } catch (Exception e) {
            this.c.getPlayer().changeMap(mapz, mapz.getPortal(0));
        }
    }

    public void warp_Instanced(int map) {
        MapleMap mapz = getMap_Instanced(map);
        try {
            this.c.getPlayer().changeMap(mapz, mapz.getPortal(Randomizer.nextInt(mapz.getPortals().size())));
        } catch (Exception e) {
            this.c.getPlayer().changeMap(mapz, mapz.getPortal(0));
        }
    }

    public void warp(int map, int portal) {
        MapleMap mapz = getWarpMap(map);
        if ((portal != 0) && (map == this.c.getPlayer().getMapId())) {
            Point portalPos = new Point(this.c.getPlayer().getMap().getPortal(portal).getPosition());
            if (portalPos.distanceSq(getPlayer().getTruePosition()) < 90000.0D) {
                this.c.getSession().write(MaplePacketCreator.instantMapWarp((byte) portal));
                this.c.getPlayer().checkFollow();
                this.c.getPlayer().getMap().movePlayer(this.c.getPlayer(), portalPos);
            } else {
                this.c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
            }
        } else {
            this.c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
        }
    }

    public void warpS(int map, int portal) {
        MapleMap mapz = getWarpMap(map);
        this.c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
    }

    public void warp(int map, String portal) {
        MapleMap mapz = getWarpMap(map);
        if ((map == 109060000) || (map == 109060002) || (map == 109060004)) {
            portal = mapz.getSnowballPortal();
        }
        if (map == this.c.getPlayer().getMapId()) {
            Point portalPos = new Point(this.c.getPlayer().getMap().getPortal(portal).getPosition());
            if (portalPos.distanceSq(getPlayer().getTruePosition()) < 90000.0D) {
                this.c.getPlayer().checkFollow();
                this.c.getSession().write(MaplePacketCreator.instantMapWarp((byte) this.c.getPlayer().getMap().getPortal(portal).getId()));
                this.c.getPlayer().getMap().movePlayer(this.c.getPlayer(), new Point(this.c.getPlayer().getMap().getPortal(portal).getPosition()));
            } else {
                this.c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
            }
        } else {
            this.c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
        }
    }

    public void warpS(int map, String portal) {
        MapleMap mapz = getWarpMap(map);
        if ((map == 109060000) || (map == 109060002) || (map == 109060004)) {
            portal = mapz.getSnowballPortal();
        }
        this.c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
    }

    public void warpMap(int mapid, int portal) {
        MapleMap map = getMap(mapid);
        for (MapleCharacter chr : this.c.getPlayer().getMap().getCharactersThreadsafe()) {
            chr.changeMap(map, map.getPortal(portal));
        }
    }

    public void playPortalSE() {
        this.c.getSession().write(MaplePacketCreator.showOwnBuffEffect(0, 7, 1, 1));
    }

    private MapleMap getWarpMap(int map) {
        return ChannelServer.getInstance(this.c.getChannel()).getMapFactory().getMap(map);
    }

    public MapleMap getMap() {
        return this.c.getPlayer().getMap();
    }

    public MapleMap getMap(int map) {
        return getWarpMap(map);
    }

    public MapleMap getMap_Instanced(int map) {
        return this.c.getPlayer().getEventInstance() == null ? getMap(map) : this.c.getPlayer().getEventInstance().getMapInstance(map);
    }

    public void spawnMonster(int id, int qty) {
        spawnMob(id, qty, this.c.getPlayer().getTruePosition());
    }

    public void spawnMobOnMap(int id, int qty, int x, int y, int map) {
        for (int i = 0; i < qty; i++) {
            getMap(map).spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), new Point(x, y));
        }
    }

    public void spawnMob(int id, int qty, int x, int y) {
        spawnMob(id, qty, new Point(x, y));
    }

    public void spawnMob(int id, int x, int y) {
        spawnMob(id, 1, new Point(x, y));
    }

    private void spawnMob(int id, int qty, Point pos) {
        for (int i = 0; i < qty; i++) {
            this.c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), pos);
        }
    }

    public void killMob(int ids) {
        this.c.getPlayer().getMap().killMonster(ids);
    }

    public void killAllMob() {
        this.c.getPlayer().getMap().killAllMonsters(true);
    }

    public void addHP(int delta) {
        this.c.getPlayer().addHP(delta);
    }

    public int getPlayerStat(String type) {
        if (type.equals("LVL")) {
            return this.c.getPlayer().getLevel();
        }
        if (type.equals("STR")) {
            return this.c.getPlayer().getStat().getStr();
        }
        if (type.equals("DEX")) {
            return this.c.getPlayer().getStat().getDex();
        }
        if (type.equals("INT")) {
            return this.c.getPlayer().getStat().getInt();
        }
        if (type.equals("LUK")) {
            return this.c.getPlayer().getStat().getLuk();
        }
        if (type.equals("HP")) {
            return this.c.getPlayer().getStat().getHp();
        }
        if (type.equals("MP")) {
            return this.c.getPlayer().getStat().getMp();
        }
        if (type.equals("MAXHP")) {
            return this.c.getPlayer().getStat().getMaxHp();
        }
        if (type.equals("MAXMP")) {
            return this.c.getPlayer().getStat().getMaxMp();
        }
        if (type.equals("RAP")) {
            return this.c.getPlayer().getRemainingAp();
        }
        if (type.equals("RSP")) {
            return this.c.getPlayer().getRemainingSp();
        }
        if (type.equals("GID")) {
            return this.c.getPlayer().getGuildId();
        }
        if (type.equals("GRANK")) {
            return this.c.getPlayer().getGuildRank();
        }
        if (type.equals("ARANK")) {
            return this.c.getPlayer().getAllianceRank();
        }
        if (type.equals("GM")) {
            return this.c.getPlayer().isGM() ? 1 : 0;
        }
        if (type.equals("ADMIN")) {
            return this.c.getPlayer().isAdmin() ? 1 : 0;
        }
        if (type.equals("GENDER")) {
            return this.c.getPlayer().getGender();
        }
        if (type.equals("FACE")) {
            return this.c.getPlayer().getFace();
        }
        if (type.equals("HAIR")) {
            return this.c.getPlayer().getHair();
        }
        return -1;
    }

    public int getAndroidStat(String type) {
        if (type.equals("HAIR")) {
            return this.c.getPlayer().getAndroid().getHair();
        }
        if (type.equals("FACE")) {
            return this.c.getPlayer().getAndroid().getFace();
        }
        if (type.equals("SKIN")) {
            return this.c.getPlayer().getAndroid().getSkin();
        }
        if (type.equals("GENDER")) {
            return this.c.getPlayer().getAndroid().getGender();
        }
        return -1;
    }

    public String getName() {
        return this.c.getPlayer().getName();
    }

    public boolean haveItem(int itemid) {
        return haveItem(itemid, 1);
    }

    public boolean haveItem(int itemid, int quantity) {
        return haveItem(itemid, quantity, false, true);
    }

    public boolean haveItem(int itemid, int quantity, boolean checkEquipped, boolean greaterOrEquals) {
        return this.c.getPlayer().haveItem(itemid, quantity, checkEquipped, greaterOrEquals);
    }

    public int getItemQuantity(int itemid) {
        return this.c.getPlayer().getItemQuantity(itemid);
    }

    public boolean canHold() {
        for (int i = 1; i <= 5; i++) {
            if (this.c.getPlayer().getInventory(MapleInventoryType.getByType((byte) i)).getNextFreeSlot() <= -1) {
                return false;
            }
        }
        return true;
    }

    public boolean canHoldSlots(int slot) {
        for (int i = 1; i <= 5; i++) {
            if (this.c.getPlayer().getInventory(MapleInventoryType.getByType((byte) i)).isFull(slot)) {
                return false;
            }
        }
        return true;
    }

    public boolean canHold(int itemid) {
        return this.c.getPlayer().getInventory(GameConstants.getInventoryType(itemid)).getNextFreeSlot() > -1;
    }

    public boolean canHold(int itemid, int quantity) {
        return MapleInventoryManipulator.checkSpace(this.c, itemid, quantity, "");
    }

    public MapleQuestStatus getQuestRecord(int id) {
        return this.c.getPlayer().getQuestNAdd(MapleQuest.getInstance(id));
    }

    public MapleQuestStatus getQuestNoRecord(int id) {
        return this.c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(id));
    }

    public byte getQuestStatus(int id) {
        return this.c.getPlayer().getQuestStatus(id);
    }

    public boolean isQuestActive(int id) {
        return getQuestStatus(id) == 1;
    }

    public boolean isQuestFinished(int id) {
        return getQuestStatus(id) == 2;
    }

    public void showQuestMsg(String msg) {
        this.c.getSession().write(MaplePacketCreator.showQuestMsg(msg));
    }

    public void forceStartQuest(int id, String data) {
        MapleQuest.getInstance(id).forceStart(this.c.getPlayer(), 0, data);
    }

    public void forceStartQuest(int id, int data, boolean filler) {
        MapleQuest.getInstance(id).forceStart(this.c.getPlayer(), 0, filler ? String.valueOf(data) : null);
    }

    public void forceStartQuest(int id) {
        MapleQuest.getInstance(id).forceStart(this.c.getPlayer(), 0, null);
    }

    public void forceCompleteQuest(int id) {
        MapleQuest.getInstance(id).forceComplete(getPlayer(), 0);
    }

    public void spawnNpc(int npcId) {
        this.c.getPlayer().getMap().spawnNpc(npcId, this.c.getPlayer().getPosition());
    }

    public void spawnNpc(int npcId, int x, int y) {
        this.c.getPlayer().getMap().spawnNpc(npcId, new Point(x, y));
    }

    public void spawnNpc(int npcId, Point pos) {
        this.c.getPlayer().getMap().spawnNpc(npcId, pos);
    }

    public void removeNpc(int mapid, int npcId) {
        this.c.getChannelServer().getMapFactory().getMap(mapid).removeNpc(npcId);
    }

    public void removeNpc(int npcId) {
        this.c.getPlayer().getMap().removeNpc(npcId);
    }

    public void forceStartReactor(int mapid, int id) {
        MapleMap map = this.c.getChannelServer().getMapFactory().getMap(mapid);

        for (MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            MapleReactor react = (MapleReactor) remo;
            if (react.getReactorId() == id) {
                react.forceStartReactor(this.c);
                break;
            }
        }
    }

    public void destroyReactor(int mapid, int id) {
        MapleMap map = this.c.getChannelServer().getMapFactory().getMap(mapid);

        for (MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            MapleReactor react = (MapleReactor) remo;
            if (react.getReactorId() == id) {
                react.hitReactor(this.c);
                break;
            }
        }
    }

    public void hitReactor(int mapid, int id) {
        MapleMap map = this.c.getChannelServer().getMapFactory().getMap(mapid);

        for (MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            MapleReactor react = (MapleReactor) remo;
            if (react.getReactorId() == id) {
                react.hitReactor(this.c);
                break;
            }
        }
    }

    public int getJob() {
        return this.c.getPlayer().getJob();
    }

    /**
     * 获取当前职业的ID
     *
     * @return
     */
    public int getJobId() {
        return this.c.getPlayer().getJob();
    }

    public String getJobName(int id) {
        return MapleCarnivalChallenge.getJobNameById(id);
    }

    public boolean isBeginnerJob() {
        return ((getJob() == 0) || (getJob() == 1000) || (getJob() == 2000) || (getJob() == 2001) || (getJob() == 2002) || (getJob() == 2003) || (getJob() == 3000) || (getJob() == 3001)) && (getLevel() < 11);
    }

    public int getLevel() {
        return this.c.getPlayer().getLevel();
    }

    public int getFame() {
        return this.c.getPlayer().getFame();
    }

    public void gainFame(int famechange) {
        gainFame(famechange, false);
    }

    public void gainFame(int famechange, boolean show) {
        this.c.getPlayer().gainFame(famechange, show);
    }

    public void getNX(int type) {
        if ((type <= 0) || (type > 2)) {
            type = 2;
        }
        this.c.getPlayer().getCSPoints(type);
    }

    public void gainNX(int amount) {
        this.c.getPlayer().modifyCSPoints(1, amount, true);
    }

    public void gainNX(int type, int amount) {
        if ((type <= 0) || (type > 2)) {
            type = 2;
        }
        this.c.getPlayer().modifyCSPoints(type, amount, true);
    }

    public void gainItemPeriod(int id, short quantity, int period) {
        gainItem(id, quantity, false, period, -1, "");
    }

    public void gainItemPeriod(int id, short quantity, long period, String owner) {
        gainItem(id, quantity, false, period, -1, owner);
    }

    public void gainItem(int id, short quantity) {
        gainItem(id, quantity, false, 0L, -1, "");
    }

    public void gainItem(int id, short quantity, boolean randomStats) {
        gainItem(id, quantity, randomStats, 0L, -1, "");
    }

    public void gainItem(int id, short quantity, boolean randomStats, int slots) {
        gainItem(id, quantity, randomStats, 0L, slots, "");
    }

    public void gainItem(int id, short quantity, long period) {
        gainItem(id, quantity, false, period, -1, "");
    }

    public void gainItem(int id, short quantity, boolean randomStats, long period, int slots) {
        gainItem(id, quantity, randomStats, period, slots, "");
    }

    public void gainItem(int id, short quantity, boolean randomStats, long period, int slots, String owner) {
        gainItem(id, quantity, randomStats, period, slots, owner, this.c);
    }

    public void gainItem(int id, short quantity, boolean randomStats, long period, int slots, String owner, MapleClient cg) {
        if (GameConstants.isLogItem(id)) {
            String itemText = new StringBuilder().append("玩家 ").append(StringUtil.getRightPaddedStr(cg.getPlayer().getName(), ' ', 13)).append(quantity >= 0 ? " 获得道具: " : " 失去道具: ").append(id).append(" 数量: ").append(StringUtil.getRightPaddedStr(String.valueOf(Math.abs(quantity)), ' ', 5)).append(" 道具名字: ").append(getItemName(id)).toString();
            log.info(new StringBuilder().append("[物品] ").append(itemText).toString());
            Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, new StringBuilder().append("[GM Message] ").append(itemText).toString()));
        }
        if (quantity >= 0) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            MapleInventoryType type = GameConstants.getInventoryType(id);
            if (!MapleInventoryManipulator.checkSpace(cg, id, quantity, "")) {
                return;
            }
            if ((type.equals(MapleInventoryType.EQUIP)) && (!GameConstants.isThrowingStar(id)) && (!GameConstants.isBullet(id))) {
                Equip item = (Equip) (randomStats ? ii.randomizeStats((Equip) ii.getEquipById(id)) : ii.getEquipById(id));
                if (period > 0L) {
                    item.setExpiration(System.currentTimeMillis() + period * 24L * 60L * 60L * 1000L);
                }
                if (slots > 0) {
                    item.setUpgradeSlots((byte) (item.getUpgradeSlots() + slots));
                }
                if (owner != null) {
                    item.setOwner(owner);
                }
                item.setGMLog(new StringBuilder().append("脚本获得 ").append(this.id).append(" (").append(this.id2).append(") 地图: ").append(cg.getPlayer().getMapId()).append(" 时间: ").append(FileoutputUtil.CurrentReadable_Time()).toString());
                String name = ii.getName(id);
                if ((id / 10000 == 114) && (name != null) && (name.length() > 0)) {
                    String msg = new StringBuilder().append("恭喜您获得勋章 <").append(name).append(">").toString();
                    cg.getPlayer().dropMessage(-1, msg);
                    cg.getPlayer().dropMessage(5, msg);
                }
                MapleInventoryManipulator.addbyItem(cg, item.copy());
            } else {
                MapleInventoryManipulator.addById(cg, id, quantity, owner == null ? "" : owner, null, period, new StringBuilder().append("脚本获得 ").append(this.id).append(" (").append(this.id2).append(") 地图: ").append(cg.getPlayer().getMapId()).append(" 时间: ").append(FileoutputUtil.CurrentReadable_Time()).toString());
            }
        } else {
            MapleInventoryManipulator.removeById(cg, GameConstants.getInventoryType(id), id, -quantity, true, false);
        }
        cg.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
    }

    public boolean removeItem(int id) {
        if (MapleInventoryManipulator.removeById_Lock(this.c, GameConstants.getInventoryType(id), id)) {
            c.getSession().write(MaplePacketCreator.getShowItemGain(id, (short) -1, true));
            return true;
        }
        return false;
    }

    public void changeMusic(String songName) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange(songName));
    }

    public void worldMessage(String message) {
        worldMessage(6, message);
    }

    public void worldMessage(int type, String message) {
        Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(type, message));
    }

    public void playerMessage(String message) {
        playerMessage(5, message);
    }

    public void mapMessage(String message) {
        mapMessage(5, message);
    }

    public void guildMessage(String message) {
        guildMessage(5, message);
    }

    public void playerMessage(int type, String message) {
        this.c.getPlayer().dropMessage(type, message);
    }

    public void mapMessage(int type, String message) {
        this.c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(type, message));
    }

    public void guildMessage(int type, String message) {
        if (getPlayer().getGuildId() > 0) {
            Guild.guildPacket(getPlayer().getGuildId(), MaplePacketCreator.serverNotice(type, message));
        }
    }

    public void topMessage(String message) {
        this.c.getSession().write(UIPacket.getTopMsg(message));
    }

    public MapleGuild getGuild() {
        return getGuild(getPlayer().getGuildId());
    }

    public MapleGuild getGuild(int guildid) {
        return Guild.getGuild(guildid);
    }

    public MapleParty getParty() {
        return this.c.getPlayer().getParty();
    }

    public int getCurrentPartyId(int mapid) {
        return getMap(mapid).getCurrentPartyId();
    }

    public boolean isLeader() {
        if (getPlayer().getParty() == null) {
            return false;
        }
        return getParty().getLeader().getId() == this.c.getPlayer().getId();
    }

    public boolean isAllPartyMembersAllowedJob(int job) {
        if (this.c.getPlayer().getParty() == null) {
            return false;
        }
        for (MaplePartyCharacter mem : this.c.getPlayer().getParty().getMembers()) {
            if (mem.getJobId() / 100 != job) {
                return false;
            }
        }
        return true;
    }

    public boolean allMembersHere() {
        if (this.c.getPlayer().getParty() == null) {
            return false;
        }
        for (MaplePartyCharacter mem : this.c.getPlayer().getParty().getMembers()) {
            MapleCharacter chr = this.c.getPlayer().getMap().getCharacterById(mem.getId());
            if (chr == null) {
                return false;
            }
        }
        return true;
    }

    public void warpParty(int mapId) {
        if ((getPlayer().getParty() == null) || (getPlayer().getParty().getMembers().size() == 1)) {
            warp(mapId, 0);
            return;
        }
        MapleMap target = getMap(mapId);
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if ((curChar != null) && ((curChar.getMapId() == cMap) || (curChar.getEventInstance() == getPlayer().getEventInstance()))) {
                curChar.changeMap(target, target.getPortal(0));
            }
        }
    }

    public void warpParty(int mapId, int portal) {
        if ((getPlayer().getParty() == null) || (getPlayer().getParty().getMembers().size() == 1)) {
            if (portal < 0) {
                warp(mapId);
            } else {
                warp(mapId, portal);
            }
            return;
        }
        boolean rand = portal < 0;
        MapleMap target = getMap(mapId);
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if ((curChar != null) && ((curChar.getMapId() == cMap) || (curChar.getEventInstance() == getPlayer().getEventInstance()))) {
                if (rand) {
                    try {
                        curChar.changeMap(target, target.getPortal(Randomizer.nextInt(target.getPortals().size())));
                    } catch (Exception e) {
                        curChar.changeMap(target, target.getPortal(0));
                    }
                } else {
                    curChar.changeMap(target, target.getPortal(portal));
                }
            }
        }
    }

    public void warpParty_Instanced(int mapId) {
        if ((getPlayer().getParty() == null) || (getPlayer().getParty().getMembers().size() == 1)) {
            warp_Instanced(mapId);
            return;
        }
        MapleMap target = getMap_Instanced(mapId);

        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if ((curChar != null) && ((curChar.getMapId() == cMap) || (curChar.getEventInstance() == getPlayer().getEventInstance()))) {
                curChar.changeMap(target, target.getPortal(0));
            }
        }
    }

    public void gainMeso(int gain) {
        this.c.getPlayer().gainMeso(gain, true, true);
    }

    public void gainExp(int gain) {
        this.c.getPlayer().gainExp(gain, true, true, true);
    }

    public void gainExpR(int gain) {
        this.c.getPlayer().gainExp(gain * this.c.getChannelServer().getExpRate(), true, true, true);
    }

    public void givePartyItems(int id, short quantity, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            if (quantity >= 0) {
                MapleInventoryManipulator.addById(chr.getClient(), id, quantity, new StringBuilder().append("Received from party interaction ").append(id).append(" (").append(this.id2).append(")").toString());
            } else {
                MapleInventoryManipulator.removeById(chr.getClient(), GameConstants.getInventoryType(id), id, -quantity, true, false);
            }
            chr.getClient().getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
        }
    }

    public void addPartyTrait(String t, int e, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            chr.getTrait(MapleTrait.MapleTraitType.valueOf(t)).addExp(e, chr);
        }
    }

    public void addPartyTrait(String t, int e) {
        if ((getPlayer().getParty() == null) || (getPlayer().getParty().getMembers().size() == 1)) {
            addTrait(t, e);
            return;
        }
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if ((curChar != null) && ((curChar.getMapId() == cMap) || (curChar.getEventInstance() == getPlayer().getEventInstance()))) {
                curChar.getTrait(MapleTrait.MapleTraitType.valueOf(t)).addExp(e, curChar);
            }
        }
    }

    public void addTrait(String t, int e) {
        getPlayer().getTrait(MapleTrait.MapleTraitType.valueOf(t)).addExp(e, getPlayer());
    }

    public void givePartyItems(int id, short quantity) {
        givePartyItems(id, quantity, false);
    }

    public void givePartyItems(int id, short quantity, boolean removeAll) {
        if ((getPlayer().getParty() == null) || (getPlayer().getParty().getMembers().size() == 1)) {
            gainItem(id, (short) (removeAll ? -getPlayer().itemQuantity(id) : quantity));
            return;
        }
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if ((curChar != null) && ((curChar.getMapId() == cMap) || (curChar.getEventInstance() == getPlayer().getEventInstance()))) {
                gainItem(id, (short) (removeAll ? -curChar.itemQuantity(id) : quantity), false, 0L, 0, "", curChar.getClient());
            }
        }
    }

    public void givePartyExp_PQ(int maxLevel, double mod, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            int amount = (int) Math.round(GameConstants.getExpNeededForLevel(chr.getLevel() > maxLevel ? maxLevel + (maxLevel - chr.getLevel()) / 10 : chr.getLevel()) / (Math.min(chr.getLevel(), maxLevel) / 5.0D) / (mod * 2.0D));
            chr.gainExp(amount * this.c.getChannelServer().getExpRate(), true, true, true);
        }
    }

    public void gainExp_PQ(int maxLevel, double mod) {
        int amount = (int) Math.round(GameConstants.getExpNeededForLevel(getPlayer().getLevel() > maxLevel ? maxLevel + getPlayer().getLevel() / 10 : getPlayer().getLevel()) / (Math.min(getPlayer().getLevel(), maxLevel) / 10.0D) / mod);
        gainExp(amount * this.c.getChannelServer().getExpRate());
    }

    public void givePartyExp_PQ(int maxLevel, double mod) {
        if ((getPlayer().getParty() == null) || (getPlayer().getParty().getMembers().size() == 1)) {
            int amount = (int) Math.round(GameConstants.getExpNeededForLevel(getPlayer().getLevel() > maxLevel ? maxLevel + getPlayer().getLevel() / 10 : getPlayer().getLevel()) / (Math.min(getPlayer().getLevel(), maxLevel) / 10.0D) / mod);
            gainExp(amount * this.c.getChannelServer().getExpRate());
            return;
        }
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if ((curChar != null) && ((curChar.getMapId() == cMap) || (curChar.getEventInstance() == getPlayer().getEventInstance()))) {
                int amount = (int) Math.round(GameConstants.getExpNeededForLevel(curChar.getLevel() > maxLevel ? maxLevel + curChar.getLevel() / 10 : curChar.getLevel()) / (Math.min(curChar.getLevel(), maxLevel) / 10.0D) / mod);
                curChar.gainExp(amount * this.c.getChannelServer().getExpRate(), true, true, true);
            }
        }
    }

    public void givePartyExp(int amount, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            chr.gainExp(amount * this.c.getChannelServer().getExpRate(), true, true, true);
        }
    }

    public void givePartyExp(int amount) {
        if ((getPlayer().getParty() == null) || (getPlayer().getParty().getMembers().size() == 1)) {
            gainExp(amount * this.c.getChannelServer().getExpRate());
            return;
        }
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if ((curChar != null) && ((curChar.getMapId() == cMap) || (curChar.getEventInstance() == getPlayer().getEventInstance()))) {
                curChar.gainExp(amount * this.c.getChannelServer().getExpRate(), true, true, true);
            }
        }
    }

    public void givePartyNX(int amount, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            chr.modifyCSPoints(1, amount, true);
        }
    }

    public void givePartyNX(int amount) {
        if ((getPlayer().getParty() == null) || (getPlayer().getParty().getMembers().size() == 1)) {
            gainNX(amount);
            return;
        }
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if ((curChar != null) && ((curChar.getMapId() == cMap) || (curChar.getEventInstance() == getPlayer().getEventInstance()))) {
                curChar.modifyCSPoints(1, amount, true);
            }
        }
    }

    public void endPartyQuest(int amount, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            chr.endPartyQuest(amount);
        }
    }

    public void endPartyQuest(int amount) {
        if ((getPlayer().getParty() == null) || (getPlayer().getParty().getMembers().size() == 1)) {
            getPlayer().endPartyQuest(amount);
            return;
        }
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if ((curChar != null) && ((curChar.getMapId() == cMap) || (curChar.getEventInstance() == getPlayer().getEventInstance()))) {
                curChar.endPartyQuest(amount);
            }
        }
    }

    public void removeFromParty(int id, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            int possesed = chr.getInventory(GameConstants.getInventoryType(id)).countById(id);
            if (possesed > 0) {
                MapleInventoryManipulator.removeById(this.c, GameConstants.getInventoryType(id), id, possesed, true, false);
                chr.getClient().getSession().write(MaplePacketCreator.getShowItemGain(id, (short) (-possesed), true));
            }
        }
    }

    public void removeFromParty(int id) {
        givePartyItems(id, (short) 0, true);
    }

    public void useSkill(int skill, int level) {
        if (level <= 0) {
            return;
        }
        SkillFactory.getSkill(skill).getEffect(level).applyTo(this.c.getPlayer());
    }

    public void useItem(int id) {
        MapleItemInformationProvider.getInstance().getItemEffect(id).applyTo(this.c.getPlayer());
        this.c.getSession().write(UIPacket.getStatusMsg(id));
    }

    public void cancelItem(int id) {
        this.c.getPlayer().cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(id), false, -1L);
    }

    public int getMorphState() {
        return this.c.getPlayer().getMorphState();
    }

    public void removeAll(int id) {
        this.c.getPlayer().removeAll(id);
    }

    public void gainCloseness(int closeness, int index) {
        MaplePet pet = getPlayer().getPet(index);
        if (pet != null) {
            pet.setCloseness(pet.getCloseness() + closeness * getChannelServer().getTraitRate());
            getClient().getSession().write(PetPacket.updatePet(pet, getPlayer().getInventory(MapleInventoryType.CASH).getItem((short) (byte) pet.getInventoryPosition()), true));
        }
    }

    public void gainClosenessAll(int closeness) {
        for (MaplePet pet : getPlayer().getPets()) {
            if ((pet != null) && (pet.getSummoned())) {
                pet.setCloseness(pet.getCloseness() + closeness);
                getClient().getSession().write(PetPacket.updatePet(pet, getPlayer().getInventory(MapleInventoryType.CASH).getItem((short) (byte) pet.getInventoryPosition()), true));
            }
        }
    }

    public void resetMap(int mapid) {
        getMap(mapid).resetFully();
    }

    public void openNpc(int id) {
        getClient().removeClickedNPC();
        NPCScriptManager.getInstance().start(getClient(), id);
    }

    public void openNpc(MapleClient cg, int id) {
        cg.removeClickedNPC();
        NPCScriptManager.getInstance().start(cg, id);
    }

    public void openNpc(int id, int npcMode) {
        getClient().removeClickedNPC();
        NPCScriptManager.getInstance().start(getClient(), id, npcMode);
    }

    /**
     * 获取当前地图的ID
     *
     * @return
     */
    public int getMapId() {
        return this.c.getPlayer().getMap().getId();
    }

    public boolean haveMonster(int mobid) {
        for (MapleMapObject obj : this.c.getPlayer().getMap().getAllMonstersThreadsafe()) {
            MapleMonster mob = (MapleMonster) obj;
            if (mob.getId() == mobid) {
                return true;
            }
        }
        return false;
    }

    public int getChannelNumber() {
        return this.c.getChannel();
    }

    /**
     * 根据地图ID获取该地图的怪物数量
     *
     * @param mapid 地图ID
     * @return
     */
    public int getMonsterCount(int mapid) {
        return this.c.getChannelServer().getMapFactory().getMap(mapid).getNumMonsters();
    }

    public void teachSkill(int id, int level, byte masterlevel) {
        getPlayer().changeSkillLevel(SkillFactory.getSkill(id), level, masterlevel);
    }

    public void teachSkill(int id, int level) {
        Skill skil = SkillFactory.getSkill(id);
        if (getPlayer().getSkillLevel(skil) > level) {
            level = getPlayer().getSkillLevel(skil);
        }
        getPlayer().changeSkillLevel(skil, level, (byte) skil.getMaxLevel());
    }

    /**
     * 根据地图ID获取该地图上的玩家数量
     *
     * @param mapid 地图ID
     * @return
     */
    public int getPlayerCount(int mapid) {
        return this.c.getChannelServer().getMapFactory().getMap(mapid).getCharactersSize();
    }

    public void dojo_getUp() {
        this.c.getSession().write(MaplePacketCreator.updateInfoQuest(1207, "pt=1;min=4;belt=1;tuto=1"));
        this.c.getSession().write(MaplePacketCreator.Mulung_DojoUp2());
        this.c.getSession().write(MaplePacketCreator.instantMapWarp((byte) 6));
    }

    public boolean dojoAgent_NextMap(boolean dojo, boolean fromresting) {
        if (dojo) {
            return Event_DojoAgent.warpNextMap(this.c.getPlayer(), fromresting, this.c.getPlayer().getMap());
        }
        return Event_DojoAgent.warpNextMap_Agent(this.c.getPlayer(), fromresting);
    }

    public boolean dojoAgent_NextMap(boolean dojo, boolean fromresting, int mapid) {
        if (dojo) {
            return Event_DojoAgent.warpNextMap(this.c.getPlayer(), fromresting, getMap(mapid));
        }
        return Event_DojoAgent.warpNextMap_Agent(this.c.getPlayer(), fromresting);
    }

    public int dojo_getPts() {
        return this.c.getPlayer().getIntNoRecord(150100);
    }

    public MapleEvent getEvent(String loc) {
        return this.c.getChannelServer().getEvent(MapleEventType.valueOf(loc));
    }

    public int getSavedLocation(String loc) {
        Integer ret = Integer.valueOf(this.c.getPlayer().getSavedLocation(SavedLocationType.fromString(loc)));
        if ((ret == null) || (ret.intValue() == -1)) {
            return 100000000;
        }
        return ret.intValue();
    }

    public void saveLocation(String loc) {
        this.c.getPlayer().saveLocation(SavedLocationType.fromString(loc));
    }

    public void saveReturnLocation(String loc) {
        this.c.getPlayer().saveLocation(SavedLocationType.fromString(loc), this.c.getPlayer().getMap().getReturnMap().getId());
    }

    public void clearSavedLocation(String loc) {
        this.c.getPlayer().clearSavedLocation(SavedLocationType.fromString(loc));
    }

    public void summonMsg(String msg) {
        if (!this.c.getPlayer().hasSummon()) {
            playerSummonHint(true);
        }
        this.c.getSession().write(UIPacket.summonMessage(msg));
    }

    public void summonMsg(int type) {
        if (!this.c.getPlayer().hasSummon()) {
            playerSummonHint(true);
        }
        this.c.getSession().write(UIPacket.summonMessage(type));
    }

    public void showInstruction(String msg, int width, int height) {
        this.c.getSession().write(MaplePacketCreator.sendHint(msg, width, height));
    }

    public void playerSummonHint(boolean summon) {
        this.c.getPlayer().setHasSummon(summon);
        this.c.getSession().write(UIPacket.summonHelper(summon));
    }

    public String getInfoQuest(int id) {
        return this.c.getPlayer().getInfoQuest(id);
    }

    public void updateInfoQuest(int id, String data) {
        this.c.getPlayer().updateInfoQuest(id, data);
    }

    public boolean getEvanIntroState(String data) {
        return getInfoQuest(22013).equals(data);
    }

    public void updateEvanIntroState(String data) {
        updateInfoQuest(22013, data);
    }

    public void Aran_Start() {
        this.c.getSession().write(UIPacket.Aran_Start());
    }

    public void evanTutorial(String data, int v1) {
        this.c.getSession().write(MaplePacketCreator.getEvanTutorial(data));
    }

    public void AranTutInstructionalBubble(String data) {
        this.c.getSession().write(UIPacket.AranTutInstructionalBalloon(data));
    }

    public void ShowWZEffect(String data) {
        this.c.getSession().write(UIPacket.AranTutInstructionalBalloon(data));
    }

    public void showWZEffect(String data) {
        this.c.getSession().write(UIPacket.ShowWZEffect(data));
    }

    public void EarnTitleMsg(String data) {
        this.c.getSession().write(UIPacket.EarnTitleMsg(data));
    }

    public void EnableUI(short i) {
        this.c.getSession().write(UIPacket.IntroEnableUI(i));
    }

    public void DisableUI(boolean enabled) {
        this.c.getSession().write(UIPacket.IntroDisableUI(enabled));
    }

    public void MovieClipIntroUI(boolean enabled) {
        this.c.getSession().write(UIPacket.IntroDisableUI(enabled));
        this.c.getSession().write(UIPacket.IntroLock(enabled));
    }

    public MapleInventoryType getInvType(int i) {
        return MapleInventoryType.getByType((byte) i);
    }

    public String getItemName(int id) {
        return MapleItemInformationProvider.getInstance().getName(id);
    }

    public void gainPet(int id, String name, int level, int closeness, int fullness, long period, short flags) {
        if ((id > 5000200) || (id < 5000000)) {
            id = 5000000;
        }
        if (level > 30) {
            level = 30;
        }
        if (closeness > 30000) {
            closeness = 30000;
        }
        if (fullness > 100) {
            fullness = 100;
        }
        try {
            MapleInventoryManipulator.addById(this.c, id, (short) 1, "", MaplePet.createPet(id, name, level, closeness, fullness, MapleInventoryIdentifier.getInstance(), id == 5000054 ? (int) period : 0, flags, 0), 45L, new StringBuilder().append("Pet from interaction ").append(id).append(" (").append(this.id2).append(")").append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    public void removeSlot(int invType, byte slot, short quantity) {
        MapleInventoryManipulator.removeFromSlot(this.c, getInvType(invType), (short) slot, quantity, true);
    }

    public void gainGP(int gp) {
        if (getPlayer().getGuildId() <= 0) {
            return;
        }
        Guild.gainGP(getPlayer().getGuildId(), gp);
    }

    public int getGP() {
        if (getPlayer().getGuildId() <= 0) {
            return 0;
        }
        return Guild.getGP(getPlayer().getGuildId());
    }

    public void showMapEffect(String path) {
        getClient().getSession().write(UIPacket.MapEff(path));
    }

    public int itemQuantity(int itemid) {
        return getPlayer().itemQuantity(itemid);
    }

    public EventInstanceManager getDisconnected(String event) {
        EventManager em = getEventManager(event);
        if (em == null) {
            return null;
        }
        for (EventInstanceManager eim : em.getInstances()) {
            if ((eim.isDisconnected(this.c.getPlayer())) && (eim.getPlayerCount() > 0)) {
                return eim;
            }
        }
        return null;
    }

    public boolean isAllReactorState(int reactorId, int state) {
        boolean ret = false;
        for (MapleReactor r : getMap().getAllReactorsThreadsafe()) {
            if (r.getReactorId() == reactorId) {
                ret = r.getState() == state;
            }
        }
        return ret;
    }

    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public void spawnMonster(int id) {
        spawnMonster(id, 1, getPlayer().getTruePosition());
    }

    public void spawnMonster(int id, int x, int y) {
        spawnMonster(id, 1, new Point(x, y));
    }

    public void spawnMonster(int id, int qty, int x, int y) {
        spawnMonster(id, qty, new Point(x, y));
    }

    public void spawnMonster(int id, int qty, Point pos) {
        for (int i = 0; i < qty; i++) {
            getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), pos);
        }
    }

    public void sendNPCText(String text, int npc) {
        getMap().broadcastMessage(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 00", (byte) 0));
    }

    public boolean getTempFlag(int flag) {
        return (this.c.getChannelServer().getTempFlag() & flag) == flag;
    }

    public void logPQ(String text) {
    }

    public void outputFileError(Throwable t) {
        FileoutputUtil.outputFileError("log\\Script_Except.log", t);
    }

    public void trembleEffect(int type, int delay) {
        this.c.getSession().write(MaplePacketCreator.trembleEffect(type, delay));
    }

    public int nextInt(int arg0) {
        return Randomizer.nextInt(arg0);
    }

    public MapleQuest getQuest(int arg0) {
        return MapleQuest.getInstance(arg0);
    }

    public void achievement(int a) {
        this.c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.achievementRatio(a));
    }

    public MapleInventory getInventory(int type) {
        return this.c.getPlayer().getInventory(MapleInventoryType.getByType((byte) type));
    }

    public boolean isGMS() {
        return GameConstants.GMS;
    }

    public int randInt(int arg0) {
        return Randomizer.nextInt(arg0);
    }

    public void sendDirectionStatus(int key, int value) {
        this.c.getSession().write(UIPacket.getDirectionInfo(key, value));
        this.c.getSession().write(UIPacket.getDirectionStatus(true));
    }

    public void sendDirectionInfo(String data) {
        this.c.getSession().write(UIPacket.getDirectionInfo(data, 2000, 0, -100, 0));
        this.c.getSession().write(UIPacket.getDirectionInfo(1, 2000));
    }

    public int getProfessions() {
        int ii = 0;

        for (int i = 0; i < 5; i++) {
            int skillId = 92000000 + i * 10000;
            if (this.c.getPlayer().getProfessionLevel(skillId) > 0) {
                ii++;
            }
        }
        return ii;
    }

    /**
     * 设置VIP
     *
     * @param vip 等级
     */
    public void setVip(int vip) {
        setVip(vip, 7);
    }

    /**
     * 设置VIP
     *
     * @param vip 等级
     * @param period 时间
     */
    public void setVip(int vip, long period) {
        this.c.getPlayer().setVip(vip);
        if (period > 0L) {
            this.c.getPlayer().setViptime(period);
        }
    }

    /**
     * 获取当前VIP等级
     *
     * @return
     */
    public int getVip() {
        return this.c.getPlayer().getVip();
    }

    /**
     * 判断是否VIP
     *
     * @return
     */
    public boolean isVip() {
        return this.c.getPlayer().getVip() > 0;
    }

    /**
     * 设置VIP时间
     *
     * @param period
     */
    public void setViptime(long period) {
        if (period != 0L) {
            this.c.getPlayer().setViptime(period);
        }
    }

    /**
     * 设置VIP成长值
     * @param vipczz 
     */
    public void setVipczz(int vipczz) {
        c.getPlayer().setVipczz(vipczz);
    }

    /**
     * 获取VIP的成长值
     * @return 
     */
    public int getVipczz() {
        return c.getPlayer().getVipczz();
    }

    public int getBossLog(String bossid) {
        return this.c.getPlayer().getBossLog(bossid);
    }

    public int getBossLog(String bossid, int type) {
        return this.c.getPlayer().getBossLog(bossid, type);
    }

    public void setBossLog(String bossid) {
        this.c.getPlayer().setBossLog(bossid);
    }

    public void setBossLog(String bossid, int type) {
        this.c.getPlayer().setBossLog(bossid, type);
    }

    public void setBossLog(String bossid, int type, int count) {
        this.c.getPlayer().setBossLog(bossid, type, count);
    }

    public void resetBossLog(String bossid) {
        this.c.getPlayer().resetBossLog(bossid);
    }

    public void resetBossLog(String bossid, int type) {
        this.c.getPlayer().resetBossLog(bossid, type);
    }

    public void getClock(int time) {
        this.c.getSession().write(MaplePacketCreator.getClock(time));
    }

    /**
     * 打开指定网址
     *
     * @param web 网址
     */
    public void openWeb(String web) {
        this.c.getSession().write(MaplePacketCreator.openWeb(web));
    }

    public boolean isCanPvp() {
        return this.c.getChannelServer().isCanPvp();
    }

    public void showDoJangRank() {
        this.c.getSession().write(MaplePacketCreator.showDoJangRank());
    }

    public int MarrageChecking() {
        if (getPlayer().getParty() == null) {
            return -1;
        }
        if (getPlayer().getMarriageId() > 0) {
            return 0;
        }
        if (getPlayer().getParty().getMembers().size() != 2) {
            return 1;
        }
        if ((getPlayer().getGender() == 0) && (!getPlayer().haveItem(1050121)) && (!getPlayer().haveItem(1050122)) && (!getPlayer().haveItem(1050113))) {
            return 5;
        }
        if ((getPlayer().getGender() == 1) && (!getPlayer().haveItem(1051129)) && (!getPlayer().haveItem(1051130)) && (!getPlayer().haveItem(1051114))) {
            return 5;
        }
        if (!getPlayer().haveItem(1112001)) {
            return 6;
        }
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            if (chr.getId() == getPlayer().getId()) {
                continue;
            }
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar == null) {
                return 2;
            }
            if (curChar.getMarriageId() > 0) {
                return 3;
            }
            if (curChar.getGender() == getPlayer().getGender()) {
                return 4;
            }
            if ((curChar.getGender() == 0) && (!curChar.haveItem(1050121)) && (!curChar.haveItem(1050122)) && (!curChar.haveItem(1050113))) {
                return 5;
            }
            if ((curChar.getGender() == 1) && (!curChar.haveItem(1051129)) && (!curChar.haveItem(1051130)) && (!curChar.haveItem(1051114))) {
                return 5;
            }
            if (!curChar.haveItem(1112001)) {
                return 6;
            }
        }
        return 9;
    }

    public int getPartyFormID() {
        int curCharID = -1;
        if (getPlayer().getParty() == null) {
            curCharID = -1;
        } else if (getPlayer().getMarriageId() > 0) {
            curCharID = -2;
        } else if (getPlayer().getParty().getMembers().size() != 2) {
            curCharID = -3;
        }
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            if (chr.getId() == getPlayer().getId()) {
                continue;
            }
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar == null) {
                curCharID = -4;
            } else {
                curCharID = chr.getId();
            }
        }
        return curCharID;
    }
    public void setattack(final int attack, final int minmapid, final int maxmapid) {
        c.getPlayer().setattack(1, attack, minmapid, maxmapid);
    }

    /**
     * 获取GM等级
     *
     * @return
     */
    public int getGMLevel() {
        return this.c.getPlayer().getGMLevel();
    }

    public void startLieDetector(boolean isItem) {
        this.c.getPlayer().startLieDetector(isItem);
    }
}