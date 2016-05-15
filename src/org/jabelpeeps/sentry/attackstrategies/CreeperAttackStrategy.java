package org.jabelpeeps.sentry.attackstrategies;

import org.bukkit.craftbukkit.v1_9_R1.entity.CraftCreeper;
import org.bukkit.entity.LivingEntity;

import net.citizensnpcs.api.ai.AttackStrategy;

public class CreeperAttackStrategy implements AttackStrategy {

    @Override
    public boolean handle( LivingEntity arg0, LivingEntity arg1 ) {

        ((CraftCreeper) arg0).getHandle().a( 1 );
        return true;
    }
}
