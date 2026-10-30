package com.crewstations.util;

/** Ids shared across the mod. Saved strings (tag prefix) must never change after release. */
public final class CSIds {

    private CSIds() {}

    public static final String HULLMOD = "crst_crew_stations";
    /** Variant tag holding a ship's Stations as JSON, see docs/adr/0001. */
    public static final String TAG_PREFIX = "crst_stations:";
    /** strings.json category. */
    public static final String STRINGS = "crst_stations";
}
