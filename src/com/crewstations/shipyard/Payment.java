package com.crewstations.shipyard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin.TransferAction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.util.Misc;

/**
 * How a {@link StationCost} would be paid at one market: each commodity from fleet cargo first, then the
 * player's storage there (only if they have access), then bought from the open market's stock (at its price
 * plus tariff, legal goods only). Built fresh
 * for each screen refresh; {@link #pay()} re-checks nothing, so build the plan right before paying.
 */
public final class Payment {

    /** One commodity's split across the three sources. */
    public static final class Line {
        public final String commodityId;
        public final int needed;
        public final int fromCargo;
        public final int fromStorage;
        public final int bought;
        public final int missing;
        /** Credits for {@link #bought}, tariff included. */
        public final int buyCredits;

        Line(String commodityId, int needed, int fromCargo, int fromStorage, int bought, int missing, int buyCredits) {
            this.commodityId = commodityId;
            this.needed = needed;
            this.fromCargo = fromCargo;
            this.fromStorage = fromStorage;
            this.bought = bought;
            this.missing = missing;
            this.buyCredits = buyCredits;
        }

        public String name() {
            return Global.getSettings().getCommoditySpec(commodityId).getName();
        }
    }

    private final MarketAPI market;
    public final StationCost cost;
    public final List<Line> lines;
    /** Station credits plus everything bought. */
    public final int totalCredits;

    private Payment(MarketAPI market, StationCost cost, List<Line> lines, int totalCredits) {
        this.market = market;
        this.cost = cost;
        this.lines = lines;
        this.totalCredits = totalCredits;
    }

    public static Payment plan(MarketAPI market, StationCost cost) {
        CargoAPI fleet = playerCargo();
        CargoAPI storage = storage(market);
        SubmarketAPI open = market.getSubmarket(Submarkets.SUBMARKET_OPEN);
        List<Line> lines = new ArrayList<Line>();
        int total = cost.credits;
        for (Map.Entry<String, Integer> e : cost.commodities.entrySet()) {
            String id = e.getKey();
            int need = e.getValue();
            int fromCargo = Math.min(need, (int) fleet.getCommodityQuantity(id));
            int rest = need - fromCargo;
            int fromStorage = storage == null ? 0 : Math.min(rest, (int) storage.getCommodityQuantity(id));
            rest -= fromStorage;
            boolean canBuy = open != null && !open.getPlugin().isIllegalOnSubmarket(id, TransferAction.PLAYER_BUY);
            int bought = canBuy ? Math.min(rest, (int) open.getCargo().getCommodityQuantity(id)) : 0;
            rest -= bought;
            int buyCredits = 0;
            if (bought > 0) {
                float tariff = open.getPlugin().getTariff();
                buyCredits = Math.round(market.getSupplyPrice(id, bought, true) * (1f + tariff));
            }
            total += buyCredits;
            lines.add(new Line(id, need, fromCargo, fromStorage, bought, rest, buyCredits));
        }
        return new Payment(market, cost, Collections.unmodifiableList(lines), total);
    }

    public boolean hasAllCommodities() {
        return firstMissing() == null;
    }

    public boolean canAfford() {
        return hasAllCommodities() && playerCredits() >= totalCredits;
    }

    /** Takes the credits and commodities. Callers check {@link #canAfford()} on a fresh plan first. */
    public void pay() {
        CargoAPI fleet = playerCargo();
        CargoAPI storage = storage(market);
        SubmarketAPI open = market.getSubmarket(Submarkets.SUBMARKET_OPEN);
        for (Line l : lines) {
            if (l.fromCargo > 0) fleet.removeCommodity(l.commodityId, l.fromCargo);
            if (l.fromStorage > 0 && storage != null) storage.removeCommodity(l.commodityId, l.fromStorage);
            if (l.bought > 0 && open != null) open.getCargo().removeCommodity(l.commodityId, l.bought);
        }
        fleet.getCredits().subtract(totalCredits);
    }

    /** The first commodity that is short, or null when everything is covered. */
    public Line firstMissing() {
        for (Line l : lines) {
            if (l.missing > 0) return l;
        }
        return null;
    }

    /** Storage cargo at this market, or null when there is none or the player has no access to it. */
    private static CargoAPI storage(MarketAPI market) {
        return Misc.playerHasStorageAccess(market) ? Misc.getStorageCargo(market) : null;
    }

    public static float playerCredits() {
        return playerCargo().getCredits().get();
    }

    private static CargoAPI playerCargo() {
        return Global.getSector().getPlayerFleet().getCargo();
    }
}
