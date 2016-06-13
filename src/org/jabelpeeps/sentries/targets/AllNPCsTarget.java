package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;


public class AllNPCsTarget extends AbstractTargetType {

    protected AllNPCsTarget() { super( 4 ); }

    @Override
    public boolean includes( LivingEntity entity ) {
        if ( entity.hasMetadata( "NPC" ) ) return true;

        return false;
    }
    @Override
    public boolean equals( Object o ) {
        if ( o != null && o instanceof AllNPCsTarget ) return true;
        
        return false;
    }
    @Override
    public int hashCode() {
        return 4;
    }
}
