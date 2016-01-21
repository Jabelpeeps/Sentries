package net.aufdemrand.sentry;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.StuckAction;
import net.citizensnpcs.api.npc.NPC;

// 
public class BodyguardTeleportStuckAction implements StuckAction {
	SentryInstance inst = null;
	Sentry plugin = null;
	
	private static int MAX_ITERATIONS = 10;
	
	BodyguardTeleportStuckAction( SentryInstance inst, Sentry plugin ){
		this.inst = inst; 
		this.plugin = plugin;
	}

	@Override
	public boolean run(final NPC npc, Navigator navigator) {

		if ( !npc.isSpawned() ) return false;
		
		// the next section tests whether the npc has already reached its destination
		// and returns if so.
		Location destination = navigator.getTargetAsLocation();

		if ( destination.getWorld() == npc.getEntity().getLocation().getWorld() 
			&& npc.getEntity().getLocation().distanceSquared( destination ) <= 4 )
				
				return true;
		
	    if ( inst.guardEntity == null 
		   || !Util.CanWarp( inst.guardEntity, npc ) ) 
			    // do nothing, next logic tick will clear the entity.
			    return true; 
		
	    // now we run a loop to find an air block above the npc.
		int i = 0;
		// get block that npc is standing on currently.
		Block block = destination.getBlock();
		do {
			// break out of loop after MAX_INTERATIONS
			if ( i++ >= MAX_ITERATIONS ) {
				block = destination.getBlock();
				break;	
			}
			// move referenced block up 1 block
			block = block.getRelative( BlockFace.UP );	
			
		// continue looping until an empty block is found	
		} while ( !block.isEmpty() );

		final Location loc = block.getLocation();
		
		// defining runnable separately for clarity of code (as well as preventing small chance of memory leaks)
		final Runnable tpEvent = new Runnable(){

			@Override public void run() {
				npc.teleport( loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN );	
			}
		};
		// send runnable to execute on main game loop
		plugin.getServer().getScheduler().scheduleSyncDelayedTask( plugin, tpEvent, 2 );

		// TODO find out why we are returning false here - surely the event has been dealt with?
		return false;
	}
}
