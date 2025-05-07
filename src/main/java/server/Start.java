package server;

import client.MapleCharacter;
import client.SkillFactory;
import client.inventory.MapleInventoryIdentifier;
//import commons.services.LoggingService;
//import commons.utils.MapleInfos;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.MapleServerHandler;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.MapleGuildRanking;
import handling.channel.PlayerStorage;
import handling.login.LoginInformationProvider;
import handling.login.LoginServer;
import handling.world.World;
import handling.world.family.MapleFamily;
import handling.world.guild.MapleGuild;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import server.Timer.BuffTimer;
import server.Timer.CloneTimer;
import server.Timer.EtcTimer;
import server.Timer.EventTimer;
import server.Timer.MapTimer;
import server.Timer.PingTimer;
import server.Timer.WorldTimer;
import server.events.MapleOxQuizFactory;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.life.MobSkillFactory;
import server.life.PlayerNPC;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.quest.MapleQuest;
import tools.GetMACAddress;
import tools.MaplePacketCreator;

public class Start {

    public static long startTime = System.currentTimeMillis();
    public static final Start instance = new Start();
    public static AtomicInteger CompletedLoadingThreads = new AtomicInteger(0);
    private int rankTime;
    private boolean ivCheck;

    public Start() {
        this.ivCheck = false;
    }

    public void run() throws InterruptedException {
        System.out.println("ChenHUI104.1");
        this.rankTime = Integer.parseInt(ServerProperties.getProperty("world.rankTime", "30"));
        this.ivCheck = Boolean.parseBoolean(ServerProperties.getProperty("world.ivCheck", "false"));
        if ((Boolean.parseBoolean(ServerProperties.getProperty("world.admin"))) || (ServerConstants.Use_Localhost)) {
            ServerConstants.Use_Fixed_IV = false;
            System.out.println("[!!! 已开启只能管理员登录模式 !!!]");
        }
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET loggedin = 0");
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            throw new RuntimeException("[EXCEPTION] Please check if the SQL server is active.");
        }

        printSection("世界服务器");
        World.init();
        System.out.println("世界服务器加载完成...");

        printSection("时钟线程");
        WorldTimer.getInstance().start();
        EtcTimer.getInstance().start();
        MapTimer.getInstance().start();
        CloneTimer.getInstance().start();
        EventTimer.getInstance().start();
        BuffTimer.getInstance().start();
        PingTimer.getInstance().start();
        System.out.println("时钟线程加载完成...");


        printSection("加载家族");
        MapleGuildRanking.getInstance().load();
        MapleGuild.loadAll();
        System.out.println("家族信息加载完成...");

        printSection("加载学院");
        MapleFamily.loadAll();
        System.out.println("学院信息加载完成...");

        printSection("加载任务");
        long startQuestTime = System.currentTimeMillis();
        MapleLifeFactory.loadQuestCounts();
        MapleQuest.initQuests();

        System.out.println("任务信息加载完成 耗时: " + (System.currentTimeMillis() - startQuestTime) / 1000 + " 秒..");

        printSection("加载道具");
        long startItemsTime = System.currentTimeMillis();
        MapleItemInformationProvider.getInstance().runEtc();
        MapleItemInformationProvider.getInstance().runItems();

        System.out.println("道具信息加载完成 耗时: " + (System.currentTimeMillis() - startItemsTime) / 1000 + " 秒..");

        printSection("加载爆率");
        long startDropTime = System.currentTimeMillis();
        MapleMonsterInformationProvider.getInstance().load();
        MapleMonsterInformationProvider.getInstance().addExtra();

        System.out.println("爆率信息加载完成 耗时: " + (System.currentTimeMillis() - startDropTime) / 1000 + " 秒..");

        printSection("加载技能");
        long startSkillsTime = System.currentTimeMillis();
        SkillFactory.loadAllSkills();

        System.out.println("技能数据信息加载完成 耗时: " + (System.currentTimeMillis() - startSkillsTime) / 1000 + " 秒..");

        printSection("BasicLoader");
        long startBasicTime = System.currentTimeMillis();
        LoginInformationProvider.getInstance();
        RandomRewards.load();
        MapleOxQuizFactory.getInstance();
        MapleCarnivalFactory.getInstance();
        MobSkillFactory.getInstance();
        SpeedRunner.loadSpeedRuns();
        MTSStorage.load();
        System.out.println("BasicLoader加载完成 耗时: " + (System.currentTimeMillis() - startBasicTime) / 1000 + " 秒..");

        printSection("MIILoader");
        long startMIITime = System.currentTimeMillis();
        MapleInventoryIdentifier.getInstance();
        System.out.println("MIILoader加载完成 耗时: " + (System.currentTimeMillis() - startMIITime) / 1000 + " 秒..");

        printSection("加载商城道具");
        long startCashItemTime = System.currentTimeMillis();
        CashItemFactory.getInstance().initialize();
        System.out.println("商城道具加载完成 耗时: " + (System.currentTimeMillis() - startCashItemTime) / 1000 + " 秒..");

        MapleServerHandler.initiate();
        printSection("登录服务器");
        LoginServer.run_startup_configurations();
        printSection("频道服务器");
        ChannelServer.startChannel_Main();
        printSection("商城服务器");
        CashShopServer.run_startup_configurations();
        Timer.CheatTimer.getInstance().register(AutobanManager.getInstance(), 60000L);
        Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown()));
        printSection("刷怪线程");
        World.registerRespawn();
        if (Boolean.parseBoolean(ServerProperties.getProperty("world.RandDrop"))) {
            System.out.println("[市场掉落物品开启]");
            ChannelServer.getInstance(1).getMapFactory().getMap(910000000).spawnRandDrop();
        }
        ShutdownServer.registerMBean();
        ServerConstants.registerMBean();
        PlayerNPC.loadAll();
        System.out.println("服务端启动完毕.请打开客户端进行游戏");
        LoginServer.setOn();
        System.out.println("[服务端已启动完毕，耗时 " + (System.currentTimeMillis() - startQuestTime) / 1000L + " 秒]");
        printSection("刷新排名");
        RankingWorker.start();
        if (Boolean.parseBoolean(ServerProperties.getProperty("world.贡献排名"))) {
        printSection("更新玩家捐献排名");
        MapleDonation.start();
    }

        System.out.println(new StringBuilder().append("\r\n欢迎您使用  ").append("ChenHUI服务端").append("  ！当前服务端版本为:  Ver. ").append(ServerConstants.MAPLE_VERSION).append(" . ").append(ServerConstants.MAPLE_PATCH).append(" \r\n").toString());

        if (Boolean.parseBoolean(ServerProperties.getProperty("world.AccCheck", "false"))) {
            printSection("启动检测");
            startCheck();
        }
        
    }
    public static void startCheck() {
        Timer.WorldTimer.getInstance().register(new Runnable() {
            public void run() {
                for (ChannelServer cserv_ : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : cserv_.getPlayerStorage().getAllCharacters()) {
                        if (chr != null) {
                            chr.startCheck();
                        }
                    }
                }
            }
        }, 30000);
    }

    public static void printSection(String s) {
        s = "-[ " + s + " ]";
        while (s.getBytes().length < 79) {
            s = "=" + s;
        }
        System.out.println(s);
    }

   /* public static void main(String[] args) throws InterruptedException {
        String[] macs = {"90-2B-34-66-31-6E"};

        String localMac = GetMACAddress.getMACAddress();

        if (localMac != null) {
            for (int i = 0; i < macs.length; i++) {
                if (macs[i].equals(localMac)) {
                    instance.run();
                    break;
                }
            }
        } else {
            System.exit(0);
        }
    }*/
    public static void main(final String args[]) throws InterruptedException {
     instance.run();
     }

    public int getRankTime() {
        return this.rankTime;
    }

    public void setRankTime(int rankTime) {
        this.rankTime = rankTime;
    }

    public boolean isIvCheck() {
        return this.ivCheck;
    }

    private static class NotifyingRunnable
            implements Runnable {

        private String LoadingThreadName;
        private long StartTime;
        private Runnable WrappedRunnable;
        private final Object ToNotify;

        private NotifyingRunnable(Runnable r, Object o, String name) {
            this.WrappedRunnable = r;
            this.ToNotify = o;
            this.LoadingThreadName = name;
        }

        public void run() {
            this.StartTime = System.currentTimeMillis();
            this.WrappedRunnable.run();
            System.out.println("[加载完成] " + this.LoadingThreadName + " | 加载耗时 " + (System.currentTimeMillis() - this.StartTime) / 1000 + " 秒. (" + Start.CompletedLoadingThreads.get() + "/10)");
            synchronized (this.ToNotify) {
                this.ToNotify.notify();
            }
        }
    }

    private static class LoadingThread extends Thread {

        protected String LoadingThreadName;

        private LoadingThread(Runnable r, String t, Object o) {
            super();
            this.LoadingThreadName = t;
        }

        public synchronized void start() {
            System.out.println("[加载...] 启动 " + this.LoadingThreadName + " 线程.");
            super.start();
        }
    }

    public static class Shutdown
            implements Runnable {

        public void run() {
            ShutdownServer.getInstance().run();
            ShutdownServer.getInstance().run();
        }
    }
}