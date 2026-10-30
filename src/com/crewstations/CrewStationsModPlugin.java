package com.crewstations;

import com.crewstations.station.Unlock;
import com.crewstations.unlock.UnlockIntel;
import com.crewstations.unlock.UnlockTracker;
import com.crewstations.util.CSLog;
import com.crewstations.util.CSSettings;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.thoughtworks.xstream.XStream;

/**
 * Entry point for the Crew Stations mod.
 * Registered via "modPlugin" in mod_info.json.
 */
public class CrewStationsModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() throws Exception {
        CSSettings.load();
        CSLog.info("application loaded");
    }

    @Override
    public void onGameLoad(boolean newGame) {
        CSLog.info("game loaded (newGame=" + newGame + ")");
        Unlock.refresh();
        // Transient: re-added at every load, never saved.
        UnlockTracker tracker = new UnlockTracker();
        Global.getSector().addTransientScript(tracker);
        Global.getSector().addTransientListener(tracker);
    }

    @Override
    public void configureXStream(XStream x) {
        // Saved class: never rename the alias.
        x.alias("crst_UnlockIntel", UnlockIntel.class);
    }
}
