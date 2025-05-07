package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.SkillFactory;
import constants.GameConstants;
import constants.ServerConstants;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.*;
import handling.world.exped.MapleExpedition;
import handling.world.guild.MapleGuild;
import java.util.List;
import org.apache.log4j.Logger;
import scripting.NPCScriptManager;
import server.maps.FieldLimitType;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.*;

/**
 * 玩家进入游戏
 */
public class InterServerHandler {

    private static final Logger log = Logger.getLogger(InterServerHandler.class);

    public static void EnterMTS(MapleClient c, MapleCharacter chr) {
        if ((chr.hasBlockedInventory()) || (chr.getMap() == null) || (chr.getEventInstance() != null) || (c.getChannelServer() == null)) {
            c.getSession().write(MaplePacketCreator.serverBlocked(4));
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.getMapId() == 180000001) {
            chr.dropMessage(1, "在这个地方无法使用此功能.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.isBanned()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.getAntiMacro().inProgress()) {
            chr.dropMessage(5, "被使用测谎仪时无法操作。");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        NPCScriptManager.getInstance().dispose(c);
        NPCScriptManager.getInstance().start(c, 9900004);
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static void EnterCS(MapleClient c, MapleCharacter chr) {
        if (!chr.isAlive()) {
            c.getPlayer().dropMessage(1, "现在不能进入商城.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.isInJailMap()) {
            chr.dropMessage(1, "在这个地方无法使用次功能.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.isBanned()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.getAntiMacro().inProgress()) {
            chr.dropMessage(5, "被使用测谎仪时无法操作。");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if ((chr.hasBlockedInventory()) || (chr.getMap() == null) || (chr.getEventInstance() != null) || (c.getChannelServer() == null)) {
            c.getSession().write(MaplePacketCreator.serverBlocked(2));
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (World.getPendingCharacterSize() >= 10) {
            chr.dropMessage(1, "服务器忙，请稍后在试。");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        ChannelServer ch = ChannelServer.getInstance(c.getChannel());
        chr.changeRemoval();
        if (chr.getMessenger() != null) {
            MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(chr);
            World.Messenger.leaveMessenger(chr.getMessenger().getId(), messengerplayer);
        }
        PlayerBuffStorage.addBuffsToStorage(chr.getId(), chr.getAllBuffs());
        PlayerBuffStorage.addCooldownsToStorage(chr.getId(), chr.getCooldowns());
        PlayerBuffStorage.addDiseaseToStorage(chr.getId(), chr.getAllDiseases());
        World.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), -10);
        ch.removePlayer(chr);
        c.updateLoginState(3, c.getSessionIPAddress());
        chr.saveToDB(false, false);
        chr.getMap().removePlayer(chr);
        c.getSession().write(MaplePacketCreator.getChannelChange(c, Integer.parseInt(handling.cashshop.CashShopServer.getIP().split(":")[1])));
        c.setPlayer(null);
        c.setReceiving(false);
    }

    public static void Loggedin(int playerid, MapleClient c) {
        ChannelServer channelServer = c.getChannelServer();

        CharacterTransfer transfer = channelServer.getPlayerStorage().getPendingCharacter(playerid);
        boolean loggedin = true;
        MapleCharacter player;
        if (transfer == null) {
            player = MapleCharacter.loadCharFromDB(playerid, c, true);
            Pair<String, String> ip = LoginServer.getLoginAuth(playerid);
            String s = c.getSessionIPAddress();
            if (ip == null || !s.substring(s.indexOf('/') + 1, s.length()).equals(ip.left)) {
                //log.info("ip = " + ip);
                if (ip != null) {
                    LoginServer.putLoginAuth(playerid, ip.left, ip.right);
                }
                c.getSession().close();
                log.info(new StringBuilder().append("检测连接地址 - 1 ").append(s).toString());
                return;
            }
            c.setTempIP(ip.right);
        } else {
            player = MapleCharacter.ReconstructChr(transfer, c, true);
            loggedin = false;
        }
        c.setPlayer(player);
        c.setAccID(player.getAccountID());
        if (!c.CheckIPAddress()) {
            c.getSession().close();
            log.info(new StringBuilder().append("检测连接地址 - 2 ").append(!c.CheckIPAddress()).toString());
            return;
        }
        int state = c.getLoginState();
        boolean allowLogin = false;
        if ((state == 1) || (state == 3) || (state == 0)) {
            allowLogin = !World.isCharacterListConnected(c.loadCharacterNames(c.getWorld()));
        }
        if (!allowLogin) {
            c.setPlayer(null);
            c.getSession().close();
            log.info(new StringBuilder().append("检测连接地址 - 3 ").append(!allowLogin).toString());
            return;
        }
        c.updateLoginState(2, c.getSessionIPAddress());
        channelServer.addPlayer(player);
        player.giveCoolDowns(PlayerBuffStorage.getCooldownsFromStorage(player.getId()));
        player.silentGiveBuffs(PlayerBuffStorage.getBuffsFromStorage(player.getId()));
        player.giveSilentDebuff(PlayerBuffStorage.getDiseaseFromStorage(player.getId()));
        c.getSession().write(MaplePacketCreator.getCharInfo(player));
        c.getSession().write(MTSCSPacket.enableCSUse());
        c.getSession().write(MaplePacketCreator.sendloginSuccess());
        c.getSession().write(MaplePacketCreator.updateMount(player, false));
        if (player.isIntern()) {
            SkillFactory.getSkill(9001004).getEffect(1).applyTo(player);
        }
        c.getSession().write(MaplePacketCreator.temporaryStats_Reset());
        player.getMap().addPlayer(player);
        try {
            int[] buddyIds = player.getBuddylist().getBuddyIds();
            World.Buddy.loggedOn(player.getName(), player.getId(), c.getChannel(), buddyIds);
            if (player.getParty() != null) {
                MapleParty party = player.getParty();
                World.Party.updateParty(party.getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(player));
                if ((party != null) && (party.getExpeditionId() > 0)) {
                    MapleExpedition me = World.Party.getExped(party.getExpeditionId());
                    if (me != null) {
                        c.getSession().write(PartyPacket.expeditionStatus(me, false));
                    }
                }
            }
            if (player.getSidekick() == null) {
                player.setSidekick(World.Sidekick.getSidekickByChr(player.getId()));
            }
            if (player.getSidekick() != null) {
                c.getSession().write(PartyPacket.updateSidekick(player, player.getSidekick(), false));
            }
            CharacterIdChannelPair[] onlineBuddies = World.Find.multiBuddyFind(player.getId(), buddyIds);
            for (CharacterIdChannelPair onlineBuddy : onlineBuddies) {
                player.getBuddylist().get(onlineBuddy.getCharacterId()).setChannel(onlineBuddy.getChannel());
            }
            c.getSession().write(BuddyListPacket.updateBuddylist(player.getBuddylist().getBuddies()));

            MapleMessenger messenger = player.getMessenger();
            if (messenger != null) {
                World.Messenger.silentJoinMessenger(messenger.getId(), new MapleMessengerCharacter(player));
                World.Messenger.updateMessenger(messenger.getId(), player.getName(), c.getChannel());
            }

            if (player.getGuildId() > 0) {
                World.Guild.setGuildMemberOnline(player.getMGC(), true, c.getChannel());
                c.getSession().write(GuildPacket.showGuildInfo(player));
                MapleGuild gs = World.Guild.getGuild(player.getGuildId());
                if (gs != null) {
                    List<byte[]> packetList = World.Alliance.getAllianceInfo(gs.getAllianceId(), true);
                    if (packetList != null) {
                        for (byte[] pack : packetList) {
                            if (pack != null) {
                                c.getSession().write(pack);
                            }
                        }
                    }
                } else {
                    player.setGuildId(0);
                    player.setGuildRank((byte) 5);
                    player.setAllianceRank((byte) 5);
                    player.saveGuildStatus();
                }
            }

            if (player.getFamilyId() > 0) {
                World.Family.setFamilyMemberOnline(player.getMFC(), true, c.getChannel());
            }
            c.getSession().write(FamilyPacket.getFamilyData());
            c.getSession().write(FamilyPacket.getFamilyInfo(player));
        } catch (Exception e) {
            FileoutputUtil.outputFileError("log\\Login_Error.log", e);
        }

        player.getClient().getSession().write(MaplePacketCreator.serverMessage(channelServer.getServerMessage()));

        player.sendMacros();

        player.showNote();

        player.sendImp();

        player.updatePartyMemberHP();

        player.startFairySchedule(false);

        player.baseSkills();

        c.getSession().write(MaplePacketCreator.getKeymap(player.getKeyLayout()));

        c.getSession().write(MaplePacketCreator.showCharCash(player));

        player.updatePetAuto();
        c.getSession().write(MaplePacketCreator.reportResponse((byte) 0, 0));
        c.getSession().write(MaplePacketCreator.enableReport());

        player.expirationTask(true, transfer == null);

        if (player.getJob() == 132) {
            player.checkBerserk();
        }
        player.spawnClones();

        player.spawnSavedPets();
        if (player.getStat().equippedSummon > 0) {
            SkillFactory.getSkill(player.getStat().equippedSummon).getEffect(1).applyTo(player);
        }

        MapleQuestStatus stat = player.getQuestNoAdd(MapleQuest.getInstance(122700));
        c.getSession().write(MaplePacketCreator.pendantSlot((stat != null) && (stat.getCustomData() != null) && (Long.parseLong(stat.getCustomData()) > System.currentTimeMillis())));

        stat = player.getQuestNoAdd(MapleQuest.getInstance(123000));
        c.getSession().write(MaplePacketCreator.quickSlot((stat != null) && (stat.getCustomData() != null) && (stat.getCustomData().length() == 8) ? stat.getCustomData() : null));
      
        int vip = player.getVip();
          c.getPlayer().dropMessage(-1, "欢迎来到一起冒险岛");
//        if (vip >= 1 && !player.isGM()) {
//            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(18, c.getChannel(), "[Vip频道]" + " : " + String.format(GameConstants.vipLogin[(vip - 1)], new Object[]{c.getPlayer().getName()})));
//        }
        
        if (loggedin) {
          String msg = new StringBuilder().append("当前游戏为: [Ver.").append(ServerConstants.MAPLE_VERSION).append(".").append(ServerConstants.MAPLE_PATCH).append("] 欢迎来到一起冒险岛.您已经安全登陆，使用 @help 可以查看您当前能使用的命令 祝您玩的愉快！.").toString();
       // String msg = new StringBuilder().append(" #r").append(player.getName()).append(" #k").append("  \r\n欢迎来到 ").append(c.getChannelServer().getServerName()).append(" ！\r\n当前服务端版本为: [Ver.#b").append(ServerConstants.MAPLE_VERSION).append(".").append(ServerConstants.MAPLE_PATCH).append("#k] 您已经安全登陆！\r\n在对话框输入 #r@help #k可以查看您当前能使用的命令!\r\n#b祝您游戏愉快！.").toString();
            player.getHyPay(1);
            if (player.getLevel() == 1) {
                player.dropMessage(1, new StringBuilder().append("欢迎来到 ").append(c.getChannelServer().getServerName()).append(", ").append(player.getName()).append(" ！\r\n使用 @help 可以查看您当前能使用的命令\r\n祝您玩的愉快！").toString());
                player.dropMessage(5, "使用 @help 可以查看您当前能使用的命令 祝您玩的愉快！");
                player.gainExp(500, true, false, true);
            } else {
                c.getSession().write(MaplePacketCreator.sendHint(msg, 200, 5));
            } 

            if (c.getChannelServer().getDoubleExp() == 2) {
                player.dropMessage(5, "当前服务器处于双倍经验活动中，祝您玩的愉快！");
            }

//            if (((player.getItemQuantity(4000463) >= 800) || (player.getCSPoints(-1) >= 900000)) && (player.getHyPay(3) < 200) && (!player.isIntern())) {
//                String msgtext = new StringBuilder().append("玩家 ").append(player.getName()).append(" 数据异常，服务器自动断开他的连接。").append(" 国庆币数量: ").append(player.getItemQuantity(4000463)).append(" 点卷总额: ").append(player.getCSPoints(-1)).toString();
//                World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, new StringBuilder().append("[GM Message] ").append(msgtext).toString()));
//                FileoutputUtil.log("log\\数据异常.log", msgtext);
//                c.getSession().close(true);
//            }
        }

        c.getSession().write(MaplePacketCreator.getInventoryStatus());
    }

    public static void ChangeChannel(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (chr.hasBlockedInventory()) || (chr.getEventInstance() != null) || (chr.getMap() == null) || (chr.isInBlockedMap()) || (FieldLimitType.ChannelSwitch.check(chr.getMap().getFieldLimit()))) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.isBanned()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.getAntiMacro().inProgress()) {
            chr.dropMessage(5, "被使用测谎仪时无法操作。");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (World.getPendingCharacterSize() >= 10) {
            chr.dropMessage(1, "服务器忙，请稍后在试。");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        int chc = slea.readByte() + 1;
        if (!World.isChannelAvailable(chc)) {
            chr.dropMessage(1, "该频道玩家已满，请切换到其它频道进行游戏。");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        chr.changeChannel(chc);
    }
}
