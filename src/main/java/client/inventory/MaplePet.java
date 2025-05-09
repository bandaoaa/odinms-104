package client.inventory;

import database.DatabaseConnection;
import java.awt.Point;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.movement.AbsoluteLifeMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;

public class MaplePet implements Serializable {

    private static final Logger log = Logger.getLogger(MaplePet.class);
    private static final long serialVersionUID = 9179541993413738569L;
    private String name;
    private int Fh = 0;
    private int stance = 0;
    private int uniqueid;
    private int petitemid;
    private int secondsLeft = 0;
    private int skillid;
    private Point pos;
    private byte fullness = 100;
    private byte level = 1;
    private byte summoned = 0;
    private short inventorypos = 0;
    private short closeness = 0;
    private short flags = 0;
    private boolean changed = false;

    public static enum PetFlag {

        ITEM_PICKUP(0x01, 5190000, 5191000),
        EXPAND_PICKUP(0x02, 5190002, 5191002),
        AUTO_PICKUP(0x04, 5190003, 5191003),
        UNPICKABLE(0x08, 5190005, -1),
        LEFTOVER_PICKUP(0x10, 5190004, 5191004),
        HP_CHARGE(0x20, 5190001, 5191001),
        MP_CHARGE(0x40, 5190006, -1),
        PET_DRAW(0x80, 5190007, -1),
        PET_DIALOGUE(0x100, 5190008, -1),
        PET_BUFF(0x200, 5190010, -1);
        private int i;
        private int item;
        private int remove;

        private PetFlag(int i, int item, int remove) {
            this.i = i;
            this.item = item;
            this.remove = remove;
        }

        public int getValue() {
            return this.i;
        }

        public boolean check(int flag) {
            return (flag & this.i) == this.i;
        }

        public static PetFlag getByAddId(int itemId) {
            for (PetFlag flag : values()) {
                if (flag.item == itemId) {
                    return flag;
                }
            }
            return null;
        }

        public static PetFlag getByDelId(int itemId) {
            for (PetFlag flag : values()) {
                if (flag.remove == itemId) {
                    return flag;
                }
            }
            return null;
        }
    }

    private MaplePet(int petitemid, int uniqueid) {
        this.petitemid = petitemid;
        this.uniqueid = uniqueid;
    }

    private MaplePet(int petitemid, int uniqueid, short inventorypos) {
        this.petitemid = petitemid;
        this.uniqueid = uniqueid;
        this.inventorypos = inventorypos;
    }

    public static MaplePet loadFromDb(int itemid, int petid, short inventorypos) {
        try {
            MaplePet ret = new MaplePet(itemid, petid, inventorypos);

            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM pets WHERE petid = ?");
            ps.setInt(1, petid);

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return null;
            }

            ret.setName(rs.getString("name"));
            ret.setCloseness(rs.getShort("closeness"));
            ret.setLevel(rs.getByte("level"));
            ret.setFullness(rs.getByte("fullness"));
            ret.setSecondsLeft(rs.getInt("seconds"));
            ret.setFlags(rs.getShort("flags"));
            ret.setBuffSkill(rs.getInt("skillid"));
            ret.changed = false;

            rs.close();
            ps.close();

            return ret;
        } catch (SQLException ex) {
            log.error("加载宠物信息出错", ex);
        }
        return null;
    }

    public void saveToDb() {
        if (!this.changed) {
            return;
        }
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE pets SET name = ?, level = ?, closeness = ?, fullness = ?, seconds = ?, flags = ?, skillid = ? WHERE petid = ?");
            ps.setString(1, this.name);
            ps.setByte(2, this.level);
            ps.setShort(3, this.closeness);
            ps.setByte(4, this.fullness);
            ps.setInt(5, this.secondsLeft);
            ps.setShort(6, this.flags);
            ps.setInt(7, this.skillid);
            ps.setInt(8, this.uniqueid);
            ps.executeUpdate();
            ps.close();
            this.changed = false;
        } catch (SQLException ex) {
            log.error("保存宠物信息出错", ex);
        }
    }

    public static MaplePet createPet(int itemid, int uniqueid) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        return createPet(itemid, ii.getName(itemid), 1, 0, 100, uniqueid, itemid == 5000054 ? 18000 : 0, ii.getPetFlagInfo(itemid), 0);
    }

    public static MaplePet createPet(int itemid, String name, int level, int closeness, int fullness, int uniqueid, int secondsLeft, short flag, int skillId) {
        if (uniqueid <= -1) {
            uniqueid = MapleInventoryIdentifier.getInstance();
        }
        try {
            PreparedStatement pse = DatabaseConnection.getConnection().prepareStatement("INSERT INTO pets (petid, name, level, closeness, fullness, seconds, flags, skillid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            pse.setInt(1, uniqueid);
            pse.setString(2, name);
            pse.setByte(3, (byte) level);
            pse.setShort(4, (short) closeness);
            pse.setByte(5, (byte) fullness);
            pse.setInt(6, secondsLeft);
            pse.setShort(7, flag);
            pse.setInt(8, skillId);
            pse.executeUpdate();
            pse.close();
        } catch (SQLException ex) {
            log.error("创建宠物信息出错", ex);
            return null;
        }
        MaplePet pet = new MaplePet(itemid, uniqueid);
        pet.setName(name);
        pet.setLevel(level);
        pet.setFullness(fullness);
        pet.setCloseness(closeness);
        pet.setFlags(flag);
        pet.setSecondsLeft(secondsLeft);
        pet.setBuffSkill(skillId);

        return pet;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        this.changed = true;
    }

    public boolean getSummoned() {
        return this.summoned > 0;
    }

    public byte getSummonedValue() {
        return this.summoned;
    }

    public void setSummoned(int summoned) {
        this.summoned = (byte) summoned;
    }

    public short getInventoryPosition() {
        return this.inventorypos;
    }

    public void setInventoryPosition(short inventorypos) {
        this.inventorypos = inventorypos;
    }

    public int getUniqueId() {
        return this.uniqueid;
    }

    public short getCloseness() {
        return this.closeness;
    }

    public void setCloseness(int closeness) {
        this.closeness = (short) closeness;
        this.changed = true;
    }

    public byte getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = (byte) level;
        this.changed = true;
    }

    public byte getFullness() {
        return this.fullness;
    }

    public void setFullness(int fullness) {
        this.fullness = (byte) fullness;
        this.changed = true;
    }

    public short getFlags() {
        return this.flags;
    }

    public void setFlags(int fffh) {
        this.flags = (short) fffh;
        this.changed = true;
    }

    public int getFh() {
        return this.Fh;
    }

    public void setFh(int Fh) {
        this.Fh = Fh;
    }

    public Point getPos() {
        return this.pos;
    }

    public void setPos(Point pos) {
        this.pos = pos;
    }

    public int getStance() {
        return this.stance;
    }

    public void setStance(int stance) {
        this.stance = stance;
    }

    public int getPetItemId() {
        return this.petitemid;
    }

    public boolean canConsume(int itemId) {
        MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        for (int petId : mii.getItemEffect(itemId).getPetsCanConsume()) {
            if (petId == petitemid) {
                return true;
            }
        }
        return false;
    }

    public void updatePosition(List<LifeMovementFragment> movement) {
        for (LifeMovementFragment move : movement) {
            if ((move instanceof LifeMovement)) {
                if ((move instanceof AbsoluteLifeMovement)) {
                    setPos(((LifeMovement) move).getPosition());
                }
                setStance(((LifeMovement) move).getNewstate());
            }
        }
    }

    public int getSecondsLeft() {
        return this.secondsLeft;
    }

    public void setSecondsLeft(int sl) {
        this.secondsLeft = sl;
        this.changed = true;
    }

    public int getBuffSkill() {
        return this.skillid;
    }

    public void setBuffSkill(int id) {
        this.skillid = id;
    }
}