package dev.cursedarts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Runs lasting techniques (Blue, Domain Expansion) one server tick at a time. */
public final class TickScheduler {
    /** Return true to keep running, false when finished. */
    public interface Task {
        boolean tick(int age);
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
                keep = false;
            }
            if (!keep) it.remove();
        }
    }
}
