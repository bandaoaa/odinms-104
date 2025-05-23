package handling.world.family;

import client.MapleCharacter;
import database.DatabaseConnection;
import handling.world.World.Broadcast;
import handling.world.World.Family;
import java.io.PrintStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import tools.MaplePacketCreator;
import tools.packet.FamilyPacket;

public final class MapleFamily implements Serializable {

    public static final long serialVersionUID = 6322150443228168192L;
    private final Map<Integer, MapleFamilyCharacter> members = new ConcurrentHashMap();
    private String leadername = null;
    private String notice;
    private int id;
    private int leaderid;
    private boolean proper = true;
    private boolean bDirty = false;
    private boolean changed = false;
    private static final Logger log = Logger.getLogger(MapleFamily.class);

    public MapleFamily(int fid) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM families WHERE familyid = ?");
            ps.setInt(1, fid);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                rs.close();
                ps.close();
                this.id = -1;
                this.proper = false;
                return;
            }
            this.id = fid;
            this.leaderid = rs.getInt("leaderid");
            this.notice = rs.getString("notice");
            rs.close();
            ps.close();

            ps = con.prepareStatement("SELECT id, name, level, job, seniorid, junior1, junior2, currentrep, totalrep FROM characters WHERE familyid = ?", 1008);
            ps.setInt(1, fid);
            rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("id") == this.leaderid) {
                    this.leadername = rs.getString("name");
                }
                this.members.put(Integer.valueOf(rs.getInt("id")), new MapleFamilyCharacter(rs.getInt("id"), rs.getShort("level"), rs.getString("name"), -1, rs.getInt("job"), fid, rs.getInt("seniorid"), rs.getInt("junior1"), rs.getInt("junior2"), rs.getInt("currentrep"), rs.getInt("totalrep"), false));
            }
            rs.close();
            ps.close();

            if ((this.leadername == null) || (this.members.size() < 2)) {
                System.err.println("Leader " + this.leaderid + " isn't in family " + this.id + ". Members: " + this.members.size() + ".  Impossible... family is disbanding.");
                writeToDB(true);
                this.proper = false;
                return;
            }

            for (MapleFamilyCharacter mfc : this.members.values()) {
                if ((mfc.getJunior1() > 0) && ((getMFC(mfc.getJunior1()) == null) || (mfc.getId() == mfc.getJunior1()))) {
                    mfc.setJunior1(0);
                }
                if ((mfc.getJunior2() > 0) && ((getMFC(mfc.getJunior2()) == null) || (mfc.getId() == mfc.getJunior2()) || (mfc.getJunior1() == mfc.getJunior2()))) {
                    mfc.setJunior2(0);
                }
                if ((mfc.getSeniorId() > 0) && ((getMFC(mfc.getSeniorId()) == null) || (mfc.getId() == mfc.getSeniorId()))) {
                    mfc.setSeniorId(0);
                }
                if ((mfc.getJunior2() > 0) && (mfc.getJunior1() <= 0)) {
                    mfc.setJunior1(mfc.getJunior2());
                    mfc.setJunior2(0);
                }
                if (mfc.getJunior1() > 0) {
                    MapleFamilyCharacter mfc2 = getMFC(mfc.getJunior1());
                    if (mfc2.getJunior1() == mfc.getId()) {
                        mfc2.setJunior1(0);
                    }
                    if (mfc2.getJunior2() == mfc.getId()) {
                        mfc2.setJunior2(0);
                    }
                    if (mfc2.getSeniorId() != mfc.getId()) {
                        mfc2.setSeniorId(mfc.getId());
                    }
                }
                if (mfc.getJunior2() > 0) {
                    MapleFamilyCharacter mfc2 = getMFC(mfc.getJunior2());
                    if (mfc2.getJunior1() == mfc.getId()) {
                        mfc2.setJunior1(0);
                    }
                    if (mfc2.getJunior2() == mfc.getId()) {
                        mfc2.setJunior2(0);
                    }
                    if (mfc2.getSeniorId() != mfc.getId()) {
                        mfc2.setSeniorId(mfc.getId());
                    }
                }
            }
            resetPedigree();
            resetDescendants();
        } catch (SQLException se) {
            log.error("[MapleFamily] 从数据库中读取学院信息出错." + se);
        }
    }

    public void resetPedigree() {
        for (MapleFamilyCharacter mfc : this.members.values()) {
            mfc.resetPedigree(this);
        }
        this.bDirty = true;
    }

    public void resetDescendants() {
        MapleFamilyCharacter mfc = getMFC(this.leaderid);
        if (mfc != null) {
            mfc.resetDescendants(this);
        }
        this.bDirty = true;
    }

    public boolean isProper() {
        return this.proper;
    }

    public static void loadAll() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT familyid FROM families");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Family.addLoadedFamily(new MapleFamily(rs.getInt("familyid")));
            }
            rs.close();
            ps.close();
        } catch (SQLException se) {
            log.error("[MapleFamily] 从数据库中读取学院信息出错." + se);
        }
    }

    public static void loadAll(Object toNotify) {
        try {
            boolean cont = false;
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT familyid FROM families");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                FamilyLoad.QueueFamilyForLoad(rs.getInt("familyid"));
                cont = true;
            }
            rs.close();
            ps.close();
            if (!cont) {
                return;
            }
        } catch (SQLException se) {
            log.error("[MapleFamily] 从数据库中读取学院信息出错." + se);
        }
        AtomicInteger FinishedThreads = new AtomicInteger(0);
        FamilyLoad.Execute(toNotify);
        synchronized (toNotify) {
            try {
                toNotify.wait();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        while (FinishedThreads.incrementAndGet() != 8) {
            synchronized (toNotify) {
                try {
                    toNotify.wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void writeToDB(boolean bDisband) {
        try {
            Connection con = DatabaseConnection.getConnection();
            if (!bDisband) {
                if (this.changed) {
                    PreparedStatement ps = con.prepareStatement("UPDATE families SET notice = ? WHERE familyid = ?");
                    ps.setString(1, this.notice);
                    ps.setInt(2, this.id);
                    ps.execute();
                    ps.close();
                }
                this.changed = false;
            } else {
                if ((this.leadername == null) || (this.members.size() < 2)) {
                    broadcast(null, -1, FCOp.DISBAND, null);
                }

                PreparedStatement ps = con.prepareStatement("DELETE FROM families WHERE familyid = ?");
                ps.setInt(1, this.id);
                ps.execute();
                ps.close();
            }
        } catch (SQLException se) {
            log.error("[MapleFamily] 保存学院信息出错." + se);
        }
    }

    public int getId() {
        return this.id;
    }

    public int getLeaderId() {
        return this.leaderid;
    }

    public String getNotice() {
        if (this.notice == null) {
            return "";
        }
        return this.notice;
    }

    public String getLeaderName() {
        return this.leadername;
    }

    public void broadcast(byte[] packet, List<Integer> cids) {
        broadcast(packet, -1, FCOp.NONE, cids);
    }

    public void broadcast(byte[] packet, int exception, List<Integer> cids) {
        broadcast(packet, exception, FCOp.NONE, cids);
    }

    public void broadcast(byte[] packet, int exceptionId, FCOp bcop, List<Integer> cids) {
        buildNotifications();
        if (this.members.size() < 2) {
            this.bDirty = true;
            return;
        }
        for (MapleFamilyCharacter mgc : this.members.values()) {
            if ((cids == null) || (cids.contains(Integer.valueOf(mgc.getId())))) {
                if (bcop == FCOp.DISBAND) {
                    if (mgc.isOnline()) {
                        Family.setFamily(0, 0, 0, 0, mgc.getCurrentRep(), mgc.getTotalRep(), mgc.getId());
                    } else {
                        setOfflineFamilyStatus(0, 0, 0, 0, mgc.getCurrentRep(), mgc.getTotalRep(), mgc.getId());
                    }
                } else if ((mgc.isOnline()) && (mgc.getId() != exceptionId)) {
                    Broadcast.sendFamilyPacket(mgc.getId(), packet, exceptionId, this.id);
                }
            }
        }
    }

    private void buildNotifications() {
        if (!this.bDirty) {
            return;
        }
        Iterator toRemove = this.members.entrySet().iterator();
        while (toRemove.hasNext()) {
            MapleFamilyCharacter mfc = (MapleFamilyCharacter) ((Map.Entry) toRemove.next()).getValue();
            if ((mfc.getJunior1() > 0) && (getMFC(mfc.getJunior1()) == null)) {
                mfc.setJunior1(0);
            }
            if ((mfc.getJunior2() > 0) && (getMFC(mfc.getJunior2()) == null)) {
                mfc.setJunior2(0);
            }
            if ((mfc.getSeniorId() > 0) && (getMFC(mfc.getSeniorId()) == null)) {
                mfc.setSeniorId(0);
            }
            if (mfc.getFamilyId() != this.id) {
                toRemove.remove();
                continue;
            }
        }
        if ((this.members.size() < 2) && (Family.getFamily(this.id) != null)) {
            Family.disbandFamily(this.id);
        }
        this.bDirty = false;
    }

    public void setOnline(int cid, boolean online, int channel) {
        MapleFamilyCharacter mgc = getMFC(cid);
        if ((mgc != null) && (mgc.getFamilyId() == this.id)) {
            if (mgc.isOnline() != online) {
                broadcast(FamilyPacket.familyLoggedIn(online, mgc.getName()), cid, mgc.getId() == this.leaderid ? null : mgc.getPedigree());
            }
            mgc.setOnline(online);
            mgc.setChannel((byte) channel);
        }
        this.bDirty = true;
    }

    public int setRep(int cid, int addrep, int oldLevel, String oldName) {
        MapleFamilyCharacter mgc = getMFC(cid);
        if ((mgc != null) && (mgc.getFamilyId() == this.id)) {
            if (oldLevel > mgc.getLevel()) {
                addrep /= 2;
            }
            if (mgc.isOnline()) {
                List dummy = new ArrayList();
                dummy.add(Integer.valueOf(mgc.getId()));
                broadcast(FamilyPacket.changeRep(addrep, oldName), -1, dummy);
                Family.setFamily(this.id, mgc.getSeniorId(), mgc.getJunior1(), mgc.getJunior2(), mgc.getCurrentRep() + addrep, mgc.getTotalRep() + addrep, mgc.getId());
            } else {
                mgc.setCurrentRep(mgc.getCurrentRep() + addrep);
                mgc.setTotalRep(mgc.getTotalRep() + addrep);
                setOfflineFamilyStatus(this.id, mgc.getSeniorId(), mgc.getJunior1(), mgc.getJunior2(), mgc.getCurrentRep(), mgc.getTotalRep(), mgc.getId());
            }
            return mgc.getSeniorId();
        }
        return 0;
    }

    public MapleFamilyCharacter addFamilyMemberInfo(MapleCharacter mc, int seniorid, int junior1, int junior2) {
        MapleFamilyCharacter ret = new MapleFamilyCharacter(mc, this.id, seniorid, junior1, junior2);
        this.members.put(Integer.valueOf(mc.getId()), ret);
        ret.resetPedigree(this);
        this.bDirty = true;
        List toRemove = new ArrayList();
        for (int i = 0; i < ret.getPedigree().size(); i++) {
            if (((Integer) ret.getPedigree().get(i)).intValue() == ret.getId()) {
                continue;
            }
            MapleFamilyCharacter mfc = getMFC(((Integer) ret.getPedigree().get(i)).intValue());
            if (mfc == null) {
                toRemove.add(Integer.valueOf(i));
            } else {
                mfc.resetPedigree(this);
            }
        }
        for (Iterator i$ = toRemove.iterator(); i$.hasNext();) {
            int i = ((Integer) i$.next()).intValue();
            ret.getPedigree().remove(i);
        }
        return ret;
    }

    public int addFamilyMember(MapleFamilyCharacter mgc) {
        mgc.setFamilyId(this.id);
        this.members.put(Integer.valueOf(mgc.getId()), mgc);
        mgc.resetPedigree(this);
        this.bDirty = true;
        for (Iterator i$ = mgc.getPedigree().iterator(); i$.hasNext();) {
            int i = ((Integer) i$.next()).intValue();
            getMFC(i).resetPedigree(this);
        }
        return 1;
    }

    public void leaveFamily(int id) {
        leaveFamily(getMFC(id), true);
    }

    public void leaveFamily(MapleFamilyCharacter mgc, boolean skipLeader) {
        this.bDirty = true;
        if ((mgc.getId() == this.leaderid) && (!skipLeader)) {
            this.leadername = null;
            Family.disbandFamily(this.id);
        } else {
            if (mgc.getJunior1() > 0) {
                MapleFamilyCharacter j = getMFC(mgc.getJunior1());
                if (j != null) {
                    j.setSeniorId(0);
                    splitFamily(j.getId(), j);
                }
            }
            if (mgc.getJunior2() > 0) {
                MapleFamilyCharacter j = getMFC(mgc.getJunior2());
                if (j != null) {
                    j.setSeniorId(0);
                    splitFamily(j.getId(), j);
                }
            }
            if (mgc.getSeniorId() > 0) {
                MapleFamilyCharacter mfc = getMFC(mgc.getSeniorId());
                if (mfc != null) {
                    if (mfc.getJunior1() == mgc.getId()) {
                        mfc.setJunior1(0);
                    } else {
                        mfc.setJunior2(0);
                    }
                }
            }
            List dummy = new ArrayList();
            dummy.add(Integer.valueOf(mgc.getId()));
            broadcast(null, -1, FCOp.DISBAND, dummy);
            resetPedigree();
        }
        this.members.remove(Integer.valueOf(mgc.getId()));
        this.bDirty = true;
    }

    public void setNotice(String notice) {
        this.changed = true;
        this.notice = notice;
    }

    public void memberLevelJobUpdate(MapleCharacter mgc) {
        MapleFamilyCharacter member = getMFC(mgc.getId());
        if (member != null) {
            int old_level = member.getLevel();
            int old_job = member.getJobId();
            member.setJobId(mgc.getJob());
            member.setLevel(mgc.getLevel());
            if (old_level != mgc.getLevel()) {
                broadcast(MaplePacketCreator.sendLevelup(true, mgc.getLevel(), mgc.getName()), mgc.getId(), mgc.getId() == this.leaderid ? null : member.getPedigree());
            }
            if (old_job != mgc.getJob()) {
                broadcast(MaplePacketCreator.sendJobup(true, mgc.getJob(), mgc.getName()), mgc.getId(), mgc.getId() == this.leaderid ? null : member.getPedigree());
            }
        }
    }

    public void disbandFamily() {
        writeToDB(true);
    }

    public MapleFamilyCharacter getMFC(int cid) {
        return (MapleFamilyCharacter) this.members.get(Integer.valueOf(cid));
    }

    public int getMemberSize() {
        return this.members.size();
    }

    public static void setOfflineFamilyStatus(int familyid, int seniorid, int junior1, int junior2, int currentrep, int totalrep, int cid) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET familyid = ?, seniorid = ?, junior1 = ?, junior2 = ?, currentrep = ?, totalrep = ? WHERE id = ?");
            ps.setInt(1, familyid);
            ps.setInt(2, seniorid);
            ps.setInt(3, junior1);
            ps.setInt(4, junior2);
            ps.setInt(5, currentrep);
            ps.setInt(6, totalrep);
            ps.setInt(7, cid);
            ps.execute();
            ps.close();
        } catch (SQLException se) {
            System.out.println("SQLException: " + se.getLocalizedMessage());
        }
    }

    public static int createFamily(int leaderId) {
        try {
            Connection con = DatabaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement("INSERT INTO families (`leaderid`) VALUES (?)", 1);
            ps.setInt(1, leaderId);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return 0;
            }
            int ret = rs.getInt(1);
            rs.close();
            ps.close();
            return ret;
        } catch (Exception e) {
            log.error("[MapleFamily] 创建学院信息出错." + e);
        }
        return 0;
    }

    public static void mergeFamily(MapleFamily newfam, MapleFamily oldfam) {
        for (MapleFamilyCharacter mgc : oldfam.members.values()) {
            mgc.setFamilyId(newfam.getId());
            if (mgc.isOnline()) {
                Family.setFamily(newfam.getId(), mgc.getSeniorId(), mgc.getJunior1(), mgc.getJunior2(), mgc.getCurrentRep(), mgc.getTotalRep(), mgc.getId());
            } else {
                setOfflineFamilyStatus(newfam.getId(), mgc.getSeniorId(), mgc.getJunior1(), mgc.getJunior2(), mgc.getCurrentRep(), mgc.getTotalRep(), mgc.getId());
            }
            newfam.members.put(Integer.valueOf(mgc.getId()), mgc);
            newfam.setOnline(mgc.getId(), mgc.isOnline(), mgc.getChannel());
        }
        newfam.resetPedigree();

        Family.disbandFamily(oldfam.getId());
    }

    public boolean splitFamily(int splitId, MapleFamilyCharacter def) {
        MapleFamilyCharacter leader = getMFC(splitId);
        if (leader == null) {
            leader = def;
            if (leader == null) {
                return false;
            }
        }
        try {
            List<MapleFamilyCharacter> all = leader.getAllJuniors(this);
            if (all.size() <= 1) {
                leaveFamily(leader, false);;
                return true;
            }
            int newId = createFamily(leader.getId());
            if (newId <= 0) {
                return false;
            }
            for (MapleFamilyCharacter mgc : all) {
                mgc.setFamilyId(newId);
                setOfflineFamilyStatus(newId, mgc.getSeniorId(), mgc.getJunior1(), mgc.getJunior2(), mgc.getCurrentRep(), mgc.getTotalRep(), mgc.getId());
                this.members.remove(Integer.valueOf(mgc.getId()));
            }
            MapleFamily newfam = Family.getFamily(newId);
            for (MapleFamilyCharacter mgc : all) {
                if (mgc.isOnline()) {
                    Family.setFamily(newId, mgc.getSeniorId(), mgc.getJunior1(), mgc.getJunior2(), mgc.getCurrentRep(), mgc.getTotalRep(), mgc.getId());
                }
                newfam.setOnline(mgc.getId(), mgc.isOnline(), mgc.getChannel());
            }

            if (this.members.size() <= 1) {
                Family.disbandFamily(this.id);
                return true;
            }
        } finally {
            if (this.members.size() <= 1) {
                Family.disbandFamily(this.id);
                return true;
            }
        }
        this.bDirty = true;
        return false;
    }

    public static enum FCOp {

        NONE, DISBAND;
    }
}