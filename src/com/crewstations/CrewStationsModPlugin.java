package com.crewstations;

import com.crewstations.station.Unlock;
import com.crewstations.util.CSLog;
import com.crewstations.util.CSSettings;
import com.fs.starfarer.api.BaseModPlugin;

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
    }
}
