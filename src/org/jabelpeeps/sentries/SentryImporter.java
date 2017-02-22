package org.jabelpeeps.sentries;

import org.jabelpeeps.sentries.targets.AllEntitiesTarget;
import org.jabelpeeps.sentries.targets.AllMonstersTarget;
import org.jabelpeeps.sentries.targets.AllNPCsTarget;
import org.jabelpeeps.sentries.targets.AllPlayersTarget;
import org.jabelpeeps.sentries.targets.OwnerTarget;

import net.aufdemrand.sentry.SentryInstance;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;

public class SentryImporter {
        
    public static int importAll() {       
        int convertedCount = 0;
        
        for ( NPC npc : CitizensAPI.getNPCRegistry() ) {
            if ( importNPC( npc ) )
                convertedCount++;
        }
        return convertedCount;
    }
     
    public static boolean importNPC( NPC npc ) {
        if ( !npc.hasTrait( net.aufdemrand.sentry.SentryTrait.class ) ) return false;
        
        SentryInstance sentry = npc.getTrait( net.aufdemrand.sentry.SentryTrait.class ).getInstance();
        if ( sentry == null ) return false;

        SentryTrait newSentry = npc.getTrait( SentryTrait.class );
        
        newSentry.armour = sentry.Armor * 0.1;
        newSentry.attackRate = sentry.AttackRateSeconds;
        
        if ( newSentry.attackRate < 1 ) newSentry.attackRate = 1;
        else if ( newSentry.attackRate > 30 ) newSentry.attackRate = 30;
        
        newSentry.range = sentry.sentryRange;
        newSentry.strength = sentry.Strength;
        newSentry.killsDrop = sentry.KillsDropInventory;
        newSentry.warningMsg = sentry.WarningMessage;
        newSentry.greetingMsg = sentry.GreetingMessage;
        newSentry.acceptsCriticals = sentry.LuckyHits;
        newSentry.iRetaliate = sentry.Retaliate;
        newSentry.healRate = sentry.HealRate;   
        newSentry.setHealth( sentry.sentryHealth );
        newSentry.invincible = sentry.Invincible;
        newSentry.range = sentry.sentryRange;
        newSentry.respawnDelay = sentry.RespawnDelaySeconds;
        newSentry.spawnLocation = sentry.Spawn;
        newSentry.dropInventory = sentry.DropInventory;
        newSentry.followDistance = sentry.FollowDistance;
        newSentry.ignoreLOS = sentry.IgnoreLOS;
        newSentry.nightVision = sentry.NightVision;
        newSentry.speed = sentry.sentrySpeed;
        newSentry.mountID = sentry.MountID;
        newSentry.weight = sentry.sentryWeight;
        newSentry.voiceRange = sentry.WarningRange;
        newSentry.weaponSpecialEffects = sentry.potionEffects;
        
        if ( sentry.guardTarget != null && !sentry.guardTarget.isEmpty() ) {
            newSentry.guardeeName = sentry.guardTarget;
        }
        // Import targets
        for ( String t : sentry.validTargets ) {
            if ( t.contains( "ENTITY:ALL" ) )
                newSentry.targets.add( new AllEntitiesTarget() );
            else if ( t.contains( "ENTITY:MONSTER" ) )
                newSentry.targets.add( new AllMonstersTarget() );
            else if ( t.contains( "ENTITY:PLAYER" ) )
                newSentry.targets.add( new AllPlayersTarget() );
            else if ( t.contains( "ENTITY:NPC" ) )
                newSentry.targets.add( new AllNPCsTarget() );
            else {
                String[] sections = Utils.colon.split( t );
                if ( sections.length != 2 ) continue;
                    
                sections[0] = sections[0].trim();
                sections[1] = sections[1].trim();
               
                if ( sections[0].equals( "NPC" ) )
                    CommandHandler.callCommand( newSentry, "target", "add", "named:npc:" + sections[1] );
                else if ( sections[0].equals( "PLAYER" ) ) 
                    CommandHandler.callCommand( newSentry, "target", "add", "named:player:" + sections[1] );
                else if ( sections[0].equals( "ENTITY" ) ) 
                    CommandHandler.callCommand( newSentry, "target", "add", "mobtype:" + sections[1] );
                else if ( sections[0].equals( "GROUP" ) ) 
                    CommandHandler.callCommand( newSentry, "group", "target", sections[1]  ); 
                else if ( sections[0].equals( "EVENT" ) ) 
                    CommandHandler.callCommand( newSentry, "event", "add", sections[1] );
                else if ( sections[0].equals( "FACTION" ) ) 
                    CommandHandler.callCommand( newSentry, "faction", "target", sections[1] );
                else if ( sections[0].equals( "FACTIONENEMIES" ) )
                    CommandHandler.callCommand( newSentry, "faction", "join", sections[1] );
                else if ( sections[0].equals( "TOWN" ) ) 
                    CommandHandler.callCommand( newSentry, "towny", "target", sections[1] );
                else if ( sections[0].equals( "WARTEAM" ) )
                    CommandHandler.callCommand( newSentry, "war", "target", sections[1] ); 
                else if ( sections[0].equals( "TEAM" ) ) 
                    CommandHandler.callCommand( newSentry, "scoreboard", "target", sections[1] );
                else if ( sections[0].equals( "CLAN" ) ) 
                    CommandHandler.callCommand( newSentry, "clan", "target", sections[1] );
                else 
                    Sentries.logger.info( "[Sentries] NPC:" + npc.getName() + ". Target could not be imported:- " + t );
            }
        }        
        // import ignores
        for ( String t : sentry.ignoreTargets ) {
            if ( t.contains( "ENTITY:ALL" ) )
                newSentry.ignores.add( new AllEntitiesTarget() );
            else if ( t.contains( "ENTITY:MONSTER" ) )
                newSentry.ignores.add( new AllMonstersTarget() );
            else if ( t.contains( "ENTITY:PLAYER" ) )
                newSentry.ignores.add( new AllPlayersTarget() );
            else if ( t.contains( "ENTITY:NPC" ) )
                newSentry.ignores.add( new AllNPCsTarget() );
            else if ( t.contains( "ENTITY:OWNER" ) )
                newSentry.ignores.add( new OwnerTarget( npc.getTrait( Owner.class ) ) );
            else {
                String[] sections = Utils.colon.split( t );
                if ( sections.length != 2 ) continue;

                sections[0] = sections[0].trim();
                sections[1] = sections[1].trim();
                
                if ( sections[0].equals( "NPC" ) )
                    CommandHandler.callCommand( newSentry, "ignore", "add", "named:npc:" + sections[1] );
                else if ( sections[0].equals( "PLAYER" ) )
                    CommandHandler.callCommand( newSentry, "ignore", "add", "named:player:" + sections[1] );
                else if ( sections[0].equals( "ENTITY" ) )
                    CommandHandler.callCommand( newSentry, "ignore", "add", "mobtype:" + sections[1] );
                else if ( sections[0].equals( "GROUP" ) )
                    CommandHandler.callCommand( newSentry, "group", "ignore", sections[1] ); 
                else if ( sections[0].equals( "FACTION" ) )
                    CommandHandler.callCommand( newSentry, "faction", "ignore", sections[1] );
                else if ( sections[0].equals( "TOWN" ) )
                    CommandHandler.callCommand( newSentry, "towny", "ignore", sections[1] );
                else if ( sections[0].equals( "WARTEAM" ) )
                    CommandHandler.callCommand( newSentry, "war", "ignore", sections[1] );
                else if ( sections[0].equals( "TEAM" ) )
                    CommandHandler.callCommand( newSentry, "scoreboard", "ignore", sections[1] );
                else if ( sections[0].equals( "CLAN" ) )
                    CommandHandler.callCommand( newSentry, "clan", "ignore", sections[1] );
                else
                    Sentries.logger.info( "[Sentries] NPC:" + npc.getName() + ". Ignore could not be imported:- " + t );
            }
        }
        npc.removeTrait( net.aufdemrand.sentry.SentryTrait.class );
        
        return true;
    }
}
