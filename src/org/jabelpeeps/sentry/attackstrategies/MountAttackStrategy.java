package org.jabelpeeps.sentry.attackstrategies;


import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.AttackStrategy;

public class MountAttackStrategy implements AttackStrategy {
  	// make the rider attack when in range.
	
	@Override
    	public boolean handle( LivingEntity attacker, LivingEntity bukkitTarget ) {
    		
		if ( attacker == bukkitTarget ) return true;
	
		Entity passenger = attacker.getPassenger();
		
		if ( passenger != null ) {		
			return CitizensAPI.getNPCRegistry()
							  .getNPC( passenger )
							  .getNavigator()
							  .getDefaultParameters()
							  .attackStrategy()
							  .handle( (LivingEntity) passenger, bukkitTarget );
		}
		//I think this does the default attack.
		return false;		
	}
}
