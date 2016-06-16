package org.jabelpeeps.sentries.targets;

import java.util.UUID;

import org.bukkit.entity.LivingEntity;


public class NamedNPCTarget extends AbstractTargetType {

    private UUID uuid;
    
    protected NamedNPCTarget() { super( 20 ); }

    @Override
    public boolean includes( LivingEntity entity ) {
        if ( !entity.hasMetadata( "NPC" ) ) return false;
        
        if ( uuid.equals( entity.getUniqueId() ) ) return true;
        
        return false;
    }

    @Override
    public void setTargetString( String NPCuuid ) { 
        uuid = UUID.fromString( NPCuuid );
    }
    
    @Override
    public String getTargetString() { 
        return String.join( ":", "NPC", uuid.toString() ); 
    }
    
    @Override
    public boolean equals( Object o ) {
        if (    o != null 
                && o instanceof NamedNPCTarget 
                && uuid.equals( ((NamedNPCTarget) o).uuid ) ) return true;

        return false;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
