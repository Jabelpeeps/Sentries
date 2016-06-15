package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;


public class AllMonstersTarget extends AbstractTargetType {

    AllMonstersTarget() { super( 3 ); }

    @Override
    public boolean includes( LivingEntity entity ) {
        if ( entity instanceof Monster ) return true;

        return false;
    }
    @Override
    public String getTargetString() { 
        return "MONSTER:ALL"; 
    }
    
    @Override
    public boolean equals( Object o ) {
        if ( o != null && o instanceof AllMonstersTarget ) return true;

        return false;
    }
    @Override
    public int hashCode() {
        return 2;
    }
}
