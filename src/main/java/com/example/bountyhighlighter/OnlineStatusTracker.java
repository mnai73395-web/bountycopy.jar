package com.example.bountyhighlighter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Pure Java. Decides ONLINE / OFFLINE / UNKNOWN for a bounty target from the client's
 * tab-list names. If the server hides or fakes its tab list, we report UNKNOWN
 * instead of marking everybody offline.
 */
public final class OnlineStatusTracker {

    public enum State { ONLINE, OFFLINE, UNKNOWN }

    private final Set<String> online = new HashSet<>();
    private boolean listUsable = false;

    /** Call every tick (or every ~1s) with the names from the tab list, including your own. */
    public void update(Collection<String> tabListNames) {
        online.clear();
        for (String n : tabListNames) online.add(n.toLowerCase(Locale.ROOT));
        // A list with only yourself (or nothing) is more likely hidden than truly empty.
        listUsable = online.size() >= 2;
    }

    public State stateOf(String name) {
        if (!listUsable) return State.UNKNOWN;
        return online.contains(name.toLowerCase(Locale.ROOT)) ? State.ONLINE : State.OFFLINE;
    }

    /** Text fallback for tooltips/chat. */
    public static String symbol(State s) {
        return switch (s) { case ONLINE -> "●"; case OFFLINE -> "○"; case UNKNOWN -> "?"; };
    }

    /** Dot color (ARGB), drawn as a small square so no font glyph is needed. */
    public static int argb(State s) {
        return switch (s) {
            case ONLINE  -> 0xFF55FF55;
            case OFFLINE -> 0xFF555555;
            case UNKNOWN -> 0xFFAAAAAA;
        };
    }
}
