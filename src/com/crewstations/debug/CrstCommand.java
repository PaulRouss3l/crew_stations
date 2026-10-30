package com.crewstations.debug;

import java.util.Map;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import com.crewstations.station.StationType;
import com.crewstations.station.Stations;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

/**
 * Debug command for the Console Commands mod (registered in data/console/commands.csv). Console
 * loads it only when it is enabled, so players without Console never load this class.
 *
 *   crst list                      the flagship's Stations and slots
 *   crst add <station> <level>     build or set a Station (same checks as a Shipyard, minus cost)
 *   crst clear [station]           remove one Station, or all
 */
public class CrstCommand implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MAP) {
            Console.showMessage("Crew Stations commands work on the campaign map only.");
            return CommandResult.WRONG_CONTEXT;
        }
        FleetMemberAPI flagship = Global.getSector().getPlayerFleet().getFlagship();
        if (flagship == null) {
            Console.showMessage("No flagship.");
            return CommandResult.ERROR;
        }
        String[] a = args.trim().isEmpty() ? new String[0] : args.trim().split("\\s+");
        if (a.length == 0) return CommandResult.BAD_SYNTAX;
        try {
            switch (a[0].toLowerCase()) {
                case "list":
                    return list(flagship);
                case "add":
                    if (a.length < 3) return CommandResult.BAD_SYNTAX;
                    return add(flagship, a[1], Integer.parseInt(a[2]));
                case "clear":
                    return clear(flagship, a.length > 1 ? a[1] : null);
                default:
                    return CommandResult.BAD_SYNTAX;
            }
        } catch (NumberFormatException e) {
            return CommandResult.BAD_SYNTAX;
        }
    }

    private CommandResult list(FleetMemberAPI ship) {
        Map<StationType, Integer> stations = Stations.read(ship);
        StringBuilder sb = new StringBuilder();
        sb.append(ship.getShipName()).append(" (").append(ship.getHullSpec().getHullNameWithDashClass())
                .append("): ").append(stations.size()).append("/").append(Stations.slots(ship)).append(" slots, crew +")
                .append(Math.round(Stations.crewPercent(stations))).append("%\n");
        for (Map.Entry<StationType, Integer> e : stations.entrySet()) {
            StationType t = e.getKey();
            boolean active = t.requirement.isMet(ship.getVariant());
            sb.append("  ").append(t.id).append(" L").append(e.getValue())
                    .append(active ? "" : " (inactive: " + t.requirement.reason() + ")").append("\n");
        }
        Console.showMessage(sb.toString());
        return CommandResult.SUCCESS;
    }

    private CommandResult add(FleetMemberAPI ship, String id, int level) {
        StationType type = StationType.byId(id);
        if (type == null) {
            Console.showMessage("Unknown station '" + id + "'. Known: " + knownIds());
            return CommandResult.BAD_SYNTAX;
        }
        String why = Stations.cannotSet(ship, type, level);
        if (why != null) {
            Console.showMessage("Cannot set " + type.id + " L" + level + ": " + why);
            return CommandResult.ERROR;
        }
        Stations.set(ship, type, level);
        return list(ship);
    }

    private CommandResult clear(FleetMemberAPI ship, String id) {
        if (id == null) {
            Stations.clear(ship);
            return list(ship);
        }
        StationType type = StationType.byId(id);
        if (type == null) {
            Console.showMessage("Unknown station '" + id + "'. Known: " + knownIds());
            return CommandResult.BAD_SYNTAX;
        }
        Stations.set(ship, type, 0);
        return list(ship);
    }

    private static String knownIds() {
        StringBuilder sb = new StringBuilder();
        for (StationType t : StationType.values()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(t.id);
        }
        return sb.toString();
    }
}
