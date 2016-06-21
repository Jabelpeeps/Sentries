package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;


public class AllNPCsTarget extends AbstractTargetType implements TargetType.Internal {

    public AllNPCsTarget() { super( 4 ); }

    @Override
    public boolean includes( LivingEntity entity ) {
        return entity.hasMetadata( "NPC" );
    }  
    @Override
    public String getTargetString() { 
        return "All:NPCs"; 
    } 
    @Override
    public boolean equals( Object o ) { 
        return o != null && o instanceof AllNPCsTarget;
    }
    @Override
    public int hashCode() {
        return 4;
    }
}
