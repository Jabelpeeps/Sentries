package org.jabelpeeps.sentries.attackstrategies;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jabelpeeps.sentries.Sentries;

import net.citizensnpcs.api.ai.AttackStrategy;

public class MountAttackStrategy implements AttackStrategy {
    // make the rider attack when in range.

    @Override
    public boolean handle( LivingEntity attacker, LivingEntity bukkitTarget ) {

        if ( attacker == bukkitTarget )
            return true;

        Entity passenger = attacker.getPassenger();

        if ( passenger != null ) {
            return Sentries.registry.getNPC( passenger )
                                    .getNavigator()
                                    .getLocalParameters()
                                    .attackStrategy()
                                    .handle( (LivingEntity) passenger, bukkitTarget );
        }
        // I think this does the default attack.
        return false;
    }
}
