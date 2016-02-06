package net.aufdemrand.sentry;

import org.bukkit.Location;

import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.StuckAction;
import net.citizensnpcs.api.npc.NPC;

public class GiveUpStuckAction implements StuckAction {
	
	SentryInstance inst;
	
	GiveUpStuckAction( SentryInstance _inst ){
		inst = _inst; 
	}

	@Override
	public boolean run( NPC npc, Navigator navigator ) {
		// inst.plugin.getServer().broadcastMessage("give up stuck action");
		
		if ( !npc.isSpawned() ) return false;
		
        	Location base = navigator.getTargetAsLocation();
        
        	if ( base.getWorld() == npc.getEntity().getLocation().getWorld() 
        	  && npc.getEntity().getLocation().distanceSquared( base ) <= 4 ) {
                		return true;
        	}	
		inst.clearTarget();
		return false;
	}
}
