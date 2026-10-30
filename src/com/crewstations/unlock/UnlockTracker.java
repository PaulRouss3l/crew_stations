package com.crewstations.unlock;

import com.crewstations.station.StationType;
import com.crewstations.station.Unlock;
import com.crewstations.util.CSSettings;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Watches for the Station unlocks and tells the player through {@link UnlockIntel}: credits and fuel once a
 * day, capital kills after each battle the player fights, and Slay the Sector's progress file once a day (a
 * Run can be won in this very session). Registered as a transient script and listener at every game load,
 * so nothing here is saved; the unlocks themselves are sector memory flags ({@link Unlock#markMet()}).
 */
public class UnlockTracker extends BaseCampaignEventListener implements EveryFrameScript {

    /** Days since the last check; starts full so the first frame after loading checks at once. */
    private float days = 1f;

    public UnlockTracker() {
        super(false);
    }

    @Override
    public void advance(float amount) {
        days += Global.getSector().getClock().convertToDays(amount);
        if (days < 1f) return;
        days = 0f;
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        CargoAPI cargo = fleet.getCargo();
        if (cargo.getCredits().get() >= CSSettings.unlockCredits()) achieved(Unlock.CREDITS);
        if (cargo.getFuel() >= CSSettings.unlockFuel()) achieved(Unlock.FUEL);
        Unlock.refresh();
        if (Unlock.stsWon()) achieved(Unlock.STS_FIRST_WIN);
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (battle == null || !battle.isPlayerInvolved() || Unlock.CAPITAL_KILL.flagged()) return;
        for (CampaignFleetAPI enemy : battle.getNonPlayerSideSnapshot()) {
            for (FleetMemberAPI lost : Misc.getSnapshotMembersLost(enemy)) {
                if (!lost.isStation() && lost.getHullSpec().getHullSize() == HullSize.CAPITAL_SHIP) {
                    achieved(Unlock.CAPITAL_KILL);
                    return;
                }
            }
        }
    }

    /** Records the unlock and posts the company's message, once per save. */
    private static void achieved(Unlock unlock) {
        if (!unlock.markMet()) return;
        for (StationType type : StationType.values()) {
            if (type.unlock == unlock) new UnlockIntel(unlock.name(), type.id).post();
        }
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }
}
