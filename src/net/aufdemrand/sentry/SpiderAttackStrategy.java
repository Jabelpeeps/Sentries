package net.aufdemrand.sentry;

import java.util.Random;

import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;

import net.minecraft.server.v1_8_R3.Entity;

public class SpiderAttackStrategy implements net.citizensnpcs.api.ai.AttackStrategy {
	
	private Random random = new Random();
	Sentry sentry = null;

	public SpiderAttackStrategy (Sentry plugin) {
		sentry = plugin;
	}

	@Override
	public boolean handle (LivingEntity attacker, LivingEntity target) {

		if ( Sentry.debug ) Sentry.debugLog( "Spider ATTACK!" );

		Entity entity = ( (CraftEntity) target ).getHandle();
		Entity me = ( (CraftEntity) attacker ).getHandle();

		if ( random.nextInt( 20 ) == 0 ) {
			
				double dX = entity.locX - me.locX;
				double dZ = entity.locZ - me.locZ;
				double straightDistance = Math.sqrt( dX * dX + dZ * dZ );
				
			// TODO consider pulling out the double values as constants
			// (at first glance they look like multiple of the same number in any case.)
				me.motX = dX / straightDistance * 0.5D * 0.800000011920929D + me.motX * 0.20000000298023224D;
				me.motZ = dZ / straightDistance * 0.5D * 0.800000011920929D + me.motZ * 0.20000000298023224D;
				me.motY = 0.4000000059604645D;
		}
		return false;
	}
}
