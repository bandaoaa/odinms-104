package server.life;

import java.awt.Point;
import java.util.concurrent.atomic.AtomicBoolean;
import server.Randomizer;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

public class SpawnPointAreaBoss extends Spawns {

    private MapleMonsterStats monster;
    private Point pos1;
    private Point pos2;
    private Point pos3;
    private long nextPossibleSpawn;
    private int mobTime;
    private int fh;
    private int f;
    private int id;
    private AtomicBoolean spawned = new AtomicBoolean(false);
    private String msg;

    public SpawnPointAreaBoss(MapleMonster monster, Point pos1, Point pos2, Point pos3, int mobTime, String msg, boolean shouldSpawn) {
        this.monster = monster.getStats();
        this.id = monster.getId();
        this.fh = monster.getFh();
        this.f = monster.getF();
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.pos3 = pos3;
        this.mobTime = (mobTime < 0 ? -1 : mobTime * 1000);
        this.msg = msg;
        this.nextPossibleSpawn = (System.currentTimeMillis() + (shouldSpawn ? 0 : this.mobTime));
    }

    public int getF() {
        return this.f;
    }

    public int getFh() {
        return this.fh;
    }

    public MapleMonsterStats getMonster() {
        return this.monster;
    }

    public byte getCarnivalTeam() {
        return -1;
    }

    public int getCarnivalId() {
        return -1;
    }

    public boolean shouldSpawn(long time) {
        if ((this.mobTime < 0) || (this.spawned.get())) {
            return false;
        }
        return this.nextPossibleSpawn <= time;
    }

    public Point getPosition() {
        int rand = Randomizer.nextInt(3);
        return rand == 1 ? this.pos2 : rand == 0 ? this.pos1 : this.pos3;
    }

    public MapleMonster spawnMonster(MapleMap map) {
        Point pos = getPosition();
        MapleMonster mob = new MapleMonster(this.id, this.monster);
        mob.setPosition(pos);
        mob.setCy(pos.y);
        mob.setRx0(pos.x - 50);
        mob.setRx1(pos.x + 50);
        mob.setFh(this.fh);
        mob.setF(this.f);
        this.spawned.set(true);
        mob.addListener(new MonsterListener() {

            public void monsterKilled() {
                nextPossibleSpawn = System.currentTimeMillis();

                if (mobTime > 0) {
                    nextPossibleSpawn += mobTime;
                }
                SpawnPointAreaBoss.this.spawned.set(false);
            }
        });
        map.spawnMonster(mob, -2);

        if (this.msg != null) {
            map.broadcastMessage(MaplePacketCreator.serverNotice(6, this.msg));
        }
        return mob;
    }

    @Override
    public int getMobTime() {
        return this.mobTime;
    }
}
