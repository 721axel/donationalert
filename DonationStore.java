package com.donationalerts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps every donation and works out the leaderboard. Saved to config/donationalerts_data.json. */
public final class DonationStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Donation> DONATIONS = new ArrayList<>();

    /** Used by the leaderboard to flash the row that just changed. */
    public static volatile String lastChangedName = "";
    public static volatile long lastChangeMs = 0L;

    public record Entry(String name, long amount) {}

    private DonationStore() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("donationalerts_data.json");
    }

    public static synchronized void load() {
        DONATIONS.clear();
        Path f = file();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f)) {
            List<Donation> list = GSON.fromJson(r, new TypeToken<List<Donation>>() {}.getType());
            if (list != null) DONATIONS.addAll(list);
        } catch (Exception e) {
            DonationAlertsClient.LOG.warn("Could not read donation data", e);
        }
    }

    private static void save() {
        List<Donation> real = new ArrayList<>();
        for (Donation d : DONATIONS) if (!d.test) real.add(d);
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(real, w);
        } catch (IOException e) {
            DonationAlertsClient.LOG.warn("Could not save donation data", e);
        }
    }

    public static synchronized void add(Donation d) {
        DONATIONS.add(d);
        lastChangedName = d.name;
        lastChangeMs = System.currentTimeMillis();
        if (!d.test) save();
    }

    /** Wipes everything, including saved data. */
    public static synchronized void clear() {
        DONATIONS.clear();
        save();
    }

    /** Removes only test donations. */
    public static synchronized void clearTests() {
        DONATIONS.removeIf(d -> d.test);
    }

    public static synchronized List<Entry> top(int n) {
        boolean single = "SINGLE".equalsIgnoreCase(Config.get().leaderboardMode);
        List<Entry> entries = new ArrayList<>();
        if (single) {
            for (Donation d : DONATIONS) entries.add(new Entry(d.name, d.amount));
        } else {
            Map<String, Entry> totals = new LinkedHashMap<>();
            for (Donation d : DONATIONS) {
                String key = d.name.toLowerCase();
                Entry old = totals.get(key);
                totals.put(key, new Entry(d.name, (old == null ? 0 : old.amount()) + d.amount));
            }
            entries.addAll(totals.values());
        }
        entries.sort(Comparator.comparingLong(Entry::amount).reversed());
        return entries.size() > n ? new ArrayList<>(entries.subList(0, n)) : entries;
    }
}
