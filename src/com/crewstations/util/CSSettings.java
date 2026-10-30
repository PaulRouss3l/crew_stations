package com.crewstations.util;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

/**
 * Tunables from data/config/settings.json ("crst_*" keys), read once at application load.
 * Code defaults cover missing or malformed keys.
 */
public final class CSSettings {

    private CSSettings() {}

    private static int[] slots = {1, 2, 3, 4};
    private static float crewPercentPerLevel = 10f;
    private static int[] creditsPerLevel = {150000, 400000, 900000};
    private static int[] heavyMachineryPerLevel = {100, 200, 300};
    private static int[] metalsPerLevel = {0, 500, 0};
    private static int[] rareMetalsPerLevel = {0, 0, 200};
    private static float[] hullCostMult = {0.5f, 1f, 2f, 4f};
    private static float refundFraction = 0.25f;
    private static boolean l3Gate = true;
    /** "type.effect" -> percent at L1, L2, L3. Only primitives and strings: safe as static state. */
    private static final Map<String, float[]> effects = new HashMap<String, float[]>();

    public static void load() {
        slots = ints("crst_slots", slots);
        crewPercentPerLevel = f("crst_crewPercentPerLevel", crewPercentPerLevel);
        creditsPerLevel = ints("crst_creditsPerLevel", creditsPerLevel);
        heavyMachineryPerLevel = ints("crst_heavyMachineryPerLevel", heavyMachineryPerLevel);
        metalsPerLevel = ints("crst_metalsPerLevel", metalsPerLevel);
        rareMetalsPerLevel = ints("crst_rareMetalsPerLevel", rareMetalsPerLevel);
        hullCostMult = floats("crst_hullCostMult", hullCostMult);
        refundFraction = f("crst_refundFraction", refundFraction);
        l3Gate = b("crst_l3Gate", l3Gate);
        effects.clear();
        try {
            JSONObject all = Global.getSettings().getJSONObject("crst_effects");
            JSONArray types = all.names();
            for (int t = 0; types != null && t < types.length(); t++) {
                String type = types.getString(t);
                JSONObject byEffect = all.getJSONObject(type);
                JSONArray keys = byEffect.names();
                for (int k = 0; keys != null && k < keys.length(); k++) {
                    String key = keys.getString(k);
                    JSONArray arr = byEffect.getJSONArray(key);
                    float[] v = new float[arr.length()];
                    for (int i = 0; i < v.length; i++) v[i] = (float) arr.getDouble(i);
                    effects.put(type + "." + key, v);
                }
            }
        } catch (Exception e) {
            CSLog.warn("settings: crst_effects missing or malformed, using code defaults");
        }
    }

    public static int slots(HullSize size) {
        int i = sizeIndex(size);
        return i < 0 || i >= slots.length ? 0 : slots[i];
    }

    public static float crewPercentPerLevel() { return crewPercentPerLevel; }

    /** Base credits of {@code level} (1-based), before the hull multiplier. */
    public static int credits(int level) { return at(creditsPerLevel, level); }
    public static int heavyMachinery(int level) { return at(heavyMachineryPerLevel, level); }
    public static int metals(int level) { return at(metalsPerLevel, level); }
    public static int rareMetals(int level) { return at(rareMetalsPerLevel, level); }
    public static float refundFraction() { return refundFraction; }
    /** True when L3 needs Orbital Works or a nanoforge. */
    public static boolean l3Gate() { return l3Gate; }

    /** Cost multiplier for this hull size (credits and commodities); 0 for fighters. */
    public static float hullCostMult(HullSize size) {
        int i = sizeIndex(size);
        return i < 0 || i >= hullCostMult.length ? 0f : hullCostMult[i];
    }

    private static int at(int[] perLevel, int level) {
        if (perLevel.length == 0) return 0;
        return perLevel[Math.max(0, Math.min(level, perLevel.length) - 1)];
    }

    /** Effect value in percent for {@code level} (1-based), or the code default. */
    public static float effect(String type, String key, int level, float[] defaults) {
        float[] v = effects.get(type + "." + key);
        if (v == null || v.length == 0) v = defaults;
        return v[Math.max(0, Math.min(level, v.length) - 1)];
    }

    /** 0 frigate .. 3 capital; -1 for fighters and unknown sizes. */
    public static int sizeIndex(HullSize size) {
        if (size == null) return -1;
        switch (size) {
            case FRIGATE: return 0;
            case DESTROYER: return 1;
            case CRUISER: return 2;
            case CAPITAL_SHIP: return 3;
            default: return -1;
        }
    }

    private static float f(String key, float def) {
        try {
            return Global.getSettings().getFloat(key);
        } catch (Exception e) {
            return def;
        }
    }

    private static boolean b(String key, boolean def) {
        try {
            return Global.getSettings().getBoolean(key);
        } catch (Exception e) {
            return def;
        }
    }

    private static float[] floats(String key, float[] def) {
        try {
            JSONArray arr = Global.getSettings().getJSONArray(key);
            float[] out = new float[arr.length()];
            for (int i = 0; i < out.length; i++) out[i] = (float) arr.getDouble(i);
            return out;
        } catch (Exception e) {
            CSLog.warn("settings: " + key + " missing or malformed, using defaults");
            return def;
        }
    }

    private static int[] ints(String key, int[] def) {
        try {
            JSONArray arr = Global.getSettings().getJSONArray(key);
            int[] out = new int[arr.length()];
            for (int i = 0; i < out.length; i++) out[i] = arr.getInt(i);
            return out;
        } catch (Exception e) {
            CSLog.warn("settings: " + key + " missing or malformed, using defaults");
            return def;
        }
    }
}
