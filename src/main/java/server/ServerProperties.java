package server;

import database.DatabaseConnection;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import tools.PropertyTool;

public class ServerProperties {

    private static final Properties props = new Properties();
    private static Properties settings = new Properties();
    private static PropertyTool propTool = new PropertyTool(new Properties());
    private static boolean showPacket;
    private static Map<String, Boolean> blockedOpcodes = new HashMap();
    private static boolean blockDefault;
    private static String[] blockSkills;
    private static final Properties skillProps = new Properties();
    private static boolean show = false;//显示封包
   // private static boolean show = true;//显示封包

    public static void loadProperties(String s) {
        try {
            FileReader fr = new FileReader(s);
            props.load(fr);
            fr.close();
        } catch (IOException ex) {
            System.out.println("加载 服务端配置.properties 配置出错 " + ex);
        }
    }

    public static void loadSkills() {
        try {
            FileReader sp = new FileReader("config/skills.properties");
            skillProps.load(sp);
            sp.close();
        } catch (IOException ex) {
            System.out.println("加载 skills.properties 配置出错 " + ex);
        }
        blockSkills = null;
        blockSkills = skillProps.getProperty("BlockSkills").split(",");
    }

    public static String[] getBlockSkills() {
        return blockSkills;
    }

    public static boolean getBlockSkills(int id) {
        String[] Str = getBlockSkills();
        String skillId = id + "";
        for (int i = 0; i < Str.length; i++) {
            if (Str[i].equals(skillId)) {
                System.out.println("禁止技能:" + skillId);
                return true;
            }
        }
        return false;
    }

    public static void loadSettings() {
        try {
            FileInputStream fis = new FileInputStream("settings.properties");
            settings.load(fis);
            fis.close();
        } catch (IOException ex) {
            System.out.println("加载 settings.properties 配置出错" + ex);
        }
        propTool = new PropertyTool(settings);
        showPacket = propTool.getSettingInt("ShowPacket", 1) > 0;
        blockDefault = propTool.getSettingInt("BlockDefault", 0) > 0;
        blockedOpcodes.clear();
        for (Entry entry : settings.entrySet()) {
            String property = (String) entry.getKey();
            if ((property.startsWith("S_")) || (property.startsWith("R_"))) {
                blockedOpcodes.put(property, propTool.getSettingInt(property, 0) > 0);
            }
        }
    }

    public static boolean isLoadShow() {
        return show;
    }

    public static boolean ShowPacket() {
        return showPacket && show;
    }

    public static boolean SendPacket(String op, String pHeaderStr) {
        if (op.equals("UNKNOWN")) {
            return blockedOpcodes.containsKey("S_" + pHeaderStr) ? ((Boolean) blockedOpcodes.get("S_" + pHeaderStr)).booleanValue() : blockDefault;
        }
        return blockedOpcodes.containsKey("S_" + op) ? ((Boolean) blockedOpcodes.get("S_" + op)).booleanValue() : blockDefault;
    }

    public static boolean RecvPacket(String op, String pHeaderStr) {
        if (op.equals("UNKNOWN")) {
            return blockedOpcodes.containsKey("R_" + pHeaderStr) ? ((Boolean) blockedOpcodes.get("R_" + pHeaderStr)).booleanValue() : blockDefault;
        }
        return blockedOpcodes.containsKey("R_" + op) ? ((Boolean) blockedOpcodes.get("R_" + op)).booleanValue() : blockDefault;
    }

    public static String getProperty(String s) {
        return props.getProperty(s);
    }

    public static void setProperty(String prop, String newInf) {
        props.setProperty(prop, newInf);
    }

    public static String getProperty(String s, String def) {
        return props.getProperty(s, def);
    }

    static {
        String toLoad = "服务端配置.properties";
        loadProperties(toLoad);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM auth_server_channel_ip");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                props.put(rs.getString("name") + rs.getInt("channelid"), rs.getString("value"));
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            System.exit(0);
        }
        if (isLoadShow()) {
            loadSettings();
        }
    }
}