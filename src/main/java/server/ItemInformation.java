package server;

import client.inventory.Equip;
import client.inventory.EquipAdditions;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import tools.Pair;

public class ItemInformation {

    public List<Integer> scrollReqs = null;
    public List<Integer> questItems = null;
    public List<Integer> incSkill = null;
    public short slotMax;
    public short itemMakeLevel;
    public Equip eq = null;
    public Map<String, Integer> equipStats;
    public double price = 0.0D;
    public int itemId;
    public int wholePrice;
    public int monsterBook;
    public int stateChange;
    public int meso;
    public int questId;
    public int totalprob;
    public int replaceItem;
    public int mob;
    public int cardSet;
    public int create;
    public int flag;
    public String name;
    public String desc;
    public String msg;
    public String replaceMsg;
    public String afterImage;
    public byte karmaEnabled;
    public List<StructRewardItem> rewardItems = null;
    public EnumMap<EquipAdditions, Pair<Integer, Integer>> equipAdditions = null;
    public Map<Integer, Map<String, Integer>> equipIncs = null;
}