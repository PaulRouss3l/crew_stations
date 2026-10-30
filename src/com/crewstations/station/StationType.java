package com.crewstations.station;

import static com.crewstations.station.StationEffect.down;
import static com.crewstations.station.StationEffect.fighters;
import static com.crewstations.station.StationEffect.less;
import static com.crewstations.station.StationEffect.up;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.crewstations.util.CSIds;
import com.crewstations.util.CSSettings;
import com.fs.starfarer.api.Global;

/**
 * The ten Station types. {@link #id} is saved in variant tags and settings keys: never rename it.
 * Effects per Level are listed in docs/DESIGN.md.
 */
public enum StationType {

    SHIELD("shield", Requirement.SHIELDS,
            down("shieldDamageTaken", 5, 10, 15, (s, id, p) -> s.getShieldDamageTakenMult().modifyMult(id, less(p))),
            up("shieldRate", 15, 30, 50, (s, id, p) -> {
                s.getShieldTurnRateMult().modifyPercent(id, p);
                s.getShieldUnfoldRateMult().modifyPercent(id, p);
            }),
            down("shieldUpkeep", 10, 20, 30, (s, id, p) -> s.getShieldUpkeepMult().modifyMult(id, less(p)))),

    LOGISTICS("logistics", Requirement.NONE,
            down("supplyUse", 5, 10, 15, (s, id, p) -> s.getSuppliesPerMonth().modifyMult(id, less(p))),
            down("fuelUse", 10, 20, 30, (s, id, p) -> s.getFuelUseMod().modifyMult(id, less(p)))),

    ENGINE("engine", Requirement.NONE, Unlock.FUEL,
            up("topSpeed", 5, 10, 15, (s, id, p) -> s.getMaxSpeed().modifyPercent(id, p)),
            up("handling", 15, 30, 50, (s, id, p) -> {
                s.getAcceleration().modifyPercent(id, p);
                s.getDeceleration().modifyPercent(id, p);
                s.getMaxTurnRate().modifyPercent(id, p);
                s.getTurnAcceleration().modifyPercent(id, p);
            })),

    ENERGY("energy", Requirement.ENERGY_SLOT,
            up("energyDamage", 3, 6, 10, (s, id, p) -> s.getEnergyWeaponDamageMult().modifyPercent(id, p)),
            down("energyFlux", 5, 10, 15, (s, id, p) -> s.getEnergyWeaponFluxCostMod().modifyMult(id, less(p)))),

    BALLISTIC("ballistic", Requirement.BALLISTIC_SLOT,
            up("ballisticRof", 5, 10, 15, (s, id, p) -> s.getBallisticRoFMult().modifyPercent(id, p)),
            down("ballisticFlux", 5, 10, 15, (s, id, p) -> s.getBallisticWeaponFluxCostMod().modifyMult(id, less(p)))),

    MISSILE("missile", Requirement.MISSILE_SLOT, Unlock.CAPITAL_KILL,
            up("missileAmmo", 25, 50, 100, (s, id, p) -> s.getMissileAmmoBonus().modifyPercent(id, p)),
            up("missileHealth", 15, 30, 50, (s, id, p) -> s.getMissileHealthBonus().modifyPercent(id, p)),
            up("missileReload", 15, 30, 50, (s, id, p) -> s.getMissileAmmoRegenMult().modifyPercent(id, p))),

    PHASE("phase", Requirement.PHASE, Unlock.CREDITS,
            down("phaseUpkeep", 10, 20, 30, (s, id, p) -> s.getPhaseCloakUpkeepCostBonus().modifyMult(id, less(p))),
            down("phaseCooldown", 10, 20, 30, (s, id, p) -> s.getPhaseCloakCooldownBonus().modifyMult(id, less(p)))),

    REACTOR("reactor", Requirement.NONE,
            up("fluxCapacity", 5, 10, 15, (s, id, p) -> s.getFluxCapacity().modifyPercent(id, p)),
            up("fluxDissipation", 5, 10, 15, (s, id, p) -> s.getFluxDissipation().modifyPercent(id, p))),

    DAMAGE_CONTROL("damage_control", Requirement.NONE,
            down("hullDamageTaken", 5, 10, 15, (s, id, p) -> s.getHullDamageTakenMult().modifyMult(id, less(p))),
            // Repair rate +p% = repair time / (1 + p%).
            up("repairRate", 25, 50, 75, (s, id, p) -> {
                s.getCombatWeaponRepairTimeMult().modifyMult(id, 1f / (1f + p / 100f));
                s.getCombatEngineRepairTimeMult().modifyMult(id, 1f / (1f + p / 100f));
            }),
            down("crewLoss", 25, 50, 75, (s, id, p) -> s.getCrewLossMult().modifyMult(id, less(p)))),

    FLIGHT_DECK("flight_deck", Requirement.BAYS,
            down("fighterReplacement", 10, 20, 30, (s, id, p) -> s.getFighterRefitTimeMult().modifyMult(id, less(p))),
            fighters("fighterSpeed", 5, 10, 15, (s, id, p) -> s.getMaxSpeed().modifyPercent(id, p))),

    /** Slay the Sector reward: crews trained in the Descent Relay's combat simulation. */
    SIMULATOR("simulator", Requirement.NONE, Unlock.STS_FIRST_WIN,
            up("maxCR", 5, 10, 15, (s, id, p) -> s.getMaxCombatReadiness().modifyFlat(id, p / 100f)),
            up("crRecovery", 15, 30, 50, (s, id, p) -> s.getBaseCRRecoveryRatePercentPerDay().modifyPercent(id, p)));

    public static final int MAX_LEVEL = 3;

    public final String id;
    public final Requirement requirement;
    /** What Station Yard Inc. needs before offering this type (most: nothing). */
    public final Unlock unlock;
    public final List<StationEffect> effects;

    StationType(String id, Requirement requirement, StationEffect... effects) {
        this(id, requirement, Unlock.NONE, effects);
    }

    StationType(String id, Requirement requirement, Unlock unlock, StationEffect... effects) {
        this.id = id;
        this.requirement = requirement;
        this.unlock = unlock;
        this.effects = Collections.unmodifiableList(Arrays.asList(effects));
    }

    public String displayName() {
        return Global.getSettings().getString(CSIds.STRINGS, "name_" + id);
    }

    /** Station art: settings.json graphics.crst_stations.<id> (vanilla hullmod icons until S3). */
    public String sprite() {
        return Global.getSettings().getSpriteName("crst_stations", id);
    }

    /** Percent of {@code fx} at {@code level}, from settings or the code default. */
    public float value(StationEffect fx, int level) {
        return CSSettings.effect(id, fx.key, level, fx.defaults);
    }

    /** "+15%" or "-10%". */
    public String valueText(StationEffect fx, int level) {
        float v = value(fx, level);
        String n = v == Math.round(v) ? String.valueOf(Math.round(v)) : String.valueOf(v);
        return (fx.reduces ? "-" : "+") + n + "%";
    }

    public static String effectName(StationEffect fx) {
        return Global.getSettings().getString(CSIds.STRINGS, "effect_" + fx.key);
    }

    public static String levelName(int level) {
        return Global.getSettings().getString(CSIds.STRINGS, "level_" + level);
    }

    /** The type with this saved id, or null. */
    public static StationType byId(String id) {
        for (StationType t : values()) {
            if (t.id.equalsIgnoreCase(id)) return t;
        }
        return null;
    }
}
