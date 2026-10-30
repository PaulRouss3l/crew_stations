package com.crewstations.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.input.Keyboard;

import com.crewstations.shipyard.Payment;
import com.crewstations.shipyard.Shipyard;
import com.crewstations.shipyard.StationCost;
import com.crewstations.station.StationEffect;
import com.crewstations.station.StationType;
import com.crewstations.station.Stations;
import com.crewstations.util.CSIds;
import com.crewstations.util.CSLog;
import com.crewstations.util.CSSettings;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * The Shipyard screen (docs/DESIGN.md "Screen"): the ship view (fleet list, ship panel, one card per Station
 * slot) and the Build picker (Station tiles and a detail panel). Both open a confirmation drawer that shows the
 * full cost or refund before anything happens. Layout follows the "Crew Stations Shipyard UI" design canvas.
 * Opened by {@code CrstShipyard open} with {@code dialog.showCustomVisualDialog(WIDTH, HEIGHT, screen)}.
 * Holds the market and fleet members only while open; nothing here is saved.
 */
public class ShipyardScreen implements CustomVisualDialogDelegate, Painter.Clicks {

    public static final float WIDTH = 1120f;
    public static final float HEIGHT = 700f;
    private static final float HEADER_H = 56f;
    private static final float FOOTER_H = 28f;
    private static final float LIST_W = 264f;
    private static final float DETAIL_W = 340f;
    private static final float DRAWER_H = 214f;
    private static final float PAD = 16f;
    private static final float GAP = 14f;
    private static final float ROW_H = 52f;
    private static final float BODY_H = HEIGHT - HEADER_H - FOOTER_H;

    static final Color GROUND = new Color(5, 9, 12);
    static final Color PANEL = new Color(11, 24, 32);
    static final Color BAND = new Color(23, 58, 69);
    static final Color LINE = new Color(43, 101, 117);
    static final Color LINE_DIM = new Color(29, 72, 85);
    static final Color GRID = new Color(15, 38, 48);
    static final Color TEXT = new Color(214, 241, 255);
    static final Color DIM = new Color(127, 152, 162);
    static final Color MUTED = new Color(69, 85, 92);
    static final Color CYAN = new Color(79, 209, 255);
    static final Color GREEN = new Color(111, 227, 138);
    static final Color PRIMARY_BG = new Color(27, 76, 90);
    static final Color CONFIRM_BG = new Color(74, 61, 16);
    static final Color HOVER = new Color(79, 209, 255, 28);
    /** Unavailable art: dimmed, not hidden. */
    static final Color ART_OFF = new Color(90, 100, 105);

    private enum Action { BUILD, UPGRADE, DISMANTLE }

    private final MarketAPI market;
    private final int maxLevel;

    private CustomPanelAPI root;
    private CustomPanelAPI content;
    private DialogCallbacks callbacks;
    private FleetMemberAPI ship;
    /** Showing the Build picker instead of the ship view. */
    private boolean picking;
    /** Tile selected in the picker. */
    private StationType pickedType;
    /** Picker category filter; null shows all. */
    private String category;
    private Action pending;
    private StationType pendingType;
    /** Rebuild on the next frame, not inside the click that asked for it (the clicked panel is removed). */
    private boolean dirty;
    private boolean closed;
    private TooltipMakerAPI fleetList;
    /** Fleet list scroll position, kept across rebuilds. */
    private float fleetScroll;
    /** Clickable panels (fleet rows, picker tiles) of the current layout. */
    private final List<Painter> clickables = new ArrayList<Painter>();

    public ShipyardScreen(MarketAPI market) {
        this.market = market;
        this.maxLevel = Shipyard.maxLevel(market);
    }

    private final CustomUIPanelPlugin plugin = new Painter(this) {
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
            for (InputEventAPI e : events) {
                for (Painter p : clickables) p.handle(e);
            }
        }
    };

    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        this.root = panel;
        this.callbacks = callbacks;
        ship = Global.getSector().getPlayerFleet().getFlagship();
        rebuild();
    }

    // ---------------------------------------------------------------- input

    @Override
    public void onButton(String id) {
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
            if (pickedType == null) pickedType = firstBuildable();
        } else if (id.equals("back")) {
            picking = false;
            pending = null;
        } else if (id.startsWith("tile:")) {
            pickedType = StationType.byId(id.substring(5));
            pending = null;
        } else if (id.startsWith("cat:")) {
            category = id.length() > 4 ? id.substring(4) : null;
            if (category != null && (pickedType == null || !category.equals(categoryOf(pickedType)))) {
                for (StationType t : StationType.values()) {
                    if (category.equals(categoryOf(t))) {
                        pickedType = t;
                        break;
                    }
                }
            }
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

    /** Why the player cannot pay for {@code level} of a Station on this ship here, or null. */
    private String cannotPay(int level) {
        Payment pay = Payment.plan(market, StationCost.of(ship.getHullSpec().getHullSize(), level));
        Payment.Line missing = pay.firstMissing();
        if (missing != null) return fmt("ui_missing", "" + missing.missing, missing.name());
        if (!pay.canAfford()) return fmt("ui_poor", Misc.getDGSCredits(pay.totalCredits));
        return null;
    }

    /** Why {@code type} cannot be built on this ship now (built, rules, Shipyard, cost), or null. */
    private String whyNotBuild(StationType type) {
        if (Stations.read(ship).containsKey(type)) return str("ui_already_built");
        String why = whyNot(type, 1);
        return why != null ? why : cannotPay(1);
    }

    /** Station types whose requirement this ship meets and that are not built yet. */
    private int fitCount() {
        Map<StationType, Integer> built = Stations.read(ship);
        int n = 0;
        for (StationType t : StationType.values()) {
            if (!built.containsKey(t) && t.requirement.isMet(ship.getVariant())) n++;
        }
        return n;
    }

    private StationType firstBuildable() {
        for (StationType t : StationType.values()) {
            if (whyNotBuild(t) == null) return t;
        }
        return StationType.values()[0];
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
        fleetList = null;
        clickables.clear();
        if (content != null) root.removeComponent(content);
        Painter paint = new Painter(this);
        content = root.createCustomPanel(WIDTH, HEIGHT, paint);
        // Opaque ground: the market dialog's text must not show through.
        paint.fill(0, 0, WIDTH, HEIGHT, GROUND);

        if (picking && ship != null && Stations.slots(ship) > 0) {
            buildPickerHeader(paint);
            buildPicker(paint);
            if (pending != null) buildDrawer(0f, WIDTH - DETAIL_W);
        } else {
            picking = false;
            buildHeader(paint);
            buildFleetList(paint);
            if (ship != null) buildShip(paint);
            if (pending != null && ship != null) buildDrawer(LIST_W, WIDTH - LIST_W);
        }
        buildFooter(paint);
        root.addComponent(content).inTL(0f, 0f);
        if (fleetList != null && fleetList.getExternalScroller() != null) {
            fleetList.getExternalScroller().setYOffset(fleetScroll);
        }
    }

    private void buildHeader(Painter paint) {
        paint.fill(0, 0, WIDTH, HEADER_H, PANEL).line(0, HEADER_H, WIDTH, HEADER_H, LINE);
        TooltipMakerAPI title = content.createUIElement(560f, HEADER_H - 12f, false);
        para(title, str("ui_works_title"), Fonts.ORBITRON_20AABOLD, TEXT, 0f);
        para(title, fmt("ui_subtitle", market.getName(), Shipyard.capSource(market)), null, DIM, 2f);
        put(content, title, PAD, 8f);

        float x = WIDTH - PAD - 100f;
        TooltipMakerAPI close = content.createUIElement(100f, 32f, false);
        button(close, str("ui_close"), "close", TEXT, BAND, 100f, 32f, true);
        put(content, close, x, 12f);
        x = chip(paint, x - GAP, str("ui_chip_max_level"), "" + maxLevel, 130f);
        chip(paint, x - 10f, str("ui_chip_credits"), Misc.getDGSCredits(Payment.playerCredits()), 210f);
    }

    /** A framed "LABEL value" chip ending at {@code right}; returns its left edge. */
    private float chip(Painter paint, float right, String label, String value, float w) {
        float x = right - w;
        paint.fill(x, 12f, w, 32f, GROUND).border(x, 12f, w, 32f, LINE);
        TooltipMakerAPI t = content.createUIElement(w - 20f, 24f, false);
        LabelAPI l = t.addPara(label + "  %s", 0f, DIM, Misc.getHighlightColor(), value);
        l.setAlignment(Alignment.MID);
        put(content, t, x + 10f, 19f);
        return x;
    }

    private void buildFooter(Painter paint) {
        float y = HEIGHT - FOOTER_H;
        paint.fill(0, y, WIDTH, FOOTER_H, PANEL).line(0, y, WIDTH, y, LINE_DIM);
        TooltipMakerAPI t = content.createUIElement(WIDTH - 2 * PAD - 120f, 20f, false);
        String hint = picking ? str("ui_footer_sources")
                : fmt("ui_footer_crew", fmtNum(CSSettings.crewPercentPerLevel()),
                        fmtNum(CSSettings.refundFraction() * 100f));
        para(t, hint, null, DIM, 0f);
        put(content, t, PAD, y + 5f);
        TooltipMakerAPI esc = content.createUIElement(110f, 20f, false);
        para(esc, str("ui_footer_esc"), null, DIM, 0f).setAlignment(Alignment.RMID);
        put(content, esc, WIDTH - PAD - 110f, y + 5f);
    }

    // ---------------------------------------------------------------- ship view

    private void buildFleetList(Painter paint) {
        float top = HEADER_H;
        paint.fill(0, top, LIST_W, BODY_H, PANEL).line(LIST_W, top, LIST_W, top + BODY_H, LINE);
        paint.fill(0, top, LIST_W, 30f, BAND);
        List<FleetMemberAPI> ships = fleet();
        TooltipMakerAPI head = content.createUIElement(LIST_W - 24f, 20f, false);
        LabelAPI h = head.addPara(str("ui_fleet") + "  %s", 0f, TEXT, DIM, fmt("ui_fleet_count", "" + ships.size()));
        h.setAlignment(Alignment.LMID);
        put(content, head, 12f, top + 7f);

        float listH = BODY_H - 30f;
        TooltipMakerAPI list = content.createUIElement(LIST_W, listH, true);
        fleetList = list;
        float rowW = LIST_W - 24f;
        List<Painter> rows = new ArrayList<Painter>();
        for (FleetMemberAPI m : ships) {
            Painter rp = new Painter(this);
            CustomPanelAPI row = Global.getSettings().createCustom(rowW, ROW_H, rp);
            buildFleetRow(row, rp, m, rowW);
            // Each row is placed below the previous one and keeps its x: offset the first only.
            PositionAPI rowPos = list.addCustom(row, 6f).getPosition();
            if (rows.isEmpty()) rowPos.setXAlignOffset(8f);
            rows.add(rp);
        }
        list.addSpacer(8f);
        PositionAPI view = content.addUIElement(list).inTL(0f, top + 30f);
        for (Painter rp : rows) rp.clip(view);
    }

    private void buildFleetRow(CustomPanelAPI row, Painter rp, FleetMemberAPI m, float w) {
        boolean sel = m == ship;
        int slots = Stations.slots(m);
        boolean auto = slots == 0;
        if (pending == null) {
            rp.clickable("ship:" + m.getId(), null, HOVER);
            clickables.add(rp);
        }
        rp.fill(0, 0, w, ROW_H, sel ? BAND : GROUND).border(0, 0, w, ROW_H, sel ? CYAN : LINE_DIM);
        rp.fill(6, 6, 40, 40, GROUND).border(6, 6, 40, 40, LINE_DIM);
        rp.sprite(m.getHullSpec().getSpriteName(), 8, 8, 36, 36, auto ? ART_OFF : null, false);

        float pipsW;
        if (auto) {
            pipsW = 40f;
            TooltipMakerAPI tag = row.createUIElement(pipsW, 16f, false);
            para(tag, str("ui_automated_tag"), null, DIM, 0f).setAlignment(Alignment.RMID);
            put(row, tag, w - pipsW - 8f, 18f);
        } else {
            int used = Stations.read(m).size();
            pipsW = slots * 11f;
            float px = w - 8f - pipsW;
            for (int i = 0; i < slots; i++) {
                if (i < used) rp.fill(px + i * 11f, 20f, 8f, 12f, Misc.getHighlightColor());
                else rp.border(px + i * 11f, 20f, 8f, 12f, CYAN);
            }
        }
        float textW = w - 56f - pipsW - 14f;
        TooltipMakerAPI t = row.createUIElement(textW, ROW_H - 8f, false);
        Color nameColor = auto ? MUTED : sel ? Misc.getHighlightColor() : TEXT;
        para(t, shorten(m.getShipName(), 22), null, nameColor, 0f);
        para(t, shorten(m.getHullSpec().getHullNameWithDashClass(), 26), null, DIM, 1f);
        put(row, t, 54f, 7f);
    }

    private void buildShip(Painter paint) {
        float x0 = LIST_W + PAD, y0 = HEADER_H + PAD, w = WIDTH - LIST_W - 2 * PAD;
        Map<StationType, Integer> stations = Stations.read(ship);
        int slots = Stations.slots(ship);

        // Ship viewport and identity.
        float vw = 210f, vh = 150f;
        paint.fill(x0, y0, vw, vh, GROUND).grid(x0, y0, vw, vh, 15f, GRID).border(x0, y0, vw, vh, LINE);
        paint.sprite(ship.getHullSpec().getSpriteName(), x0 + 10, y0 + 16, vw - 20, vh - 24, slots == 0 ? ART_OFF : null, false);
        TooltipMakerAPI size = content.createUIElement(vw - 12f, 14f, false);
        para(size, hullSizeName(ship).toUpperCase(), null, DIM, 0f);
        put(content, size, x0 + 6f, y0 + 3f);

        float ix = x0 + vw + 18f, iw = w - vw - 18f;
        TooltipMakerAPI id = content.createUIElement(iw, 50f, false);
        para(id, ship.getShipName(), Fonts.ORBITRON_20AABOLD, Misc.getHighlightColor(), 0f);
        para(id, ship.getHullSpec().getHullNameWithDashClass() + " " + ship.getHullSpec().getDesignation(), null, DIM, 2f);
        put(content, id, ix, y0 + 18f);

        if (slots == 0) {
            TooltipMakerAPI auto = content.createUIElement(iw, 40f, false);
            para(auto, str("ui_automated_ship"), null, DIM, 0f);
            put(content, auto, ix, y0 + 84f);
            return;
        }
        float bw = (iw - 2 * 10f) / 3f, by = y0 + 84f, bh = 50f;
        for (int i = 0; i < 3; i++) {
            paint.fill(ix + i * (bw + 10f), by, bw, bh, PANEL).border(ix + i * (bw + 10f), by, bw, bh, LINE_DIM);
        }
        statBox(ix, by, bw, str("ui_stat_slots"), null);
        for (int i = 0; i < slots; i++) {
            if (i < stations.size()) paint.fill(ix + 10f + i * 22f, by + 28f, 18f, 10f, CYAN);
            else paint.border(ix + 10f + i * 22f, by + 28f, 18f, 10f, CYAN);
        }
        TooltipMakerAPI count = content.createUIElement(60f, 16f, false);
        para(count, stations.size() + " / " + slots, null, TEXT, 0f);
        put(content, count, ix + 14f + slots * 22f, by + 25f);
        statBox(ix + bw + 10f, by, bw, str("ui_stat_skeleton"),
                crewLine(ship.getHullSpec().getMinCrew(), ship.getMinCrew(), stations));
        statBox(ix + 2 * (bw + 10f), by, bw, str("ui_stat_max_crew"),
                crewLine(ship.getHullSpec().getMaxCrew(), ship.getMaxCrew(), stations));

        // One card per slot (more if a Station sits over the slot count, e.g. after a settings change).
        float cy = y0 + vh + GAP, ch = HEIGHT - FOOTER_H - PAD - cy;
        int cards = Math.max(slots, stations.size());
        float cw = Math.min(300f, (w - (cards - 1) * GAP) / cards);
        int i = 0;
        for (Map.Entry<StationType, Integer> e : stations.entrySet()) {
            stationCard(x0 + i * (cw + GAP), cy, cw, ch, e.getKey(), e.getValue());
            i++;
        }
        int fits = fitCount();
        for (; i < slots; i++) emptyCard(x0 + i * (cw + GAP), cy, cw, ch, i + 1, fits);
    }

    private void statBox(float x, float y, float w, String label, String value) {
        TooltipMakerAPI t = content.createUIElement(w - 20f, 40f, false);
        para(t, label, null, DIM, 0f);
        if (value != null) {
            String[] p = value.split("\\|");
            multi(t, "%s  %s  %s  %s", 4f, TEXT, new Color[] { TEXT, DIM, Misc.getNegativeHighlightColor(), DIM },
                    p[0], "->", p[1], p[2]);
        }
        put(content, t, x + 10f, y + 6f);
    }

    /** "base|now|+N%" for a crew stat box. */
    private static String crewLine(float base, float now, Map<StationType, Integer> stations) {
        return Math.round(base) + "|" + Math.round(now) + "|+" + fmtNum(Stations.crewPercent(stations)) + "%";
    }

    private void stationCard(float x, float y, float w, float h, StationType type, int level) {
        boolean active = type.requirement.isMet(ship.getVariant());
        boolean locked = pending != null;
        Painter cp = new Painter(this);
        CustomPanelAPI card = content.createCustomPanel(w, h, cp);
        cp.fill(0, 0, w, h, PANEL).border(0, 0, w, h, LINE);

        boolean narrow = w < 260f;
        float artH = narrow ? 90f : 120f;
        cp.fill(1, 1, w - 2, artH - 1, GROUND).grid(1, 1, w - 2, artH - 1, 12f, GRID).line(0, artH, w, artH, LINE);
        cp.sprite(type.sprite(), w / 2f - 56f, 4f, 112f, 112f, active ? null : ART_OFF, false);
        float bandY = artH, bandH = 52f;
        cp.fill(1, bandY, w - 2, bandH, BAND);
        Color pip = active ? Misc.getHighlightColor() : DIM;
        for (int i = 0; i < StationType.MAX_LEVEL; i++) {
            if (i < level) cp.fill(10f + i * 26f, bandY + 34f, 22f, 6f, pip);
            else cp.border(10f + i * 26f, bandY + 34f, 22f, 6f, pip);
        }
        TooltipMakerAPI head = card.createUIElement(w - 20f, 30f, false);
        para(head, type.displayName().toUpperCase(), Fonts.ORBITRON_12, TEXT, 0f);
        put(card, head, 10f, bandY + 8f);
        TooltipMakerAPI lv = card.createUIElement(w - 100f, 16f, false);
        para(lv, fmt("ui_level_short", "" + level, StationType.levelName(level).toUpperCase()), null, pip, 0f);
        put(card, lv, 92f, bandY + 30f);

        boolean maxed = level >= StationType.MAX_LEVEL;
        float ty = bandY + bandH + 10f;
        if (!active) {
            TooltipMakerAPI off = card.createUIElement(w - 20f, 18f, false);
            para(off, fmt("ui_inactive", type.requirement.reason()), null, Misc.getNegativeHighlightColor(), 0f);
            put(card, off, 10f, ty);
            ty += 22f;
        }
        int[] levels = maxed ? new int[] { level } : new int[] { level, level + 1 };
        String[] heads = maxed ? new String[] { str("ui_col_now") } : new String[] { str("ui_col_now"), fmt("ui_col_level", "" + (level + 1)) };
        effectTable(card, 10f, ty, w - 20f, type, levels, heads, active ? 0 : -1);

        // Footer: price or reason, then the buttons.
        float fy = narrow ? h - 106f : h - 88f;
        cp.line(0, fy, w, fy, LINE_DIM);
        TooltipMakerAPI note = card.createUIElement(w - 20f, 36f, false);
        String why = null;
        if (!maxed) {
            why = active ? whyNot(type, level + 1) : type.requirement.reason();
            if (why == null) why = cannotPay(level + 1);
        }
        if (maxed) para(note, str("ui_max_level"), null, DIM, 0f);
        else if (why != null && active) para(note, why, null, Misc.getNegativeHighlightColor(), 0f);
        else if (why == null) para(note, priceLine(level + 1), null, DIM, 0f);
        put(card, note, 10f, fy + 8f);

        // Side by side on wide cards; stacked full width on narrow ones (capital ships, 4 slots).
        float bw = narrow ? w - 20f : 96f, bh = narrow ? 26f : 30f;
        TooltipMakerAPI btns = card.createUIElement(w - 20f, narrow ? 2 * bh + 4f : bh, false);
        ButtonAPI up = null;
        if (!maxed) {
            up = button(btns, fmt("ui_upgrade_button", "" + (level + 1)), "upgrade:" + type.id, Color.WHITE,
                    PRIMARY_BG, narrow ? w - 20f : w - 20f - bw - 8f, bh, true);
            up.setEnabled(why == null && !locked);
        }
        ButtonAPI dis = button(btns, str("ui_dismantle_button"), "dismantle:" + type.id, DIM, GROUND, bw, bh, false);
        dis.setEnabled(!locked);
        if (up != null && narrow) dis.getPosition().belowLeft(up, 4f);
        else if (up != null) dis.getPosition().rightOfTop(up, 8f);
        else if (!narrow) dis.getPosition().setXAlignOffset(w - 20f - bw);
        put(card, btns, 10f, narrow ? h - 2 * bh - 14f : h - 42f);
        content.addComponent(card).inTL(x, y);
    }

    private void emptyCard(float x, float y, float w, float h, int slot, int fits) {
        Painter cp = new Painter(this);
        CustomPanelAPI card = content.createCustomPanel(w, h, cp);
        cp.fill(0, 0, w, h, new Color(7, 15, 20)).dashed(0, 0, w, h, LINE);
        float cy = h / 2f - 90f;
        TooltipMakerAPI label = card.createUIElement(w - 20f, 16f, false);
        para(label, fmt("ui_slot_n", "" + slot), Fonts.ORBITRON_12, DIM, 0f).setAlignment(Alignment.MID);
        put(card, label, 10f, cy);
        float bx = w / 2f - 28f, by = cy + 28f;
        cp.border(bx, by, 56f, 56f, LINE).line(bx + 28f, by + 14f, bx + 28f, by + 42f, CYAN)
                .line(bx + 14f, by + 28f, bx + 42f, by + 28f, CYAN);
        TooltipMakerAPI t = card.createUIElement(w - 20f, 70f, false);
        para(t, fmt("ui_fit_count", "" + fits, "" + StationType.values().length), null, DIM, 0f).setAlignment(Alignment.MID);
        ButtonAPI b = button(t, str("ui_build_station"), "pick", Color.WHITE, PRIMARY_BG, 170f, 32f, true);
        b.getPosition().setXAlignOffset((w - 20f - 170f) / 2f);
        b.setEnabled(pending == null);
        put(card, t, 10f, by + 72f);
        content.addComponent(card).inTL(x, y);
    }

    /**
     * Effect rows with one value column per level. {@code hiCol}: the column drawn in the highlight colour
     * (the current or buildable level), -1 for none.
     */
    private void effectTable(CustomPanelAPI p, float x, float y, float w, StationType type, int[] levels, String[] heads,
            int hiCol) {
        float colW = 46f, labelW = w - levels.length * colW;
        TooltipMakerAPI hl = p.createUIElement(labelW, 14f, false);
        para(hl, str("ui_col_effect"), null, DIM, 0f);
        put(p, hl, x, y);
        for (int c = 0; c < levels.length; c++) {
            TooltipMakerAPI hc = p.createUIElement(colW, 14f, false);
            para(hc, heads[c], null, c == hiCol ? Misc.getHighlightColor() : DIM, 0f).setAlignment(Alignment.RMID);
            put(p, hc, x + labelW + c * colW, y);
        }
        float rowH = labelW < 120f ? 36f : 32f, ry = y + 18f;
        for (StationEffect fx : type.effects) {
            TooltipMakerAPI l = p.createUIElement(labelW - 6f, rowH, false);
            para(l, StationType.effectName(fx), null, hiCol < 0 ? DIM : TEXT, 0f);
            put(p, l, x, ry);
            for (int c = 0; c < levels.length; c++) {
                TooltipMakerAPI v = p.createUIElement(colW, rowH, false);
                para(v, type.valueText(fx, levels[c]), null, c == hiCol ? Misc.getHighlightColor() : DIM, 0f)
                        .setAlignment(Alignment.RMID);
                put(p, v, x + labelW + c * colW, ry);
            }
            ry += rowH;
        }
    }

    // ---------------------------------------------------------------- picker

    private void buildPickerHeader(Painter paint) {
        paint.fill(0, 0, WIDTH, HEADER_H, PANEL).line(0, HEADER_H, WIDTH, HEADER_H, LINE);
        TooltipMakerAPI back = content.createUIElement(90f, 32f, false);
        button(back, str("ui_back"), "back", TEXT, BAND, 90f, 32f, true).setEnabled(pending == null);
        put(content, back, PAD, 12f);
        int slot = Math.min(Stations.read(ship).size() + 1, Stations.slots(ship));
        TooltipMakerAPI title = content.createUIElement(600f, HEADER_H - 12f, false);
        para(title, str("ui_picker_title"), Fonts.ORBITRON_20AABOLD, TEXT, 0f);
        para(title, fmt("ui_picker_sub", ship.getShipName(), "" + slot, "" + Stations.slots(ship), market.getName(),
                "" + maxLevel), null, DIM, 2f);
        put(content, title, PAD + 90f + GAP, 8f);
        chip(paint, WIDTH - PAD, str("ui_chip_credits"), Misc.getDGSCredits(Payment.playerCredits()), 210f);
    }

    private void buildPicker(Painter paint) {
        float x0 = PAD, y0 = HEADER_H + PAD, w = WIDTH - DETAIL_W - 2 * PAD;
        boolean locked = pending != null;

        // Category tabs, from the Station types themselves.
        Set<String> cats = new LinkedHashSet<String>();
        for (StationType t : StationType.values()) cats.add(categoryOf(t));
        TooltipMakerAPI tabs = content.createUIElement(w, 28f, false);
        ButtonAPI prev = button(tabs, fmt("ui_tab_all", "" + StationType.values().length), "cat:",
                category == null ? TEXT : DIM, category == null ? BAND : GROUND, 90f, 24f, false);
        prev.setEnabled(!locked);
        for (String c : cats) {
            boolean on = c.equals(category);
            ButtonAPI b = button(tabs, categoryName(c).toUpperCase(), "cat:" + c, on ? TEXT : DIM, on ? BAND : GROUND,
                    110f, 24f, false);
            b.getPosition().rightOfTop(prev, 8f);
            b.setEnabled(!locked);
            prev = b;
        }
        put(content, tabs, x0, y0);

        List<StationType> shown = new ArrayList<StationType>();
        for (StationType t : StationType.values()) {
            if (category == null || category.equals(categoryOf(t))) shown.add(t);
        }
        int cols = 5;
        float ty = y0 + 36f, gridH = HEIGHT - FOOTER_H - PAD - ty;
        float tw = (w - (cols - 1) * 10f) / cols, th = (gridH - 10f) / 2f;
        for (int i = 0; i < shown.size(); i++) {
            pickerTile(shown.get(i), x0 + (i % cols) * (tw + 10f), ty + (i / cols) * (th + 10f), tw, th, locked);
        }
        buildDetail(paint, WIDTH - DETAIL_W, HEADER_H, DETAIL_W, BODY_H);
    }

    private void pickerTile(StationType type, float x, float y, float w, float h, boolean locked) {
        Integer built = Stations.read(ship).get(type);
        String why = built != null ? null : whyNotBuild(type);
        boolean fits = built != null || type.requirement.isMet(ship.getVariant());
        boolean sel = type == pickedType;
        Painter tp = new Painter(this);
        if (!locked) {
            tp.clickable("tile:" + type.id, null, HOVER);
            clickables.add(tp);
        }
        CustomPanelAPI tile = content.createCustomPanel(w, h, tp);
        tp.fill(0, 0, w, h, sel ? new Color(18, 48, 58) : fits ? PANEL : new Color(7, 12, 15));
        if (sel) tp.borderAbove(0, 0, w, h, Misc.getHighlightColor()).borderAbove(1, 1, w - 2, h - 2, Misc.getHighlightColor());
        else tp.border(0, 0, w, h, fits ? LINE : new Color(29, 42, 47));

        float artY = 24f, artH = h - artY - 56f;
        tp.fill(8, artY, w - 16, artH, GROUND).border(8, artY, w - 16, artH, LINE_DIM);
        tp.sprite(type.sprite(), 12, artY + 4, w - 24, artH - 8, fits ? null : ART_OFF, false);

        TooltipMakerAPI cat = tile.createUIElement(w - 16f, 14f, false);
        para(cat, categoryName(categoryOf(type)).toUpperCase(), null, DIM, 0f);
        put(tile, cat, 8f, 5f);
        TooltipMakerAPI t = tile.createUIElement(w - 12f, 50f, false);
        para(t, type.displayName(), null, fits ? TEXT : MUTED, 0f).setAlignment(Alignment.MID);
        String status;
        Color sc;
        if (built != null) {
            status = fmt("ui_installed", "" + built);
            sc = GREEN;
        } else if (why != null) {
            // Rule reasons are short; a cost reason is summarised here and spelled out in the detail panel.
            String rule = whyNot(type, 1);
            status = rule != null ? rule : str("ui_cant_afford");
            sc = Misc.getNegativeHighlightColor();
        } else {
            status = shortPrice(1);
            sc = DIM;
        }
        para(t, status, null, sc, 2f).setAlignment(Alignment.MID);
        put(tile, t, 6f, h - 50f);
        content.addComponent(tile).inTL(x, y);
    }

    private void buildDetail(Painter paint, float x, float y, float w, float h) {
        StationType type = pickedType != null ? pickedType : firstBuildable();
        paint.fill(x, y, w, h, PANEL).line(x, y, x, y + h, LINE).fill(x + 1, y, w - 1, 92f, BAND);
        paint.fill(x + PAD, y + 14f, 64f, 64f, GROUND).border(x + PAD, y + 14f, 64f, 64f, CYAN);
        paint.sprite(type.sprite(), x + PAD + 2f, y + 16f, 60f, 60f, null, false);
        TooltipMakerAPI head = content.createUIElement(w - 64f - 3 * PAD + 4f, 70f, false);
        para(head, type.displayName().toUpperCase(), Fonts.ORBITRON_12, TEXT, 0f);
        para(head, description(type), null, DIM, 4f);
        put(content, head, x + PAD + 64f + 12f, y + 16f);

        float iy = y + 108f, iw = w - 2 * PAD;
        effectTable(content, x + PAD, iy, iw, type, new int[] { 1, 2, 3 },
                new String[] { fmt("ui_col_level", "1"), fmt("ui_col_level", "2"), fmt("ui_col_level", "3") }, 0);
        iy += 18f + type.effects.size() * 32f + 10f;

        TooltipMakerAPI info = content.createUIElement(iw, 110f, false);
        para(info, str("ui_requires"), null, DIM, 0f);
        String req = type.requirement.reason();
        para(info, req == null ? str("ui_req_none") : capitalize(req), null, TEXT, 2f);
        para(info, str("ui_crew_head"), null, DIM, 10f);
        float base = ship.getHullSpec().getMinCrew();
        float now = ship.getMinCrew();
        float after = now + base * CSSettings.crewPercentPerLevel() / 100f;
        info.addPara(str("ui_crew_preview"), 2f, TEXT, Misc.getNegativeHighlightColor(), "" + Math.round(now),
                "" + Math.round(after), fmtNum(CSSettings.crewPercentPerLevel()) + "%");
        put(content, info, x + PAD, iy);

        // Cost block and Build button at the bottom.
        Integer built = Stations.read(ship).get(type);
        String why = built != null ? str("ui_already_built") : whyNotBuild(type);
        float by = y + h - 172f;
        paint.line(x, by, x + w, by, LINE_DIM);
        TooltipMakerAPI cost = content.createUIElement(iw, 114f, false);
        Payment pay = Payment.plan(market, StationCost.of(ship.getHullSpec().getHullSize(), 1));
        LabelAPI cr = cost.addPara(str("ui_res_credits") + "  %s", 0f, DIM, Misc.getHighlightColor(),
                Misc.getDGSCredits(pay.cost.credits));
        cr.setAlignment(Alignment.LMID);
        for (Payment.Line l : pay.lines) {
            if (l.missing > 0) para(cost, fmt("ui_cost_missing_short", l.name(), "" + l.missing), null,
                    Misc.getNegativeHighlightColor(), 3f);
            else multi(cost, l.name() + "  %s  %s", 3f, DIM, new Color[] { TEXT, GREEN }, "" + l.needed, sources(l));
        }
        if (why != null && built == null) para(cost, why, null, Misc.getNegativeHighlightColor(), 6f);
        put(content, cost, x + PAD, by + 10f);

        TooltipMakerAPI btn = content.createUIElement(iw, 36f, false);
        ButtonAPI b = button(btn, built != null ? fmt("ui_installed", "" + built) : str("ui_build_level1"),
                "build:" + type.id, Color.WHITE, PRIMARY_BG, iw, 34f, true);
        b.setEnabled(why == null && pending == null);
        put(content, btn, x + PAD, y + h - 48f);
    }

    // ---------------------------------------------------------------- confirmation drawer

    private void buildDrawer(float x, float w) {
        float y = HEIGHT - FOOTER_H - DRAWER_H, iw = w - 2 * PAD;
        Painter dp = new Painter(this);
        CustomPanelAPI d = content.createCustomPanel(w, DRAWER_H, dp);
        dp.fill(0, 0, w, DRAWER_H, PANEL).fill(0, 0, w, 2f, CYAN);
        String shipName = ship.getShipName();
        boolean ok;

        TooltipMakerAPI title = d.createUIElement(iw, 40f, false);
        if (pending == Action.DISMANTLE) {
            Integer level = Stations.read(ship).get(pendingType);
            int lv = level == null ? 0 : level;
            para(title, fmt("ui_drawer_dismantle", pendingType.displayName().toUpperCase(), "" + lv),
                    Fonts.ORBITRON_12, TEXT, 0f);
            para(title, fmt("ui_drawer_on", shipName), null, DIM, 2f);
            put(d, title, PAD, 14f);
            TooltipMakerAPI body = d.createUIElement(iw, 60f, false);
            para(body, str("ui_drawer_no_materials"), null, TEXT, 0f);
            body.addPara(str("ui_refund") + " %s", 6f, TEXT, Misc.getHighlightColor(),
                    Misc.getDGSCredits(StationCost.refund(ship.getHullSpec().getHullSize(), lv)));
            put(d, body, PAD, 62f);
            ok = level != null;
        } else {
            int level = nextLevel(pendingType);
            String head = pending == Action.BUILD
                    ? fmt("ui_drawer_build", pendingType.displayName().toUpperCase())
                    : fmt("ui_drawer_upgrade", pendingType.displayName().toUpperCase(), "" + level);
            para(title, head, Fonts.ORBITRON_12, TEXT, 0f);
            para(title, fmt("ui_drawer_on", shipName), null, DIM, 2f);
            put(d, title, PAD, 14f);

            Payment pay = Payment.plan(market, StationCost.of(ship.getHullSpec().getHullSize(), level));
            float resW = iw * 0.28f, colW = (iw - resW) / 4f, ry = 58f;
            String[] heads = { str("ui_col_needed"), str("ui_col_cargo"), str("ui_col_storage"), str("ui_col_buy") };
            cell(d, str("ui_col_resource"), PAD, ry, resW, DIM, false);
            for (int c = 0; c < 4; c++) cell(d, heads[c], PAD + resW + c * colW, ry, colW, DIM, true);
            ry += 18f;
            cell(d, str("ui_res_credits"), PAD, ry, resW, TEXT, false);
            cell(d, Misc.getDGSCredits(pay.cost.credits), PAD + resW, ry, colW, Misc.getHighlightColor(), true);
            for (int c = 1; c < 4; c++) cell(d, "-", PAD + resW + c * colW, ry, colW, DIM, true);
            for (Payment.Line l : pay.lines) {
                ry += 18f;
                cell(d, l.name(), PAD, ry, resW, TEXT, false);
                cell(d, "" + l.needed, PAD + resW, ry, colW, TEXT, true);
                cell(d, l.fromCargo > 0 ? "" + l.fromCargo : "-", PAD + resW + colW, ry, colW, l.fromCargo > 0 ? TEXT : DIM, true);
                cell(d, l.fromStorage > 0 ? "" + l.fromStorage : "-", PAD + resW + 2 * colW, ry, colW,
                        l.fromStorage > 0 ? TEXT : DIM, true);
                String buy = l.missing > 0 ? fmt("ui_short", "" + l.missing)
                        : l.bought > 0 ? fmt("ui_buy_for", "" + l.bought, Misc.getDGSCredits(l.buyCredits)) : "-";
                Color bc = l.missing > 0 ? Misc.getNegativeHighlightColor() : l.bought > 0 ? Misc.getHighlightColor() : DIM;
                cell(d, buy, PAD + resW + 3 * colW, ry, colW, bc, true);
            }
            boolean rich = Payment.playerCredits() >= pay.totalCredits;
            TooltipMakerAPI total = d.createUIElement(iw - 300f, 22f, false);
            multi(total, str("ui_total") + "  %s  %s", 0f, DIM,
                    new Color[] { rich ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor(), DIM },
                    Misc.getDGSCredits(pay.totalCredits), fmt("ui_of_credits", Misc.getDGSCredits(Payment.playerCredits())));
            put(d, total, PAD, DRAWER_H - 40f);
            ok = pay.canAfford() && whyNot(pendingType, level) == null;
        }
        dp.line(PAD, DRAWER_H - 50f, w - PAD, DRAWER_H - 50f, LINE_DIM);
        TooltipMakerAPI btns = d.createUIElement(290f, 34f, false);
        ButtonAPI no = button(btns, str("ui_cancel"), "cancel", TEXT, GROUND, 120f, 32f, false);
        ButtonAPI yes = button(btns, str("ui_confirm"), "confirm", Misc.getHighlightColor(), CONFIRM_BG, 150f, 32f, true);
        yes.getPosition().rightOfTop(no, 12f);
        yes.setEnabled(ok);
        put(d, btns, w - PAD - 290f, DRAWER_H - 42f);
        content.addComponent(d).inTL(x, y);
    }

    private static void cell(CustomPanelAPI p, String text, float x, float y, float w, Color c, boolean right) {
        TooltipMakerAPI t = p.createUIElement(w, 16f, false);
        LabelAPI l = t.addPara(text, c, 0f);
        if (right) l.setAlignment(Alignment.RMID);
        put(p, t, x, y);
    }

    // ---------------------------------------------------------------- helpers

    private static void put(CustomPanelAPI p, TooltipMakerAPI t, float x, float y) {
        p.addUIElement(t).inTL(x, y);
    }

    /** Plain paragraph (no format pass, so ship names with % are safe), optionally in another font. */
    private static LabelAPI para(TooltipMakerAPI t, String text, String font, Color c, float pad) {
        if (font != null) t.setParaFont(font);
        LabelAPI l = t.addPara(text, c, pad);
        if (font != null) t.setParaFontDefault();
        return l;
    }

    /** Paragraph in {@code base} with each highlight in its own colour. {@code format} must not hold user text. */
    private static LabelAPI multi(TooltipMakerAPI t, String format, float pad, Color base, Color[] hl, String... args) {
        t.setParaFontColor(base);
        LabelAPI l = t.addPara(format, pad, hl, args);
        t.setParaFontColor(Misc.getTextColor());
        return l;
    }

    private static ButtonAPI button(TooltipMakerAPI t, String text, Object data, Color base, Color bg, float w, float h,
            boolean cut) {
        return t.addButton(text, data, base, bg, Alignment.MID, cut ? CutStyle.TL_BR : CutStyle.NONE, w, h, 0f);
    }

    /** "800,000¢, 400 heavy machinery, 1000 metals" for a card footer. */
    private String priceLine(int level) {
        StationCost cost = StationCost.of(ship.getHullSpec().getHullSize(), level);
        StringBuilder sb = new StringBuilder(Misc.getDGSCredits(cost.credits));
        for (Map.Entry<String, Integer> e : cost.commodities.entrySet()) {
            sb.append(", ").append(e.getValue()).append(" ")
                    .append(Global.getSettings().getCommoditySpec(e.getKey()).getName().toLowerCase());
        }
        return sb.toString();
    }

    /** "300,000¢ + materials" for a picker tile. */
    private String shortPrice(int level) {
        StationCost cost = StationCost.of(ship.getHullSpec().getHullSize(), level);
        return cost.commodities.isEmpty() ? Misc.getDGSCredits(cost.credits)
                : fmt("ui_price_short", Misc.getDGSCredits(cost.credits));
    }

    /** "in cargo" / "cargo 120, storage 80, buy 20" for the picker's cost block. */
    private static String sources(Payment.Line l) {
        if (l.fromCargo == l.needed) return str("ui_src_all_cargo");
        List<String> parts = new ArrayList<String>();
        if (l.fromCargo > 0) parts.add(fmt("ui_src_cargo", "" + l.fromCargo));
        if (l.fromStorage > 0) parts.add(fmt("ui_src_storage", "" + l.fromStorage));
        if (l.bought > 0) parts.add(fmt("ui_src_buy", "" + l.bought, Misc.getDGSCredits(l.buyCredits)));
        return String.join(", ", parts);
    }

    private static String categoryOf(StationType type) {
        return str("category_" + type.id);
    }

    private static String categoryName(String category) {
        return str("cat_" + category);
    }

    private static String description(StationType type) {
        return str("desc_" + type.id);
    }

    private static String hullSizeName(FleetMemberAPI m) {
        return str("size_" + m.getHullSpec().getHullSize().name().toLowerCase());
    }

    private static String fmtNum(float v) {
        return v == Math.round(v) ? String.valueOf(Math.round(v)) : String.valueOf(v);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
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
