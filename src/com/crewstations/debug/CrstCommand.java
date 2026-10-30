package com.crewstations.debug;

import java.util.List;
import java.util.Map;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import com.crewstations.station.StationType;
import com.crewstations.station.Stations;
import com.crewstations.station.Unlock;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

/**
 * Debug command for the Console Commands mod (registered in data/console/commands.csv). Console
 * loads it only when it is enabled, so players without Console never load this class.
 *
 * Commands act on the selected ship: the flagship unless {@code crst select} picked another one
 * (needed for automated ships, which the player cannot pilot).
 *
 *   crst ships                     numbered fleet list, * = selected ship
 *   crst select <n>|flagship       act on fleet ship n, or back on the flagship
 *   crst list                      the selected ship's Stations and slots
 *   crst add <station> <level>     build or set a Station (same checks as a Shipyard, minus cost)
 *   crst clear [station]           remove one Station, or all
 *   crst unlock [on|off|auto]      reward Station unlocks; on/off forces offered/locked until the game closes
 */
public class CrstCommand implements BaseCommand {

    /** FleetMemberAPI id of the selected ship; null = flagship. An id string, never the member itself. */
    private static String selectedId;

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MAP) {
            Console.showMessage("Crew Stations commands work on the campaign map only.");
            return CommandResult.WRONG_CONTEXT;
        }
        String[] a = args.trim().isEmpty() ? new String[0] : args.trim().split("\\s+");
        if (a.length == 0) return CommandResult.BAD_SYNTAX;
        try {
            switch (a[0].toLowerCase()) {
                case "ships":
                    return ships();
                case "select":
                    if (a.length < 2) return CommandResult.BAD_SYNTAX;
                    return select(a[1]);
                case "unlock":
                    return unlock(a.length > 1 ? a[1] : null);
                default:
                    break;
            }
            FleetMemberAPI ship = selected();
            if (ship == null) {
                Console.showMessage("No flagship.");
                return CommandResult.ERROR;
            }
            switch (a[0].toLowerCase()) {
                case "list":
                    return list(ship);
                case "add":
                    if (a.length < 3) return CommandResult.BAD_SYNTAX;
                    return add(ship, a[1], Integer.parseInt(a[2]));
                case "clear":
                    return clear(ship, a.length > 1 ? a[1] : null);
                default:
                    return CommandResult.BAD_SYNTAX;
            }
        } catch (NumberFormatException e) {
            return CommandResult.BAD_SYNTAX;
        }
    }

    /** Shows the reward unlocks; on/off forces them offered/locked for this game session, auto reads the files. */
    private static CommandResult unlock(String mode) {
        if ("on".equalsIgnoreCase(mode)) Unlock.setOverride(true);
        else if ("off".equalsIgnoreCase(mode)) Unlock.setOverride(false);
        else if ("auto".equalsIgnoreCase(mode)) Unlock.setOverride(null);
        else if (mode != null) return CommandResult.BAD_SYNTAX;
        Unlock.refresh();
        Console.showMessage("Slay the Sector won Run found: " + (Unlock.stsWon() ? "yes" : "no")
                + ". Override: " + Unlock.override() + ". Simulator Bay offered: "
                + (StationType.SIMULATOR.unlock.isMet() ? "yes" : "no") + ".");
        return CommandResult.SUCCESS;
    }

    private static List<FleetMemberAPI> fleet() {
        return Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
    }

    /** The selected ship, falling back to the flagship when it left the fleet. */
    private static FleetMemberAPI selected() {
        if (selectedId != null) {
            for (FleetMemberAPI m : fleet()) {
                if (m.getId().equals(selectedId)) return m;
            }
            selectedId = null;
        }
        return Global.getSector().getPlayerFleet().getFlagship();
    }

    private CommandResult ships() {
        FleetMemberAPI current = selected();
        List<FleetMemberAPI> members = fleet();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < members.size(); i++) {
            FleetMemberAPI m = members.get(i);
            sb.append(m == current ? " * " : "   ").append(i + 1).append(". ").append(m.getShipName()).append(" (")
                    .append(m.getHullSpec().getHullNameWithDashClass()).append(")");
            if (m.isFighterWing()) sb.append(" wing");
            else if (Stations.slots(m) == 0) sb.append(" automated");
            else sb.append(" ").append(Stations.read(m).size()).append("/").append(Stations.slots(m));
            if (m.isFlagship()) sb.append(" [flagship]");
            sb.append("\n");
        }
        Console.showMessage(sb.toString());
        return CommandResult.SUCCESS;
    }

    private CommandResult select(String arg) {
        if (arg.equalsIgnoreCase("flagship")) {
            selectedId = null;
            return list(selected());
        }
        List<FleetMemberAPI> members = fleet();
        int n = Integer.parseInt(arg);
        if (n < 1 || n > members.size()) {
            Console.showMessage("No ship " + n + ": pick 1 to " + members.size() + " (see crst ships).");
            return CommandResult.ERROR;
        }
        selectedId = members.get(n - 1).getId();
        return list(selected());
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
