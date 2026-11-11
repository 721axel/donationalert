package com.donationalerts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Settings, stored in config/donationalerts.json. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config instance = new Config();

    public boolean enabled = true;
    /** Only react to payments sent to you. Set true to react to every "X paid Y" message (handy for testing). */
    public boolean trackAll = false;
    /** Leave empty to use your own Minecraft username. */
    public String trackedName = "";
    /** Ignore anything smaller than this. */
    public long minAmount = 1000L;
    /** Groups: 1 = payer, 2 = recipient, 3 = number, 4 = k/m/b/t suffix. Edit this if Donut changes its pay message. */
    public String chatRegex = "(\\w{3,16}) paid (\\w{3,16})\\s+\\D{0,3}?(\\d[\\d,]*(?:\\.\\d+)?)\\s*([kKmMbBtT])?(?!\\w)";

    public boolean playSounds = true;
    public boolean screenShake = true;

    public boolean leaderboardVisible = true;
    public int leaderboardSize = 5;
    /** TOTAL = sum per donor, SINGLE = biggest single donations. */
    public String leaderboardMode = "TOTAL";
    /** PODIUM (top 3 with heads and a crown) or LIST (top N rows). */
    public String leaderboardStyle = "PODIUM";
    /** TOP_LEFT or TOP_RIGHT. */
    public String leaderboardAnchor = "TOP_LEFT";
    public int leaderboardOffsetY = 6;
    public float leaderboardScale = 0.9f;

    public Map<String, Long> thresholds = new LinkedHashMap<>();

    public static Config get() {
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("donationalerts.json");
    }

    public static void load() {
        Path f = file();
        if (Files.exists(f)) {
            try (Reader r = Files.newBufferedReader(f)) {
                Config loaded = GSON.fromJson(r, Config.class);
                if (loaded != null) instance = loaded;
            } catch (Exception e) {
                DonationAlertsClient.LOG.warn("Could not read config, using defaults", e);
            }
        }
        if (instance.thresholds == null) instance.thresholds = new LinkedHashMap<>();
        for (Tier t : Tier.values()) {
            instance.thresholds.putIfAbsent(t.name(), t.defaultThreshold);
        }
        save();
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(instance, w);
        } catch (IOException e) {
            DonationAlertsClient.LOG.warn("Could not save config", e);
        }
    }
}
