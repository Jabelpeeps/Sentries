package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;


public class AllNPCsTarget extends AbstractTargetType implements TargetType.Internal {

    public AllNPCsTarget() { 
        super( 4 );
        targetString = "All:NPCs";
    }
    @Override
    public boolean includes( LivingEntity entity ) {
        return entity.hasMetadata( "NPC" );
    } 
    @Override
    public boolean equals( Object o ) { 
        return o != null && o instanceof AllNPCsTarget;
    }
}
