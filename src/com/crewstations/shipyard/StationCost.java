package com.crewstations.shipyard;

import java.util.LinkedHashMap;
import java.util.Map;

import com.crewstations.util.CSSettings;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;

/**
 * Price of one Level on one hull size: credits and commodities, each base times the hull multiplier
 * (docs/DESIGN.md "Cost"). Refunds are worked out from the current settings, nothing is stored.
 */
public final class StationCost {

    public final int credits;
    /** Commodity id -> amount, only non-zero entries, in a stable display order. */
    public final Map<String, Integer> commodities;

    private StationCost(int credits, Map<String, Integer> commodities) {
        this.credits = credits;
        this.commodities = commodities;
    }

    /** What building or upgrading to {@code level} costs. */
    public static StationCost of(HullSize size, int level) {
        float mult = CSSettings.hullCostMult(size);
        Map<String, Integer> c = new LinkedHashMap<String, Integer>();
        put(c, Commodities.HEAVY_MACHINERY, CSSettings.heavyMachinery(level) * mult);
        put(c, Commodities.METALS, CSSettings.metals(level) * mult);
        put(c, Commodities.RARE_METALS, CSSettings.rareMetals(level) * mult);
        return new StationCost(Math.round(CSSettings.credits(level) * mult), c);
    }

    /** Credits Dismantle gives back for a Station at {@code level}: a share of every Level's credits. */
    public static int refund(HullSize size, int level) {
        int paid = 0;
        for (int l = 1; l <= level; l++) paid += of(size, l).credits;
        return Math.round(paid * CSSettings.refundFraction());
    }

    private static void put(Map<String, Integer> c, String id, float amount) {
        int n = Math.round(amount);
        if (n > 0) c.put(id, n);
    }
}
