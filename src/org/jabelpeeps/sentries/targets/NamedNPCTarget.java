package org.jabelpeeps.sentries.targets;

import java.util.UUID;

import org.bukkit.entity.LivingEntity;
import org.jabelpeeps.sentries.Sentries;


public class NamedNPCTarget extends AbstractTargetType implements TargetType.Internal {

    private final UUID uuid;
    
    public NamedNPCTarget( UUID NPCuuid ) { 
        super( 20 ); 
        uuid = NPCuuid;
    }
    @Override
    public boolean includes( LivingEntity entity ) {
        return  entity.hasMetadata( "NPC" ) 
                && uuid.equals( entity.getUniqueId() );
    }
    @Override
    public String getTargetString() { 
        return String.join( ":", "Named", "NPC", uuid.toString() ); 
    } 
    @Override
    public String getPrettyString() { 
        return String.join( ":", "Named", "NPC", Sentries.registry.getByUniqueId( uuid ).getName() ); 
    }   
    @Override
    public boolean equals( Object o ) {
        return  o != null 
                && o instanceof NamedNPCTarget 
                && uuid.equals( ((NamedNPCTarget) o).uuid );
    }
    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
