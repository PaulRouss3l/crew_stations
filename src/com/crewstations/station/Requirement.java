package com.crewstations.station;

import com.crewstations.util.CSIds;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

/** What a ship needs for a Station type to be built and active. */
public enum Requirement {
    NONE(null),
    SHIELDS("req_shields"),
    PHASE("req_phase"),
    BAYS("req_bays"),
    ENERGY_SLOT("req_energy"),
    BALLISTIC_SLOT("req_ballistic"),
    MISSILE_SLOT("req_missile");

    /** Makeshift Shield Generator: gives a shieldless hull basic shields. */
    private static final String MAKESHIFT_SHIELDS = "frontshield";
    /** Converted Fighter Bay: turns the hull's bays into cargo holds. */
    private static final String CONVERTED_FIGHTER_BAY = "converted_fighterbay";

    private final String reasonKey;

    Requirement(String reasonKey) {
        this.reasonKey = reasonKey;
    }

    public boolean isMet(ShipVariantAPI v) {
        if (v == null || v.getHullSpec() == null) return false;
        ShieldType shield = v.getHullSpec().getShieldType();
        switch (this) {
            case NONE:
                return true;
            case SHIELDS:
                if (v.hasHullMod(HullMods.SHIELD_SHUNT)) return false;
                return shield == ShieldType.FRONT || shield == ShieldType.OMNI || v.hasHullMod(MAKESHIFT_SHIELDS);
            case PHASE:
                return shield == ShieldType.PHASE;
            case BAYS:
                if (v.hasHullMod(HullMods.CONVERTED_HANGAR) || v.hasHullMod(HullMods.CONVERTED_BAY)) return true;
                return v.getHullSpec().getFighterBays() > 0 && !v.hasHullMod(CONVERTED_FIGHTER_BAY);
            case ENERGY_SLOT:
                return hasSlot(v, WeaponType.ENERGY);
            case BALLISTIC_SLOT:
                return hasSlot(v, WeaponType.BALLISTIC);
            case MISSILE_SLOT:
                return hasSlot(v, WeaponType.MISSILE);
            default:
                return false;
        }
    }

    /** Player-facing reason the requirement is not met ("needs shields"), or null for NONE. */
    public String reason() {
        return reasonKey == null ? null : Global.getSettings().getString(CSIds.STRINGS, reasonKey);
    }

    /** A weapon slot that accepts {@code type}, or a built-in weapon of that type. */
    private static boolean hasSlot(ShipVariantAPI v, WeaponType type) {
        for (WeaponSlotAPI slot : v.getHullSpec().getAllWeaponSlotsCopy()) {
            if (!slot.isWeaponSlot() || slot.isDecorative() || slot.isSystemSlot()) continue;
            if (accepts(slot.getWeaponType(), type)) return true;
            if (slot.isBuiltIn()) {
                WeaponSpecAPI w = v.getWeaponSpec(slot.getId());
                if (w != null && w.getType() == type) return true;
            }
        }
        return false;
    }

    private static boolean accepts(WeaponType slot, WeaponType type) {
        if (slot == type || slot == WeaponType.UNIVERSAL) return true;
        switch (slot) {
            case HYBRID: return type == WeaponType.BALLISTIC || type == WeaponType.ENERGY;
            case COMPOSITE: return type == WeaponType.BALLISTIC || type == WeaponType.MISSILE;
            case SYNERGY: return type == WeaponType.ENERGY || type == WeaponType.MISSILE;
            default: return false;
        }
    }
}
