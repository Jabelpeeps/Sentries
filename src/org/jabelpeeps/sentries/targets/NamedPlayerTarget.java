package org.jabelpeeps.sentries.targets;

import java.util.UUID;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


public class NamedPlayerTarget extends AbstractTargetType {

    private UUID uuid;
    
    protected NamedPlayerTarget() { 
        super( 10 ); 
    }

    @Override
    public boolean includes( LivingEntity entity ) {
        if (  !(entity instanceof Player) || entity.hasMetadata( "NPC" ) ) return false;
        
        if ( uuid.equals( entity.getUniqueId() ) ) return true;
               
        return false;
    }

    @Override
    public void setTargetString( String target ) { 
        uuid = UUID.fromString( target );
    }
    
    @Override
    public String getTargetString() { 
        return String.join( ":", "Player", uuid.toString() ); 
    }
    @Override
    public boolean equals( Object o ) {
        if (    o != null 
                && o instanceof NamedPlayerTarget 
                && uuid.equals( ((NamedPlayerTarget) o).uuid ) )
            return true;

        return false;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

}
