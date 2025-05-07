package server.maps;

import java.util.ArrayList;
import java.util.List;

public enum MapleQuickMove {

    战斗广场("", "移动到可以和其他玩家比试实力的大乱斗区域#c<战斗广场-赤壁>#。\n#c30级以上可以移动", 9070004, 0, 30, new int[]{100000000, 101000000, 102000000, 103000000, 104000000, 120000000, 130000101, 140000000, 200000000, 211000000, 220000000, 221000000, 222000000, 230000000, 240000000, 250000000, 251000000, 260000000, 261000000, 310000000}),
    怪物公园("", "移动到可以和队员们一起对付强大怪物的组队游戏区域\n#c<怪物公园>#。\n#c20级以上可以参加", 9071003, 1, 0, new int[]{100000000, 101000000, 102000000, 103000000, 104000000, 120000000, 130000101, 140000000, 200000000, 211000000, 220000000, 221000000, 222000000, 230000000, 240000000, 250000000, 251000000, 260000000, 261000000, 310000000}),
    次元之镜("", "使用可以移动到组队任务等各种主题地图上的#c<次元之镜>#。", 9010022, 2, 10, new int[]{100000000, 101000000, 102000000, 103000000, 104000000, 105000000, 120000000, 130000101, 140000000, 200000000, 211000000, 220000000, 221000000, 222000000, 230000000, 240000000, 250000000, 251000000, 260000000, 261000000, 310000000}),
    自由市场("", "移动到可以和其他玩家交易物品的#c<自由市场>#。", 9000087, 3, 0, new int[]{100000100, 101000000, 102000000, 103000000, 104000000, 120000000, 130000101, 140000000, 200000000, 211000000, 220000000, 221000000, 222000000, 230000000, 240000000, 250000000, 251000000, 260000000, 261000000, 310000000, 500000000, 540000000, 550000000, 600000000, 702000000, 800000000}),
    匠人街("", "移动到专业技术村庄#c<匠人街>#。\n#c30级以上可以移动", 9000088, 4, 30, new int[]{100000000, 101000000, 102000000, 103000000, 104000000, 105000000, 120000000, 130000101, 140000000, 200000000, 211000000, 220000000, 221000000, 222000000, 230000000, 240000000, 250000000, 251000000, 260000000, 261000000, 310000000}),
    出租车("", "使用将角色移动到附近主要地区的#c<出租车>#。", 9000089, 6, 0, new int[]{100000000, 101000000, 102000000, 103000000, 104000000, 120000000});
    public String name;
    public String desc;
    public int npcid;
    public int level;
    public int type;
    public int[] maps;

    private MapleQuickMove(String name, String desc, int npcid, int type, int level, int[] maps) {
        this.name = name;
        this.desc = desc;
        this.npcid = npcid;
        this.type = type;
        this.level = level;
        this.maps = maps;
    }

    public int getNpcId() {
        return this.npcid;
    }

    public static MapleQuickMove getNpcId(int npcid) {
        for (MapleQuickMove pn : values()) {
            if (pn.getNpcId() == npcid) {
                return pn;
            }
        }
        return null;
    }

    public static MapleQuickMove getMapId(int mapid) {
        for (MapleQuickMove pn : values()) {
            for (int i : pn.maps) {
                if (mapid == i) {
                    return pn;
                }
            }
        }
        return null;
    }

    public static List<MapleQuickMove> showQuickMove(int mapid) {
        List ret = new ArrayList();
        for (MapleQuickMove pn : values()) {
            for (int i : pn.maps) {
                if (mapid == i) {
                    ret.add(pn);
                }
            }
        }
        return ret;
    }

    public static boolean isQuickMoveMap(int mapid) {
        switch (mapid) {
            case 100000000:
            case 101000000:
            case 102000000:
            case 103000000:
            case 104000000:
            case 105000000:
            case 120000000:
            case 130000101:
            case 140000000:
            case 200000000:
            case 211000000:
            case 220000000:
            case 221000000:
            case 222000000:
            case 230000000:
            case 240000000:
            case 250000000:
            case 251000000:
            case 260000000:
            case 261000000:
            case 310000000:
                return true;
        }
        return false;
    }
}
