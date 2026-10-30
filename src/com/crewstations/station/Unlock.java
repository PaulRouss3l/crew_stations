package com.crewstations.station;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.crewstations.util.CSLog;
import com.crewstations.util.CSSettings;
import com.fs.starfarer.api.Global;

/**
 * What a Station type needs before Station Yard Inc. offers it. Most types are always offered; reward types
 * come from another mod's cross-save progress, read from {@code saves/common/} so neither mod depends on the
 * other (docs/adr/0002). Read at game load and when the showroom opens, then cached: never per frame.
 * A Station already built stays, even if the unlock disappears later.
 */
public enum Unlock {

    NONE,
    /** Won at least one Run of Slay the Sector, any faction (its {@code slay_the_sector_meta.json}). */
    STS_FIRST_WIN;

    private static final String STS_META = "slay_the_sector_meta.json";

    /** Cached result for STS_FIRST_WIN; primitives only, safe as static state. */
    private static boolean stsWon;

    public boolean isMet() {
        if (this == NONE || CSSettings.unlockAll()) return true;
        return stsWon;
    }

    /** Re-reads the other mods' progress files. Cheap, but call it on events (game load, showroom open) only. */
    public static void refresh() {
        boolean before = stsWon;
        stsWon = readStsWon();
        if (stsWon && !before) CSLog.info("Slay the Sector win found: Simulator Bay offered");
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
}
