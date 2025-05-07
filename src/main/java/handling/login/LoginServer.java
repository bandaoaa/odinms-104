package handling.login;

import constants.GameConstants;
import handling.MapleServerHandler;
import handling.netty.ServerConnection;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import server.ServerProperties;
import tools.Pair;

public class LoginServer {

    private static short port;
    private static final short DEFAULT_PORT = 8484;
    private static ServerConnection acceptor;
    private static Map<Integer, Integer> load = new HashMap();
    private static String serverName;
    private static String eventMessage;
    private static byte flag;
    private static int maxCharacters;
    private static int userLimit;
    private static int usersOn = 0;
    private static boolean finishedShutdown = true;
    private static boolean adminOnly = false;
    private static boolean autoReg = false;
    private static boolean useSha1Hash = false;
    private static boolean phantom = false;
    private static HashMap<Integer, Pair<String, String>> loginAuth = new HashMap<Integer, Pair<String, String>>();
    private static HashSet<String> loginIPAuth = new HashSet();

    public static void putLoginAuth(int chrid, String ip, String tempIP) {
        loginAuth.put(chrid, new Pair<String, String>(ip, tempIP));
        loginIPAuth.add(ip);
    }

    public static Pair<String, String> getLoginAuth(int chrid) {
        return loginAuth.remove(chrid);
    }

    public static boolean containsIPAuth(String ip) {
        return loginIPAuth.contains(ip);
    }

    public static void removeIPAuth(String ip) {
        loginIPAuth.remove(ip);
    }

    public static void addIPAuth(String ip) {
        loginIPAuth.add(ip);
    }

    public static void addChannel(int channel) {
        load.put(Integer.valueOf(channel), Integer.valueOf(0));
    }

    public static void removeChannel(int channel) {
        load.remove(Integer.valueOf(channel));
    }

    public static void run_startup_configurations() {
        userLimit = Integer.parseInt(ServerProperties.getProperty("login.userlimit"));
        serverName = ServerProperties.getProperty("login.serverName");
        eventMessage = ServerProperties.getProperty("login.eventMessage");
        flag = Byte.parseByte(ServerProperties.getProperty("login.flag"));
        adminOnly = Boolean.parseBoolean(ServerProperties.getProperty("world.admin", "false"));
        maxCharacters = Integer.parseInt(ServerProperties.getProperty("login.maxCharacters"));
        autoReg = Boolean.parseBoolean(ServerProperties.getProperty("login.autoReg", "false"));
        useSha1Hash = Boolean.parseBoolean(ServerProperties.getProperty("login.useSha1Hash", "false"));
        phantom = Boolean.parseBoolean(ServerProperties.getProperty("login.phantom", "false"));

        port = Short.parseShort(ServerProperties.getProperty("login.port", String.valueOf(8484)));

        try {
            acceptor = new ServerConnection(port, 0, -1, false);
            acceptor.run();
            System.out.println("登录器服务器绑定端口: " + port + ".");
            System.out.println("当前设置最大在线: " + userLimit + " 人 默认角色数: " + maxCharacters + " 人 自动注册: " + autoReg);
        } catch (Exception e) {
            System.err.println("登录器服务器绑定端口: " + port + " 失败" + e);
        }
    }

    public static void shutdown() {
        if (finishedShutdown) {
            return;
        }
        System.out.println("正在关闭登录服务器...");
        acceptor.close();
        finishedShutdown = true;
    }

    public static String getServerName() {
        return serverName;
    }

    public static String getTrueServerName() {
        return serverName.substring(0, serverName.length() - (GameConstants.GMS ? 2 : 3));
    }

    public static String getEventMessage() {
        return eventMessage;
    }

    public static byte getFlag() {
        return flag;
    }

    public static int getMaxCharacters() {
        return maxCharacters;
    }

    public static Map<Integer, Integer> getLoad() {
        return load;
    }

    public static void setLoad(Map<Integer, Integer> load_, int usersOn_) {
        load = load_;
        usersOn = usersOn_;
    }

    public static void setEventMessage(String newMessage) {
        eventMessage = newMessage;
    }

    public static void setFlag(byte newflag) {
        flag = newflag;
    }

    public static int getUserLimit() {
        return userLimit;
    }

    public static int getUsersOn() {
        return usersOn;
    }

    public static void setUserLimit(int newLimit) {
        userLimit = newLimit;
    }

    public static boolean isAdminOnly() {
        return adminOnly;
    }

    public static boolean isShutdown() {
        return finishedShutdown;
    }

    public static void setOn() {
        finishedShutdown = false;
    }

    public static boolean isAutoReg() {
        return autoReg;
    }

    public static boolean isUseSha1Hash() {
        return useSha1Hash;
    }

    public static boolean is开启幻影() {
        return phantom;
    }

    public static void 启用幻影(int type) {
        phantom = type > 0;
    }
}