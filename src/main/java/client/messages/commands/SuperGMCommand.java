package client.messages.commands;

import client.*;
import client.anticheat.CheatingOffense;
import client.inventory.*;
import client.messages.CommandProcessorUtil;
import constants.BattleConstants;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.MapleServerHandler;
import handling.channel.ChannelServer;
import handling.world.World;
import java.awt.Point;
import java.io.InputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import server.*;
import server.Timer;
import server.life.*;
import server.maps.*;
import server.quest.MapleQuest;
import tools.HexTool;
import tools.MaplePacketCreator;
import tools.StringUtil;
import tools.packet.MobPacket;

public class SuperGMCommand {

    public static ServerConstants.PlayerGMRank getPlayerLevelRequired() {
        return ServerConstants.PlayerGMRank.SUPERGM;
    }

    public static class 漂浮公告 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "用法: !漂浮公告 <道具ID> <公告信息>");
                return 0;
            }
            int itemId = Integer.parseInt(splitted[1]);
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " 这个道具不存在.");
                return 0;
            }
            if (!ii.isFloatCashItem(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " 不具有漂浮公告的效果.");
                return 0;
            }
            World.Broadcast.startMapEffect(StringUtil.joinStringFrom(splitted, 2), itemId);
            return 1;
        }
    }

    public static class ItemSize extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "当前加载的物品数量: " + MapleItemInformationProvider.getInstance().getAllItems().size());
            return 1;
        }
    }

    public static class BuffItemEX extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 1) {
                c.getPlayer().dropMessage(6, "用法: !BuffItemEX <道具ID>");
                return 0;
            }
            int itemId = Integer.parseInt(splitted[1]);
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " 这个道具不存在.");
                return 0;
            }
            ii.getItemEffectEX(itemId).applyTo(c.getPlayer());
            return 1;
        }
    }

    public static class BuffItem extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 1) {
                c.getPlayer().dropMessage(6, "用法: !BuffItem <道具ID>");
                return 0;
            }
            int itemId = Integer.parseInt(splitted[1]);
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " 这个道具不存在.");
                return 0;
            }
            ii.getItemEffect(Integer.parseInt(splitted[1])).applyTo(c.getPlayer());
            return 1;
        }
    }

    public static class 技能 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "用法: !技能 <技能ID> <技能等级>");
                return 0;
            }
            SkillFactory.getSkill(Integer.parseInt(splitted[1])).getEffect(Integer.parseInt(splitted[2])).applyTo(c.getPlayer());
            return 1;
        }
    }

    public static class SendAllNote extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            String text;
            if (splitted.length >= 1) {
                text = StringUtil.joinStringFrom(splitted, 1);
                for (MapleCharacter mch : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                    c.getPlayer().sendNote(mch.getName(), text);
                    if (mch != null) {
                        mch.showNote();
                    }
                }
            } else {
                c.getPlayer().dropMessage(6, "用法: !sendallnote <text>");
                return 0;
            }
            return 1;
        }
    }

    public static class ResetReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetReactors();
            return 1;
        }
    }

    public static class SetReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().setReactorState(Byte.parseByte(splitted[1]));
            return 1;
        }
    }

    public static class DReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            List<MapleMapObject> reactors = map.getMapObjectsInRange(c.getPlayer().getPosition(), (1.0D / 0.0D), Arrays.asList(new MapleMapObjectType[]{MapleMapObjectType.REACTOR}));
            if (splitted[1].equals("all")) {
                for (MapleMapObject reactorL : reactors) {
                    MapleReactor reactor2l = (MapleReactor) reactorL;
                    c.getPlayer().getMap().destroyReactor(reactor2l.getObjectId());
                }
            } else {
                c.getPlayer().getMap().destroyReactor(Integer.parseInt(splitted[1]));
            }
            return 1;
        }
    }

    public static class FHReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().getReactorByOid(Integer.parseInt(splitted[1])).forceHitReactor(Byte.parseByte(splitted[2]));
            return 1;
        }
    }

    public static class HReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().getReactorByOid(Integer.parseInt(splitted[1])).hitReactor(c);
            return 1;
        }
    }

    public static class FCompleteQuest extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceComplete(c.getPlayer(), Integer.parseInt(splitted[2]));
            return 1;
        }
    }

    public static class FStartQuest extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceStart(c.getPlayer(), Integer.parseInt(splitted[2]), splitted.length >= 4 ? splitted[3] : null);
            return 1;
        }
    }

    public static class CompleteQuest extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).complete(c.getPlayer(), Integer.parseInt(splitted[2]), Integer.valueOf(Integer.parseInt(splitted[3])));
            return 1;
        }
    }

    public static class StartQuest extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).start(c.getPlayer(), Integer.parseInt(splitted[2]));
            return 1;
        }
    }

    public static class ResetQuest extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forfeit(c.getPlayer());
            return 1;
        }
    }

    public static class ResetMap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetFully();
            return 1;
        }
    }

    public static class GetPokemon extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if ((c.getPlayer().countBattlers() >= 6) || (splitted.length < 2)) {
                c.getPlayer().dropMessage(5, "You have 6 pokemons already, !getpokemon <mobID>");
                return 0;
            }
            MapleMonsterStats theMob = MapleLifeFactory.getMonsterStats(Integer.parseInt(splitted[1]));
            if (theMob == null) {
                c.getPlayer().dropMessage(5, "输入的怪物ID不存在.");
                return 0;
            }
            Battler theB = new Battler(theMob.getLevel(), 0, c.getPlayer().getId(), theMob.getId(), theMob.getName(), BattleConstants.PokemonNature.randomNature(), 0, (byte) -1, (byte) 100, (byte) 100, (byte) 100, (byte) 100, (byte) 100, (byte) 100, (byte) 100, (byte) 100, (byte) Randomizer.nextInt(2));
            if (theB.getFamily() == null) {
                c.getPlayer().dropMessage(5, "mob cannot be used in pokemon.");
                return 0;
            }
            c.getPlayer().getBattlers()[c.getPlayer().countBattlers()] = theB;
            c.getPlayer().getMonsterBook().monsterCaught(c, theMob.getId(), theMob.getName());
            c.getPlayer().changedBattler();
            c.getPlayer().dropMessage(6, "操作完成.");
            return 1;
        }
    }

    public static class ClearPokedex extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMonsterBook().getCards().clear();
            c.getPlayer().getMonsterBook().changed();
            c.getPlayer().dropMessage(5, "操作完成.");
            return 1;
        }
    }

    public static class SeePokedex extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (BattleConstants.PokedexEntry e : BattleConstants.getAllPokedex()) {
                c.getPlayer().getMonsterBook().getCards().put(Integer.valueOf(e.id), Integer.valueOf(1));
            }
            c.getPlayer().getMonsterBook().changed();
            c.getPlayer().dropMessage(5, "操作完成.");
            return 1;
        }
    }

    public static class FillPokedex extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (BattleConstants.PokedexEntry e : BattleConstants.getAllPokedex()) {
                c.getPlayer().getMonsterBook().getCards().put(Integer.valueOf(e.id), Integer.valueOf(2));
            }
            c.getPlayer().getMonsterBook().changed();
            c.getPlayer().dropMessage(5, "操作完成.");
            return 1;
        }
    }

    public static class ListBook extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            List mbList = new ArrayList(MapleItemInformationProvider.getInstance().getMonsterBook().entrySet());
            Collections.sort(mbList, new BookComparator());
            int page = Integer.parseInt(splitted[1]);
            for (int e = page * 8; e < Math.min(mbList.size(), (page + 1) * 8); e++) {
                c.getPlayer().dropMessage(6, e + ": " + ((Map.Entry) mbList.get(e)).getKey() + " - " + ((Map.Entry) mbList.get(e)).getValue());
            }
            return 0;
        }

        public static class BookComparator implements Comparator<Map.Entry<Integer, Integer>>, Serializable {

            @Override
            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                if (((Integer) o1.getValue()).intValue() > ((Integer) o2.getValue()).intValue()) {
                    return 1;
                }
                if (o1.getValue() == o2.getValue()) {
                    return 0;
                }
                return -1;
            }
        }
    }

    public static class FillBook extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (Iterator i$ = MapleItemInformationProvider.getInstance().getMonsterBook().keySet().iterator(); i$.hasNext();) {
                int e = ((Integer) i$.next()).intValue();
                c.getPlayer().getMonsterBook().getCards().put(Integer.valueOf(e), Integer.valueOf(2));
            }
            c.getPlayer().getMonsterBook().changed();
            c.getPlayer().dropMessage(5, "完成.");
            return 1;
        }
    }

    public static class AddIPMonitor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleServerHandler.addIP(splitted[1]);
            return 1;
        }
    }

    public static class ReloadIPMonitor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleServerHandler.reloadLoggedIPs();
            return 1;
        }
    }

    public static class Rev extends CommandExecute {

        private static int revision = -1;

        public static int getRevision() {
            if (revision != -1) {
                return revision;
            }
            InputStream svninfo = AdminCommand.class.getResourceAsStream("/all-wcprops");
            if (svninfo == null) {
                return revision;
            }
            Scanner sc = new Scanner(svninfo);
            while (sc.hasNext()) {
                String[] s = sc.next().split("/");
                if ((s.length > 1) && (s[1].equals("svn"))) {
                    revision = Integer.parseInt(s[5]);
                    break;
                }
            }
            sc.close();

            return revision;
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            if (getRevision() != -1) {
                player.dropMessage(5, "This is revision " + revision + ".");
            } else {
                player.dropMessage(5, "Can't find revision T_T");
            }
            return 1;
        }
    }

    public static class AddHost extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                ServerConstants.eligibleIP.add(InetAddress.getByName(splitted[1]).getHostAddress().replace("/", ""));
                c.getPlayer().dropMessage(5, "IP: " + splitted[1] + " added. (" + InetAddress.getByName(splitted[1]).getHostAddress() + ")");
            } catch (Exception e) {
                return 0;
            }
            return 1;
        }
    }

    public static class AddIP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            ServerConstants.eligibleIP.add(splitted[1].replace("/", ""));
            c.getPlayer().dropMessage(5, "IP: " + splitted[1] + " added.");
            return 1;
        }
    }

    public static class Crash extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if ((victim != null) && (c.getPlayer().getGMLevel() >= victim.getGMLevel())) {
                victim.getClient().getSession().write(HexTool.getByteArrayFromHexString("1A 00"));
                return 1;
            }
            c.getPlayer().dropMessage(6, "The victim does not exist.");
            return 0;
        }
    }

    public static class TestBuffTimer extends SuperGMCommand.TestTimer {

        public TestBuffTimer() {
            this.toTest = Timer.BuffTimer.getInstance();
        }
    }

    public static class TestWorldTimer extends SuperGMCommand.TestTimer {

        public TestWorldTimer() {
            this.toTest = Timer.WorldTimer.getInstance();
        }
    }

    public static class TestMapTimer extends SuperGMCommand.TestTimer {

        public TestMapTimer() {
            this.toTest = Timer.MapTimer.getInstance();
        }
    }

    public static class TestEtcTimer extends SuperGMCommand.TestTimer {

        public TestEtcTimer() {
            this.toTest = Timer.EtcTimer.getInstance();
        }
    }

    public static class TestCloneTimer extends SuperGMCommand.TestTimer {

        public TestCloneTimer() {
            this.toTest = Timer.CloneTimer.getInstance();
        }
    }

    public static class TestEventTimer extends SuperGMCommand.TestTimer {

        public TestEventTimer() {
            this.toTest = Timer.EventTimer.getInstance();
        }
    }

    public static abstract class TestTimer extends CommandExecute {

        protected Timer toTest = null;

        @Override
        public int execute(final MapleClient c, String[] splitted) {
            final int sec = Integer.parseInt(splitted[1]);
            c.getPlayer().dropMessage(5, "Message will pop up in " + sec + " seconds.");
            c.getPlayer().dropMessage(5, "Active: " + this.toTest.getSES().getActiveCount() + " Core: " + this.toTest.getSES().getCorePoolSize() + " Largest: " + this.toTest.getSES().getLargestPoolSize() + " Max: " + this.toTest.getSES().getMaximumPoolSize() + " Current: " + this.toTest.getSES().getPoolSize() + " Status: " + this.toTest.getSES().isShutdown() + this.toTest.getSES().isTerminated() + this.toTest.getSES().isTerminating());
            final long oldMillis = System.currentTimeMillis();
            this.toTest.schedule(new Runnable() {

                @Override
                public void run() {
                    c.getPlayer().dropMessage(5, "Message has popped up in " + (System.currentTimeMillis() - oldMillis) / 1000L + " seconds, expected was " + sec + " seconds");
                    c.getPlayer().dropMessage(5, "Active: " + SuperGMCommand.TestTimer.this.toTest.getSES().getActiveCount() + " Core: " + SuperGMCommand.TestTimer.this.toTest.getSES().getCorePoolSize() + " Largest: " + SuperGMCommand.TestTimer.this.toTest.getSES().getLargestPoolSize() + " Max: " + SuperGMCommand.TestTimer.this.toTest.getSES().getMaximumPoolSize() + " Current: " + SuperGMCommand.TestTimer.this.toTest.getSES().getPoolSize() + " Status: " + SuperGMCommand.TestTimer.this.toTest.getSES().isShutdown() + SuperGMCommand.TestTimer.this.toTest.getSES().isTerminated() + SuperGMCommand.TestTimer.this.toTest.getSES().isTerminating());
                }
            }, sec * 1000);

            return 1;
        }
    }

    public static class Respawn extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().respawn(true);
            return 1;
        }
    }

    public static class ReloadMap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int mapId = Integer.parseInt(splitted[1]);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                if ((cserv.getMapFactory().isMapLoaded(mapId)) && (cserv.getMapFactory().getMap(mapId).getCharactersSize() > 0)) {
                    c.getPlayer().dropMessage(5, "There exists characters on channel " + cserv.getChannel());
                    return 0;
                }
            }
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                if (cserv.getMapFactory().isMapLoaded(mapId)) {
                    cserv.getMapFactory().removeMap(mapId);
                }
            }
            return 1;
        }
    }

    public static class PTS extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                try {
                    //c.getSession().getHandler().messageReceived(c.getSession(), MaplePacketCreator.getPacketFromHexString(StringUtil.joinStringFrom(splitted, 1)));
                } catch (Exception e) {
                    c.getPlayer().dropMessage(6, "错误: " + e);
                }
            } else {
                c.getPlayer().dropMessage(6, "请输入要测试的数据包信息!");
            }
            return 1;
        }
    }

    public static class Packet extends SuperGMCommand.P {
    }

    public static class P extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                c.getSession().write(MaplePacketCreator.getPacketFromHexString(StringUtil.joinStringFrom(splitted, 1)));
            } else {
                c.getPlayer().dropMessage(6, "请输入要测试的数据包信息!");
            }
            return 1;
        }
    }

    public static class CPS extends SuperGMCommand.PS {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            builder = new StringBuilder();
            return 1;
        }
    }

    public static class APS extends SuperGMCommand.PS {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                builder.append(StringUtil.joinStringFrom(splitted, 1));
                c.getPlayer().dropMessage(6, "String is now: " + builder.toString());
            } else {
                c.getPlayer().dropMessage(6, "请输入要测试的数据包信息!");
            }
            return 1;
        }
    }

    public static class PS extends CommandExecute {

        protected static StringBuilder builder = new StringBuilder();

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (builder.length() > 1) {
                c.getSession().write(MaplePacketCreator.getPacketFromHexString(builder.toString()));
                builder = new StringBuilder();
            } else {
                c.getPlayer().dropMessage(6, "请输入要测试的数据包信息!");
            }
            return 1;
        }
    }

    public static class Spawn extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int mid = Integer.parseInt(splitted[1]);
            int num = Math.min(CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1), 500);
            Integer level = CommandProcessorUtil.getNamedIntArg(splitted, 1, "lvl");
            Long hp = CommandProcessorUtil.getNamedLongArg(splitted, 1, "hp");
            Integer exp = CommandProcessorUtil.getNamedIntArg(splitted, 1, "exp");
            Double php = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "php");
            Double pexp = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "pexp");
            MapleMonster onemob;
            try {
                onemob = MapleLifeFactory.getMonster(mid);
            } catch (RuntimeException e) {
                c.getPlayer().dropMessage(5, "错误: " + e.getMessage());
                return 0;
            }
            if (onemob == null) {
                c.getPlayer().dropMessage(5, "输入的怪物不存在.");
                return 0;
            }
            long newhp = 0L;
            int newexp = 0;
            if (hp != null) {
                newhp = hp.longValue();
            } else if (php != null) {
                newhp = (long) (onemob.getMobMaxHp() * (php.doubleValue() / 100.0D));
            } else {
                newhp = onemob.getMobMaxHp();
            }
            if (exp != null) {
                newexp = exp.intValue();
            } else if (pexp != null) {
                newexp = (int) (onemob.getMobExp() * (pexp.doubleValue() / 100.0D));
            } else {
                newexp = onemob.getMobExp();
            }
            if (newhp < 1L) {
                newhp = 1L;
            }

            OverrideMonsterStats overrideStats = new OverrideMonsterStats(newhp, onemob.getMobMaxMp(), newexp, false);
            for (int i = 0; i < num; i++) {
                MapleMonster mob = MapleLifeFactory.getMonster(mid);
                mob.setHp(newhp);
                if (level != null) {
                    mob.changeLevel(level.intValue(), false);
                } else {
                    mob.setOverrideStats(overrideStats);
                }
                c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, c.getPlayer().getPosition());
            }
            return 1;
        }
    }

    public static class ServerMessage extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            String outputMessage = StringUtil.joinStringFrom(splitted, 1);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                cserv.setServerMessage(outputMessage);
            }
            return 1;
        }
    }

    public static class DestroyPNPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                c.getPlayer().dropMessage(6, "Destroying playerNPC...");
                MapleNPC npc = c.getPlayer().getMap().getNPCByOid(Integer.parseInt(splitted[1]));
                if ((npc instanceof PlayerNPC)) {
                    ((PlayerNPC) npc).destroy(true);
                    c.getPlayer().dropMessage(6, "Done");
                } else {
                    c.getPlayer().dropMessage(6, "!destroypnpc [objectid]");
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "NPC failed... : " + e.getMessage());
            }
            return 1;
        }
    }

    public static class MakeOfflineP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                c.getPlayer().dropMessage(6, "Making playerNPC...");
                MapleClient cs = new MapleClient(null, null, null);
                MapleCharacter chhr = MapleCharacter.loadCharFromDB(MapleCharacterUtil.getIdByName(splitted[1]), cs, false);
                if (chhr == null) {
                    c.getPlayer().dropMessage(6, splitted[1] + " does not exist");
                    return 0;
                }
                PlayerNPC npc = new PlayerNPC(chhr, Integer.parseInt(splitted[2]), c.getPlayer().getMap(), c.getPlayer());
                npc.addToServer();
                c.getPlayer().dropMessage(6, "Done");
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "NPC failed... : " + e.getMessage());
            }
            return 1;
        }
    }

    public static class 制作永久NPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                c.getPlayer().dropMessage(6, "创立玩家NPC...");
                MapleCharacter chhr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (chhr == null) {
                    c.getPlayer().dropMessage(6, splitted[1] + " 不在线");
                    return 0;
                }
                PlayerNPC npc = new PlayerNPC(chhr, Integer.parseInt(splitted[2]), c.getPlayer().getMap(), c.getPlayer());
                npc.addToServer();
                c.getPlayer().dropMessage(6, "做");
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "NPC失败... : " + e.getMessage());
            }
            return 1;
        }
    }

    public static class 制作永久怪物 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "用法: !制作永久怪物 <怪物ID> <刷新时间 默认按秒计算>");
                return 0;
            }
            int mobid = Integer.parseInt(splitted[1]);
            int mobTime = Integer.parseInt(splitted[2]);
            if (splitted[2] == null) {
                mobTime = 1;
            }
            MapleMonster mob = MapleLifeFactory.getMonster(mobid);
            if (mob != null) {
                int xpos = c.getPlayer().getPosition().x;
                int ypos = c.getPlayer().getPosition().y;
                int fh = c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId();
                mob.setPosition(c.getPlayer().getPosition());
                mob.setCy(ypos);
                mob.setRx0(xpos + 50);
                mob.setRx1(xpos - 50);
                mob.setFh(fh);
                try {
                    PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO spawns ( idd, f, fh, cy, rx0, rx1, type, x, y, mid, mobtime ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
                    ps.setInt(1, mobid);
                    ps.setInt(2, 0);
                    ps.setInt(3, fh);
                    ps.setInt(4, ypos);
                    ps.setInt(5, xpos + 50);
                    ps.setInt(6, xpos - 50);
                    ps.setString(7, "m");
                    ps.setInt(8, xpos);
                    ps.setInt(9, ypos);
                    ps.setInt(10, c.getPlayer().getMapId());
                    ps.setInt(11, mobTime);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    c.getPlayer().dropMessage(6, "保存怪物信息到数据库中出现错误.");
                }
                c.getPlayer().getMap().addMonsterSpawn(mob, mobTime, (byte) -1, null);
            } else {
                c.getPlayer().dropMessage(6, "你应该输入一个正确的 怪物-Id.");
                return 0;
            }
            return 1;
        }
    }

    public static class MakeNpc extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 1) {
                c.getPlayer().dropMessage(6, "用法: !MakeNpc <NpcID>");
                return 0;
            }
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            if ((npc != null) && (!npc.getName().equals("MISSINGNO"))) {
                int xpos = c.getPlayer().getPosition().x;
                int ypos = c.getPlayer().getPosition().y;
                int fh = c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId();
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(ypos);
                npc.setRx0(xpos + 50);
                npc.setRx1(xpos - 50);
                npc.setFh(fh);
                npc.setCustom(true);
                try {
                    PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO spawns ( idd, f, fh, cy, rx0, rx1, type, x, y, mid ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
                    ps.setInt(1, npcId);
                    ps.setInt(2, 0);
                    ps.setInt(3, fh);
                    ps.setInt(4, ypos);
                    ps.setInt(4, ypos);
                    ps.setInt(5, xpos + 50);
                    ps.setInt(6, xpos - 50);
                    ps.setString(7, "n");
                    ps.setInt(8, xpos);
                    ps.setInt(9, ypos);
                    ps.setInt(10, c.getPlayer().getMapId());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    c.getPlayer().dropMessage(6, "保存Npc信息到数据库中出现错误.");
                }
                c.getPlayer().getMap().addMapObject(npc);
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc, true));
            } else {
                c.getPlayer().dropMessage(6, "你应该输入一个正确的 Npc-Id.");
                return 0;
            }
            return 1;
        }
    }

    public static class NPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            if ((npc != null) && (!npc.getName().equals("MISSINGNO"))) {
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(c.getPlayer().getPosition().y);
                npc.setRx0(c.getPlayer().getPosition().x + 50);
                npc.setRx1(c.getPlayer().getPosition().x - 50);
                npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                npc.setCustom(true);
                c.getPlayer().getMap().addMapObject(npc);
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc, true));
            } else {
                c.getPlayer().dropMessage(6, "你应该输入一个正确的 Npc-Id");
                return 0;
            }
            return 1;
        }
    }

    public static class KillAllExp extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = (1.0D / 0.0D);

            if (splitted.length > 1) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "输入的地图不存在.");
                return 0;
            }

            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(new MapleMapObjectType[]{MapleMapObjectType.MONSTER}))) {
                MapleMonster mob = (MapleMonster) monstermo;
                mob.damage(c.getPlayer(), mob.getHp(), false);
            }
            return 1;
        }
    }

    public static class KillAllDrops extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = (1.0D / 0.0D);

            if (splitted.length > 1) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "输入的地图不存在.");
                return 0;
            }

            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(new MapleMapObjectType[]{MapleMapObjectType.MONSTER}))) {
                MapleMonster mob = (MapleMonster) monstermo;
                map.killMonster(mob, c.getPlayer(), true, false, (byte) 1);
            }
            return 1;
        }
    }

    public static class KillMonster extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = (1.0D / 0.0D);

            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(new MapleMapObjectType[]{MapleMapObjectType.MONSTER}))) {
                MapleMonster mob = (MapleMonster) monstermo;
                if (mob.getId() == Integer.parseInt(splitted[1])) {
                    mob.damage(c.getPlayer(), mob.getHp(), false);
                }
            }
            return 1;
        }
    }

    public static class HitMonster extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = (1.0D / 0.0D);
            int damage = Integer.parseInt(splitted[1]);

            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(new MapleMapObjectType[]{MapleMapObjectType.MONSTER}))) {
                MapleMonster mob = (MapleMonster) monstermo;
                if (mob.getId() == Integer.parseInt(splitted[2])) {
                    map.broadcastMessage(MobPacket.damageMonster(mob.getObjectId(), damage));
                    mob.damage(c.getPlayer(), damage, false);
                }
            }
            return 1;
        }
    }

    public static class HitAll extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = (1.0D / 0.0D);
            if (splitted.length > 1) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "输入的地图不存在.");
                return 0;
            }
            int damage = Integer.parseInt(splitted[1]);

            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(new MapleMapObjectType[]{MapleMapObjectType.MONSTER}))) {
                MapleMonster mob = (MapleMonster) monstermo;
                map.broadcastMessage(MobPacket.damageMonster(mob.getObjectId(), damage));
                mob.damage(c.getPlayer(), damage, false);
            }
            return 1;
        }
    }

    public static class HitMonsterByOID extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            int targetId = Integer.parseInt(splitted[1]);
            int damage = Integer.parseInt(splitted[2]);
            MapleMonster monster = map.getMonsterByOid(targetId);
            if (monster != null) {
                map.broadcastMessage(MobPacket.damageMonster(targetId, damage));
                monster.damage(c.getPlayer(), damage, false);
            }
            return 1;
        }
    }

    public static class ClearSquads extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Collection<MapleSquad> squadz = new ArrayList<MapleSquad>(c.getChannelServer().getAllSquads().values());
            for (MapleSquad squads : squadz) {
                squads.clear();
            }
            return 1;
        }
    }

    public static class SReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleReactor reactor = new MapleReactor(MapleReactorFactory.getReactor(Integer.parseInt(splitted[1])), Integer.parseInt(splitted[1]));
            reactor.setDelay(-1);
            c.getPlayer().getMap().spawnReactorOnGroundBelow(reactor, new Point(c.getPlayer().getTruePosition().x, c.getPlayer().getTruePosition().y - 20));
            return 1;
        }
    }

    public static class TMegaphone extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            World.toggleMegaphoneMuteState();
            c.getPlayer().dropMessage(6, new StringBuilder().append("Megaphone state : ").append(c.getChannelServer().getMegaphoneMuteState() ? "Enabled" : "Disabled").toString());
            return 1;
        }
    }

    public static class ToggleOffense extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                CheatingOffense co = CheatingOffense.valueOf(splitted[1]);
                co.setEnabled(!co.isEnabled());
            } catch (IllegalArgumentException iae) {
                c.getPlayer().dropMessage(6, "Offense " + splitted[1] + " not found");
            }
            return 1;
        }
    }

    public static class ShowTrace extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                throw new IllegalArgumentException();
            }
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            Thread t = threads[Integer.parseInt(splitted[1])];
            c.getPlayer().dropMessage(6, t.toString() + ":");
            for (StackTraceElement elem : t.getStackTrace()) {
                c.getPlayer().dropMessage(6, elem.toString());
            }
            return 1;
        }
    }

    public static class Threads extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            String filter = "";
            if (splitted.length > 1) {
                filter = splitted[1];
            }
            for (int i = 0; i < threads.length; i++) {
                String tstring = threads[i].toString();
                if (tstring.toLowerCase().indexOf(filter.toLowerCase()) > -1) {
                    c.getPlayer().dropMessage(6, i + ": " + tstring);
                }
            }
            return 1;
        }
    }

    public static class FCompleteOther extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[2])).forceComplete(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]), Integer.parseInt(splitted[3]));
            return 1;
        }
    }

    public static class FStartOther extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[2])).forceStart(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]), Integer.parseInt(splitted[3]), splitted.length > 4 ? splitted[4] : null);
            return 1;
        }
    }

    public static class ResetOther extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[2])).forfeit(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]));
            return 1;
        }
    }

    public static class 监视玩家 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (target != null) {
                if (target.getClient().isMonitored()) {
                    target.getClient().setMonitored(false);
                    c.getPlayer().dropMessage(5, "停止对 " + target.getName() + " 的监视.");
                } else {
                    target.getClient().setMonitored(true);
                    c.getPlayer().dropMessage(5, "开始监视 " + target.getName() + " 的信息.");
                }
            } else {
                c.getPlayer().dropMessage(5, "当前频道没有找到该玩家.");
                return 0;
            }
            return 1;
        }
    }

    public static class SpeakWorld extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter victim : cserv.getPlayerStorage().getAllCharacters()) {
                    if (victim.getId() != c.getPlayer().getId()) {
                        victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
                    }
                }
            }
            return 1;
        }
    }

    public static class SpeakChn extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter victim : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                if (victim.getId() != c.getPlayer().getId()) {
                    victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
                }
            }
            return 1;
        }
    }

    public static class SpeakMap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter victim : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (victim.getId() != c.getPlayer().getId()) {
                    victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
                }
            }
            return 1;
        }
    }

    public static class GiveVPoint extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "需要输入玩家的名字和数量.");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "错误: 要操作的玩家必须是在同一线或者玩家不存在.");
            } else {
                chrs.setVPoints(chrs.getVPoints() + Integer.parseInt(splitted[2]));
                c.getPlayer().dropMessage(6, splitted[1] + " has " + chrs.getVPoints() + " vpoints, after giving " + splitted[2] + ".");
            }
            return 1;
        }
    }

    public static class GivePoint extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "需要输入玩家的名字和数量.");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "错误: 要操作的玩家必须是在同一线或者玩家不存在.");
            } else {
                chrs.setPoints(chrs.getPoints() + Integer.parseInt(splitted[2]));
                c.getPlayer().dropMessage(6, splitted[1] + " has " + chrs.getPoints() + " points, after giving " + splitted[2] + ".");
            }
            return 1;
        }
    }

    public static class Vac extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (!c.getPlayer().isHidden()) {
                c.getPlayer().dropMessage(6, "You can only vac monsters while in hide.");
                return 0;
            }
            for (MapleMapObject mmo : c.getPlayer().getMap().getAllMonstersThreadsafe()) {
                MapleMonster monster = (MapleMonster) mmo;
                c.getPlayer().getMap().broadcastMessage(MobPacket.moveMonster(false, -1, 0, 0, 0, monster.getObjectId(), monster.getTruePosition(), c.getPlayer().getLastRes()));
                monster.setPosition(c.getPlayer().getPosition());
            }

            return 1;
        }
    }

    public static class Marry extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "用法: !Marry  <name> <itemid>");
                return 0;
            }
            int itemId = Integer.parseInt(splitted[2]);
            if (!GameConstants.isEffectRing(itemId)) {
                c.getPlayer().dropMessage(6, "Invalid itemID.");
            } else {
                MapleCharacter fff = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (fff == null) {
                    c.getPlayer().dropMessage(6, "玩家必须在线.");
                } else {
                    int[] ringID = {MapleInventoryIdentifier.getInstance(), MapleInventoryIdentifier.getInstance()};
                    try {
                        MapleCharacter[] chrz = {fff, c.getPlayer()};
                        for (int i = 0; i < chrz.length; i++) {
                            Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId, ringID[i]);
                            if (eq == null) {
                                c.getPlayer().dropMessage(6, "Invalid itemID.");
                                return 0;
                            }
                            MapleInventoryManipulator.addbyItem(chrz[i].getClient(), eq.copy());
                            chrz[i].dropMessage(6, new StringBuilder().append("结婚成功 ").append(chrz[0].getName()).toString());
                        }
                        MapleRing.addToDB(itemId, c.getPlayer(), fff.getName(), fff.getId(), ringID);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
            return 1;
        }
    }

    public static class Drop extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int itemId = Integer.parseInt(splitted[1]);
            short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (GameConstants.isPet(itemId)) {
                c.getPlayer().dropMessage(5, "宠物道具请通过商城购买.");
            } else if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " 不存在.");
            } else {
                Item toDrop;
                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    toDrop = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                } else {
                    toDrop = new Item(itemId, (short) 0, quantity, (short) 0);
                }
                if (!c.getPlayer().isAdmin()) {
                    toDrop.setGMLog(c.getPlayer().getName() + " 使用 !drop");
                    toDrop.setOwner(c.getPlayer().getName());
                }
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
            }
            return 1;
        }
    }

    public static class UnlockInv extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Map<Item, MapleInventoryType> eqs = new HashMap<Item, MapleInventoryType>();
            boolean add = false;
            if ((splitted.length < 2) || (splitted[1].equals("all"))) {
                for (MapleInventoryType type : MapleInventoryType.values()) {
                    for (Item item : c.getPlayer().getInventory(type)) {
                        if (ItemFlag.LOCK.check(item.getFlag())) {
                            item.setFlag((short) (byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                            add = true;
                        }

                        if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                            item.setFlag((short) (byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                            add = true;
                        }

                        if (add) {
                            eqs.put(item, type);
                        }
                        add = false;
                    }
                }
            } else if (splitted[1].equals("eqp")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).newList()) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((short) (byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                    }

                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((short) (byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                    }

                    if (add) {
                        eqs.put(item, MapleInventoryType.EQUIP);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("eq")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIP)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((short) (byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                    }

                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((short) (byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                    }

                    if (add) {
                        eqs.put(item, MapleInventoryType.EQUIP);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("u")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.USE)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((short) (byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                    }

                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((short) (byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                    }

                    if (add) {
                        eqs.put(item, MapleInventoryType.USE);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("s")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.SETUP)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((short) (byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                    }

                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((short) (byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                    }

                    if (add) {
                        eqs.put(item, MapleInventoryType.SETUP);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("e")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((short) (byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                    }

                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((short) (byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                    }

                    if (add) {
                        eqs.put(item, MapleInventoryType.ETC);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("c")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.CASH)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((short) (byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                    }

                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((short) (byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                    }

                    if (add) {
                        eqs.put(item, MapleInventoryType.CASH);
                    }
                    add = false;
                }
            } else {
                c.getPlayer().dropMessage(6, "[all/eqp/eq/u/s/e/c]");
            }

            for (Entry<Item, MapleInventoryType> eq : eqs.entrySet()) {
                c.getPlayer().forceReAddItem_NoUpdate(eq.getKey().copy(), eq.getValue());
            }
            return 1;
        }
    }

    public static class GiveSkill extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            Skill skill = SkillFactory.getSkill(Integer.parseInt(splitted[2]));
            byte level = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1);
            byte masterlevel = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 4, 1);
            if (level > skill.getMaxLevel()) {
                level = (byte) skill.getMaxLevel();
            }
            if (masterlevel > skill.getMaxLevel()) {
                masterlevel = (byte) skill.getMaxLevel();
            }
            victim.changeSkillLevel(skill, level, masterlevel);
            return 1;
        }
    }
}