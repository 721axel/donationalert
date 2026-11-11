package com.donationalerts;

/**
 * Donation tiers. Thresholds can be overridden in config/donationalerts.json.
 * introMs = cinematic intro before the banner, mainMs = how long the banner stays.
 */
public enum Tier {
    COMMON("DONATION", 0L, 0xFFFFFF, 3500, 0),
    NICE("NICE DONATION!", 10_000L, 0x55FF55, 4500, 0),
    BIG("BIG DONATION!", 50_000L, 0x55FFFF, 5500, 0),
    HUGE("HUGE DONATION!", 250_000L, 0xFFAA00, 6500, 0),
    LEGENDARY("LEGENDARY DONATION!", 1_000_000L, 0xFFE24A, 7500, 0),
    GODLIKE("GODLIKE DONATION!", 10_000_000L, 0x3DFF6E, 8000, 4200),
    UNIVERSE("UNIVERSE-BREAKING DONATION!", 100_000_000L, 0xFF66FF, 10000, 8000);

    public final String title;
    public final long defaultThreshold;
    public final int color;
    public final int mainMs;
    public final int introMs;

    Tier(String title, long defaultThreshold, int color, int mainMs, int introMs) {
        this.title = title;
        this.defaultThreshold = defaultThreshold;
        this.color = color;
        this.mainMs = mainMs;
        this.introMs = introMs;
    }

    public long threshold() {
        Long override = Config.get().thresholds.get(name());
        return override != null ? override : defaultThreshold;
    }

    public boolean atLeast(Tier other) {
        return ordinal() >= other.ordinal();
    }

    public static Tier forAmount(long amount) {
        Tier best = COMMON;
        for (Tier t : values()) {
            if (amount >= t.threshold()) best = t;
        }
        return best;
    }
}
