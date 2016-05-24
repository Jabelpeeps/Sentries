package org.jabelpeeps.sentries;

import org.bukkit.Location;

import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.StuckAction;
import net.citizensnpcs.api.npc.NPC;

class GiveUpStuckAction implements StuckAction {

    SentryTrait inst;

    GiveUpStuckAction( SentryTrait _inst ) {
        inst = _inst;
    }

    @Override
    public boolean run( NPC npc, Navigator navigator ) {

        if ( !npc.isSpawned() )
            return false;

        Location target = navigator.getTargetAsLocation();
        Location present = npc.getEntity().getLocation();

        if ( target.getWorld() == present.getWorld()
                && present.distanceSquared( target ) <= 4 ) {
            return true;
        }
        inst.clearTarget();
        return false;
    }
}
