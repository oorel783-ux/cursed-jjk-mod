package dev.cursedarts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Runs lasting techniques one server tick at a time. */
public final class TickScheduler {
    public interface Task {
        /** Return true to keep running, false when finished (the task tidies up after itself). */
        boolean tick(int age);

        /** Called only if the task is cut short (server stopping or an error). Must undo any world changes. */
        default void cleanup() {}
    }

    private static final class Entry {
        final Task task;
        int age = 0;
        Entry(Task task) { this.task = task; }
    }

    private static final List<Entry> TASKS = new ArrayList<>();
    private static final List<Entry> PENDING = new ArrayList<>();

    private TickScheduler() {}

    public static void add(Task task) {
        PENDING.add(new Entry(task));
    }

    static void tick() {
        TASKS.addAll(PENDING);
        PENDING.clear();
        Iterator<Entry> it = TASKS.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            boolean keep;
            try {
                keep = e.task.tick(e.age++);
            } catch (Exception ex) {
                ex.printStackTrace();
                try { e.task.cleanup(); } catch (Exception ignored) {}
                keep = false;
            }
            if (!keep) it.remove();
        }
    }

    static void cleanupAll() {
        TASKS.addAll(PENDING);
        PENDING.clear();
        for (Entry e : TASKS) {
            try { e.task.cleanup(); } catch (Exception ignored) {}
        }
        TASKS.clear();
    }
}
