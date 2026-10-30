package com.crewstations.hullmods;

import java.awt.Color;
import java.util.Map;

import com.crewstations.station.StationEffect;
import com.crewstations.station.StationType;
import com.crewstations.station.Stations;
import com.crewstations.util.CSIds;
import com.crewstations.util.CSSettings;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Built-in hullmod (perma-mod, 0 OP) present while a ship has Stations. Applies every active Station's
 * effects and the crew cost of all Stations; the tooltip lists them. Data: {@link Stations}.
 */
public class CrewStationsHullMod extends BaseHullMod {

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        ShipVariantAPI v = stats.getVariant();
        if (v == null || Misc.isAutomated(v)) return;
        Map<StationType, Integer> stations = Stations.read(v);
        if (stations.isEmpty()) return;

        float crew = Stations.crewPercent(stations);
        stats.getMinCrewMod().modifyPercent(id, crew);
        stats.getMaxCrewMod().modifyPercent(id, crew);

        for (Map.Entry<StationType, Integer> e : stations.entrySet()) {
            StationType type = e.getKey();
            if (!type.requirement.isMet(v)) continue;
            for (StationEffect fx : type.effects) {
                if (fx.onFighters) continue;
                fx.applier.apply(stats, id + "_" + type.id, value(type, fx, e.getValue()));
            }
        }
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        ShipVariantAPI v = ship.getVariant();
        if (v == null || Misc.isAutomated(v)) return;
        Integer level = Stations.read(v).get(StationType.FLIGHT_DECK);
        if (level == null || !StationType.FLIGHT_DECK.requirement.isMet(v)) return;
        for (StationEffect fx : StationType.FLIGHT_DECK.effects) {
            if (!fx.onFighters) continue;
            fx.applier.apply(fighter.getMutableStats(), id + "_" + StationType.FLIGHT_DECK.id,
                    value(StationType.FLIGHT_DECK, fx, level));
        }
    }

    @Override
    public boolean affectsOPCosts() {
        return false;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width,
            boolean isForModSpec) {
        Color hl = Misc.getHighlightColor();
        Color gray = Misc.getGrayColor();
        if (ship == null || ship.getVariant() == null) {
            tooltip.addPara(str("tooltip_modspec"), 10f);
            return;
        }
        ShipVariantAPI v = ship.getVariant();
        if (Misc.isAutomated(v)) return;
        Map<StationType, Integer> stations = Stations.read(v);
        // Slots follow the parent ship on modules.
        ShipAPI parent = ship.getParentStation() != null ? ship.getParentStation() : ship;
        tooltip.addSectionHeading(String.format(str("tooltip_heading"), stations.size(),
                CSSettings.slots(parent.getHullSize())), Alignment.MID, 10f);

        for (Map.Entry<StationType, Integer> e : stations.entrySet()) {
            StationType type = e.getKey();
            int level = e.getValue();
            if (!type.requirement.isMet(v)) {
                tooltip.addPara(str("tooltip_inactive"), 10f, gray, gray, type.displayName(), "" + level,
                        type.requirement.reason());
                continue;
            }
            tooltip.addPara(str("tooltip_station"), 10f, hl, type.displayName(), "" + level,
                    StationType.levelName(level));
            tooltip.setBulletedListMode("  - ");
            for (StationEffect fx : type.effects) {
                tooltip.addPara(StationType.effectName(fx) + " %s", 3f, hl, type.valueText(fx, level));
            }
            tooltip.setBulletedListMode(null);
        }
        String crew = "+" + Misc.getRoundedValueMaxOneAfterDecimal(Stations.crewPercent(stations)) + "%";
        tooltip.addPara(str("tooltip_crew"), 10f, Misc.getNegativeHighlightColor(), crew);
    }

    private static float value(StationType type, StationEffect fx, int level) {
        return type.value(fx, level);
    }

    private static String str(String key) {
        return Global.getSettings().getString(CSIds.STRINGS, key);
    }
}
