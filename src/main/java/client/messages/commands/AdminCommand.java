package client.messages.commands;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.ServerConstants.PlayerGMRank;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import scripting.NPCScriptManager;
import scripting.PortalScriptManager;
import scripting.ReactorScriptManager;
import server.MapleInventoryManipulator;
import server.MapleShopFactory;
import server.ShutdownServer;
import server.Timer;
import server.life.MapleMonsterInformationProvider;
import tools.MaplePacketCreator;
import tools.packet.UIPacket;
import tools.performance.CPUSampler;

public class AdminCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.ADMIN;
    }

    public static class 启用幻影 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "请输入数字，数字大于0开启幻影，等于0关闭幻影。");
                return 0;
            }
            LoginServer.启用幻影(Integer.parseInt(splitted[1]));
            c.getPlayer().dropMessage(5, "当前开启状态: " + LoginServer.is开启幻影());
            return 1;
        }
    }

    public static class 查看股价 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "当前的股价为: " + ChannelServer.getInstance(1).getSharePrice());
            return 1;
        }
    }

    public static class 降低股价 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "请输入数量.");
                return 0;
            }
            int share = Integer.parseInt(splitted[1]);
            ChannelServer.getInstance(1).decreaseShare(share);
            c.getPlayer().dropMessage(5, "股价降低: " + share + " 当前的股价为: " + ChannelServer.getInstance(1).getSharePrice());
            return 1;
        }
    }

    public static class 增加股价 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "请输入数量.");
                return 0;
            }
            int share = Integer.parseInt(splitted[1]);
            ChannelServer.getInstance(1).increaseShare(share);
            c.getPlayer().dropMessage(5, "股价提高: " + share + " 当前的股价为: " + ChannelServer.getInstance(1).getSharePrice());
            return 1;
        }
    }

    public static class 封包调试 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.StartWindow();
            return 1;
        }
    }

    public static class 重载活动 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer instance : ChannelServer.getAllInstances()) {
                instance.reloadEvents();
            }
            c.getPlayer().dropMessage(5, "重新加载活动脚本完成.");
            return 1;
        }
    }

    public static class 重载商店 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleShopFactory.getInstance().clear();
            c.getPlayer().dropMessage(5, "重新加载商店贩卖道具完成.");
            return 1;
        }
    }

    public static class 重载传送 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            PortalScriptManager.getInstance().clearScripts();
            c.getPlayer().dropMessage(5, "重新加载传送点脚本完成.");
            return 1;
        }
    }

    public static class 重载爆率 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMonsterInformationProvider.getInstance().clearDrops();
            ReactorScriptManager.getInstance().clearDrops();
            c.getPlayer().dropMessage(5, "重新加载爆率完成.");
            return 1;
        }
    }

    public static class 重载包头 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            SendPacketOpcode.reloadValues();
            RecvPacketOpcode.reloadValues();
            c.getPlayer().dropMessage(5, "重新获取包头完成.");
            return 1;
        }
    }

    public static class GainVP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "请输入数量.");
                return 0;
            }
            c.getPlayer().setVPoints(c.getPlayer().getVPoints() + Integer.parseInt(splitted[1]));
            return 1;
        }
    }

    public static class GainP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "请输入数量.");
                return 0;
            }
            c.getPlayer().setPoints(c.getPlayer().getPoints() + Integer.parseInt(splitted[1]));
            return 1;
        }
    }

    public static class 刷抵用卷 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "请输入数量.");
                return 0;
            }
            c.getPlayer().modifyCSPoints(2, Integer.parseInt(splitted[1]), true);
            return 1;
        }
    }

    public static class 刷点卷 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "请输入数量.");
                return 0;
            }
            c.getPlayer().modifyCSPoints(1, Integer.parseInt(splitted[1]), true);
            return 1;
        }
    }

    public static class 刷钱 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().gainMeso(2147483647 - c.getPlayer().getMeso(), true);
            return 1;
        }
    }

    public static class Subcategory extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setSubcategory(Byte.parseByte(splitted[1]));
            return 1;
        }
    }

    public static class StopProfiling extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            CPUSampler sampler = CPUSampler.getInstance();
            try {
                String filename = "odinprofile.txt";
                if (splitted.length > 1) {
                    filename = splitted[1];
                }
                File file = new File(filename);
                if (file.exists()) {
                    c.getPlayer().dropMessage(6, "输入的文件名字已经存在，请重新输入1个新的文件名。");
                    return 0;
                }
                sampler.stop();
                FileWriter fw = new FileWriter(file);
                sampler.save(fw, 1, 10);
                fw.close();
            } catch (IOException e) {
                System.err.println("保存文件出错." + e);
            }
            sampler.reset();
            c.getPlayer().dropMessage(6, "已经停止服务端性能监测.");
            return 1;
        }
    }

    public static class StartProfiling extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            CPUSampler sampler = CPUSampler.getInstance();
            sampler.addIncluded("client");
            sampler.addIncluded("constants");
            sampler.addIncluded("database");
            sampler.addIncluded("handling");
            sampler.addIncluded("provider");
            sampler.addIncluded("scripting");
            sampler.addIncluded("server");
            sampler.addIncluded("tools");
            sampler.start();
            c.getPlayer().dropMessage(6, "已经开启服务端性能监测.");
            return 1;
        }
    }

    public static class ShutdownTime extends AdminCommand.Shutdown {

        private static ScheduledFuture<?> ts = null;
        private int minutesLeft = 0;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            this.minutesLeft = Integer.parseInt(splitted[1]);
            c.getPlayer().dropMessage(6, "游戏将在 " + this.minutesLeft + " 分钟之后关闭...");
            if ((ts == null) && ((t == null) || (!t.isAlive()))) {
                t = new Thread(ShutdownServer.getInstance());
                ts = Timer.EventTimer.getInstance().register(new Runnable() {

                    @Override
                    public void run() {
                        if (AdminCommand.ShutdownTime.this.minutesLeft == 0) {
                            ShutdownServer.getInstance().shutdown();
                            AdminCommand.Shutdown.t.start();
                            AdminCommand.ShutdownTime.ts.cancel(false);
                            return;
                        }
                        World.Broadcast.broadcastMessage(UIPacket.clearMidMsg());
                        World.Broadcast.broadcastMessage(UIPacket.getMidMsg("游戏将于 " + AdminCommand.ShutdownTime.this.minutesLeft + " 分钟之后关闭维护.请玩家安全下线.", true, 0));
                        World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(0, " 游戏将于 " + AdminCommand.ShutdownTime.this.minutesLeft + " 分钟之后关闭维护.请玩家安全下线."));
                        //AdminCommand.ShutdownTime.access$010(AdminCommand.ShutdownTime.this);
                    }
                }, 60000);
            } else {
                c.getPlayer().dropMessage(6, "已经使用过一次这个命令，暂时无法使用.");
            }
            return 1;
        }
    }

    public static class Shutdown extends CommandExecute {

        protected static Thread t = null;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "游戏即将关闭...");
            if ((t == null) || (!t.isAlive())) {
                t = new Thread(ShutdownServer.getInstance());
                ShutdownServer.getInstance().shutdown();
                t.start();
            } else {
                c.getPlayer().dropMessage(6, "已经使用过一次这个命令，暂时无法使用.");
            }
            return 1;
        }
    }

    public static class 查看爆率 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9010000, 1);
            return 1;
        }
    }

    public static class DCAll extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int range = -1;
            if (splitted[1].equals("m")) {
                range = 0;
            } else if (splitted[1].equals("c")) {
                range = 1;
            } else if (splitted[1].equals("w")) {
                range = 2;
            }
            if (range == -1) {
                range = 1;
            }
            if (range == 0) {
                c.getPlayer().getMap().disconnectAll();
                c.getPlayer().dropMessage(5, "已成功断开当前地图所有玩家的连接.");
            } else if (range == 1) {
                c.getChannelServer().getPlayerStorage().disconnectAll(true);
                c.getPlayer().dropMessage(5, "已成功断开当前频道所有玩家的连接.");
            } else if (range == 2) {
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    cserv.getPlayerStorage().disconnectAll(true);
                }
                c.getPlayer().dropMessage(5, "已成功断开当前游戏所有玩家的连接.");
            }
            return 1;
        }
    }

    public static class 经验信息 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "当前游戏设置信息:");
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                StringBuilder rateStr = new StringBuilder("频道 ");
                rateStr.append(cserv.getChannel());
                rateStr.append(" 经验: ");
                rateStr.append(cserv.getExpRate());
                rateStr.append(" 金币: ");
                rateStr.append(cserv.getMesoRate());
                rateStr.append(" 爆率: ");
                rateStr.append(cserv.getDropRate());
                rateStr.append(" 活动: ");
                rateStr.append(cserv.getDoubleExp());
                c.getPlayer().dropMessage(5, rateStr.toString());
            }
            return 1;
        }
    }

    public static class 双倍经验 extends CommandExecute {

        private int change = 0;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            this.change = Integer.parseInt(splitted[1]);
            if ((this.change == 1) || (this.change == 2)) {
                c.getPlayer().dropMessage(5, "以前 - 经验: " + c.getChannelServer().getExpRate() + " 金币: " + c.getChannelServer().getMesoRate() + " 爆率: " + c.getChannelServer().getDropRate());
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    cserv.setDoubleExp(this.change);
                }
                c.getPlayer().dropMessage(5, "现在 - 经验: " + c.getChannelServer().getExpRate() + " 金币: " + c.getChannelServer().getMesoRate() + " 爆率: " + c.getChannelServer().getDropRate());
                c.getSession().write(MaplePacketCreator.startMapEffect("双倍经验和双倍爆率活动开始.", 5120000, true));
                return 1;
            }
            c.getPlayer().dropMessage(5, "输入的数字无效，1为关闭活动经验，2为开启活动经验。当前输入为: " + this.change);
            return 0;
        }
    }

    public static class MesoRate extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                int rate = Integer.parseInt(splitted[1]);
                if ((splitted.length > 2) && (splitted[2].equalsIgnoreCase("all"))) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setMesoRate(rate);
                    }
                } else {
                    c.getChannelServer().setMesoRate(rate);
                }
                c.getPlayer().dropMessage(6, "金币爆率已经修改为: " + rate + "倍.");
            } else {
                c.getPlayer().dropMessage(6, "用法: !mesorate <number> [all]");
            }
            return 1;
        }
    }

    public static class ExpRate extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                int rate = Integer.parseInt(splitted[1]);
                if ((splitted.length > 2) && (splitted[2].equalsIgnoreCase("all"))) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setExpRate(rate);
                    }
                } else {
                    c.getChannelServer().setExpRate(rate);
                }
                c.getPlayer().dropMessage(6, "经验倍率已经修改为: " + rate + "倍.");
            } else {
                c.getPlayer().dropMessage(6, "用法: !exprate <number> [all]");
            }
            return 1;
        }
    }

    public static class 给所有人点卷 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "用法: !给所有人点卷 [点卷类型1-2] [点卷数量]");
                return 0;
            }
            int type = Integer.parseInt(splitted[1]);
            int quantity = Integer.parseInt(splitted[2]);
            if ((type <= 0) || (type > 2)) {
                type = 2;
            }
            if (quantity > 9000) {
                quantity = 9000;
            }
            int ret = 0;
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                    mch.modifyCSPoints(type, quantity, false);
                    mch.dropMessage(-11, new StringBuilder().append("[系统提示] 恭喜您获得管理员赠送给您的").append(type == 1 ? "点券 " : " 抵用券 ").append(quantity).append(" 点.").toString());
                    ret++;
                }
            }
            c.getPlayer().dropMessage(6, new StringBuilder().append("命令使用成功，当前共有: ").append(ret).append(" 个玩家获得: ").append(quantity).append(" 点的").append(type == 1 ? "点券 " : " 抵用券 ").append(" 总计: ").append(ret * quantity).toString());
            return 1;
        }
    }

    public static class 给所有人冒险币 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                    mch.gainMeso(Integer.parseInt(splitted[1]), true);
                }
            }
            return 1;
        }
    }

    public static class StripEveryone extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleInventory equip;
            ChannelServer cs = c.getChannelServer();
            for (MapleCharacter mchr : cs.getPlayerStorage().getAllCharacters()) {
                if (mchr.isGM()) {
                    continue;
                }
                MapleInventory equipped = mchr.getInventory(MapleInventoryType.EQUIPPED);
                equip = mchr.getInventory(MapleInventoryType.EQUIP);
                List<Short> ids = new ArrayList();
                for (Item item : equipped.newList()) {
                    ids.add(Short.valueOf(item.getPosition()));
                }
                for (short id : ids) {
                    MapleInventoryManipulator.unequip(mchr.getClient(), id, equip.getNextFreeSlot());
                }
            }

            return 1;
        }
    }
}