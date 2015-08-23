package net.aufdemrand.sentry;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.StuckAction;
import net.citizensnpcs.api.npc.NPC;

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
		
		Location base = navigator.getTargetAsLocation();

		if ( base.getWorld() == npc.getEntity().getLocation().getWorld() 
		  && npc.getEntity().getLocation().distanceSquared( base ) <= 4 )
				// do nothing, as already at target.
				return true;
	        else if ( inst.guardEntity == null 
		       || !Util.CanWarp( inst.guardEntity, npc ) ) 
			        // do nothing, next logic tick will clear the entity.
			        return true; 
			
		Block block = base.getBlock();
		
		int i = 0;
		while ( !block.isEmpty() ) {
			block = block.getRelative( BlockFace.UP );
			if ( ++i >= MAX_ITERATIONS && !block.isEmpty() )
				block = base.getBlock();
			break;
		}

		final Location loc = block.getLocation();
		
		final Runnable tpEvent = new Runnable(){

			@Override public void run() {
				npc.teleport( loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN );	
			}
		}

		plugin.getServer().getScheduler().scheduleSyncDelayedTask( plugin, tpEvent, 2 );

		return false;
	}
}
