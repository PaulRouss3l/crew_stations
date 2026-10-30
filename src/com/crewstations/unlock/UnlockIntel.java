package com.crewstations.unlock;

import java.awt.Color;
import java.util.Set;

import com.crewstations.station.StationType;
import com.crewstations.util.CSIds;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * A message from Station Yard Inc.: a Station was unlocked and is now in The Catalogue. Posted with a
 * notification, then fades out of the Intel screen after {@link #DAYS_SHOWN} days. Saved with the game, so it
 * holds ids only (alias in CrewStationsModPlugin.configureXStream; never rename).
 */
public class UnlockIntel extends BaseIntelPlugin {

    /** Days the message stays in the Intel screen. */
    private static final float DAYS_SHOWN = 3f;
    public static final String TAG = "Station Yard Inc.";

    /** {@link com.crewstations.station.Unlock} name, picks the letter text. */
    private final String unlock;
    private final String stationId;

    public UnlockIntel(String unlock, String stationId) {
        this.unlock = unlock;
        this.stationId = stationId;
    }

    public void post() {
        Global.getSector().getIntelManager().addIntel(this, false);
        endAfterDelay(DAYS_SHOWN);
    }

    private StationType type() {
        return StationType.byId(stationId);
    }

    private String stationName() {
        StationType t = type();
        return t == null ? stationId : t.displayName();
    }

    @Override
    protected String getName() {
        return String.format(str("intel_title"), stationName());
    }

    @Override
    protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {
        info.addPara(str("intel_bullet"), tc, initPad);
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        StationType t = type();
        if (t != null) info.addImage(t.sprite(), 128f, 128f, 0f);
        info.addPara(str("intel_" + unlock.toLowerCase()), 10f);
        info.addPara(str("intel_where"), Misc.getHighlightColor(), 10f);
        info.addPara(str("intel_signoff"), Misc.getGrayColor(), 10f);
    }

    @Override
    public String getIcon() {
        StationType t = type();
        return t != null ? t.sprite() : "graphics/crew_stations/hullmods/crew_stations.png";
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(TAG);
        return tags;
    }

    private static String str(String key) {
        return Global.getSettings().getString(CSIds.STRINGS, key);
    }
}
