package org.jabelpeeps.sentries.pluginbridges;

import java.util.StringJoiner;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jabelpeeps.sentries.CommandHandler;
import org.jabelpeeps.sentries.PluginBridge;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.Sentries;
import org.jabelpeeps.sentries.SentryAttack;
import org.jabelpeeps.sentries.SentryListener;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;
import org.jabelpeeps.sentries.commands.SentriesComplexCommand;

import com.shampaggon.crackshot.CSUtility;
import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;
import net.citizensnpcs.api.trait.trait.MobType;

public class CrackShotBridge implements PluginBridge, Listener {

    @Getter final String activationMessage = "CrackShot detected, extra event listener added for custom weapons.";
    
    @Override
    public boolean activate() {
        CommandHandler.addCommand( "crackshot", new CrackShotCommand() );
        Bukkit.getPluginManager().registerEvents( this, Sentries.plugin );
        return true;
    }

    @EventHandler( priority = EventPriority.MONITOR )
    public void weaponDamage( WeaponDamageEntityEvent ev ) {
       
        Player player = ev.getPlayer();
        
        if ( player == null ) {
            player = (Player) Utils.getSource( ev.getDamager() );
            if ( player == null ) return;
        }
        
        Entity victim = ev.getVictim();
        if ( !(victim instanceof LivingEntity ) ) return;
        
        double damage = ev.getDamage();
        
        SentryListener.processEventForTargets( player, (LivingEntity) victim, damage );
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

        @Getter private String name = "CrackShot - ";
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
            Entity proj = arg0.getWorld().spawn( arg0.getEyeLocation(), projectile );
            
            crack.getHandle().callShootEvent( (Player) arg0, proj, config.getName() );
            
            return true;
        }
        
        // included to complete SentryAttack implementation, although the returned value will not be used. 
        @Override public int getDamage() { return 0; }
        // TODO find out how CrackShot records the range of its weapons.
        @Override public double getApproxRange() { return 32; }
    }
    
    @AllArgsConstructor
    enum Weapons {
        SNOWBALL( false, EntityType.SNOWBALL ), 
        EGG( false, EntityType.EGG ), 
        ARROW( false, EntityType.ARROW ),  
        WITHERSKULL( false, EntityType.WITHER_SKULL ), 
        GRENADE( true, null ), 
        FLARE( true, null ), 
        FIREBALL( false, EntityType.FIREBALL ),
        ENERGY( true, null ), 
        SPLASH( true, null );

        final boolean hasSubType;
        final EntityType projectile;
        
        protected Class<? extends Entity> getProjectile() {  
            // TODO add code to implement the attacks with configurable entities.
            return projectile.getEntityClass();
        }
    }
}
