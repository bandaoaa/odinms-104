package handling.world;

import java.io.Serializable;

public class CheaterData implements Serializable, Comparable<CheaterData> {

    private static final long serialVersionUID = -8733673311051249885L;
    private int points;
    private String info;

    public CheaterData(int points, String info) {
        this.points = points;
        this.info = info;
    }

    public String getInfo() {
        return this.info;
    }

    public int getPoints() {
        return this.points;
    }

    @Override
    public int compareTo(CheaterData o) {
        int thisVal = getPoints();
        int anotherVal = o.getPoints();
        return thisVal == anotherVal ? 0 : thisVal < anotherVal ? 1 : -1;
    }

    @Override
    public boolean equals(Object oth) {
        if (!(oth instanceof CheaterData)) {
            return false;
        }
        CheaterData obj = (CheaterData) oth;
        return (obj.points == this.points) && (obj.info.equals(this.info));
    }
}
