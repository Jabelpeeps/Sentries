package org.jabelpeeps.sentries.pluginbridges;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jabelpeeps.sentries.PluginBridge;
import org.jabelpeeps.sentries.Sentries;
import org.jabelpeeps.sentries.SentryListener;

import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;

import lombok.Getter;

public class CrackShotBridge implements PluginBridge, Listener {

    @Getter final String activationMessage = "CrackSot detected, extra events listener added for custom weapons.";
    
    @Override
    public boolean activate() {
        Bukkit.getPluginManager().registerEvents( this, Sentries.plugin );
        return true;
    }

    @EventHandler( priority = EventPriority.MONITOR )
    public void weaponDamage( WeaponDamageEntityEvent ev ) {
              
//        SentryTrait inst = Util.getSentryTrait( ev.getVictim() );
//        
//        if ( inst == null ) return;
//        
//        if ( System.currentTimeMillis() < inst.okToTakedamage + 500 ) return;
//        inst.okToTakedamage = System.currentTimeMillis();
       
        Entity damager = ev.getDamager();
        if ( damager == null || !(damager instanceof LivingEntity ) ) return;
        
        Entity victim = ev.getVictim();
        if ( !(victim instanceof LivingEntity ) ) return;
        
        double damage = ev.getDamage();
        
        SentryListener.processEventForTargets( (LivingEntity) damager, (LivingEntity) victim, damage );
    }
}
