package com.crewstations;

import com.crewstations.util.CSLog;
import com.fs.starfarer.api.BaseModPlugin;

/**
 * Entry point for the Crew Stations mod.
 * Registered via "modPlugin" in mod_info.json.
 */
public class CrewStationsModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() throws Exception {
        CSLog.info("Hello world: application loaded");
    }

    @Override
    public void onGameLoad(boolean newGame) {
        CSLog.info("Hello world: game loaded (newGame=" + newGame + ")");
    }
}
