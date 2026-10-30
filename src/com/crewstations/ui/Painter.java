package com.crewstations.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;

/**
 * Panel plugin that draws flat shapes and sprites for one custom panel, and forwards button clicks.
 * Shapes are queued in panel-local coordinates (top-left origin, y down) while the screen is laid out, then
 * drawn every frame from that list: no per-frame allocation or game queries.
 * "Below" ops draw under the panel's children (backgrounds, frames); "above" ops draw over them.
 */
class Painter implements CustomUIPanelPlugin {

    interface Clicks {
        void onButton(String id);
    }

    private interface Op {
        void draw(float left, float top, float alpha);
    }

    private final Clicks clicks;
    private final List<Op> below = new ArrayList<Op>();
    private final List<Op> above = new ArrayList<Op>();
    private PositionAPI pos;
    /** Set for a panel that is itself clickable (fleet rows, picker tiles); null otherwise. */
    private String clickId;
    /** Area the panel must also be inside to take clicks, e.g. a scrolling list's viewport. */
    private PositionAPI clip;
    private Color hoverColor;
    private boolean hover;

    Painter(Clicks clicks) {
        this.clicks = clicks;
    }

    /** Makes the whole panel one button that sends {@code id}, lit with {@code hoverColor} under the mouse. */
    Painter clickable(String id, PositionAPI clip, Color hoverColor) {
        this.clickId = id;
        this.clip = clip;
        this.hoverColor = hoverColor;
        return this;
    }

    Painter fill(float x, float y, float w, float h, Color c) {
        below.add((l, t, a) -> rect(l + x, t - y - h, w, h, c, a, GL11.GL_QUADS));
        return this;
    }

    Painter border(float x, float y, float w, float h, Color c) {
        below.add((l, t, a) -> rect(l + x + 0.5f, t - y - h + 0.5f, w - 1f, h - 1f, c, a, GL11.GL_LINE_LOOP));
        return this;
    }

    /** Border drawn over the children, e.g. the selection frame of a clickable tile. */
    Painter borderAbove(float x, float y, float w, float h, Color c) {
        above.add((l, t, a) -> rect(l + x + 0.5f, t - y - h + 0.5f, w - 1f, h - 1f, c, a, GL11.GL_LINE_LOOP));
        return this;
    }

    Painter dashed(float x, float y, float w, float h, Color c) {
        below.add((l, t, a) -> {
            float x0 = l + x + 0.5f, y1 = t - y - 0.5f, x1 = x0 + w - 1f, y0 = y1 - h + 1f;
            begin(c, a, GL11.GL_LINES);
            for (float d = 0; d < w; d += 10f) {
                float e = Math.min(d + 5f, w - 1f);
                GL11.glVertex2f(x0 + d, y0); GL11.glVertex2f(x0 + e, y0);
                GL11.glVertex2f(x0 + d, y1); GL11.glVertex2f(x0 + e, y1);
            }
            for (float d = 0; d < h; d += 10f) {
                float e = Math.min(d + 5f, h - 1f);
                GL11.glVertex2f(x0, y0 + d); GL11.glVertex2f(x0, y0 + e);
                GL11.glVertex2f(x1, y0 + d); GL11.glVertex2f(x1, y0 + e);
            }
            end();
        });
        return this;
    }

    /** Square grid lines, the backdrop of ship and Station art. */
    Painter grid(float x, float y, float w, float h, float step, Color c) {
        below.add((l, t, a) -> {
            begin(c, a, GL11.GL_LINES);
            for (float d = step; d < w; d += step) {
                GL11.glVertex2f(l + x + d + 0.5f, t - y); GL11.glVertex2f(l + x + d + 0.5f, t - y - h);
            }
            for (float d = step; d < h; d += step) {
                GL11.glVertex2f(l + x, t - y - d - 0.5f); GL11.glVertex2f(l + x + w, t - y - d - 0.5f);
            }
            end();
        });
        return this;
    }

    Painter line(float x1, float y1, float x2, float y2, Color c) {
        below.add((l, t, a) -> {
            begin(c, a, GL11.GL_LINES);
            GL11.glVertex2f(l + x1 + 0.5f, t - y1 - 0.5f);
            GL11.glVertex2f(l + x2 + 0.5f, t - y2 - 0.5f);
            end();
        });
        return this;
    }

    /** Sprite scaled to fit the box, aspect kept, centred. {@code tint} null = full colour. */
    Painter sprite(String path, float x, float y, float w, float h, Color tint, boolean overChildren) {
        if (path == null) return this;
        SpriteAPI s = Global.getSettings().getSprite(path);
        if (s == null || s.getWidth() <= 0 || s.getHeight() <= 0) return this;
        float scale = Math.min(w / s.getWidth(), h / s.getHeight());
        float sw = s.getWidth() * scale, sh = s.getHeight() * scale;
        Op op = (l, t, a) -> {
            s.setSize(sw, sh);
            s.setColor(tint != null ? tint : Color.WHITE);
            s.setAlphaMult(a);
            s.renderAtCenter(l + x + w / 2f, t - y - h / 2f);
        };
        (overChildren ? above : below).add(op);
        return this;
    }

    /** Area the panel must also be inside to take clicks (set once the list holding it is placed). */
    void clip(PositionAPI area) {
        this.clip = area;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.pos = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
        draw(below, alphaMult);
        if (hover && hoverColor != null && pos != null) {
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            rect(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(), hoverColor, alphaMult, GL11.GL_QUADS);
            GL11.glPopAttrib();
        }
    }

    @Override
    public void render(float alphaMult) {
        draw(above, alphaMult);
    }

    private void draw(List<Op> ops, float alphaMult) {
        if (pos == null || ops.isEmpty()) return;
        float left = pos.getX(), top = pos.getY() + pos.getHeight();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(1f);
        for (Op op : ops) op.draw(left, top, alphaMult);
        GL11.glPopAttrib();
    }

    private static void rect(float x, float y, float w, float h, Color c, float alpha, int mode) {
        begin(c, alpha, mode);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x, y + h);
        end();
    }

    private static void begin(Color c, float alpha, int mode) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f * alpha);
        GL11.glBegin(mode);
    }

    private static void end() {
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    @Override
    public void advance(float amount) {}

    @Override
    public void processInput(List<InputEventAPI> events) {
        for (InputEventAPI e : events) handle(e);
    }

    /**
     * Hover and click for a clickable panel. Also called by the screen's root plugin, in case nested panels do
     * not get input of their own; the first caller consumes the click, so it fires once.
     */
    void handle(InputEventAPI e) {
        if (clickId == null || pos == null) return;
        boolean inside = pos.containsEvent(e) && (clip == null || clip.containsEvent(e));
        if (e.isMouseMoveEvent()) hover = inside;
        if (e.isConsumed()) return;
        if (inside && e.isLMBDownEvent()) {
            e.consume();
            Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 1f);
            if (clicks != null) clicks.onButton(clickId);
        }
    }

    @Override
    public void buttonPressed(Object id) {
        if (clicks != null) clicks.onButton(String.valueOf(id));
    }
}
