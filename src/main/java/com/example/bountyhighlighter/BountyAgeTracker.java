package com.example.bountyhighlighter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Pure Java (no Minecraft classes) so it can be unit-tested.
 *
 * The server does not tell the client when a bounty was created, so "age" here means
 * "time since this client first saw the entry appear". To keep that honest:
 *  - entries already present during the FIRST bounty-GUI session after joining are
 *    "baseline" (age unknown, shown in a neutral color instead of fake-green);
 *  - an entry whose amount goes up is treated as freshly updated (configurable);
 *  - entries not seen for a long time are dropped (claimed / expired).
 */
public final class BountyAgeTracker {

    public enum Band { UNKNOWN, GREEN, YELLOW, ORANGE, RED }

    private record Entry(long firstSeen, long lastSeen, long amount, boolean baseline) {}

    // Defaults taken from the requested colors: green = new, yellow = 5-10 min, red = 20-30 min.
    // Orange (10-20 min) fills the gap between the two ranges that were specified.
    public long greenMaxMs  = 5 * 60_000L;
    public long yellowMaxMs = 10 * 60_000L;
    public long orangeMaxMs = 20 * 60_000L;   // anything older is red
    public long forgetAfterMs = 45 * 60_000L;
    public boolean resetOnAmountIncrease = true;

    private final Map<String, Entry> entries = new HashMap<>();
    private boolean inScreen = false;
    private boolean baselineDone = false;

    public void onScreenOpened() { inScreen = true; }

    public void onScreenClosed() {
        if (inScreen && !baselineDone) baselineDone = true;
        inScreen = false;
    }

    /** Call for every head you can see. amount < 0 means "couldn't parse". */
    public void observe(String name, long amount, long nowMs) {
        String key = name.toLowerCase(Locale.ROOT);
        Entry old = entries.get(key);
        boolean baseline = inScreen && !baselineDone;
        if (old == null) {
            entries.put(key, new Entry(nowMs, nowMs, amount, baseline));
            return;
        }
        boolean increased = resetOnAmountIncrease && amount >= 0 && old.amount >= 0 && amount > old.amount;
        if (increased) {
            entries.put(key, new Entry(nowMs, nowMs, amount, false));
        } else {
            entries.put(key, new Entry(old.firstSeen, nowMs, amount >= 0 ? amount : old.amount, old.baseline));
        }
    }

    public void prune(long nowMs) {
        entries.values().removeIf(e -> nowMs - e.lastSeen > forgetAfterMs);
    }

    public void clear() { entries.clear(); inScreen = false; baselineDone = false; }

    /** Age in ms, or -1 if unknown (never seen, or baseline entry). */
    public long ageMs(String name, long nowMs) {
        Entry e = entries.get(name.toLowerCase(Locale.ROOT));
        if (e == null || e.baseline) return -1;
        return Math.max(0, nowMs - e.firstSeen);
    }

    public Band band(String name, long nowMs) {
        long age = ageMs(name, nowMs);
        if (age < 0) return Band.UNKNOWN;
        if (age < greenMaxMs) return Band.GREEN;
        if (age < yellowMaxMs) return Band.YELLOW;
        if (age < orangeMaxMs) return Band.ORANGE;
        return Band.RED;
    }

    /** Semi-transparent ARGB overlay color for a slot highlight. */
    public static int argb(Band b) {
        return switch (b) {
            case GREEN   -> 0x9055FF55;
            case YELLOW  -> 0x90FFFF55;
            case ORANGE  -> 0x90FFAA00;
            case RED     -> 0x90FF5555;
            case UNKNOWN -> 0x60AAAAAA;
        };
    }

    public static String formatAge(long ms) {
        if (ms < 0) return "unknown";
        long s = ms / 1000;
        return s >= 60 ? (s / 60) + "m " + (s % 60) + "s" : s + "s";
    }
}
