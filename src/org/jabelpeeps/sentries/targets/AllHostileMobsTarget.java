package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;


public class AllHostileMobsTarget extends AbstractTargetType {

    AllHostileMobsTarget() { super( 3 ); }

    @Override
    public boolean includes( LivingEntity entity ) {
        if ( entity instanceof Monster ) return true;

        return false;
    }
    @Override
    public boolean equals( Object o ) {
        if ( o != null && o instanceof AllHostileMobsTarget ) return true;

        return false;
    }
    @Override
    public int hashCode() {
        return 2;
    }
}
