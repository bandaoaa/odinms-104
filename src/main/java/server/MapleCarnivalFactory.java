package server;

import client.MapleDisease;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.life.MobSkill;
import server.life.MobSkillFactory;

public class MapleCarnivalFactory {

    private static MapleCarnivalFactory instance = new MapleCarnivalFactory();
    private Map<Integer, MCSkill> skills = new HashMap();
    private Map<Integer, MCSkill> guardians = new HashMap();
    private MapleDataProvider dataRoot = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Skill.wz"));

    public MapleCarnivalFactory() {
        initialize();
    }

    public static MapleCarnivalFactory getInstance() {
        return instance;
    }

    private void initialize() {
        if (!this.skills.isEmpty()) {
            return;
        }
        for (MapleData z : this.dataRoot.getData("MCSkill.img")) {
            this.skills.put(Integer.valueOf(Integer.parseInt(z.getName())), new MCSkill(MapleDataTool.getInt("spendCP", z, 0), MapleDataTool.getInt("mobSkillID", z, 0), MapleDataTool.getInt("level", z, 0), MapleDataTool.getInt("target", z, 1) > 1));
        }
        for (MapleData z : this.dataRoot.getData("MCGuardian.img")) {
            this.guardians.put(Integer.valueOf(Integer.parseInt(z.getName())), new MCSkill(MapleDataTool.getInt("spendCP", z, 0), MapleDataTool.getInt("mobSkillID", z, 0), MapleDataTool.getInt("level", z, 0), true));
        }
    }

    public MCSkill getSkill(int id) {
        return (MCSkill) this.skills.get(Integer.valueOf(id));
    }

    public MCSkill getGuardian(int id) {
        return (MCSkill) this.guardians.get(Integer.valueOf(id));
    }

    public static class MCSkill {

        public int cpLoss;
        public int skillid;
        public int level;
        public boolean targetsAll;

        public MCSkill(int _cpLoss, int _skillid, int _level, boolean _targetsAll) {
            this.cpLoss = _cpLoss;
            this.skillid = _skillid;
            this.level = _level;
            this.targetsAll = _targetsAll;
        }

        public MobSkill getSkill() {
            return MobSkillFactory.getMobSkill(this.skillid, 1);
        }

        public MapleDisease getDisease() {
            if (this.skillid <= 0) {
                return MapleDisease.getRandom();
            }
            return MapleDisease.getBySkill(this.skillid);
        }
    }
}