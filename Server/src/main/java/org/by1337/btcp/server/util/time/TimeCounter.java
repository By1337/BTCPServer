package org.by1337.btcp.server.util.time;

import java.util.concurrent.TimeUnit;

public class TimeCounter {
    private long time;
    private final long timeStart;

    public TimeCounter() {
        this.time = System.nanoTime();
        this.timeStart = System.nanoTime();
    }

    public long getTimeMillis() {
        return TimeUnit.NANOSECONDS.toMillis(getTimeNanos());
    }
    public long getTotalTimeMillis() {
        return TimeUnit.NANOSECONDS.toMillis(getTotalTimeNanos());
    }
    public long getTimeNanos() {
        return System.nanoTime() - time;
    }

    public long getTotalTimeNanos() {
        return System.nanoTime() - timeStart;
    }

    public void reset() {
        this.time = System.nanoTime();
    }

    public String getTimeFormat(){
        long x = getTimeMillis();
        int sec = (int) (x / 1000);
        int mils = (int) (x % 1000);
        return sec + "." + mils;
    }
}
