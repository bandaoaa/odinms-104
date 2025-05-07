package handling.cashshop;

import handling.MapleServerHandler;
import handling.channel.PlayerStorage;
import handling.netty.ServerConnection;

import java.net.InetSocketAddress;

import server.MTSStorage;
import server.ServerProperties;

public class CashShopServer {

    private static String ip;
    private static ServerConnection acceptor;
    private static PlayerStorage players;
    private static PlayerStorage playersMTS;
    private static boolean finishedShutdown = false;
    private static short port;
    private static final short DEFAULT_PORT = 8900;

    public static void run_startup_configurations() {
        port = Short.parseShort(ServerProperties.getProperty("cashshop.port", String.valueOf(8900)));
        ip = ServerProperties.getProperty("world.host") + ":" + port;


        players = new PlayerStorage(-10);
        playersMTS = new PlayerStorage(-20);
        try {
            acceptor = new ServerConnection(port, 0, -1, true);
            acceptor.run();

            System.out.println("商城服务器绑定端口: " + port + ".");
        } catch (Exception e) {
            System.err.println("商城服务器绑定端口 " + port + " 失败");
            throw new RuntimeException("绑定端口失败.", e);
        }
    }

    public static String getIP() {
        return ip;
    }

    public static PlayerStorage getPlayerStorage() {
        return players;
    }

    public static PlayerStorage getPlayerStorageMTS() {
        return playersMTS;
    }

    public static void shutdown() {
        if (finishedShutdown) {
            return;
        }
        System.out.println("正在关闭商城服务器...");
        players.disconnectAll();
        playersMTS.disconnectAll();
        MTSStorage.getInstance().saveBuyNow(true);
        System.out.println("商城服务器解除端口绑定...");
        acceptor.close();
        finishedShutdown = true;
    }

    public static boolean isShutdown() {
        return finishedShutdown;
    }
}