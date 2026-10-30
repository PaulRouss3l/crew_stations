package com.crewstations.station;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;

/**
 * One stat bonus of a Station type. Values are percents per Level, from settings
 * ({@code crst_effects.<type>.<key>}) with {@link #defaults} as fallback.
 */
public final class StationEffect {

    public interface Applier {
        /** @param pct the positive percent for the Station's Level */
        void apply(MutableShipStatsAPI stats, String id, float pct);
    }

    /** Settings key and strings.json suffix ({@code effect_<key>}). */
    public final String key;
    public final float[] defaults;
    /** True when the bonus lowers the stat (shown as "-x%"). */
    public final boolean reduces;
    /** True when the effect applies to the ship's fighters, not the ship. */
    public final boolean onFighters;
    public final Applier applier;

    private StationEffect(String key, float[] defaults, boolean reduces, boolean onFighters, Applier applier) {
        this.key = key;
        this.defaults = defaults;
        this.reduces = reduces;
        this.onFighters = onFighters;
        this.applier = applier;
    }

    static StationEffect up(String key, float l1, float l2, float l3, Applier a) {
        return new StationEffect(key, new float[] {l1, l2, l3}, false, false, a);
    }

    static StationEffect down(String key, float l1, float l2, float l3, Applier a) {
        return new StationEffect(key, new float[] {l1, l2, l3}, true, false, a);
    }

    static StationEffect fighters(String key, float l1, float l2, float l3, Applier a) {
        return new StationEffect(key, new float[] {l1, l2, l3}, false, true, a);
    }

    /** Multiplier for a reduction of {@code pct} percent, clamped so a stat never goes negative. */
    static float less(float pct) {
        return Math.max(0f, 1f - pct / 100f);
    }
}
