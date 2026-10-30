package com.crewstations.rulecmd;

import java.util.List;
import java.util.Map;

import com.crewstations.shipyard.Shipyard;
import com.crewstations.ui.ShipyardScreen;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

/**
 * rules.csv command (data/campaign/rules.csv):
 *   CrstShipyard isShipyard   condition: the market being visited is a Shipyard
 *   CrstShipyard open         opens the Shipyard screen
 */
public class CrstShipyard extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
            Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || dialog.getInteractionTarget() == null || params.isEmpty()) return false;
        MarketAPI market = dialog.getInteractionTarget().getMarket();
        String action = params.get(0).getString(memoryMap);
        if ("isShipyard".equals(action)) return Shipyard.isShipyard(market);
        if ("open".equals(action)) {
            if (!Shipyard.isShipyard(market)) return false;
            dialog.showCustomVisualDialog(ShipyardScreen.WIDTH, ShipyardScreen.HEIGHT, new ShipyardScreen(market));
            return true;
        }
        return false;
    }
}
