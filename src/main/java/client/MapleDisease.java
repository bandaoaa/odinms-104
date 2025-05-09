package client;

import handling.Buffstat;
import java.io.Serializable;
import server.Randomizer;

public enum MapleDisease implements Serializable, Buffstat {

    STUN(131072, 1, 123),
    POISON(262144, 1, 125),
    SEAL(524288, 1, 120),
    DARKNESS(1048576, 1, 121),
    WEAKEN(1073741824, 1, 122),
    CURSE(-2147483648, 1, 124),
    SLOW(1, 2, 126),
    MORPH(2, 2, 172),
    SEDUCE(128, 2, 128),
    ZOMBIFY(16384, 2, 133),
    REVERSE_DIRECTION(524288, 2, 132),
    POTION(512, 3, 134),
    SHADOW(1024, 3, 135),
    BLIND(2048, 3, 136),
    FREEZE(131072, 3, 137),
    DISABLE_POTENTIAL(16777216, 4, 138),
    TORNADO(268435456, 4, 173),
    FLAG(-2147483648, 5, 799);
    private static final long serialVersionUID = 0L;
    private int i;
    private int first;
    private int disease;

    private MapleDisease(int i, int first, int disease) {
        this.i = i;
        this.first = first;
        this.disease = disease;
    }

    @Override
    public int getPosition() {
        return this.first;
    }

    @Override
    public int getValue() {
        return this.i;
    }

    public int getDisease() {
        return this.disease;
    }

    public static MapleDisease getRandom() {
        while (true) {
            for (MapleDisease dis : values()) {
                if (Randomizer.nextInt(values().length) == 0) {
                    return dis;
                }
            }
        }
    }

    public static MapleDisease getBySkill(int skill) {
        for (MapleDisease d : values()) {
            if (d.getDisease() == skill) {
                return d;
            }
        }
        return null;
    }
}