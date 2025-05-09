package handling.world.guild;

import handling.world.World.Guild;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;

public class GuildLoad {

    public static final int NumSavingThreads = 6;
    private static Map<Integer, Map<Integer, MapleBBSThread.MapleBBSReply>> replies = null;
    private static final TimingThread[] Threads = new TimingThread[6];
    private static final Logger log = Logger.getLogger(GuildLoad.class);
    private static final AtomicInteger Distribute;

    public static void QueueGuildForLoad(int hm, Map<Integer, Map<Integer, MapleBBSThread.MapleBBSReply>> replie) {
        int Current = Distribute.getAndIncrement() % 6;
        Threads[Current].getRunnable().Queue(Integer.valueOf(hm));
        if (replies == null) {
            replies = replie;
        }
    }

    public static void Execute(Object ToNotify) {
        for (int i = 0; i < Threads.length; i++) {
            Threads[i].getRunnable().SetToNotify(ToNotify);
        }
        for (int i = 0; i < Threads.length; i++) {
            Threads[i].start();
        }
    }

    static {
        for (int i = 0; i < Threads.length; i++) {
            Threads[i] = new TimingThread(new GuildLoadRunnable());
        }

        Distribute = new AtomicInteger(0);
    }

    private static class TimingThread extends Thread {

        private final GuildLoad.GuildLoadRunnable ext;

        public TimingThread(GuildLoad.GuildLoadRunnable r) {
            super();
            this.ext = r;
        }

        public GuildLoad.GuildLoadRunnable getRunnable() {
            return this.ext;
        }
    }

    private static class GuildLoadRunnable
            implements Runnable {

        private Object ToNotify;
        private ArrayBlockingQueue<Integer> Queue = new ArrayBlockingQueue(1000);

        @Override
        public void run() {
            try {
                while (!this.Queue.isEmpty()) {
                    Guild.addLoadedGuild(new MapleGuild(((Integer) this.Queue.take()).intValue(), GuildLoad.replies));
                }
                synchronized (this.ToNotify) {
                    this.ToNotify.notify();
                }
            } catch (InterruptedException ex) {
                GuildLoad.log.error("[GuildLoad] 加载家族信息出错." + ex);
            }
        }

        private void Queue(Integer hm) {
            this.Queue.add(hm);
        }

        private void SetToNotify(Object o) {
            if (this.ToNotify == null) {
                this.ToNotify = o;
            }
        }
    }
}