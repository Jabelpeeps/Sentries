package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;


public class MobTypeTarget extends AbstractTargetType {

    EntityType type;
    
    protected MobTypeTarget() { 
        super( 15 );
    }

    @Override
    public boolean includes( LivingEntity entity ) {
        if ( !(entity instanceof Creature ) ) return false;
        
        if ( entity.getType().equals( type ) ) return true;
        
        return false;
    }

    @Override
    public void setTargetString( String target ) {         
        type = EntityType.valueOf( target );
    }
    
    @Override
    public String getTargetString() { 
        return String.join( ":", "MobType", type.toString() ); 
    }
    
    @Override
    public boolean equals( Object o ) {
        if (    o != null 
                && o instanceof MobTypeTarget 
                && type.equals( ((MobTypeTarget) o).type ) )
            return true;

        return false;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

}
