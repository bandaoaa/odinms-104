package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import client.MapleQuestStatus;
import client.MapleStat;
import client.MapleTrait;
import client.MapleTrait.MapleTraitType;
import client.PlayerStats;
import client.Skill;
import client.SkillFactory;
import client.anticheat.CheatTracker;
import client.anticheat.CheatingOffense;
import client.inventory.Equip;
import client.inventory.Equip.ScrollResult;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MapleMount;
import client.inventory.MaplePet;
import constants.GameConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.channel.PlayerStorage;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.World.Broadcast;
import java.awt.Point;
import java.awt.Rectangle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

import scripting.EventInstanceManager;
import scripting.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.RandomRewards;
import server.Randomizer;
import server.ScriptedItem;
import server.StructItemOption;
import server.StructPotentialItem;
import server.StructRewardItem;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.FieldLimitType;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.SavedLocationType;
import server.quest.MapleQuest;
import server.shops.HiredMerchant;
import server.shops.IMaplePlayerShop;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.MTSCSPacket;
import tools.packet.PlayerShopPacket;

public class InventoryHandler {

    public static final int OWL_ID = 1;

    public static void ItemMove(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().hasBlockedInventory()) {
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        c.getPlayer().updateTick(slea.readInt());
        MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
        short src = slea.readShort();
        short dst = slea.readShort();
        short quantity = slea.readShort();
        if ((src < 0) && (dst > 0)) {
            MapleInventoryManipulator.unequip(c, src, dst);
        } else if (dst < 0) {
            MapleInventoryManipulator.equip(c, src, dst);
        } else if (dst == 0) {
            MapleInventoryManipulator.drop(c, type, src, quantity);
        } else {
            MapleInventoryManipulator.move(c, type, src, dst);
        }
    }

    public static void SwitchBag(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().hasBlockedInventory()) {
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        c.getPlayer().updateTick(slea.readInt());
        short src = (short) slea.readInt();
        short dst = (short) slea.readInt();
        if ((src < 100) || (dst < 100)) {
            return;
        }
        MapleInventoryManipulator.move(c, MapleInventoryType.ETC, src, dst);
    }

    public static void MoveBag(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().hasBlockedInventory()) {
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        c.getPlayer().updateTick(slea.readInt());
        boolean srcFirst = slea.readInt() > 0;
        short dst = (short) slea.readInt();
        if (slea.readByte() != 4) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        short src = slea.readShort();
        MapleInventoryManipulator.move(c, MapleInventoryType.ETC, srcFirst ? dst : src, srcFirst ? src : dst);
    }

    public static void ItemSort(LittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        MapleInventoryType pInvType = MapleInventoryType.getByType(slea.readByte());
        if ((pInvType == MapleInventoryType.UNDEFINED) || (c.getPlayer().hasBlockedInventory())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        MapleInventory pInv = c.getPlayer().getInventory(pInvType);
        boolean sorted = false;
        while (!sorted) {
            byte freeSlot = (byte) pInv.getNextFreeSlot();
            if (freeSlot != -1) {
                byte itemSlot = -1;
                for (byte i = (byte) (freeSlot + 1); i <= pInv.getSlotLimit(); i = (byte) (i + 1)) {
                    if (pInv.getItem((short) i) != null) {
                        itemSlot = i;
                        break;
                    }
                }
                if (itemSlot > 0) {
                    MapleInventoryManipulator.move(c, pInvType, (short) itemSlot, (short) freeSlot);
                } else {
                    sorted = true;
                }
            } else {
                sorted = true;
            }
        }
        c.getSession().write(MaplePacketCreator.finishedSort(pInvType.getType()));
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static void ItemGather(LittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        if (c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        byte mode = slea.readByte();
        if (mode == 5) {
            c.getPlayer().dropMessage(1, "特殊栏道具暂不开放已种类道具排列.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        MapleInventoryType invType = MapleInventoryType.getByType(mode);
        MapleInventory Inv = c.getPlayer().getInventory(invType);

        List<Item> itemMap = new LinkedList<Item>();
        for (Item item : Inv.list()) {
            itemMap.add(item.copy());
        }
        for (Item itemStats : itemMap) {
            MapleInventoryManipulator.removeFromSlot(c, invType, itemStats.getPosition(), itemStats.getQuantity(), true, false);
        }

        List<Item> sortedItems = sortItems(itemMap);
        for (Item item : sortedItems) {
            MapleInventoryManipulator.addFromDrop(c, item, false);
        }
        c.getSession().write(MaplePacketCreator.finishedGather(mode));
        c.getSession().write(MaplePacketCreator.enableActions());
        itemMap.clear();
        sortedItems.clear();
    }

    private static List<Item> sortItems(List<Item> passedMap) {
        List<Integer> itemIds = new ArrayList<Integer>();
        for (Item item : passedMap) {
            itemIds.add(item.getItemId());
        }
        Collections.sort(itemIds);

        List<Item> sortedList = new LinkedList<Item>();
        for (Integer val : itemIds) {
            for (Item item : passedMap) {
                if (val.intValue() == item.getItemId()) {
                    sortedList.add(item);
                    passedMap.remove(item);
                    break;
                }
            }
        }

        return sortedList;
    }

    public static boolean UseRewardItem(byte slot, int itemId, MapleClient c, MapleCharacter chr) {
        Item toUse = c.getPlayer().getInventory(GameConstants.getInventoryType(itemId)).getItem((short) slot);
        c.getSession().write(MaplePacketCreator.enableActions());
        if ((toUse != null) && (toUse.getQuantity() >= 1) && (toUse.getItemId() == itemId) && (!chr.hasBlockedInventory())) {
            if ((chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot() > -1) && (chr.getInventory(MapleInventoryType.USE).getNextFreeSlot() > -1) && (chr.getInventory(MapleInventoryType.SETUP).getNextFreeSlot() > -1) && (chr.getInventory(MapleInventoryType.ETC).getNextFreeSlot() > -1)) {
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                if (itemId == 2028048) {
                    int mesars = 5000000;
                    if ((mesars > 0) && (chr.getMeso() < 2147483647 - mesars)) {
                        int gainmes = Randomizer.nextInt(mesars);
                        chr.gainMeso(gainmes, true, true);
                        c.getSession().write(MTSCSPacket.sendMesobagSuccess(gainmes));
                        MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(itemId), itemId, 1, false, false);
                        return true;
                    }
                    chr.dropMessage(1, "金币已达到上限无法使用这个道具.");
                    return false;
                }

                Pair<Integer, List<StructRewardItem>> rewards = ii.getRewardItem(itemId);

                if ((rewards != null) && (rewards.getLeft() > 0)) {
                    while (true) {
                        for (StructRewardItem reward : rewards.getRight()) {
                            if ((reward.prob > 0) && (Randomizer.nextInt(((Integer) rewards.getLeft()).intValue()) < reward.prob)) {
                                if (GameConstants.getInventoryType(reward.itemid) == MapleInventoryType.EQUIP) {
                                    Item item = ii.getEquipById(reward.itemid);
                                    if (reward.period > 0L) {
                                        item.setExpiration(System.currentTimeMillis() + reward.period * 60L * 60L * 10L);
                                    }
                                    item.setGMLog(new StringBuilder().append("Reward item: ").append(itemId).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                                    if (chr.isGM()) {
                                        chr.dropMessage(5, new StringBuilder().append("打开道具获得: ").append(item.getItemId()).toString());
                                    }
                                    MapleInventoryManipulator.addbyItem(c, item);
                                } else {
                                    if (chr.isGM()) {
                                        chr.dropMessage(5, new StringBuilder().append("打开道具获得: ").append(reward.itemid).append(" - ").append(reward.quantity).toString());
                                    }
                                    MapleInventoryManipulator.addById(c, reward.itemid, reward.quantity, new StringBuilder().append("Reward item: ").append(itemId).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                                }
                                MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(itemId), itemId, 1, false, false);

                                c.getSession().write(MaplePacketCreator.showRewardItemAnimation(reward.itemid, reward.effect));
                                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showRewardItemAnimation(reward.itemid, reward.effect, chr.getId()), false);
                                return true;
                            }
                        }
                    }
                }
                chr.dropMessage(6, "出现未知错误.");
            } else {
                chr.dropMessage(6, "背包空间不足。");
            }
        }
        return false;
    }

    public static void UseItem(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (!chr.isAlive()) || (chr.getMapId() == 749040100) || (chr.getMap() == null) || (chr.hasDisease(MapleDisease.POTION)) || (chr.hasBlockedInventory()) || (chr.inPVP())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        long time = System.currentTimeMillis();
        if (chr.getNextConsume() > time) {
            chr.dropMessage(5, "暂时无法使用这个道具，请稍后在试。");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        c.getPlayer().updateTick(slea.readInt());
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        Item toUse = chr.getInventory(MapleInventoryType.USE).getItem((short) slot);

        if ((toUse == null) || (toUse.getQuantity() < 1) || (toUse.getItemId() != itemId)) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if ((itemId >= 2003516) && (itemId <= 2003519)) {
            chr.dropMessage(1, "无法使用这个道具效果");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) {
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
                if (chr.getMap().getConsumeItemCoolTime() > 0) {
                    chr.setNextConsume(time + chr.getMap().getConsumeItemCoolTime() * 1000);
                }
            }
        } else {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static void UseCosmetic(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (!chr.isAlive()) || (chr.getMap() == null) || (chr.hasBlockedInventory()) || (chr.inPVP())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        Item toUse = chr.getInventory(MapleInventoryType.USE).getItem((short) slot);

        if ((toUse == null) || (toUse.getQuantity() < 1) || (toUse.getItemId() != itemId) || (itemId / 10000 != 254) || (itemId / 1000 % 10 != chr.getGender())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
        }
    }

    public static void UseReturnScroll(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if ((!chr.isAlive()) || (chr.getMapId() == 749040100) || (chr.hasBlockedInventory()) || (chr.isInBlockedMap()) || (chr.inPVP())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        c.getPlayer().updateTick(slea.readInt());
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        Item toUse = chr.getInventory(MapleInventoryType.USE).getItem((short) slot);
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if ((toUse == null) || (toUse.getQuantity() < 1) || (toUse.getItemId() != itemId)) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) {
            if (ii.getItemEffect(toUse.getItemId()).applyReturnScroll(chr)) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        } else {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static void UseMagnify(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {//放大镜处理
        chr.updateTick(slea.readInt());
        chr.setScrolledPosition((short) 0);
        byte src = (byte) slea.readShort();
        byte dst = (byte) slea.readShort();
        boolean insight = (src == 127) && (chr.getTrait(MapleTrait.MapleTraitType.sense).getLevel() >= 30);
        Item magnify = chr.getInventory(MapleInventoryType.USE).getItem((short) src);
        Equip toScroll;
        if (dst < 0) {
            toScroll = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) dst);
        } else {
            toScroll = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem((short) dst);
        }

        if (((magnify == null) && (!insight)) || (toScroll == null) || (c.getPlayer().hasBlockedInventory())) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int reqLevel = ii.getReqLevel(toScroll.getItemId()) / 10;
        if ((toScroll.getState() == 1) && ((insight) || (magnify.getItemId() == 2460003) || ((magnify.getItemId() == 2460002) && (reqLevel <= 12)) || ((magnify.getItemId() == 2460001) && (reqLevel <= 7)) || ((magnify.getItemId() == 2460000) && (reqLevel <= 3)))) {
            List pots = new LinkedList(ii.getAllPotentialInfo().values());
            int new_state = Math.abs(toScroll.getPotential1());
            int stateLines = c.getChannelServer().getStateLines();
            //20=SS 19=S 18=A 17=B 0=C
            if ((new_state > 20) || (new_state < 17)) {
                new_state = 17;
            }
            int lines = 2;
            if (toScroll.getPotential2() != 0) {
                lines++;
            }
            if (toScroll.getPotential3() != 0) {
                lines++;
            }
            if (toScroll.getPotential4() != 0) {
                lines++;
            }
            if (lines > stateLines) {
                lines = stateLines;
            }

            while (toScroll.getState() != new_state) {
                for (int i = 0; i < lines; i++) {
                    boolean rewarded = false;
                    while (!rewarded) {
                        StructItemOption pot = (StructItemOption) ((List) pots.get(Randomizer.nextInt(pots.size()))).get(reqLevel);//载入属性
                        if ((pot != null) && (pot.reqLevel / 10 <= reqLevel) && (GameConstants.optionTypeFits(pot.optionType, toScroll.getItemId())) && (GameConstants.optionTypeFitsX(pot.opID, toScroll.getItemId())) && (GameConstants.potentialIDFits(pot.opID, new_state, i))) {
                            if (i == 0) {
                                toScroll.setPotential1(pot.opID);
                            } else if (i == 1) {
                                toScroll.setPotential2(pot.opID);
                            } else if (i == 2) {
                                toScroll.setPotential3(pot.opID);
                            } else if (i == 3) {
                                toScroll.setPotential4(pot.opID);
                            } else if (i == 4) {
                                toScroll.setPotential5(pot.opID);
                            }
                            rewarded = true;
                        }
                    }
                }
            }
            if ((toScroll.getState() >= 18) && (toScroll.getStateMsg() < 3)) {
                String medal = "";
                Item medalItem = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -26);
                if (medalItem != null) {
                    medal = new StringBuilder().append("<").append(ii.getName(medalItem.getItemId())).append("> ").toString();
                }
                if ((toScroll.getState() == 18) && (toScroll.getStateMsg() == 0)) {
                    toScroll.setStateMsg(1);
                    chr.finishAchievement(52);
                    if (!chr.isAdmin()) {
                        String msg = new StringBuilder().append(medal).append(chr.getName()).append(" : 鉴定出 A 级装备，大家祝贺他(她)吧！").toString();
                        Broadcast.broadcastSmega(MaplePacketCreator.itemMegaphone(msg, true, c.getChannel(), toScroll));
                    }
                } else if ((toScroll.getState() == 19) && (toScroll.getStateMsg() <= 1)) {
                    toScroll.setStateMsg(2);
                    chr.finishAchievement(53);
                    if (!chr.isAdmin()) {
                        String msg = new StringBuilder().append(medal).append(chr.getName()).append(" : 鉴定出 S 级装备，大家祝贺他(她)吧！").toString();
                        Broadcast.broadcastSmega(MaplePacketCreator.itemMegaphone(msg, true, c.getChannel(), toScroll));
                    }
                } else if ((toScroll.getState() == 20) && (toScroll.getStateMsg() <= 2)) {
                    toScroll.setStateMsg(3);
                    chr.finishAchievement(54);
                    if (!chr.isAdmin()) {
                        String msg = new StringBuilder().append(medal).append(chr.getName()).append(" : 鉴定出 SS 级装备，大家祝贺他(她)吧！").toString();
                        Broadcast.broadcastSmega(MaplePacketCreator.itemMegaphone(msg, true, c.getChannel(), toScroll));
                    }
                }
            }
            chr.getTrait(MapleTrait.MapleTraitType.insight).addExp((insight ? 10 : magnify.getItemId() + 2 - 2460000) * 2, chr);
            chr.getMap().broadcastMessage(MaplePacketCreator.放大镜效果(chr.getId(), toScroll.getPosition()));
            if (!insight) {
                c.getSession().write(MaplePacketCreator.scrolledItem(magnify, toScroll, false, dst >= 0));
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, magnify.getPosition(), (short) 1, false);
            } else {
                chr.forceReAddItem(toScroll, MapleInventoryType.EQUIP);
            }
            c.getSession().write(MaplePacketCreator.enableActions());
        } else {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
        }
    }

    public static void addToScrollLog(int accountID, int charID, int scrollID, int itemID, byte oldSlots, byte newSlots, byte viciousHammer, String result, boolean ws, boolean ls, int vega) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO scroll_log VALUES(DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, accountID);
            ps.setInt(2, charID);
            ps.setInt(3, scrollID);
            ps.setInt(4, itemID);
            ps.setByte(5, oldSlots);
            ps.setByte(6, newSlots);
            ps.setByte(7, viciousHammer);
            ps.setString(8, result);
            ps.setByte(9, (byte) (ws ? 1 : 0));
            ps.setByte(10, (byte) (ls ? 1 : 0));
            ps.setInt(11, vega);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            FileoutputUtil.outputFileError("log\\Packet_Except.log", e);
        }
    }

    public static boolean UseUpgradeScroll(short slot, short dst, short ws, MapleClient c, MapleCharacter chr, boolean cash) {
        return UseUpgradeScroll(slot, dst, ws, c, chr, 0, cash);
    }

    public static boolean UseUpgradeScroll(short slot, short dst, short ws, MapleClient c, MapleCharacter chr, int vegas, boolean cash) {
        boolean whiteScroll = false;
        boolean legendarySpirit = false;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        chr.setScrolledPosition((short) 0);
        if ((ws & 0x2) == 2) {
            whiteScroll = true;
        }
        Equip toScroll;
        if (dst < 0) {
            toScroll = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        } else {
            legendarySpirit = true;
            toScroll = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(dst);
        }
        if ((toScroll == null) || (c.getPlayer().hasBlockedInventory())) {
            return false;
        }
        byte oldLevel = toScroll.getLevel();
        byte oldEnhance = toScroll.getEnhance();
        byte oldState = toScroll.getState();
        short oldFlag = toScroll.getFlag();
        byte oldSlots = toScroll.getUpgradeSlots();
        byte oldVH = toScroll.getViciousHammer();
        int itemID = toScroll.getItemId();
        Item scroll;
        if (cash) {
            scroll = chr.getInventory(MapleInventoryType.CASH).getItem(slot);
        } else {
            scroll = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        }
        if (scroll == null) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, "砸卷错误: scroll == null");
            }
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }
        if (chr.isAdmin()) {
            chr.dropMessage(-9, new StringBuilder().append("砸卷信息: 卷轴ID ").append(scroll.getItemId()).append(" 卷轴名字 ").append(ii.getName(scroll.getItemId())).toString());
        }
        if ((!GameConstants.is特殊卷轴(scroll.getItemId())) && (!GameConstants.is白医卷轴(scroll.getItemId())) && (!GameConstants.is强化卷轴(scroll.getItemId())) && (!GameConstants.is潜能卷轴(scroll.getItemId()))) {
            if (toScroll.getUpgradeSlots() < 1) {
                chr.dropMessage(1, new StringBuilder().append("当前装备可升级次数为: ").append(toScroll.getUpgradeSlots()).toString());
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return false;
            }
        } else if (GameConstants.is强化卷轴(scroll.getItemId())) {
            if ((toScroll.getUpgradeSlots() >= 1) || (toScroll.getEnhance() >= 100) || (vegas > 0) || (ii.isCash(toScroll.getItemId()))) {
                if (chr.isAdmin()) {
                    chr.dropMessage(-9, new StringBuilder().append("砸卷错误: isEquipScroll ").append(toScroll.getUpgradeSlots() >= 1).append(" ").append(toScroll.getEnhance() >= 100).append(" ").append(vegas > 0).append(" ").append(ii.isCash(toScroll.getItemId())).toString());
                }
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return false;
            }
        } else if (GameConstants.is潜能卷轴(scroll.getItemId())) {
            boolean isA级潜能卷轴 = scroll.getItemId() / 100 == 20497;
            boolean is军团盾 = (toScroll.getItemId() >= 1099000) && (toScroll.getItemId() <= 1099004);
            boolean is箭矢卡片 = toScroll.getItemId() / 10000 == 135;
            if (((!isA级潜能卷轴) && (toScroll.getState() >= 1)) || ((isA级潜能卷轴) && (toScroll.getState() >= 18)) || ((toScroll.getLevel() == 0) && (toScroll.getUpgradeSlots() == 0) && (!is箭矢卡片) && (!isA级潜能卷轴) && (!is军团盾)) || (vegas > 0) || (ii.isCash(toScroll.getItemId()))) {
                if (chr.isAdmin()) {
                    chr.dropMessage(-9, new StringBuilder().append("砸卷错误: isPotentialScroll ").append(toScroll.getState() >= 1).append(" ").append((toScroll.getLevel() == 0) && (toScroll.getUpgradeSlots() == 0) && (!is箭矢卡片) && (!isA级潜能卷轴) && (!is军团盾)).append(" ").append(vegas > 0).append(" ").append(ii.isCash(toScroll.getItemId())).toString());
                }
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return false;
            }
        } else if (GameConstants.is特殊卷轴(scroll.getItemId())) {
            if ((ii.isCash(toScroll.getItemId())) || (toScroll.getEnhance() >= 12)) {
                if (chr.isAdmin()) {
                    chr.dropMessage(-9, new StringBuilder().append("砸卷错误: isSpecialScroll isCash - ").append(ii.isCash(toScroll.getItemId())).append(" getEnhance - ").append(toScroll.getEnhance() >= 12).toString());
                }
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return false;
            }
        }
        if ((!GameConstants.canScroll(toScroll.getItemId())) && (!GameConstants.is枫叶卷轴(toScroll.getItemId()))) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, new StringBuilder().append("砸卷错误: !canScroll ").append(!GameConstants.canScroll(toScroll.getItemId())).append(" !isChaosScroll ").append(!GameConstants.is枫叶卷轴(toScroll.getItemId())).toString());
            }
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }
        if (((GameConstants.is白医卷轴(scroll.getItemId())) || (GameConstants.is饰品制炼书(scroll.getItemId())) || (GameConstants.isGeneralScroll(scroll.getItemId())) || (GameConstants.is枫叶卷轴(scroll.getItemId()))) && ((vegas > 0) || (ii.isCash(toScroll.getItemId())))) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, new StringBuilder().append("砸卷错误: isCleanSlate ").append(GameConstants.is白医卷轴(scroll.getItemId())).append(" isTablet ").append(GameConstants.is饰品制炼书(scroll.getItemId())).toString());
            }
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }
        if ((GameConstants.is饰品制炼书(scroll.getItemId())) && (toScroll.getDurability() < 0)) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, new StringBuilder().append("砸卷错误: isTablet ").append(GameConstants.is饰品制炼书(scroll.getItemId())).append(" getDurability ").append(toScroll.getDurability() < 0).toString());
            }
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }
        if ((!GameConstants.is饰品制炼书(scroll.getItemId())) && (!GameConstants.is潜能卷轴(scroll.getItemId())) && (!GameConstants.is强化卷轴(scroll.getItemId())) && (!GameConstants.is白医卷轴(scroll.getItemId())) && (!GameConstants.is特殊卷轴(scroll.getItemId())) && (!GameConstants.is枫叶卷轴(scroll.getItemId())) && (toScroll.getDurability() >= 0)) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, "砸卷错误: !isTablet ----- 1");
            }
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }
        Item wscroll = null;

        List scrollReqs = ii.getScrollReqs(scroll.getItemId());
        if ((scrollReqs != null) && (scrollReqs.size() > 0) && (!scrollReqs.contains(Integer.valueOf(toScroll.getItemId())))) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, "砸卷错误: scrollReqs != null && scrollReqs.size() > 0 && !scrollReqs.contains(toScroll.getItemId())");
            }
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }
        if (whiteScroll) {
            wscroll = chr.getInventory(MapleInventoryType.USE).findById(2340000);
            if (wscroll == null) {
                if (chr.isAdmin()) {
                    chr.dropMessage(-9, "砸卷错误: wscroll == null");
                }
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                whiteScroll = false;
            }
        }
        if ((GameConstants.is饰品制炼书(scroll.getItemId())) || (GameConstants.isGeneralScroll(scroll.getItemId()))) {
            switch (scroll.getItemId() % 1000 / 100) {
                case 0:
                    if ((!GameConstants.isTwoHanded(toScroll.getItemId())) && (GameConstants.isWeapon(toScroll.getItemId()))) {
                        break;
                    }
                    if (chr.isAdmin()) {
                        chr.dropMessage(-9, "砸卷错误: 最后检测 --- 0");
                    }
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    return false;
                case 1:
                    if ((GameConstants.isTwoHanded(toScroll.getItemId())) && (GameConstants.isWeapon(toScroll.getItemId()))) {
                        break;
                    }
                    if (chr.isAdmin()) {
                        chr.dropMessage(-9, "砸卷错误: 最后检测 --- 1");
                    }
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    return false;
                case 2:
                    if ((!GameConstants.isAccessory(toScroll.getItemId())) && (!GameConstants.isWeapon(toScroll.getItemId()))) {
                        break;
                    }
                    if (chr.isAdmin()) {
                        chr.dropMessage(-9, "砸卷错误: 最后检测 --- 2");
                    }
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    return false;
                case 3:
                    if ((GameConstants.isAccessory(toScroll.getItemId())) && (!GameConstants.isWeapon(toScroll.getItemId()))) {
                        break;
                    }
                    if (chr.isAdmin()) {
                        chr.dropMessage(-9, "砸卷错误: 最后检测 --- 3");
                    }
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    return false;
            }

        } else if ((!GameConstants.is配饰卷轴(scroll.getItemId())) && (!GameConstants.is枫叶卷轴(scroll.getItemId())) && (!GameConstants.is白医卷轴(scroll.getItemId())) && (!GameConstants.is强化卷轴(scroll.getItemId())) && (!GameConstants.is潜能卷轴(scroll.getItemId())) && (!GameConstants.is特殊卷轴(scroll.getItemId()))
                && (!ii.canScroll(scroll.getItemId(), toScroll.getItemId()))) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, "砸卷错误: 最后检测 --- 4");
            }
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }

        if ((GameConstants.is配饰卷轴(scroll.getItemId())) && (!GameConstants.isAccessory(toScroll.getItemId()))) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, "砸卷错误: 最后检测 --- 5");
            }
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }
        if (scroll.getQuantity() <= 0) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, "砸卷错误: 最后检测 --- 6");
            }
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }
        if ((legendarySpirit) && (vegas == 0)
                && (chr.getSkillLevel(SkillFactory.getSkill(PlayerStats.getSkillByJob(1003, chr.getJob()))) <= 0)) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, "砸卷错误: 最后检测 --- 7");
            }
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }

        Equip scrolled = (Equip) ii.scrollEquipWithId(toScroll, scroll, whiteScroll, chr, vegas);
        ScrollResult scrollSuccess;
        if ((scrolled == null) && (GameConstants.is强化卷轴(scroll.getItemId()))) {
            if (ItemFlag.防爆卷轴.check(oldFlag)) {
                scrolled = toScroll;
                scrollSuccess = Equip.ScrollResult.失败;
                scrolled.setFlag((short) (oldFlag - ItemFlag.防爆卷轴.getValue()));
            } else {
                scrollSuccess = Equip.ScrollResult.消失;
            }
        } else {

            if (scrolled == null) {
                scrollSuccess = Equip.ScrollResult.消失;
            } else {
                if (((scroll.getItemId() / 100 == 20497) && (scrolled.getState() == 1)) || (scrolled.getLevel() > oldLevel) || (scrolled.getEnhance() > oldEnhance) || (scrolled.getState() > oldState) || (scrolled.getFlag() > oldFlag)) {
                    scrollSuccess = Equip.ScrollResult.成功;
                } else {
                    if ((GameConstants.is白医卷轴(scroll.getItemId())) && (scrolled.getUpgradeSlots() > oldSlots)) {
                        scrollSuccess = Equip.ScrollResult.成功;
                    } else {
                        scrollSuccess = Equip.ScrollResult.失败;
                    }
                }
            }
        }
        if (ItemFlag.防护卷轴.check(oldFlag)) {
            if (scrolled != null) {
                scrolled.setFlag((short) (scrolled.getFlag() - ItemFlag.防护卷轴.getValue()));
            }
        } else {
            chr.getInventory(GameConstants.getInventoryType(scroll.getItemId())).removeItem(scroll.getPosition(), (short) 1, false);
        }
        if (whiteScroll) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, wscroll.getPosition(), (short) 1, false, false);
        } else if ((scrollSuccess == Equip.ScrollResult.失败) && (scrolled.getUpgradeSlots() < oldSlots) && (chr.getInventory(MapleInventoryType.CASH).findById(5640000) != null)) {
            chr.setScrolledPosition(scrolled.getPosition());
            if (vegas == 0) {
                c.getSession().write(MaplePacketCreator.pamSongUI());
            }
        }
        if (scrollSuccess == Equip.ScrollResult.消失) {
            c.getSession().write(MaplePacketCreator.scrolledItem(scroll, toScroll, true, false));
            if (dst < 0) {
                chr.getInventory(MapleInventoryType.EQUIPPED).removeItem(toScroll.getPosition());
            } else {
                chr.getInventory(MapleInventoryType.EQUIP).removeItem(toScroll.getPosition());
            }
        } else if (vegas == 0) {
            c.getSession().write(MaplePacketCreator.scrolledItem(scroll, scrolled, false, false));
        }
        chr.getMap().broadcastMessage(chr, MaplePacketCreator.getScrollEffect(chr.getId(), scrollSuccess, legendarySpirit, whiteScroll, scroll.getItemId(), toScroll.getItemId()), vegas == 0);

        if ((dst < 0) && ((scrollSuccess == Equip.ScrollResult.成功) || (scrollSuccess == Equip.ScrollResult.消失)) && (vegas == 0)) {
            chr.equipChanged();
        }
        return true;
    }

    public static boolean UseSkillBook(byte slot, int itemId, MapleClient c, MapleCharacter chr) {
        Item toUse = chr.getInventory(GameConstants.getInventoryType(itemId)).getItem((short) slot);
        if ((toUse == null) || (toUse.getQuantity() < 1) || (toUse.getItemId() != itemId) || (chr.hasBlockedInventory())) {
            return false;
        }
        Map skilldata = MapleItemInformationProvider.getInstance().getEquipStats(toUse.getItemId());
        if (skilldata == null) {
            return false;
        }
        boolean canuse = false;
        boolean success = false;
        int skill = 0;
        int maxlevel = 0;

        Integer SuccessRate = (Integer) skilldata.get("success");
        Integer ReqSkillLevel = (Integer) skilldata.get("reqSkillLevel");
        Integer MasterLevel = (Integer) skilldata.get("masterLevel");

        byte i = 0;
        while (true) {
            Integer CurrentLoopedSkillId = (Integer) skilldata.get(new StringBuilder().append("skillid").append(i).toString());
            i = (byte) (i + 1);
            if ((CurrentLoopedSkillId == null) || (MasterLevel == null)) {
                break;
            }
            Skill CurrSkillData = SkillFactory.getSkill(CurrentLoopedSkillId.intValue());
            if ((CurrSkillData != null) && (CurrSkillData.canBeLearnedBy(chr.getJob())) && ((ReqSkillLevel == null) || (chr.getSkillLevel(CurrSkillData) >= ReqSkillLevel.intValue())) && (chr.getMasterLevel(CurrSkillData) < MasterLevel.intValue())) {
                canuse = true;
                if ((SuccessRate == null) || (Randomizer.nextInt(100) <= SuccessRate.intValue())) {
                    success = true;
                    chr.changeSkillLevel(CurrSkillData, chr.getSkillLevel(CurrSkillData), (byte) MasterLevel.intValue());
                } else {
                    success = false;
                }
                MapleInventoryManipulator.removeFromSlot(c, GameConstants.getInventoryType(itemId), (short) slot, (short) 1, false);
                break;
            }
        }
        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.useSkillBook(chr, skill, maxlevel, canuse, success));
        c.getSession().write(MaplePacketCreator.enableActions());
        return canuse;
    }

    public static void UseSpReset(byte slot, int itemId, MapleClient c, MapleCharacter chr) {
        Item toUse = chr.getInventory(GameConstants.getInventoryType(itemId)).getItem((short) slot);
        if ((toUse != null) && (toUse.getQuantity() > 0) && (toUse.getItemId() == itemId) && (!chr.hasBlockedInventory()) && (itemId / 10000 == 250)) {
            chr.dropMessage(1, "目前不支持该道具的使用。");
            c.getSession().write(MaplePacketCreator.enableActions());
        } else {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static void UseApReset(byte slot, int itemId, MapleClient c, MapleCharacter chr) {
        Item toUse = chr.getInventory(GameConstants.getInventoryType(itemId)).getItem((short) slot);
        if ((toUse != null) && (toUse.getQuantity() > 0) && (toUse.getItemId() == itemId) && (!chr.hasBlockedInventory()) && (itemId / 10000 == 250)) {
            chr.resetStats(4, 4, 4, 4);
            MapleInventoryManipulator.removeFromSlot(c, GameConstants.getInventoryType(itemId), (short) slot, (short) 1, false);
            c.getSession().write(MaplePacketCreator.enableActions());
        } else {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static void UseCatchItem(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        byte slot = (byte) slea.readShort();
        int itemid = slea.readInt();
        MapleMonster mob = chr.getMap().getMonsterByOid(slea.readInt());
        Item toUse = chr.getInventory(MapleInventoryType.USE).getItem((short) slot);
        MapleMap map = chr.getMap();
        if ((toUse != null) && (toUse.getQuantity() > 0) && (toUse.getItemId() == itemid) && (mob != null) && (!chr.hasBlockedInventory()) && (itemid / 10000 == 227) && (MapleItemInformationProvider.getInstance().getCardMobId(itemid) == mob.getId())) {
            if ((!MapleItemInformationProvider.getInstance().isMobHP(itemid)) || (mob.getHp() <= mob.getMobMaxHp() / 2L)) {
                map.broadcastMessage(MaplePacketCreator.catchMonster(mob.getObjectId(), itemid, (byte) 1));
                map.killMonster(mob, chr, true, false, (byte) 1);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false, false);
                if (MapleItemInformationProvider.getInstance().getCreateId(itemid) > 0) {
                    MapleInventoryManipulator.addById(c, MapleItemInformationProvider.getInstance().getCreateId(itemid), (short) 1, new StringBuilder().append("Catch item ").append(itemid).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                }
            } else {
                map.broadcastMessage(MaplePacketCreator.catchMonster(mob.getObjectId(), itemid, (byte) 0));
                c.getSession().write(MaplePacketCreator.catchMob(mob.getId(), itemid, (byte) 0));
            }
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static void UseMountFood(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        c.getPlayer().updateTick(slea.readInt());
        byte slot = (byte) slea.readShort();
        int itemid = slea.readInt();
        Item toUse = chr.getInventory(MapleInventoryType.USE).getItem((short) slot);
        MapleMount mount = chr.getMount();
        if ((itemid / 10000 == 226) && (toUse != null) && (toUse.getQuantity() > 0) && (toUse.getItemId() == itemid) && (mount != null) && (!c.getPlayer().hasBlockedInventory())) {
            int fatigue = mount.getFatigue();
            boolean levelup = false;
            mount.setFatigue((byte) -30);
            if (fatigue > 0) {
                mount.increaseExp();
                int level = mount.getLevel();
                if ((level < 30) && (mount.getExp() >= GameConstants.getMountExpNeededForLevel(level + 1))) {
                    mount.setLevel((byte) (level + 1));
                    levelup = true;
                }
            }
            chr.getMap().broadcastMessage(MaplePacketCreator.updateMount(chr, levelup));
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static void UseScriptedNPCItem(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        c.getPlayer().updateTick(slea.readInt());
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        Item toUse = chr.getInventory(GameConstants.getInventoryType(itemId)).getItem((short) slot);
        long expiration_days = 0L;
        int mountid = 0;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        ScriptedItem info = ii.getScriptedItemInfo(itemId);
        if (info == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if ((toUse != null) && (toUse.getQuantity() >= 1) && (toUse.getItemId() == itemId) && (!chr.hasBlockedInventory()) && (!chr.inPVP())) {
            MapleQuestStatus marr;
            long lastTime;
            switch (toUse.getItemId()) {
                case 2430007:
                    MapleInventory inventory = chr.getInventory(MapleInventoryType.SETUP);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
                    if ((inventory.countById(3994102) >= 20) && (inventory.countById(3994103) >= 20) && (inventory.countById(3994104) >= 20) && (inventory.countById(3994105) >= 20)) {
                        MapleInventoryManipulator.addById(c, 2430008, (short) 1, new StringBuilder().append("Scripted item: ").append(itemId).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994102, 20, false, false);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994103, 20, false, false);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994104, 20, false, false);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994105, 20, false, false);
                    } else {
                        MapleInventoryManipulator.addById(c, 2430007, (short) 1, new StringBuilder().append("Scripted item: ").append(itemId).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                    }
                    NPCScriptManager.getInstance().start(c, 2084001);
                    break;
                case 2430008:
                    chr.saveLocation(SavedLocationType.RICHIE);

                    boolean warped = false;
                    for (int i = 390001000; i <= 390001004; i++) {
                        MapleMap map = c.getChannelServer().getMapFactory().getMap(i);
                        if (map.getCharactersSize() == 0) {
                            chr.changeMap(map, map.getPortal(0));
                            warped = true;
                            break;
                        }
                    }
                    if (warped) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "All maps are currently in use, please try again later.");
                    }
                    break;
                case 2430112:
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430112) >= 25) {
                            if ((MapleInventoryManipulator.checkSpace(c, 2049400, 1, "")) && (MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 25, true, false))) {
                                MapleInventoryManipulator.addById(c, 2049400, (short) 1, new StringBuilder().append("Scripted item: ").append(toUse.getItemId()).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                            } else {
                                c.getPlayer().dropMessage(5, "消耗栏空间位置不足.");
                            }
                        } else if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430112) >= 10) {
                            if ((MapleInventoryManipulator.checkSpace(c, 2049401, 1, "")) && (MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 10, true, false))) {
                                MapleInventoryManipulator.addById(c, 2049401, (short) 1, new StringBuilder().append("Scripted item: ").append(toUse.getItemId()).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                            } else {
                                c.getPlayer().dropMessage(5, "消耗栏空间位置不足.");
                            }
                        } else {
                            NPCScriptManager.getInstance().startItem(c, info.getNpc(), toUse.getItemId());
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "消耗栏空间位置不足.");
                    }
                    break;
                case 2430481:
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430481) >= 100) {
                            if ((MapleInventoryManipulator.checkSpace(c, 2049701, 1, "")) && (MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 100, true, false))) {
                                MapleInventoryManipulator.addById(c, 2049701, (short) 1, new StringBuilder().append("Scripted item: ").append(toUse.getItemId()).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                            } else {
                                c.getPlayer().dropMessage(5, "消耗栏空间位置不足.");
                            }
                        } else if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430481) >= 30) {
                            if ((MapleInventoryManipulator.checkSpace(c, 2049400, 1, "")) && (MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 30, true, false))) {
                                MapleInventoryManipulator.addById(c, 2049400, (short) 1, new StringBuilder().append("Scripted item: ").append(toUse.getItemId()).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                            } else {
                                c.getPlayer().dropMessage(5, "消耗栏空间位置不足.");
                            }
                        } else {
                            NPCScriptManager.getInstance().startItem(c, info.getNpc(), toUse.getItemId());
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "消耗栏空间位置不足.");
                    }
                    break;
                case 2430760:
                    if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430760) >= 10) {
                            if ((MapleInventoryManipulator.checkSpace(c, 5750000, 1, "")) && (MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 10, true, false))) {
                                MapleInventoryManipulator.addById(c, 5750000, (short) 1, new StringBuilder().append("Scripted item: ").append(toUse.getItemId()).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                            } else {
                                c.getPlayer().dropMessage(5, "请检测背包空间是否足够.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "10个星岩魔方碎片才可以兑换1个星岩魔方.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "请检测背包空间是否足够.");
                    }
                    break;
                case 2430691:
                    if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430691) >= 10) {
                            if ((MapleInventoryManipulator.checkSpace(c, 5750001, 1, "")) && (MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 10, true, false))) {
                                MapleInventoryManipulator.addById(c, 5750001, (short) 1, new StringBuilder().append("Scripted item: ").append(toUse.getItemId()).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                            } else {
                                c.getPlayer().dropMessage(5, "请检测背包空间是否足够.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "10个星岩电钻机碎片才可以兑换1个星岩电钻机.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "请检测背包空间是否足够.");
                    }
                    break;
                case 2430692:
                    if (c.getPlayer().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430692) >= 1) {
                            int rank = Randomizer.nextInt(100) < 30 ? 1 : Randomizer.nextInt(100) < 4 ? 2 : 0;
                            List pots = new LinkedList(ii.getAllSocketInfo(rank).values());
                            int newId = 0;
                            while (newId == 0) {
                                StructItemOption pot = (StructItemOption) pots.get(Randomizer.nextInt(pots.size()));
                                if (pot != null) {
                                    newId = pot.opID;
                                }
                            }
                            if ((MapleInventoryManipulator.checkSpace(c, newId, 1, "")) && (MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false))) {
                                MapleInventoryManipulator.addById(c, newId, (short) 1, new StringBuilder().append("Scripted item: ").append(toUse.getItemId()).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                                c.getSession().write(MaplePacketCreator.getShowItemGain(newId, (short) 1, true));
                            } else {
                                c.getPlayer().dropMessage(5, "请检测背包空间是否足够.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "您没有星岩箱子.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "请检测背包空间是否足够.");
                    }

                    break;
                case 5680019: {
                    int hair = 32150 + c.getPlayer().getHair() % 10;
                    c.getPlayer().setHair(hair);
                    c.getPlayer().updateSingleStat(MapleStat.发型, hair);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, (short) slot, (short) 1, false);

                    break;
                }
                case 5680020: {
                    int hair = 32160 + c.getPlayer().getHair() % 10;
                    c.getPlayer().setHair(hair);
                    c.getPlayer().updateSingleStat(MapleStat.发型, hair);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, (short) slot, (short) 1, false);

                    break;
                }
                case 3994225:
                    c.getPlayer().dropMessage(5, "Please bring this item to the NPC.");
                    break;
                case 2430212:
                    marr = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(122500));
                    if (marr.getCustomData() == null) {
                        marr.setCustomData("0");
                    }
                    lastTime = Long.parseLong(marr.getCustomData());
                    if (lastTime + 600000L > System.currentTimeMillis()) {
                        c.getPlayer().dropMessage(5, "疲劳恢复药 10分钟内只能使用1次，请稍后在试。");
                    } else {
                        if (c.getPlayer().getFatigue() <= 0) {
                            break;
                        }
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
                        c.getPlayer().setFatigue(c.getPlayer().getFatigue() - 5);
                    }
                    break;
                case 2430213:
                    marr = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(122500));
                    if (marr.getCustomData() == null) {
                        marr.setCustomData("0");
                    }
                    lastTime = Long.parseLong(marr.getCustomData());
                    if (lastTime + 600000L > System.currentTimeMillis()) {
                        c.getPlayer().dropMessage(5, "疲劳恢复药 10分钟内只能使用1次，请稍后在试。");
                    } else {
                        if (c.getPlayer().getFatigue() <= 0) {
                            break;
                        }
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
                        c.getPlayer().setFatigue(c.getPlayer().getFatigue() - 10);
                    }
                    break;
                case 2430214:
                case 2430220:
                    if (c.getPlayer().getFatigue() <= 0) {
                        break;
                    }
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
                    c.getPlayer().setFatigue(c.getPlayer().getFatigue() - 30);
                    break;
                case 2430227:
                    if (c.getPlayer().getFatigue() <= 0) {
                        break;
                    }
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
                    c.getPlayer().setFatigue(c.getPlayer().getFatigue() - 50);
                    break;
                case 2430231:
                    marr = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(122500));
                    if (marr.getCustomData() == null) {
                        marr.setCustomData("0");
                    }
                    lastTime = Long.parseLong(marr.getCustomData());
                    if (lastTime + 600000L > System.currentTimeMillis()) {
                        c.getPlayer().dropMessage(5, "疲劳恢复药 10分钟内只能使用1次，请稍后在试。");
                    } else {
                        if (c.getPlayer().getFatigue() <= 0) {
                            break;
                        }
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
                        c.getPlayer().setFatigue(c.getPlayer().getFatigue() - 40);
                    }
                    break;
                case 2430144:
                    int itemid = Randomizer.nextInt(373) + 2290000;
                    if ((!MapleItemInformationProvider.getInstance().itemExists(itemid)) || (MapleItemInformationProvider.getInstance().getName(itemid).contains("Special")) || (MapleItemInformationProvider.getInstance().getName(itemid).contains("Event"))) {
                        break;
                    }
                    MapleInventoryManipulator.addById(c, itemid, (short) 1, new StringBuilder().append("Reward item: ").append(toUse.getItemId()).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
                    break;
                case 2430370:
                    if (!MapleInventoryManipulator.checkSpace(c, 2028062, 1, "")) {
                        break;
                    }
                    MapleInventoryManipulator.addById(c, 2028062, (short) 1, new StringBuilder().append("Reward item: ").append(toUse.getItemId()).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
                    break;
                case 2430158:
                    if (c.getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000630) >= 100) {
                            if ((MapleInventoryManipulator.checkSpace(c, 4310010, 1, "")) && (MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false))) {
                                MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000630, 100, true, false);
                                MapleInventoryManipulator.addById(c, 4310010, (short) 1, new StringBuilder().append("Scripted item: ").append(toUse.getItemId()).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                            } else {
                                c.getPlayer().dropMessage(5, "其他栏空间位置不足.");
                            }
                        } else if (c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000630) >= 50) {
                            if ((MapleInventoryManipulator.checkSpace(c, 4310009, 1, "")) && (MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false))) {
                                MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000630, 50, true, false);
                                MapleInventoryManipulator.addById(c, 4310009, (short) 1, new StringBuilder().append("Scripted item: ").append(toUse.getItemId()).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                            } else {
                                c.getPlayer().dropMessage(5, "其他栏空间位置不足.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "需要50个净化图腾才能兑换出狮子王的贵族勋章，100个净化图腾才能兑换狮子王的皇家勋章。");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "其他栏空间位置不足.");
                    }
                    break;
                case 2430159:
                    MapleQuest.getInstance(3182).forceComplete(c.getPlayer(), 2161004);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
                    break;
                case 2430200:
                    if (c.getPlayer().getQuestStatus(31152) != 2) {
                        c.getPlayer().dropMessage(5, "You have no idea how to use it.");
                    } else if (c.getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() >= 1) {
                        if ((c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000660) >= 1) && (c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000661) >= 1) && (c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000662) >= 1) && (c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000663) >= 1)) {
                            if ((MapleInventoryManipulator.checkSpace(c, 4032923, 1, "")) && (MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) && (MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000660, 1, true, false)) && (MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000661, 1, true, false)) && (MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000662, 1, true, false)) && (MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000663, 1, true, false))) {
                                MapleInventoryManipulator.addById(c, 4032923, (short) 1, new StringBuilder().append("Scripted item: ").append(toUse.getItemId()).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                            } else {
                                c.getPlayer().dropMessage(5, "其他栏空间位置不足.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "There needs to be 1 of each Stone for a Dream Key.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "其他栏空间位置不足.");
                    }

                    break;
                case 2430130:
                case 2430131:
                    if (GameConstants.is反抗者(c.getPlayer().getJob())) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
                        c.getPlayer().gainExp(20000 + c.getPlayer().getLevel() * 50 * c.getChannelServer().getExpRate(), true, true, false);
                    } else {
                        c.getPlayer().dropMessage(5, "您无法使用这个道具。");
                    }
                    break;
                case 2430132:
                case 2430133:
                case 2430134:
                case 2430142:
                    if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 1) {
                        if ((c.getPlayer().getJob() == 3200) || (c.getPlayer().getJob() == 3210) || (c.getPlayer().getJob() == 3211) || (c.getPlayer().getJob() == 3212)) {
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
                            MapleInventoryManipulator.addById(c, 1382101, (short) 1, new StringBuilder().append("Scripted item: ").append(itemId).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                        } else if ((c.getPlayer().getJob() == 3300) || (c.getPlayer().getJob() == 3310) || (c.getPlayer().getJob() == 3311) || (c.getPlayer().getJob() == 3312)) {
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
                            MapleInventoryManipulator.addById(c, 1462093, (short) 1, new StringBuilder().append("Scripted item: ").append(itemId).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                        } else if ((c.getPlayer().getJob() == 3500) || (c.getPlayer().getJob() == 3510) || (c.getPlayer().getJob() == 3511) || (c.getPlayer().getJob() == 3512)) {
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
                            MapleInventoryManipulator.addById(c, 1492080, (short) 1, new StringBuilder().append("Scripted item: ").append(itemId).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
                        } else {
                            c.getPlayer().dropMessage(5, "您无法使用这个道具。");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "背包空间不足。");
                    }

                    break;
                case 2430455:
                    NPCScriptManager.getInstance().startItem(c, 9010000, 2430455);
                    break;
                case 2430036:
                    mountid = 1027;
                    expiration_days = 1L;
                    break;
                case 2430170:
                    mountid = 1027;
                    expiration_days = 7L;
                    break;
                case 2430037:
                    mountid = 1028;
                    expiration_days = 1L;
                    break;
                case 2430038:
                    mountid = 1029;
                    expiration_days = 1L;
                    break;
                case 2430039:
                    mountid = 1030;
                    expiration_days = 1L;
                    break;
                case 2430040:
                    mountid = 1031;
                    expiration_days = 1L;
                    break;
                case 2430223:
                    mountid = 1031;
                    expiration_days = 15L;
                    break;
                case 2430259:
                    mountid = 1031;
                    expiration_days = 3L;
                    break;
                case 2430242:
                    mountid = 80001018;
                    expiration_days = 10L;
                    break;
                case 2430243:
                    mountid = 80001019;
                    expiration_days = 10L;
                    break;
                case 2430261:
                    mountid = 80001019;
                    expiration_days = 3L;
                    break;
                case 2430249:
                    mountid = 80001027;
                    expiration_days = 3L;
                    break;
                case 2430225:
                    mountid = 1031;
                    expiration_days = 10L;
                    break;
                case 2430053:
                    mountid = 1027;
                    expiration_days = 1L;
                    break;
                case 2430054:
                    mountid = 1028;
                    expiration_days = 30L;
                    break;
                case 2430055:
                    mountid = 1029;
                    expiration_days = 30L;
                    break;
                case 2430257:
                    mountid = 1029;
                    expiration_days = 7L;
                    break;
                case 2430056:
                    mountid = 1035;
                    expiration_days = 30L;
                    break;
                case 2430057:
                    mountid = 1033;
                    expiration_days = 30L;
                    break;
                case 2430072:
                    mountid = 1034;
                    expiration_days = 7L;
                    break;
                case 2430073:
                    mountid = 1036;
                    expiration_days = 15L;
                    break;
                case 2430074:
                    mountid = 1037;
                    expiration_days = 15L;
                    break;
                case 2430272:
                    mountid = 1038;
                    expiration_days = 3L;
                    break;
                case 2430275:
                    mountid = 80001033;
                    expiration_days = 7L;
                    break;
                case 2430075:
                    mountid = 1038;
                    expiration_days = 15L;
                    break;
                case 2430076:
                    mountid = 1039;
                    expiration_days = 15L;
                    break;
                case 2430077:
                    mountid = 1040;
                    expiration_days = 15L;
                    break;
                case 2430080:
                    mountid = 1042;
                    expiration_days = 20L;
                    break;
                case 2430082:
                    mountid = 1044;
                    expiration_days = 7L;
                    break;
                case 2430260:
                    mountid = 1044;
                    expiration_days = 3L;
                    break;
                case 2430091:
                    mountid = 1049;
                    expiration_days = 10L;
                    break;
                case 2430092:
                    mountid = 1050;
                    expiration_days = 10L;
                    break;
                case 2430263:
                    mountid = 1050;
                    expiration_days = 3L;
                    break;
                case 2430093:
                    mountid = 1051;
                    expiration_days = 10L;
                    break;
                case 2430101:
                    mountid = 1052;
                    expiration_days = 10L;
                    break;
                case 2430102:
                    mountid = 1053;
                    expiration_days = 10L;
                    break;
                case 2430103:
                    mountid = 1054;
                    expiration_days = 30L;
                    break;
                case 2430266:
                    mountid = 1054;
                    expiration_days = 3L;
                    break;
                case 2430265:
                    mountid = 1151;
                    expiration_days = 3L;
                    break;
                case 2430258:
                    mountid = 1115;
                    expiration_days = 365L;
                    break;
                case 2430117:
                    mountid = 1036;
                    expiration_days = 365L;
                    break;
                case 2430118:
                    mountid = 1039;
                    expiration_days = 365L;
                    break;
                case 2430119:
                    mountid = 1040;
                    expiration_days = 365L;
                    break;
                case 2430120:
                    mountid = 1037;
                    expiration_days = 365L;
                    break;
                case 2430271:
                    mountid = 1069;
                    expiration_days = 3L;
                    break;
                case 2430136:
                    mountid = 1069;
                    expiration_days = 15L;
                    break;
                case 2430137:
                    mountid = 1069;
                    expiration_days = 30L;
                    break;
                case 2430138:
                    mountid = 1069;
                    expiration_days = 365L;
                    break;
                case 2430145:
                    mountid = 1070;
                    expiration_days = 30L;
                    break;
                case 2430146:
                    mountid = 1070;
                    expiration_days = 365L;
                    break;
                case 2430147:
                    mountid = 1071;
                    expiration_days = 30L;
                    break;
                case 2430148:
                    mountid = 1071;
                    expiration_days = 365L;
                    break;
                case 2430135:
                    mountid = 1065;
                    expiration_days = 15L;
                    break;
                case 2430149:
                    mountid = 1072;
                    expiration_days = 30L;
                    break;
                case 2430262:
                    mountid = 1072;
                    expiration_days = 3L;
                    break;
                case 2430179:
                    mountid = 1081;
                    expiration_days = 15L;
                    break;
                case 2430264:
                    mountid = 1081;
                    expiration_days = 3L;
                    break;
                case 2430201:
                    mountid = 1096;
                    expiration_days = 3L;
                    break;
                case 2430228:
                    mountid = 1101;
                    expiration_days = 15L;
                    break;
                case 2430276:
                    mountid = 1101;
                    expiration_days = 15L;
                    break;
                case 2430277:
                    mountid = 1101;
                    expiration_days = 365L;
                    break;
                case 2430283:
                    mountid = 1025;
                    expiration_days = 10L;
                    break;
                case 2430291:
                    mountid = 1145;
                    expiration_days = -1L;
                    break;
                case 2430293:
                    mountid = 1146;
                    expiration_days = -1L;
                    break;
                case 2430295:
                    mountid = 1147;
                    expiration_days = -1L;
                    break;
                case 2430297:
                    mountid = 1148;
                    expiration_days = -1L;
                    break;
                case 2430299:
                    mountid = 1149;
                    expiration_days = -1L;
                    break;
                case 2430301:
                    mountid = 1150;
                    expiration_days = -1L;
                    break;
                case 2430303:
                    mountid = 1151;
                    expiration_days = -1L;
                    break;
                case 2430305:
                    mountid = 1152;
                    expiration_days = -1L;
                    break;
                case 2430307:
                    mountid = 1153;
                    expiration_days = -1L;
                    break;
                case 2430309:
                    mountid = 1154;
                    expiration_days = -1L;
                    break;
                case 2430311:
                    mountid = 1156;
                    expiration_days = -1L;
                    break;
                case 2430313:
                    mountid = 1156;
                    expiration_days = -1L;
                    break;
                case 2430315:
                    mountid = 1118;
                    expiration_days = -1L;
                    break;
                case 2430317:
                    mountid = 1121;
                    expiration_days = -1L;
                    break;
                case 2430319:
                    mountid = 1122;
                    expiration_days = -1L;
                    break;
                case 2430321:
                    mountid = 1123;
                    expiration_days = -1L;
                    break;
                case 2430323:
                    mountid = 1124;
                    expiration_days = -1L;
                    break;
                case 2430325:
                    mountid = 1129;
                    expiration_days = -1L;
                    break;
                case 2430327:
                    mountid = 1130;
                    expiration_days = -1L;
                    break;
                case 2430329:
                    mountid = 1063;
                    expiration_days = -1L;
                    break;
                case 2430331:
                    mountid = 1025;
                    expiration_days = -1L;
                    break;
                case 2430333:
                    mountid = 1034;
                    expiration_days = -1L;
                    break;
                case 2430335:
                    mountid = 1136;
                    expiration_days = -1L;
                    break;
                case 2430337:
                    mountid = 1051;
                    expiration_days = -1L;
                    break;
                case 2430339:
                    mountid = 1138;
                    expiration_days = -1L;
                    break;
                case 2430341:
                    mountid = 1139;
                    expiration_days = -1L;
                    break;
                case 2430343:
                    mountid = 1027;
                    expiration_days = -1L;
                    break;
                case 2430346:
                    mountid = 1029;
                    expiration_days = -1L;
                    break;
                case 2430348:
                    mountid = 1028;
                    expiration_days = -1L;
                    break;
                case 2430350:
                    mountid = 1033;
                    expiration_days = -1L;
                    break;
                case 2430352:
                    mountid = 1064;
                    expiration_days = -1L;
                    break;
                case 2430354:
                    mountid = 1096;
                    expiration_days = -1L;
                    break;
                case 2430356:
                    mountid = 1101;
                    expiration_days = -1L;
                    break;
                case 2430358:
                    mountid = 1102;
                    expiration_days = -1L;
                    break;
                case 2430360:
                    mountid = 1054;
                    expiration_days = -1L;
                    break;
                case 2430362:
                    mountid = 1053;
                    expiration_days = -1L;
                    break;
                case 2430292:
                    mountid = 1145;
                    expiration_days = 90L;
                    break;
                case 2430294:
                    mountid = 1146;
                    expiration_days = 90L;
                    break;
                case 2430296:
                    mountid = 1147;
                    expiration_days = 90L;
                    break;
                case 2430298:
                    mountid = 1148;
                    expiration_days = 90L;
                    break;
                case 2430300:
                    mountid = 1149;
                    expiration_days = 90L;
                    break;
                case 2430302:
                    mountid = 1150;
                    expiration_days = 90L;
                    break;
                case 2430304:
                    mountid = 1151;
                    expiration_days = 90L;
                    break;
                case 2430306:
                    mountid = 1152;
                    expiration_days = 90L;
                    break;
                case 2430308:
                    mountid = 1153;
                    expiration_days = 90L;
                    break;
                case 2430310:
                    mountid = 1154;
                    expiration_days = 90L;
                    break;
                case 2430312:
                    mountid = 1156;
                    expiration_days = 90L;
                    break;
                case 2430314:
                    mountid = 1156;
                    expiration_days = 90L;
                    break;
                case 2430316:
                    mountid = 1118;
                    expiration_days = 90L;
                    break;
                case 2430318:
                    mountid = 1121;
                    expiration_days = 90L;
                    break;
                case 2430320:
                    mountid = 1122;
                    expiration_days = 90L;
                    break;
                case 2430322:
                    mountid = 1123;
                    expiration_days = 90L;
                    break;
                case 2430326:
                    mountid = 1129;
                    expiration_days = 90L;
                    break;
                case 2430328:
                    mountid = 1130;
                    expiration_days = 90L;
                    break;
                case 2430330:
                    mountid = 1063;
                    expiration_days = 90L;
                    break;
                case 2430332:
                    mountid = 1025;
                    expiration_days = 90L;
                    break;
                case 2430334:
                    mountid = 1034;
                    expiration_days = 90L;
                    break;
                case 2430336:
                    mountid = 1136;
                    expiration_days = 90L;
                    break;
                case 2430338:
                    mountid = 1051;
                    expiration_days = 90L;
                    break;
                case 2430340:
                    mountid = 1138;
                    expiration_days = 90L;
                    break;
                case 2430342:
                    mountid = 1139;
                    expiration_days = 90L;
                    break;
                case 2430344:
                    mountid = 1027;
                    expiration_days = 90L;
                    break;
                case 2430347:
                    mountid = 1029;
                    expiration_days = 90L;
                    break;
                case 2430349:
                    mountid = 1028;
                    expiration_days = 90L;
                    break;
                case 2430351:
                    mountid = 1033;
                    expiration_days = 90L;
                    break;
                case 2430353:
                    mountid = 1064;
                    expiration_days = 90L;
                    break;
                case 2430355:
                    mountid = 1096;
                    expiration_days = 90L;
                    break;
                case 2430357:
                    mountid = 1101;
                    expiration_days = 90L;
                    break;
                case 2430359:
                    mountid = 1102;
                    expiration_days = 90L;
                    break;
                case 2430361:
                    mountid = 1054;
                    expiration_days = 90L;
                    break;
                case 2430363:
                    mountid = 1053;
                    expiration_days = 90L;
                    break;
                case 2430324:
                    mountid = 1158;
                    expiration_days = -1L;
                    break;
                case 2430345:
                    mountid = 1158;
                    expiration_days = 90L;
                    break;
                case 2430367:
                    mountid = 1115;
                    expiration_days = 3L;
                    break;
                case 2430365:
                    mountid = 1025;
                    expiration_days = 365L;
                    break;
                case 2430366:
                    mountid = 1025;
                    expiration_days = 15L;
                    break;
                case 2430369:
                    mountid = 1049;
                    expiration_days = 10L;
                    break;
                case 2430392:
                    mountid = 80001038;
                    expiration_days = 90L;
                    break;
                case 2430476:
                    mountid = 1039;
                    expiration_days = 15L;
                    break;
                case 2430477:
                    mountid = 1039;
                    expiration_days = 365L;
                    break;
                case 2430232:
                    mountid = 1106;
                    expiration_days = 10L;
                    break;
                case 2430511:
                    mountid = 80001033;
                    expiration_days = 15L;
                    break;
                case 2430512:
                    mountid = 80001033;
                    expiration_days = 365L;
                    break;
                case 2430536:
                    mountid = 80001114;
                    expiration_days = -1L;
                    break;
                case 2430537:
                    mountid = 80001114;
                    expiration_days = 90L;
                    break;
                case 2430229:
                    mountid = 1102;
                    expiration_days = 60L;
                    break;
                case 2430199:
                    mountid = 1102;
                    expiration_days = 1L;
                    break;
                case 2430206:
                    mountid = 1089;
                    expiration_days = 7L;
                    break;
                case 2430211:
                    mountid = 80001009;
                    expiration_days = 30L;
                    break;
                default:
                    NPCScriptManager.getInstance().startItem(c, info.getNpc(), toUse.getItemId());
            }
        }

        if (mountid > 0) {
            mountid = PlayerStats.getSkillByJob(mountid, c.getPlayer().getJob());
            int fk = GameConstants.getMountItem(mountid, c.getPlayer());
            if ((GameConstants.GMS) && (fk > 0) && (mountid < 80001000)) {
                for (int i = 80001001; i < 80001999; i++) {
                    Skill skill = SkillFactory.getSkill(i);
                    if ((skill != null) && (GameConstants.getMountItem(skill.getId(), c.getPlayer()) == fk)) {
                        mountid = i;
                        break;
                    }
                }
            }
            if (c.getPlayer().getSkillLevel(mountid) > 0) {
                c.getPlayer().dropMessage(5, "您已经拥有了这个骑宠的技能.");
            } else if ((SkillFactory.getSkill(mountid) == null) || (GameConstants.getMountItem(mountid, c.getPlayer()) == 0)) {
                c.getPlayer().dropMessage(5, "您无法使用这个骑宠的技能.");
            } else if (expiration_days > 0L) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(mountid), 1, (byte) 1, System.currentTimeMillis() + expiration_days * 24L * 60L * 60L * 1000L);
                c.getPlayer().dropMessage(5, "恭喜您获得骑宠技能.");
            }
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static void UseSummonBag(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if ((!chr.isAlive()) || (chr.hasBlockedInventory()) || (chr.inPVP())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        c.getPlayer().updateTick(slea.readInt());
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        Item toUse = chr.getInventory(MapleInventoryType.USE).getItem((short) slot);
        if ((toUse != null) && (toUse.getQuantity() >= 1) && (toUse.getItemId() == itemId) && ((c.getPlayer().getMapId() < 910000000) || (c.getPlayer().getMapId() > 910000022))) {
            Map<String, Integer> toSpawn = MapleItemInformationProvider.getInstance().getEquipStats(itemId);
            if (toSpawn == null) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            MapleMonster ht = null;
            int type = 0;
            for (Entry<String, Integer> i : toSpawn.entrySet()) {
                if ((((String) i.getKey()).startsWith("mob")) && (Randomizer.nextInt(99) <= ((Integer) i.getValue()).intValue())) {
                    ht = MapleLifeFactory.getMonster(Integer.parseInt(((String) i.getKey()).substring(3)));
                    chr.getMap().spawnMonster_sSack(ht, chr.getPosition(), type);
                }
            }
            if (ht == null) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static void UseTreasureChest(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        short slot = slea.readShort();
        int itemid = slea.readInt();
        boolean useCash = slea.readByte() > 0;
        Item toUse = chr.getInventory(MapleInventoryType.ETC).getItem((short) (byte) slot);
        if ((toUse == null) || (toUse.getQuantity() <= 0) || (toUse.getItemId() != itemid) || (chr.hasBlockedInventory())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if ((!chr.getCheatTracker().canMZD()) && (!chr.isGM())) {
            chr.dropMessage(5, "你需要等待5秒之后才能使用谜之蛋.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        int keyIDforRemoval = 0;

        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int reward;
        String box;
        String key;
        int price;
        switch (toUse.getItemId()) {
            case 4280000:
                reward = RandomRewards.getGoldBoxReward();
                keyIDforRemoval = 5490000;
                box = "永恒的谜之蛋";
                key = "永恒的热度";
                price = 800;
                break;
            case 4280001:
                reward = RandomRewards.getSilverBoxReward();
                keyIDforRemoval = 5490001;
                box = "重生的谜之蛋";
                key = "重生的热度";
                price = 500;
                break;
            default:
                return;
        }

        int amount = 1;
        switch (reward) {
            case 2000004:
                amount = 200;
                break;
            case 2000005:
                amount = 100;
        }

        if ((useCash) && (chr.getCSPoints(2) < price)) {
            chr.dropMessage(1, new StringBuilder().append("抵用券不足").append(price).append("点，请到商城购买“抵用券兑换包”即可充值抵用券！").toString());
            c.getSession().write(MaplePacketCreator.enableActions());
        } else if (chr.getInventory(MapleInventoryType.CASH).countById(keyIDforRemoval) < 0) {
            chr.dropMessage(1, new StringBuilder().append("孵化").append(box).append("需要").append(key).append("，请到商城购买！").toString());
            c.getSession().write(MaplePacketCreator.enableActions());
        } else if ((chr.getInventory(MapleInventoryType.CASH).countById(keyIDforRemoval) > 0) || ((useCash) && (chr.getCSPoints(2) > price))) {
            Item item = MapleInventoryManipulator.addbyId_Gachapon(c, reward, (short) amount);
            if (item == null) {
                chr.dropMessage(1, "孵化失败，请重试一次。");
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, (short) (byte) slot, (short) 1, true);
            if (useCash) {
                chr.modifyCSPoints(2, -price, true);
            } else {
                MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, keyIDforRemoval, 1, true, false);
            }
            c.getSession().write(MaplePacketCreator.getShowItemGain(reward, (short) amount, true));
            byte rareness = GameConstants.gachaponRareItem(item.getItemId());
            if (rareness > 0) {
                Broadcast.broadcastMessage(MaplePacketCreator.getGachaponMega(c.getPlayer().getName(), new StringBuilder().append(" : 从").append(box).append("中获得{").append(ii.getName(item.getItemId())).append("}！大家一起恭喜他（她）吧！！！！").toString(), item, rareness, c.getChannel()));
            }
        } else {
            chr.dropMessage(5, new StringBuilder().append("孵化").append(box).append("失败，进检查是否有").append(key).append("或者抵用卷大于").append(price).append("点。").toString());
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static void Pickup_Player(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (c.getPlayer().hasBlockedInventory()) {
            return;
        }
        chr.updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        slea.skip(1);
        Point Client_Reportedpos = slea.readPos();
        if ((chr == null) || (chr.getMap() == null)) {
            return;
        }
        MapleMapObject ob = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.ITEM);
        if (ob == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        MapleMapItem mapitem = (MapleMapItem) ob;
        Lock lock = mapitem.getLock();
        lock.lock();
        try {
            if (mapitem.isPickedUp()) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if ((mapitem.getQuest() > 0) && (chr.getQuestStatus(mapitem.getQuest()) != 1)) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if ((mapitem.getOwner() != chr.getId()) && (((!mapitem.isPlayerDrop()) && (mapitem.getDropType() == 0)) || ((mapitem.isPlayerDrop()) && (chr.getMap().getEverlast())))) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if ((!mapitem.isPlayerDrop()) && (mapitem.getDropType() == 1) && (mapitem.getOwner() != chr.getId()) && ((chr.getParty() == null) || (chr.getParty().getMemberById(mapitem.getOwner()) == null))) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            double Distance = Client_Reportedpos.distanceSq(mapitem.getPosition());
            if ((Distance > 5000.0D) && ((mapitem.getMeso() > 0) || (mapitem.getItemId() != 4001025))) {
                chr.getCheatTracker().registerOffense(CheatingOffense.ITEMVAC_CLIENT, String.valueOf(Distance));
                Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, new StringBuilder().append("[GM Message] ").append(chr.getName()).append(" (等级 ").append(chr.getLevel()).append(") 全屏捡物。地图ID: ").append(chr.getMapId()).toString()));
            } else if (chr.getPosition().distanceSq(mapitem.getPosition()) > 640000.0D) {
                chr.getCheatTracker().registerOffense(CheatingOffense.ITEMVAC_SERVER);
                Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, new StringBuilder().append("[GM Message] ").append(chr.getName()).append(" (等级 ").append(chr.getLevel()).append(") 全屏捡物。地图ID: ").append(chr.getMapId()).toString()));
            }
            if (mapitem.getMeso() > 0) {
                if ((chr.getParty() != null) && (mapitem.getOwner() != chr.getId())) {
                    List<MapleCharacter> toGive = new LinkedList<MapleCharacter>();
                    int splitMeso = mapitem.getMeso() * 40 / 100;
                    for (MaplePartyCharacter z : chr.getParty().getMembers()) {
                        MapleCharacter m = chr.getMap().getCharacterById(z.getId());
                        if ((m != null) && (m.getId() != chr.getId())) {
                            toGive.add(m);
                        }
                    }
                    for (MapleCharacter m : toGive) {
                        m.gainMeso(splitMeso / toGive.size() + (m.getStat().hasPartyBonus ? (int) (mapitem.getMeso() / 20.0D) : 0), true);
                    }
                    chr.gainMeso(mapitem.getMeso() - splitMeso, true);
                } else {
                    chr.gainMeso(mapitem.getMeso(), true);
                }
                removeItem(chr, mapitem, ob);
            } else if (MapleItemInformationProvider.getInstance().isPickupBlocked(mapitem.getItemId())) {
                c.getSession().write(MaplePacketCreator.enableActions());
                c.getPlayer().dropMessage(5, "这个道具无法捡取.");
            } else if ((c.getPlayer().inPVP()) && (Integer.parseInt(c.getPlayer().getEventInstance().getProperty("ice")) == c.getPlayer().getId())) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                c.getSession().write(MaplePacketCreator.enableActions());
            } else if (useItem(c, mapitem.getItemId())) {
                removeItem(c.getPlayer(), mapitem, ob);

                if (mapitem.getItemId() / 10000 == 291) {
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getCapturePosition(c.getPlayer().getMap()));
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.resetCapture());
                }
            } else if ((mapitem.getItemId() / 10000 != 291) && (MapleInventoryManipulator.checkSpace(c, mapitem.getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner()))) {
                if ((mapitem.getItem().getQuantity() >= 50) && (mapitem.getItemId() == 2340000)) {
                    c.setMonitored(true);
                }
                MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster);
                removeItem(chr, mapitem, ob);
            } else {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        } finally {
            lock.unlock();
        }
    }

    public static void Pickup_Pet(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        if ((c.getPlayer().hasBlockedInventory()) || (c.getPlayer().inPVP())) {
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        byte petz = (byte) slea.readInt();
        MaplePet pet = chr.getPet(petz);
        slea.skip(1);
        chr.updateTick(slea.readInt());
        Point Client_Reportedpos = slea.readPos();
        MapleMapObject ob = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.ITEM);
        if ((ob == null) || (pet == null)) {
            return;
        }
        MapleMapItem mapitem = (MapleMapItem) ob;
        Lock lock = mapitem.getLock();
        lock.lock();
        try {
            if (mapitem.isPickedUp()) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return;
            }
            if ((mapitem.getOwner() != chr.getId()) && (mapitem.isPlayerDrop())) {
                return;
            }
            if ((mapitem.getOwner() != chr.getId()) && (((!mapitem.isPlayerDrop()) && (mapitem.getDropType() == 0)) || ((mapitem.isPlayerDrop()) && (chr.getMap().getEverlast())))) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if ((!mapitem.isPlayerDrop()) && (mapitem.getDropType() == 1) && (mapitem.getOwner() != chr.getId()) && ((chr.getParty() == null) || (chr.getParty().getMemberById(mapitem.getOwner()) == null))) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            double Distance = Client_Reportedpos.distanceSq(mapitem.getPosition());
            if ((Distance > 10000.0D) && ((mapitem.getMeso() > 0) || (mapitem.getItemId() != 4001025))) {
                chr.getCheatTracker().registerOffense(CheatingOffense.PET_ITEMVAC_CLIENT, String.valueOf(Distance));
                Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, new StringBuilder().append("[GM Message] ").append(chr.getName()).append(" (等级 ").append(chr.getLevel()).append(") 全屏宠吸。地图ID: ").append(chr.getMapId()).toString()));
            } else if (pet.getPos().distanceSq(mapitem.getPosition()) > 640000.0D) {
                chr.getCheatTracker().registerOffense(CheatingOffense.PET_ITEMVAC_SERVER);
                Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, new StringBuilder().append("[GM Message] ").append(chr.getName()).append(" (等级 ").append(chr.getLevel()).append(") 全屏宠吸。地图ID: ").append(chr.getMapId()).toString()));
            }
            if (mapitem.getMeso() > 0) {
                if ((chr.getParty() != null) && (mapitem.getOwner() != chr.getId())) {
                    List<MapleCharacter> toGive = new LinkedList<MapleCharacter>();
                    int splitMeso = mapitem.getMeso() * 40 / 100;
                    for (MaplePartyCharacter z : chr.getParty().getMembers()) {
                        MapleCharacter m = chr.getMap().getCharacterById(z.getId());
                        if ((m != null) && (m.getId() != chr.getId())) {
                            toGive.add(m);
                        }
                    }
                    for (MapleCharacter m : toGive) {
                        m.gainMeso(splitMeso / toGive.size() + (m.getStat().hasPartyBonus ? (int) (mapitem.getMeso() / 20.0D) : 0), true);
                    }
                    chr.gainMeso(mapitem.getMeso() - splitMeso, true);
                } else {
                    chr.gainMeso(mapitem.getMeso(), true);
                }
                removeItem_Pet(chr, mapitem, petz);
            } else if ((MapleItemInformationProvider.getInstance().isPickupBlocked(mapitem.getItemId())) || (mapitem.getItemId() / 10000 == 291)) {
                c.getSession().write(MaplePacketCreator.enableActions());
            } else if (useItem(c, mapitem.getItemId())) {
                removeItem_Pet(chr, mapitem, petz);
            } else if (MapleInventoryManipulator.checkSpace(c, mapitem.getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner())) {
                for (Iterator i$ = chr.getExcluded().iterator(); i$.hasNext();) {
                    int i = ((Integer) i$.next()).intValue();
                    if (mapitem.getItem().getItemId() == i) {
                        return;
                    }
                }
                if ((mapitem.getItem().getQuantity() >= 50) && (mapitem.getItemId() == 2340000)) {
                    c.setMonitored(true);
                }
                MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster);
                removeItem_Pet(chr, mapitem, petz);
            }
        } finally {
            lock.unlock();
        }
    }

    public static boolean useItem(MapleClient c, int id) {
        if (GameConstants.isUse(id)) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            MapleStatEffect eff = ii.getItemEffect(id);
            if (eff == null) {
                return false;
            }

            if (id / 10000 == 291) {
                boolean area = false;
                for (Rectangle rect : c.getPlayer().getMap().getAreas()) {
                    if (rect.contains(c.getPlayer().getTruePosition())) {
                        area = true;
                        break;
                    }
                }
                if ((!c.getPlayer().inPVP()) || ((c.getPlayer().getTeam() == id - 2910000) && (area))) {
                    return false;
                }
            }
            int consumeval = eff.getConsume();
            if (consumeval > 0) {
                consumeItem(c, eff);
                consumeItem(c, ii.getItemEffectEX(id));
                c.getSession().write(MaplePacketCreator.getShowItemGain(id, (short) 1));
                return true;
            }
        }
        return false;
    }

    public static void consumeItem(MapleClient c, MapleStatEffect eff) {
        if (eff == null) {
            return;
        }
        if (eff.getConsume() == 2) {
            if ((c.getPlayer().getParty() != null) && (c.getPlayer().isAlive())) {
                for (MaplePartyCharacter pc : c.getPlayer().getParty().getMembers()) {
                    MapleCharacter chr = c.getPlayer().getMap().getCharacterById(pc.getId());
                    if ((chr != null) && (chr.isAlive())) {
                        eff.applyTo(chr);
                    }
                }
            } else {
                eff.applyTo(c.getPlayer());
            }
        } else if (c.getPlayer().isAlive()) {
            eff.applyTo(c.getPlayer());
        }
    }

    public static void removeItem_Pet(MapleCharacter chr, MapleMapItem mapitem, int pet) {
        mapitem.setPickedUp(true);
        chr.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, chr.getId(), pet));
        chr.getMap().removeMapObject(mapitem);
        if (mapitem.isRandDrop()) {
            chr.getMap().spawnRandDrop();
        }
    }

    private static void removeItem(MapleCharacter chr, MapleMapItem mapitem, MapleMapObject ob) {
        mapitem.setPickedUp(true);
        chr.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, chr.getId()), mapitem.getPosition());
        chr.getMap().removeMapObject(ob);
        if (mapitem.isRandDrop()) {
            chr.getMap().spawnRandDrop();
        }
    }

    public static void OwlMinerva(LittleEndianAccessor slea, MapleClient c) {
        byte slot = (byte) slea.readShort();
        int itemid = slea.readInt();
        Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem((short) slot);
        if ((toUse != null) && (toUse.getQuantity() > 0) && (toUse.getItemId() == itemid) && (itemid == 2310000) && (!c.getPlayer().hasBlockedInventory())) {
            int itemSearch = slea.readInt();
            List hms = c.getChannelServer().searchMerchant(itemSearch);
            if (hms.size() > 0) {
                c.getSession().write(MaplePacketCreator.getOwlSearched(itemSearch, hms));
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemid, 1, true, false);
            } else {
                c.getPlayer().dropMessage(1, "没有找到这个道具.");
            }
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static void Owl(LittleEndianAccessor slea, MapleClient c) {
        if ((c.getPlayer().getMapId() >= 910000000) && (c.getPlayer().getMapId() <= 910000022)) {
            c.getSession().write(MaplePacketCreator.getOwlOpen());
        } else {
            c.getPlayer().dropMessage(5, "商店搜索器只能在自由市场使用.");
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static void OwlWarp(LittleEndianAccessor slea, MapleClient c) {
        c.getSession().write(MaplePacketCreator.enableActions());
        if ((c.getPlayer().getMapId() >= 910000000) && (c.getPlayer().getMapId() <= 910000022) && (!c.getPlayer().hasBlockedInventory())) {
            int id = slea.readInt();
            int type = slea.readByte();
            int map = slea.readInt();
            if ((map >= 910000001) && (map <= 910000022)) {
                MapleMap mapp = c.getChannelServer().getMapFactory().getMap(map);
                c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                HiredMerchant merchant = null;
                ;
                switch (1) {
                    case 0:
                        List<MapleMapObject> objects = mapp.getAllHiredMerchantsThreadsafe();
                        for (MapleMapObject ob : objects) {
                            if ((ob instanceof IMaplePlayerShop)) {
                                IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                                if ((ips instanceof HiredMerchant)) {
                                    HiredMerchant merch = (HiredMerchant) ips;
                                    if (merch.getOwnerId() == id) {
                                        merchant = merch;
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    case 1:
                        objects = mapp.getAllHiredMerchantsThreadsafe();
                        for (MapleMapObject ob : objects) {
                            if ((ob instanceof IMaplePlayerShop)) {
                                IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                                if ((ips instanceof HiredMerchant)) {
                                    HiredMerchant merch = (HiredMerchant) ips;
                                    if (merch.getStoreId() == id) {
                                        merchant = merch;
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    default:
                        MapleMapObject ob = mapp.getMapObject(id, MapleMapObjectType.HIRED_MERCHANT);
                        if (!(ob instanceof IMaplePlayerShop)) {
                            break;
                        }
                        IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                        if (!(ips instanceof HiredMerchant)) {
                            break;
                        }
                        merchant = (HiredMerchant) ips;
                }

                if (merchant != null) {
                    if (merchant.isOwner(c.getPlayer())) {
                        merchant.setOpen(false);
                        merchant.removeAllVisitors(18, 1);
                        c.getPlayer().setPlayerShop(merchant);
                        c.getSession().write(PlayerShopPacket.getHiredMerch(c.getPlayer(), merchant, false));
                    } else if ((!merchant.isOpen()) || (!merchant.isAvailable())) {
                        c.getPlayer().dropMessage(1, "主人正在整理商店物品\r\n请稍后再度光临！");
                    } else if (merchant.getFreeSlot() == -1) {
                        c.getPlayer().dropMessage(1, "店铺已达到最大人数\r\n请稍后再度光临！");
                    } else if (merchant.isInBlackList(c.getPlayer().getName())) {
                        c.getPlayer().dropMessage(1, "你被禁止进入该店铺.");
                    } else {
                        c.getPlayer().setPlayerShop(merchant);
                        merchant.addVisitor(c.getPlayer());
                        c.getSession().write(PlayerShopPacket.getHiredMerch(c.getPlayer(), merchant, false));
                    }
                } else {
                    c.getPlayer().dropMessage(1, "主人正在整理商店物品\r\n请稍后再度光临！");
                }
            }
        }
    }

    public static void PamSong(LittleEndianAccessor slea, MapleClient c) {
        Item pam = c.getPlayer().getInventory(MapleInventoryType.CASH).findById(5640000);
        if ((slea.readByte() > 0) && (c.getPlayer().getScrolledPosition() != 0) && (pam != null) && (pam.getQuantity() > 0)) {
            MapleInventoryType inv = c.getPlayer().getScrolledPosition() < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP;
            Item item = c.getPlayer().getInventory(inv).getItem(c.getPlayer().getScrolledPosition());
            c.getPlayer().setScrolledPosition((short) 0);
            if (item != null) {
                Equip eq = (Equip) item;
                eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() + 1));
                c.getPlayer().forceReAddItem_Flag(eq, inv);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, pam.getPosition(), (short) 1, true, false);
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.pamsSongEffect(c.getPlayer().getId()));
            }
        } else {
            c.getPlayer().setScrolledPosition((short) 0);
        }
    }

    public static void TeleRock(LittleEndianAccessor slea, MapleClient c) {
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem((short) slot);
        if ((toUse == null) || (toUse.getQuantity() < 1) || (toUse.getItemId() != itemId) || (itemId / 10000 != 232) || (c.getPlayer().hasBlockedInventory())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        boolean used = UseTeleRock(slea, c, itemId);
        if (used) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static boolean UseTeleRock(LittleEndianAccessor slea, MapleClient c, int itemId) {
        boolean used = false;
        if (slea.readByte() == 0) {
            MapleMap target = c.getChannelServer().getMapFactory().getMap(slea.readInt());
            if (((itemId == 5041000) && (c.getPlayer().isRockMap(target.getId()))) || ((itemId != 5041000) && (c.getPlayer().isRegRockMap(target.getId()))) || (((itemId == 5040004) || (itemId == 5041001)) && ((c.getPlayer().isHyperRockMap(target.getId())) || (GameConstants.isHyperTeleMap(target.getId())))
                    && (!FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())) && (!FieldLimitType.VipRock.check(target.getFieldLimit())) && (!c.getPlayer().isInBlockedMap()))) {
                c.getPlayer().changeMap(target, target.getPortal(0));
                used = true;
            }
        } else {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
            if ((victim != null) && (!victim.isIntern()) && (c.getPlayer().getEventInstance() == null) && (victim.getEventInstance() == null)) {
                if ((!FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())) && (!FieldLimitType.VipRock.check(c.getChannelServer().getMapFactory().getMap(victim.getMapId()).getFieldLimit())) && (!victim.isInBlockedMap()) && (!c.getPlayer().isInBlockedMap()) && ((itemId == 5041000) || (itemId == 5040004) || (itemId == 5041001) || (victim.getMapId() / 100000000 == c.getPlayer().getMapId() / 100000000))) {
                    c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestPortal(victim.getTruePosition()));
                    used = true;
                }
            } else {
                c.getPlayer().dropMessage(1, "在此频道未找到该玩家.");
            }
        }
        return used;
    }

    public static void UseNebulite(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {//使用星岩
        chr.updateTick(slea.readInt());
        chr.setScrolledPosition((short) 0);
        Item nebulite = chr.getInventory(MapleInventoryType.SETUP).getItem((short) (byte) slea.readShort());
        int nebuliteId = slea.readInt();
        Item toMount = chr.getInventory(MapleInventoryType.EQUIP).getItem((short) (byte) slea.readShort());
        if ((nebulite == null) || (nebuliteId != nebulite.getItemId()) || (toMount == null) || (chr.hasBlockedInventory())) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return;
        }
        Equip eqq = (Equip) toMount;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        boolean success = false;
        if (eqq.getSocket1() == 0) {
            StructItemOption pot = ii.getSocketInfo(nebuliteId);
            if ((pot != null) && (GameConstants.optionTypeFits(pot.optionType, eqq.getItemId()))) {
                eqq.setSocket1(pot.opID);
            }

            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.SETUP, nebulite.getPosition(), (short) 1, false);
            chr.forceReAddItem(toMount, MapleInventoryType.EQUIP);
            success = true;
        }
        chr.getMap().broadcastMessage(MaplePacketCreator.showNebuliteEffect(c.getPlayer().getId(), success));
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static void UseAlienSocket(LittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        Item alienSocket = c.getPlayer().getInventory(MapleInventoryType.USE).getItem((short) (byte) slea.readShort());
        int alienSocketId = slea.readInt();
        Item toMount = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) (byte) slea.readShort());
        if ((alienSocket == null) || (alienSocketId != alienSocket.getItemId()) || (toMount == null) || (c.getPlayer().hasBlockedInventory())) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return;
        }

        Equip eqq = (Equip) toMount;
        if (eqq.getSocketState() != 0) {
            c.getPlayer().dropMessage(1, "This item already has a socket.");
        } else {
            eqq.setSocket1(0);
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, alienSocket.getPosition(), (short) 1, false);
            c.getPlayer().forceReAddItem(toMount, MapleInventoryType.EQUIP);
        }
        c.getSession().write(MTSCSPacket.useAlienSocket(true));
    }

    public static void UseNebuliteFusion(LittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        int nebuliteId1 = slea.readInt();
        Item nebulite1 = c.getPlayer().getInventory(MapleInventoryType.SETUP).getItem((short) (byte) slea.readShort());
        int nebuliteId2 = slea.readInt();
        Item nebulite2 = c.getPlayer().getInventory(MapleInventoryType.SETUP).getItem((short) (byte) slea.readShort());
        int mesos = slea.readInt();
        int premiumQuantity = slea.readInt();
        if ((nebulite1 == null) || (nebulite2 == null) || (nebuliteId1 != nebulite1.getItemId()) || (nebuliteId2 != nebulite2.getItemId()) || ((mesos == 0) && (premiumQuantity == 0)) || ((mesos != 0) && (premiumQuantity != 0)) || (mesos < 0) || (premiumQuantity < 0) || (c.getPlayer().hasBlockedInventory())) {
            c.getPlayer().dropMessage(1, "Failed to fuse Nebulite.");
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return;
        }
        int grade1 = GameConstants.getNebuliteGrade(nebuliteId1);
        int grade2 = GameConstants.getNebuliteGrade(nebuliteId2);
        int highestRank = grade1 > grade2 ? grade1 : grade2;
        if ((grade1 == -1) || (grade2 == -1) || ((highestRank == 3) && (premiumQuantity != 2)) || ((highestRank == 2) && (premiumQuantity != 1)) || ((highestRank == 1) && (mesos != 5000)) || ((highestRank == 0) && (mesos != 3000)) || ((mesos > 0) && (c.getPlayer().getMeso() < mesos)) || ((premiumQuantity > 0) && (c.getPlayer().getItemQuantity(4420000, false) < premiumQuantity)) || (grade1 >= 4) || (grade2 >= 4) || (c.getPlayer().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < 1)) {
            c.getSession().write(MaplePacketCreator.useNebuliteFusion(c.getPlayer().getId(), 0, false));
            return;
        }
        int avg = (grade1 + grade2) / 2;
        int rank = Randomizer.nextInt(100) < 4 ? 0 : avg != 0 ? avg - 1 : Randomizer.nextInt(100) < 70 ? avg : avg != 3 ? avg + 1 : avg;

        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        List pots = new LinkedList(ii.getAllSocketInfo(rank).values());
        int newId = 0;
        while (newId == 0) {
            StructItemOption pot = (StructItemOption) pots.get(Randomizer.nextInt(pots.size()));
            if (pot != null) {
                newId = pot.opID;
            }
        }
        if (mesos > 0) {
            c.getPlayer().gainMeso(-mesos, true);
        } else if (premiumQuantity > 0) {
            MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4420000, premiumQuantity, false, false);
        }
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.SETUP, nebulite1.getPosition(), (short) 1, false);
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.SETUP, nebulite2.getPosition(), (short) 1, false);
        MapleInventoryManipulator.addById(c, newId, (short) 1, new StringBuilder().append("Fused from ").append(nebuliteId1).append(" and ").append(nebuliteId2).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
        c.getSession().write(MaplePacketCreator.useNebuliteFusion(c.getPlayer().getId(), newId, true));
    }
}