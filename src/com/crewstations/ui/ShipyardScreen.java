package com.crewstations.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import com.crewstations.shipyard.Payment;
import com.crewstations.shipyard.Shipyard;
import com.crewstations.shipyard.StationCost;
import com.crewstations.station.StationEffect;
import com.crewstations.station.StationType;
import com.crewstations.station.Stations;
import com.crewstations.util.CSIds;
import com.crewstations.util.CSLog;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * The Shipyard screen (docs/DESIGN.md "Screen"): fleet on the left, the selected ship's Station slots on the
 * right, a Build picker, and a confirmation bar that shows the full cost or refund before anything happens.
 * Opened by {@code CrstShipyard open} with {@code dialog.showCustomVisualDialog(WIDTH, HEIGHT, screen)}.
 * Holds the market and fleet members only while open; nothing here is saved.
 */
public class ShipyardScreen implements CustomVisualDialogDelegate {

    public static final float WIDTH = 1120f;
    public static final float HEIGHT = 700f;
    private static final float HEADER_H = 40f;
    private static final float LIST_W = 270f;
    private static final float CONFIRM_H = 180f;
    private static final float FOOTER_H = 30f;
    private static final float PAD = 10f;

    private enum Action { BUILD, UPGRADE, DISMANTLE }

    private final MarketAPI market;
    private final int maxLevel;

    private CustomPanelAPI root;
    private CustomPanelAPI content;
    private DialogCallbacks callbacks;

    private FleetMemberAPI ship;
    /** Showing the Build picker instead of the slot cards. */
    private boolean picking;
    private Action pending;
    private StationType pendingType;
    /** Rebuild on the next frame, not inside the click that asked for it (the clicked panel is removed). */
    private boolean dirty;
    private boolean closed;
    private TooltipMakerAPI fleetList;
    /** Fleet list scroll position, kept across rebuilds. */
    private float fleetScroll;

    public ShipyardScreen(MarketAPI market) {
        this.market = market;
        this.maxLevel = Shipyard.maxLevel(market);
    }

    private final CustomUIPanelPlugin plugin = new ButtonsOnly() {
        @Override
        public void processInput(List<InputEventAPI> events) {
            for (InputEventAPI e : events) {
                if (!e.isConsumed() && e.isKeyDownEvent() && e.getEventValue() == Keyboard.KEY_ESCAPE) {
                    e.consume();
                    if (pending != null) pending = null;
                    else if (picking) picking = false;
                    else close();
                    dirty = true;
                }
            }
        }
    };

    /** Plugin of the rebuilt panels: forwards button clicks only, no per-frame work. */
    private class ButtonsOnly implements CustomUIPanelPlugin {
        @Override public void positionChanged(PositionAPI position) {}
        @Override public void renderBelow(float alphaMult) {}
        @Override public void render(float alphaMult) {}
        @Override public void advance(float amount) {}
        @Override public void processInput(List<InputEventAPI> events) {}

        @Override
        public void buttonPressed(Object id) {
            onButton(String.valueOf(id));
        }
    }

    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        this.root = panel;
        this.callbacks = callbacks;
        ship = Global.getSector().getPlayerFleet().getFlagship();

        TooltipMakerAPI header = panel.createUIElement(WIDTH - 2 * PAD, HEADER_H, false);
        header.addTitle(fmt("ui_title", market.getName()));
        ButtonAPI close = header.addButton(str("ui_close"), "close", 100f, 24f, 0f);
        close.getPosition().inTR(0f, 0f);
        panel.addUIElement(header).inTL(PAD, PAD);
        rebuild();
    }

    // ---------------------------------------------------------------- input

    private void onButton(String id) {
        if (closed) return;
        if (id.equals("close")) {
            close();
            return;
        }
        if (id.startsWith("ship:")) {
            FleetMemberAPI m = member(id.substring(5));
            if (m != null) ship = m;
            picking = false;
            pending = null;
        } else if (id.equals("pick")) {
            picking = true;
            pending = null;
        } else if (id.equals("back")) {
            picking = false;
            pending = null;
        } else if (id.startsWith("build:")) {
            pending = Action.BUILD;
            pendingType = StationType.byId(id.substring(6));
        } else if (id.startsWith("upgrade:")) {
            pending = Action.UPGRADE;
            pendingType = StationType.byId(id.substring(8));
        } else if (id.startsWith("dismantle:")) {
            pending = Action.DISMANTLE;
            pendingType = StationType.byId(id.substring(10));
        } else if (id.equals("cancel")) {
            pending = null;
        } else if (id.equals("confirm")) {
            confirm();
        }
        dirty = true;
    }

    /** Applies the pending action after re-checking everything against the current fleet and cargo. */
    private void confirm() {
        if (pending == null || pendingType == null || ship == null) return;
        String name = pendingType.displayName() + " on " + ship.getShipName();
        if (pending == Action.DISMANTLE) {
            Integer level = Stations.read(ship).get(pendingType);
            if (level != null) {
                int refund = StationCost.refund(ship.getHullSpec().getHullSize(), level);
                Stations.set(ship, pendingType, 0);
                Global.getSector().getPlayerFleet().getCargo().getCredits().add(refund);
                CSLog.info("dismantled " + name + " at " + market.getId() + ", refund " + refund);
            }
        } else {
            int level = nextLevel(pendingType);
            if (whyNot(pendingType, level) == null) {
                Payment pay = Payment.plan(market, StationCost.of(ship.getHullSpec().getHullSize(), level));
                if (pay.canAfford()) {
                    pay.pay();
                    Stations.set(ship, pendingType, level);
                    picking = false;
                    CSLog.info("built " + name + " level " + level + " at " + market.getId() + " for "
                            + pay.totalCredits + " credits");
                }
            }
        }
        pending = null;
    }

    private void close() {
        if (closed) return;
        closed = true;
        callbacks.dismissDialog();
    }

    // ---------------------------------------------------------------- rules

    private int nextLevel(StationType type) {
        Integer now = Stations.read(ship).get(type);
        return now == null ? 1 : now + 1;
    }

    /** Why {@code type} cannot go to {@code level} here, or null. Adds the Shipyard cap to the ship rules. */
    private String whyNot(StationType type, int level) {
        String why = Stations.cannotSet(ship, type, level);
        if (why != null) return why;
        if (level > maxLevel) return fmt("ui_cap_reason", "" + maxLevel);
        return null;
    }

    private static List<FleetMemberAPI> fleet() {
        List<FleetMemberAPI> out = new ArrayList<FleetMemberAPI>();
        for (FleetMemberAPI m : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (!m.isFighterWing()) out.add(m);
        }
        return out;
    }

    private static FleetMemberAPI member(String id) {
        for (FleetMemberAPI m : fleet()) {
            if (m.getId().equals(id)) return m;
        }
        return null;
    }

    // ---------------------------------------------------------------- layout

    private void rebuild() {
        if (ship == null || member(ship.getId()) == null) {
            List<FleetMemberAPI> f = fleet();
            ship = f.isEmpty() ? null : f.get(0);
        }
        if (fleetList != null && fleetList.getExternalScroller() != null) {
            fleetScroll = fleetList.getExternalScroller().getYOffset();
        }
        if (content != null) root.removeComponent(content);
        float h = HEIGHT - HEADER_H - 2 * PAD;
        content = root.createCustomPanel(WIDTH, h, new ButtonsOnly());

        buildFleetList(h);
        float rightW = WIDTH - LIST_W - 3 * PAD;
        float bodyH = h - FOOTER_H - (pending != null ? CONFIRM_H + PAD : 0f);
        TooltipMakerAPI body = content.createUIElement(rightW, bodyH, true);
        if (ship != null) {
            if (picking) buildPicker(body, rightW);
            else buildShip(body, rightW);
        }
        content.addUIElement(body).inTL(LIST_W + 2 * PAD, 0f);
        if (pending != null && ship != null) {
            TooltipMakerAPI bar = content.createUIElement(rightW, CONFIRM_H, false);
            buildConfirm(bar, rightW);
            content.addUIElement(bar).inTL(LIST_W + 2 * PAD, bodyH + PAD);
        }
        TooltipMakerAPI footer = content.createUIElement(rightW, FOOTER_H, false);
        footer.addPara(fmt("ui_footer", Misc.getDGSCredits(Payment.playerCredits()), Shipyard.capSource(market),
                "" + maxLevel), Misc.getGrayColor(), 0f);
        content.addUIElement(footer).inTL(LIST_W + 2 * PAD, h - FOOTER_H);
        root.addComponent(content).inTL(0f, HEADER_H + PAD);
        if (fleetList.getExternalScroller() != null) fleetList.getExternalScroller().setYOffset(fleetScroll);
    }

    private void buildFleetList(float h) {
        TooltipMakerAPI list = content.createUIElement(LIST_W, h, true);
        fleetList = list;
        list.addSectionHeading(str("ui_fleet"), Alignment.MID, 0f);
        for (FleetMemberAPI m : fleet()) {
            int slots = Stations.slots(m);
            String tag = slots == 0 ? str("ui_automated") : Stations.read(m).size() + "/" + slots;
            String text = (m == ship ? "> " : "") + shorten(m.getShipName() + " (" + m.getHullSpec().getHullName() + ")", 30)
                    + " " + tag;
            Color base = m == ship ? Misc.getHighlightColor() : slots == 0 ? Misc.getGrayColor() : Misc.getBasePlayerColor();
            list.addButton(text, "ship:" + m.getId(), base, Misc.getDarkPlayerColor(), LIST_W - 16f, 24f, 4f);
        }
        content.addUIElement(list).inTL(PAD, 0f);
    }

    private void buildShip(TooltipMakerAPI ui, float w) {
        Map<StationType, Integer> stations = Stations.read(ship);
        int slots = Stations.slots(ship);
        List<FleetMemberAPI> one = new ArrayList<FleetMemberAPI>();
        one.add(ship);
        ui.addShipList(1, 1, 80f, Misc.getBasePlayerColor(), one, 0f);
        ui.addPara(ship.getShipName() + " - " + ship.getHullSpec().getHullNameWithDashClass(), Misc.getHighlightColor(), 6f);
        if (slots == 0) {
            ui.addPara(str("ui_automated_ship"), Misc.getGrayColor(), 6f);
            return;
        }
        ui.addPara(fmt("ui_slots", "" + stations.size(), "" + slots), 4f);
        float base = ship.getHullSpec().getMinCrew();
        ui.addPara(fmt("ui_crew", "" + Math.round(base), "" + Math.round(ship.getMinCrew()),
                "+" + Math.round(Stations.crewPercent(stations)) + "%"), Misc.getGrayColor(), 4f);

        int cards = Math.max(slots, stations.size());
        float cardW = Math.min(210f, (w - (cards - 1) * PAD) / cards);
        float cardH = 330f;
        CustomPanelAPI row = Global.getSettings().createCustom(w, cardH, new ButtonsOnly());
        int i = 0;
        for (Map.Entry<StationType, Integer> e : stations.entrySet()) {
            TooltipMakerAPI card = row.createUIElement(cardW, cardH, false);
            buildStationCard(card, cardW, e.getKey(), e.getValue());
            row.addUIElement(card).inTL(i * (cardW + PAD), 0f);
            i++;
        }
        for (; i < slots; i++) {
            TooltipMakerAPI card = row.createUIElement(cardW, cardH, false);
            card.addSectionHeading(str("ui_empty_slot"), Alignment.MID, 0f);
            card.addSpacer(60f);
            card.addButton(str("ui_build_button"), "pick", cardW - 20f, 30f, 10f);
            row.addUIElement(card).inTL(i * (cardW + PAD), 0f);
        }
        ui.addCustom(row, 12f);
    }

    private void buildStationCard(TooltipMakerAPI card, float w, StationType type, int level) {
        boolean active = type.requirement.isMet(ship.getVariant());
        card.addSectionHeading(type.displayName(), Alignment.MID, 0f);
        card.addImage(type.sprite(), 64f, 64f, 8f);
        card.addPara(fmt("ui_level", "" + level, StationType.levelName(level)) + "  " + pips(level),
                Misc.getHighlightColor(), 6f);
        if (!active) card.addPara(fmt("ui_inactive", type.requirement.reason()), Misc.getNegativeHighlightColor(), 4f);
        effects(card, type, level, active);
        if (level < StationType.MAX_LEVEL) {
            ButtonAPI up = card.addButton(fmt("ui_upgrade_button", "" + (level + 1)), "upgrade:" + type.id,
                    w - 20f, 24f, 10f);
            String why = active ? whyNot(type, level + 1) : type.requirement.reason();
            if (why == null) why = cannotPay(level + 1);
            up.setEnabled(why == null);
            if (why != null && active) card.addPara(why, Misc.getNegativeHighlightColor(), 3f);
            else if (why == null) card.addPara(priceLine(level + 1), Misc.getGrayColor(), 3f);
        } else {
            card.addPara(str("ui_max_level"), Misc.getGrayColor(), 10f);
        }
        card.addButton(str("ui_dismantle_button"), "dismantle:" + type.id, w - 20f, 24f, 6f);
    }

    private void buildPicker(TooltipMakerAPI ui, float w) {
        ui.addSectionHeading(str("ui_choose_station"), Alignment.MID, 0f);
        ui.addButton(str("ui_back"), "back", 100f, 24f, 6f);
        int cols = 5;
        float cardW = (w - (cols - 1) * PAD) / cols;
        float cardH = 250f;
        CustomPanelAPI grid = Global.getSettings().createCustom(w, 2 * cardH + PAD, new ButtonsOnly());
        StationType[] types = StationType.values();
        for (int i = 0; i < types.length; i++) {
            StationType type = types[i];
            TooltipMakerAPI card = grid.createUIElement(cardW, cardH, false);
            String why = Stations.read(ship).containsKey(type) ? str("ui_already_built") : whyNot(type, 1);
            if (why == null) why = cannotPay(1);
            card.addSectionHeading(type.displayName(), Alignment.MID, 0f);
            card.addImage(type.sprite(), 48f, 48f, 6f);
            effects(card, type, 1, why == null);
            ButtonAPI build = card.addButton(str("ui_build"), "build:" + type.id, cardW - 20f, 24f, 8f);
            build.setEnabled(why == null);
            if (why != null) card.addPara(why, Misc.getNegativeHighlightColor(), 3f);
            else card.addPara(priceLine(1), Misc.getGrayColor(), 3f);
            grid.addUIElement(card).inTL((i % cols) * (cardW + PAD), (i / cols) * (cardH + PAD));
        }
        ui.addCustom(grid, 8f);
    }

    private void buildConfirm(TooltipMakerAPI ui, float w) {
        String shipName = ship.getShipName();
        ui.addSectionHeading(str("ui_confirm"), Alignment.MID, 0f);
        boolean ok;
        if (pending == Action.DISMANTLE) {
            Integer level = Stations.read(ship).get(pendingType);
            int lv = level == null ? 0 : level;
            String refund = Misc.getDGSCredits(StationCost.refund(ship.getHullSpec().getHullSize(), lv));
            ui.addPara(fmt("ui_pending_dismantle", pendingType.displayName(), "" + lv, shipName), Misc.getTextColor(), 6f);
            ui.addPara(str("ui_refund") + " %s", 4f, Misc.getHighlightColor(), refund);
            ok = level != null;
        } else {
            int level = nextLevel(pendingType);
            String title = pending == Action.BUILD
                    ? fmt("ui_pending_build", pendingType.displayName(), shipName)
                    : fmt("ui_pending_upgrade", pendingType.displayName(), "" + level, shipName);
            ui.addPara(title, Misc.getHighlightColor(), 6f);
            Payment pay = Payment.plan(market, StationCost.of(ship.getHullSpec().getHullSize(), level));
            ui.addPara(fmt("ui_cost_credits", Misc.getDGSCredits(pay.cost.credits)), 4f);
            for (Payment.Line l : pay.lines) {
                if (l.missing > 0) {
                    ui.addPara(fmt("ui_cost_missing", l.name(), "" + l.missing), Misc.getNegativeHighlightColor(), 3f);
                } else {
                    ui.addPara(sourcesLine(l), 3f);
                }
            }
            boolean rich = Payment.playerCredits() >= pay.totalCredits;
            ui.addPara(str("ui_cost_total") + " %s / %s", 4f,
                    rich ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor(),
                    Misc.getDGSCredits(pay.totalCredits), Misc.getDGSCredits(Payment.playerCredits()));
            ok = pay.canAfford() && whyNot(pendingType, level) == null;
        }
        ButtonAPI yes = ui.addButton(str("ui_confirm"), "confirm", 140f, 24f, 8f);
        yes.setEnabled(ok);
        ButtonAPI no = ui.addButton(str("ui_cancel"), "cancel", 140f, 24f, 0f);
        no.getPosition().rightOfTop(yes, PAD);
    }

    private void effects(TooltipMakerAPI ui, StationType type, int level, boolean active) {
        Color text = active ? Misc.getTextColor() : Misc.getGrayColor();
        Color hl = active ? Misc.getHighlightColor() : Misc.getGrayColor();
        for (StationEffect fx : type.effects) {
            ui.addPara(StationType.effectName(fx) + " %s", 3f, text, hl, type.valueText(fx, level));
        }
    }

    /** Why the player cannot pay for {@code level} of a Station on this ship here, or null. */
    private String cannotPay(int level) {
        Payment pay = Payment.plan(market, StationCost.of(ship.getHullSpec().getHullSize(), level));
        Payment.Line missing = pay.firstMissing();
        if (missing != null) return fmt("ui_missing", "" + missing.missing, missing.name());
        if (!pay.canAfford()) return fmt("ui_poor", Misc.getDGSCredits(pay.totalCredits));
        return null;
    }

    /** "Cost: 150,000¢ + materials" style line for the cards. */
    private String priceLine(int level) {
        StationCost cost = StationCost.of(ship.getHullSpec().getHullSize(), level);
        StringBuilder sb = new StringBuilder(Misc.getDGSCredits(cost.credits));
        for (Map.Entry<String, Integer> e : cost.commodities.entrySet()) {
            sb.append(", ").append(e.getValue()).append(" ")
                    .append(Global.getSettings().getCommoditySpec(e.getKey()).getName().toLowerCase());
        }
        return fmt("ui_price", sb.toString());
    }

    /** "Heavy machinery: 400 (cargo 120, storage 200, buy 80 for 12,000¢)", leaving out empty sources. */
    private static String sourcesLine(Payment.Line l) {
        List<String> parts = new ArrayList<String>();
        if (l.fromCargo > 0) parts.add(fmt("ui_src_cargo", "" + l.fromCargo));
        if (l.fromStorage > 0) parts.add(fmt("ui_src_storage", "" + l.fromStorage));
        if (l.bought > 0) parts.add(fmt("ui_src_buy", "" + l.bought, Misc.getDGSCredits(l.buyCredits)));
        return l.name() + ": " + l.needed + " (" + String.join(", ", parts) + ")";
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    /** "[##-]" for level 2 of 3: the font has no filled-circle glyph. */
    private static String pips(int level) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 1; i <= StationType.MAX_LEVEL; i++) sb.append(i <= level ? '#' : '-');
        return sb.append(']').toString();
    }

    private static String str(String key) {
        return Global.getSettings().getString(CSIds.STRINGS, key);
    }

    private static String fmt(String key, String... args) {
        return String.format(str(key), (Object[]) args);
    }

    // ---------------------------------------------------------------- dialog plumbing

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return plugin;
    }

    @Override
    public float getNoiseAlpha() {
        return 0f;
    }

    /** Rebuilds here, outside the panels' own update loops. */
    @Override
    public void advance(float amount) {
        if (dirty && !closed) {
            dirty = false;
            rebuild();
        }
    }

    @Override
    public void reportDismissed(int option) {}
}
