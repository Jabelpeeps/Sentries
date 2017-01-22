package org.jabelpeeps.sentries;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TNTPrimed;

abstract class ThrownTNT {
    
    static Map<TNTPrimed, LivingEntity> thrown = new HashMap<>();
    
    static LivingEntity getThrower( TNTPrimed tnt ) {        
        return thrown.get( tnt ); 
    }
    
    static void addTNT( TNTPrimed tnt, LivingEntity thrower ) {
        thrown.put( tnt, thrower );
        
        Bukkit.getScheduler()
              .runTaskLaterAsynchronously( Sentries.plugin, () -> thrown.remove( tnt ), tnt.getFuseTicks() + 10 );
    }
}
