package org.jabelpeeps.sentries.pluginbridges;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jabelpeeps.sentries.PluginBridge;
import org.jabelpeeps.sentries.Sentries;
import org.jabelpeeps.sentries.SentryListener;

import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;

import lombok.Getter;

public class CrackShotBridge implements PluginBridge, Listener {

    @Getter final String activationMessage = "CrackShot detected, integrations added for custom weapons.";
    
    @Override
    public boolean activate() {
        Bukkit.getPluginManager().registerEvents( this, Sentries.plugin );
        return true;
    }

    @EventHandler( priority = EventPriority.MONITOR )
    public void weaponDamage( WeaponDamageEntityEvent ev ) {
       
        Entity damager = ev.getDamager();
        if ( damager == null || !(damager instanceof LivingEntity ) ) return;
        
        Entity victim = ev.getVictim();
        if ( !(victim instanceof LivingEntity ) ) return;
        
        double damage = ev.getDamage();
        

        SentryListener.processEventForTargets( (LivingEntity) damager, (LivingEntity) victim, damage );
    }
    
    static class CrackShotCommand implements SentriesComplexCommand {

        @Getter private String shortHelp = "arm a sentry with CrackShot";
        @Getter private String perm = "sentries.crackshot";
        private CSUtility crack = new CSUtility();
        private String helpText;

        @Override
        public String getLongHelp() {
            if ( helpText == null ) {
                StringJoiner joiner = new StringJoiner( System.lineSeparator() );
                
                joiner.add( "Allows sentries to carry the weapons provided by CrackShot" );

                joiner.add( Utils.join( "do ", Col.GOLD, "/sentry crackshot ", 
                                               Col.RESET, "to see if a sentry is already carrying a CrackShot weapon." ) );
                joiner.add( Utils.join( "do ", Col.GOLD, "/shot list ", Col.RESET, "to see the list of weapons in CrackShot." ) );
                joiner.add( Utils.join( "do ", Col.GOLD, "/sentry crackshot <WeaponName> ", 
                                               Col.RESET, "to give your choosen weapon to a sentry" ) );
                joiner.add( Utils.join( "do ", Col.GOLD, "/sentry crackshot clear", 
                                               Col.RESET, "to remove the current Crackshot weapon" ) );
                joiner.add( Utils.join( Col.YELLOW, "Note: ", Col.RESET, 
                        "As CrackShot handles the damage from its weapons, the strength value for this sentry will be ignored." ) );
                
                helpText = joiner.toString();
            }
            return helpText;
        }

        @Override
        public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {

            if ( inst.getNPC().getTrait( MobType.class ).getType() != EntityType.PLAYER ) {
                Utils.sendMessage( sender, S.ERROR, " Only Player-type NPC's can carry Crackshot weapons." );
                return;
            }
            
            Equipment equip = inst.getNPC().getTrait( Equipment.class );          
            ItemStack item = equip.get( EquipmentSlot.HAND );
            String weapon = null;            
            
            if ( item != null ) weapon = crack.getWeaponTitle( item );
            
            if ( args.length <= 1 + nextArg ) {
                
                if ( weapon != null )
                    Utils.sendMessage( sender, Col.YELLOW, npcName, " is armed with a ", weapon );
                else
                    Utils.sendMessage( sender, Col.RED, npcName, " is not carrying a CrackShot weapon." );
                
                return;
            }
            
            if ( S.CLEAR.equalsIgnoreCase( args[nextArg + 1] ) ) {
                if ( weapon != null ) {
                    equip.set( EquipmentSlot.HAND, null );
                    Utils.sendMessage( sender, Col.GREEN, weapon, " removed from ", npcName );
                }
                else
                    Utils.sendMessage( sender, Col.RED, npcName, " is not carrying a CrackShot weapon." );
                
                inst.updateAttackType();
                return;             
            }

            ConfigurationSection config = crack.getHandle().weaponConfig.getConfigurationSection( args[nextArg + 1] );
            
            if ( config != null ) {
                inst.setMyAttack( new CrackShotAttack( config ) );            
                item = crack.generateWeapon( args[nextArg + 1] );
                equip.set( EquipmentSlot.HAND, item );
            }
            else
                Utils.sendMessage( sender, S.ERROR, "Weapon name not recognised." );
        }
    }
    
    static class CrackShotAttack implements SentryAttack {

        @Getter private String name = "CrackShot:";
        private CSUtility crack = new CSUtility();
        private ConfigurationSection config;
        private Weapons weapon;
        private Class<? extends Entity> projectile;
       
        CrackShotAttack( ConfigurationSection conf ) {
            config = conf;
            name += conf.getName();
            weapon = Weapons.valueOf( conf.getString( "Shooting.Projectile_Type" ).toUpperCase() );
            projectile = weapon.getProjectile();
        }
       
        @Override
        public boolean handle( LivingEntity arg0, LivingEntity arg1 ) {
            
            if ( projectile == null ) return false;
            
            Entity proj = arg0.getWorld().spawn( arg0.getEyeLocation(), projectile );

            crack.setProjectile( (Player) arg0, (Projectile) proj, config.getName() );
            crack.getHandle().callShootEvent( (Player) arg0, proj, config.getName() );
            
            // crack.spawnMine( player, loc, weaponTitle );
            // crack.generateExplosion( player, loc, weaponTitle );
            // crack.getHandle().launchGrenade( player, parent_node, delay, vel, splitLoc, cTimes );
            // crack.getHandle().fireProjectile( player, parentNode, leftClick );
            // crack.getHandle().csminion.callAirstrike( mark, parent_node, player );
            // crack.getHandle().csminion.weaponInteraction( shooter, parent_node, leftClick );
            return true;
        }
        
        // included to complete SentryAttack implementation, although the returned value will not be used. 
        @Override public int getDamage() { return 0; }
        // TODO find out how CrackShot records the range of its weapons.
        @Override public double getApproxRange() { return 32; }
    }
    
    enum Weapons {
        SNOWBALL, EGG, ARROW, WITHERSKULL, FIREBALL, GRENADE, FLARE, ENERGY, SPLASH;
        
        protected Class<? extends Entity> getProjectile() { 
            switch ( this ) {
                case ARROW: return Arrow.class;
                case EGG: return Egg.class;
                case FIREBALL: return LargeFireball.class;
                case SNOWBALL: return Snowball.class;
                case WITHERSKULL: return WitherSkull.class;
                default: return null;
            }
        }
    }
}
