package com.crewstations.station;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.crewstations.util.CSIds;
import com.crewstations.util.CSLog;
import com.crewstations.util.CSSettings;
import com.fs.starfarer.api.Global;

/**
 * What a Station type needs before Station Yard Inc. offers it. Most types are always offered. Achievement
 * unlocks are per save, kept as sector memory flags set by {@link com.crewstations.unlock.UnlockTracker}.
 * The Slay the Sector reward is cross-save, read from that mod's file in {@code saves/common/} so neither mod
 * depends on the other (docs/adr/0002). A Station already built stays, even if its unlock goes away.
 */
public enum Unlock {

    NONE(null),
    /** Won at least one Run of Slay the Sector, any faction (its {@code slay_the_sector_meta.json}). */
    STS_FIRST_WIN("$crst_notified_stsWin"),
    /** Destroyed or disabled an enemy capital ship in a battle the player fought. */
    CAPITAL_KILL("$crst_unlocked_capitalKill"),
    /** Held at least {@code crst_unlockCredits} credits. */
    CREDITS("$crst_unlocked_credits"),
    /** Carried at least {@code crst_unlockFuel} fuel. */
    FUEL("$crst_unlocked_fuel");

    public static final String STS_MOD_ID = "slay_the_sector";
    private static final String STS_META = "slay_the_sector_meta.json";

    /**
     * Sector memory flag set the first time the unlock is met in this save. For the achievements it is the
     * unlock itself; for the Slay the Sector reward it only records that the player was told.
     */
    public final String memoryKey;

    /** Cached result for STS_FIRST_WIN; primitives only, safe as static state. */
    private static boolean stsWon;
    /**
     * Console override ({@code crst unlock on|off|auto}): 1 = offered, -1 = locked even when met, 0 = normal.
     * Lasts until the game is closed, never saved.
     */
    private static int override;

    Unlock(String memoryKey) {
        this.memoryKey = memoryKey;
    }

    public boolean isMet() {
        if (this == NONE) return true;
        if (override != 0) return override > 0;
        if (CSSettings.unlockAll()) return true;
        if (this == STS_FIRST_WIN) return stsWon;
        return flagged();
    }

    /** True once this unlock was recorded in the current save. */
    public boolean flagged() {
        return memoryKey != null && Global.getSector() != null
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(memoryKey);
    }

    /** Records this unlock in the current save. Returns true the first time. */
    public boolean markMet() {
        if (memoryKey == null || flagged()) return false;
        Global.getSector().getMemoryWithoutUpdate().set(memoryKey, true);
        CSLog.info("unlocked " + name());
        return true;
    }

    /**
     * What the player must do, for greyed tiles and the detail panel ("Destroy an enemy capital ship").
     * For the Slay the Sector reward, says the mod is needed when it is not enabled.
     */
    public String howTo() {
        if (this == STS_FIRST_WIN && !Global.getSettings().getModManager().isModEnabled(STS_MOD_ID)) {
            return str("unlock_sts_missing");
        }
        String text = str("unlock_" + name().toLowerCase());
        if (this == CREDITS) return String.format(text, String.format("%,d", CSSettings.unlockCredits()));
        if (this == FUEL) return String.format(text, String.format("%,d", CSSettings.unlockFuel()));
        return text;
    }

    /** Whether Slay the Sector's progress file shows a won Run, as last read. */
    public static boolean stsWon() {
        return stsWon;
    }

    /** "on", "off" or "auto". */
    public static String override() {
        return override > 0 ? "on" : override < 0 ? "off" : "auto";
    }

    /** {@code on} true = offered, false = locked, null = normal rules. */
    public static void setOverride(Boolean on) {
        override = on == null ? 0 : on ? 1 : -1;
    }

    /** Re-reads the other mods' progress files. Cheap, but call it on events (game load, showroom open) only. */
    public static void refresh() {
        stsWon = readStsWon();
    }

    private static boolean readStsWon() {
        try {
            if (!Global.getSettings().fileExistsInCommon(STS_META)) return false;
            JSONObject root = Global.getSettings().readJSONFromCommon(STS_META, false);
            JSONArray winners = root.optJSONArray("winArchetypes");
            if (winners != null && winners.length() > 0) return true;
            JSONObject archetypes = root.optJSONObject("archetypes");
            if (archetypes == null) return false;
            for (Iterator<?> it = archetypes.keys(); it.hasNext();) {
                JSONObject a = archetypes.optJSONObject(String.valueOf(it.next()));
                if (a != null && a.optInt("wins", 0) > 0) return true;
            }
        } catch (Exception e) {
            CSLog.warn("Slay the Sector progress unreadable: " + e);
        }
        return false;
    }

    private static String str(String key) {
        return Global.getSettings().getString(CSIds.STRINGS, key);
    }
}
