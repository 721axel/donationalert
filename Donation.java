package com.donationalerts;

/** One donation. Plain fields so Gson can read/write it. */
public final class Donation {
    public String name;
    public long amount;
    public long time;
    /** Test donations are shown on the leaderboard but never saved to disk. */
    public transient boolean test;

    public Donation() {}

    public Donation(String name, long amount, boolean test) {
        this.name = name;
        this.amount = amount;
        this.time = System.currentTimeMillis();
        this.test = test;
    }
}
