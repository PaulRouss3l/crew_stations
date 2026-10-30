package com.crewstations.shipyard;

import com.crewstations.station.StationType;
import com.crewstations.util.CSSettings;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Items;

/**
 * Shipyard rules (docs/DESIGN.md "Shipyards"): a market with a functional, undisrupted industry tagged
 * {@code heavyindustry}, whose owner is not hostile to the player. L3 needs Orbital Works or a nanoforge.
 */
public final class Shipyard {

    private Shipyard() {}

    public static boolean isShipyard(MarketAPI market) {
        if (market == null || market.isHidden()) return false;
        if (!market.isPlayerOwned() && market.getFaction().isHostileTo(Factions.PLAYER)) return false;
        return bestIndustry(market) != null;
    }

    /** Highest Level this Shipyard can build: 0 if not a Shipyard, 2, or 3. */
    public static int maxLevel(MarketAPI market) {
        if (!isShipyard(market)) return 0;
        if (!CSSettings.l3Gate()) return StationType.MAX_LEVEL;
        return hasL3Industry(market) ? StationType.MAX_LEVEL : 2;
    }

    /** Short player-facing name of what sets the cap, e.g. "Orbital Works" or "Heavy Industry". */
    public static String capSource(MarketAPI market) {
        Industry best = bestIndustry(market);
        return best == null ? "" : best.getCurrentName();
    }

    /** A working heavy-industry industry, preferring one that unlocks L3. */
    private static Industry bestIndustry(MarketAPI market) {
        Industry found = null;
        for (Industry ind : market.getIndustries()) {
            if (!isWorkingHeavyIndustry(ind)) continue;
            if (unlocksL3(ind)) return ind;
            if (found == null) found = ind;
        }
        return found;
    }

    private static boolean hasL3Industry(MarketAPI market) {
        for (Industry ind : market.getIndustries()) {
            if (isWorkingHeavyIndustry(ind) && unlocksL3(ind)) return true;
        }
        return false;
    }

    private static boolean isWorkingHeavyIndustry(Industry ind) {
        return ind.getSpec() != null && ind.getSpec().hasTag(Industries.TAG_HEAVYINDUSTRY)
                && ind.isFunctional() && !ind.isDisrupted();
    }

    private static boolean unlocksL3(Industry ind) {
        if (Industries.ORBITALWORKS.equals(ind.getId())) return true;
        SpecialItemData item = ind.getSpecialItem();
        if (item == null) return false;
        return Items.PRISTINE_NANOFORGE.equals(item.getId()) || Items.CORRUPTED_NANOFORGE.equals(item.getId());
    }
}
