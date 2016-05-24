package org.jabelpeeps.sentries.attackstrategies;

import java.util.Random;

import org.bukkit.entity.LivingEntity;
import org.jabelpeeps.sentries.Sentries;

import net.citizensnpcs.api.ai.AttackStrategy;
import net.citizensnpcs.util.NMS;

public class SpiderAttackStrategy implements AttackStrategy {

    private Random random = new Random();
    // Sentries sentry;

    public SpiderAttackStrategy() { // Sentries plugin ) {
        // sentry = plugin;
    }

    @Override
    public boolean handle( LivingEntity attacker, LivingEntity target ) {

        if ( Sentries.debug )
            Sentries.debugLog( "Spider ATTACK!" );

        if ( random.nextInt( 20 ) == 0 ) {

            double spiderX = attacker.getLocation().getX();
            double spiderZ = attacker.getLocation().getZ();

            double targetX = target.getLocation().getX();
            double targetZ = target.getLocation().getZ();

            double diffX = targetX - spiderX;
            double diffZ = targetZ - spiderZ;
            double straightDistance = Math.sqrt( diffX * diffX + diffZ * diffZ );

            NMS.getHandle( attacker ).motX = diffX / straightDistance * 0.5D * 0.800000011920929D
                                                + NMS.getHandle( attacker ).motX * 0.20000000298023224D;
            NMS.getHandle( attacker ).motZ = diffZ / straightDistance * 0.5D * 0.800000011920929D
                                                + NMS.getHandle( attacker ).motZ * 0.20000000298023224D;
            NMS.getHandle( attacker ).motY = 0.4000000059604645D;
        }
        return false;
    }
}
