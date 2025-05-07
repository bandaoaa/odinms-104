package client.status;

import client.MapleDisease;
import constants.GameConstants;
import handling.Buffstat;
import java.io.Serializable;

public enum MonsterStatus implements Serializable, Buffstat {

    物攻(0x1, 1),
    物防(0x2, 1),
    魔攻(0x4, 1),
    魔防(0x8, 1),
    命中(0x10, 1),
    回避(0x20, 1),
    速度(0x40, 1),
    眩晕(0x80, 1),
    结冰(0x100, 1),
    中毒(0x200, 1),
    封印(0x400, 1),
    挑衅(0x800, 1),
    物攻提升(0x1000, 1),
    物防提升(0x2000, 1),
    魔攻提升(0x4000, 1),
    魔防提升(0x8000, 1),
    巫毒(0x10000, 1),
    影网(0x20000, 1),
    免疫物攻(0x40000, 1),
    免疫魔攻(0x40000, 1),
    免疫伤害(0x80000, 1),
    忍者伏击(0x200000, 1),
    烈焰喷射(0x400000, 1),
    恐慌(0x1000000, 1),
    心灵控制(0x2000000, 1),
    反射物攻(0x20000000, 1),
    反射魔攻(0x40000000, 1),
    抗压(0x2, 2),
    鬼刻符(0x4, 2),
    怪物炸弹(0x8, 2),
    魔击无效(0x10, 2),
    空白BUFF(0x8000000, 1, true),
    召唤怪物(0x80000000, 1, true),
    EMPTY_1(0x20, 2, !GameConstants.GMS),
    EMPTY_2(0x40, 2, true),
    EMPTY_3(0x80, 2, true),
    EMPTY_4(0x100, 2, GameConstants.GMS),
    EMPTY_5(0x200, 2, GameConstants.GMS),
    EMPTY_6(0x04000, 2, GameConstants.GMS);
    static final long serialVersionUID = 0L;
    private final int i;
    private final int first;
    private final boolean end;

    private MonsterStatus(int i, int first) {
        this.i = i;
        this.first = first;
        this.end = false;
    }

    private MonsterStatus(int i, int first, boolean end) {
        this.i = i;
        this.first = first;
        this.end = end;
    }

    public int getPosition() {
        return this.first;
    }

    public boolean isEmpty() {
        return this.end;
    }

    public int getValue() {
        return this.i;
    }

    public static MonsterStatus getBySkill_Pokemon(int skill) {
        switch (skill) {
            case 120:
                return 封印;
            case 121:
                return 恐慌;
            case 123:
                return 眩晕;
            case 125:
                return 中毒;
            case 126:
                return 速度;
            case 137:
                return 结冰;
            case 122:
            case 124:
            case 127:
            case 128:
            case 129:
            case 130:
            case 131:
            case 132:
            case 133:
            case 134:
            case 135:
            case 136:
        }
        return null;
    }

    public static MapleDisease getLinkedDisease(MonsterStatus skill) {
        switch (skill) {
            case 眩晕:
            case 影网:
                return MapleDisease.STUN;
            case 中毒:
            case 心灵控制:
                return MapleDisease.POISON;
            case 封印:
            case 魔击无效:
                return MapleDisease.SEAL;
            case 结冰:
                return MapleDisease.FREEZE;
            case 反射物攻:
                return MapleDisease.DARKNESS;
            case 速度:
                return MapleDisease.SLOW;
        }
        return null;
    }
}