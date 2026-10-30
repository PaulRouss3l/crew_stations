package com.crewstations.station;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.crewstations.util.CSIds;
import com.crewstations.util.CSLog;
import com.crewstations.util.CSSettings;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;

/**
 * A ship's Stations, stored as one JSON variant tag ({@link CSIds#TAG_PREFIX}) on the ship's own variant and
 * on each module variant (docs/adr/0001). The JSON maps Station type id to Level, e.g. {"shield":2}.
 */
public final class Stations {

    private Stations() {}

    /** Malformed tags already logged: read() runs on every stat recalculation, so warn once each. */
    private static final Set<String> warned = new HashSet<String>();

    /** Stations on this variant, in enum order. Empty when none or unreadable. */
    public static Map<StationType, Integer> read(ShipVariantAPI v) {
        Map<StationType, Integer> out = new EnumMap<StationType, Integer>(StationType.class);
        String tag = findTag(v);
        if (tag == null) return out;
        try {
            JSONObject json = new JSONObject(tag.substring(CSIds.TAG_PREFIX.length()));
            JSONArray names = json.names();
            for (int i = 0; names != null && i < names.length(); i++) {
                StationType type = StationType.byId(names.getString(i));
                int level = json.optInt(names.getString(i), 0);
                if (type == null || level <= 0) continue;
                out.put(type, Math.min(level, StationType.MAX_LEVEL));
            }
        } catch (Exception e) {
            if (warned.add(tag)) CSLog.warn("unreadable Station tag on " + v.getHullVariantId() + ": " + tag);
        }
        return out;
    }

    public static Map<StationType, Integer> read(FleetMemberAPI member) {
        return read(member.getVariant());
    }

    /** Station slots for this ship's hull size; 0 for automated ships and fighters. */
    public static int slots(FleetMemberAPI member) {
        if (Misc.isAutomated(member) || member.isFighterWing()) return 0;
        return CSSettings.slots(member.getHullSpec().getHullSize());
    }

    /** Skeleton/max crew percent added by these Stations (inactive ones count). */
    public static float crewPercent(Map<StationType, Integer> stations) {
        int levels = 0;
        for (int l : stations.values()) levels += l;
        return levels * CSSettings.crewPercentPerLevel();
    }

    /**
     * Why {@code type} cannot go to {@code level} on this ship, or null if it can. Covers automated ships,
     * slots, requirements and Level order (build at L1, then one Level at a time; no downgrade). Shipyard rules
     * come later.
     */
    public static String cannotSet(FleetMemberAPI member, StationType type, int level) {
        if (Misc.isAutomated(member)) return "automated ships have no crew for Stations";
        if (member.isFighterWing()) return "not a ship";
        if (level < 1 || level > StationType.MAX_LEVEL) return "level must be 1 to " + StationType.MAX_LEVEL;
        Map<StationType, Integer> current = read(member);
        if (!current.containsKey(type) && current.size() >= slots(member)) return "no free Station slot";
        if (!type.requirement.isMet(member.getVariant())) return type.requirement.reason();
        int now = current.containsKey(type) ? current.get(type) : 0;
        if (level != now + 1) return now == 0 ? "build at level 1 first" : "upgrade one level at a time (now level " + now + ")";
        return null;
    }

    /** Sets (level >= 1) or removes (level <= 0) one Station. Callers check {@link #cannotSet} first. */
    public static void set(FleetMemberAPI member, StationType type, int level) {
        Map<StationType, Integer> stations = read(member);
        if (level <= 0) stations.remove(type);
        else stations.put(type, Math.min(level, StationType.MAX_LEVEL));
        write(member, stations);
    }

    public static void clear(FleetMemberAPI member) {
        write(member, new EnumMap<StationType, Integer>(StationType.class));
    }

    private static void write(FleetMemberAPI member, Map<StationType, Integer> stations) {
        if (stations.equals(read(member))) return;
        ShipVariantAPI v = ownVariant(member.getVariant());
        String tag = null;
        if (!stations.isEmpty()) {
            JSONObject json = new JSONObject();
            try {
                for (Map.Entry<StationType, Integer> e : stations.entrySet()) json.put(e.getKey().id, e.getValue());
            } catch (Exception e) {
                CSLog.error("could not encode Stations", e);
                return;
            }
            tag = CSIds.TAG_PREFIX + json;
        }
        apply(v, tag);
        for (String slot : v.getModuleSlots()) {
            ShipVariantAPI module = v.getModuleVariant(slot);
            if (module == null) continue;
            module = ownVariant(module);
            apply(module, tag);
            v.setModuleVariant(slot, module);
        }
        member.setVariant(v, false, true);
        member.updateStats();
    }

    /** Puts {@code tag} (null = none) on the variant and adds or removes the hullmod to match. */
    private static void apply(ShipVariantAPI v, String tag) {
        for (String t : new ArrayList<String>(v.getTags())) {
            if (t.startsWith(CSIds.TAG_PREFIX)) v.removeTag(t);
        }
        if (tag != null) {
            v.addTag(tag);
            // Perma-mod even if a removable copy is already there (console addhullmod, autofit goal variants).
            if (!v.getPermaMods().contains(CSIds.HULLMOD)) v.addPermaMod(CSIds.HULLMOD);
        } else {
            v.removePermaMod(CSIds.HULLMOD);
            v.removeMod(CSIds.HULLMOD);
        }
    }

    /** A variant owned by this one ship: stock and non-refit variants are cloned so other ships stay untouched. */
    private static ShipVariantAPI ownVariant(ShipVariantAPI v) {
        if (!v.isStockVariant() && v.getSource() == VariantSource.REFIT) return v;
        ShipVariantAPI copy = v.clone();
        copy.setSource(VariantSource.REFIT);
        copy.setOriginalVariant(null);
        return copy;
    }

    private static String findTag(ShipVariantAPI v) {
        if (v == null) return null;
        List<String> found = new ArrayList<String>();
        for (String t : v.getTags()) {
            if (t.startsWith(CSIds.TAG_PREFIX)) found.add(t);
        }
        if (found.size() > 1 && warned.add(v.getHullVariantId())) {
            CSLog.warn("several Station tags on " + v.getHullVariantId() + ", using the first");
        }
        return found.isEmpty() ? null : found.get(0);
    }
}
