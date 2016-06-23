package org.jabelpeeps.sentries.commands;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.Sentries;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;
import org.jabelpeeps.sentries.targets.AllEntitiesTarget;
import org.jabelpeeps.sentries.targets.AllMonstersTarget;
import org.jabelpeeps.sentries.targets.AllNPCsTarget;
import org.jabelpeeps.sentries.targets.AllPlayersTarget;
import org.jabelpeeps.sentries.targets.MobTypeTarget;
import org.jabelpeeps.sentries.targets.NamedNPCTarget;
import org.jabelpeeps.sentries.targets.NamedPlayerTarget;
import org.jabelpeeps.sentries.targets.TargetType;

import net.citizensnpcs.api.npc.NPC;


public class TargetComand implements SentriesComplexCommand {

    private String targetCommandHelp;

    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
                
        if ( args.length <= nextArg + 1 ) {
            sender.sendMessage( getLongHelp() );
            return;
        }

        String subCommand = args[nextArg + 1].toLowerCase();
        
        if ( S.LIST.equals( subCommand ) ) {
            StringJoiner joiner = new StringJoiner( ", " );
            inst.targets.forEach( t -> joiner.add( t.getTargetString() ) );

            Util.sendMessage( sender, Col.GREEN, npcName, "'s Targets: ", joiner.toString() );
            return;
        }
        
        if ( S.CLEARALL.equals( subCommand ) ) {
            inst.targets.removeIf( i -> i instanceof TargetType.Internal );
            inst.clearTarget();
            Util.sendMessage( sender, Col.GREEN, npcName, ": ALL Targets cleared" );
            return;
        }
        
        if ( (S.ADD + S.REMOVE).contains( subCommand ) ) {    
            
            if ( args.length <= nextArg + 2 ) {
                Util.sendMessage( sender, S.ERROR, "Missing arguments!", Col.RESET, " try '/sentry help target'" );
                return;
            }
            TargetType target = null;
            String[] targetArgs = Util.colon.split( args[nextArg + 2] );
            
            if ( targetArgs.length < 2 ) {
                Util.sendMessage( sender, S.ERROR, "Malformed target token!", Col.RESET, " try '/sentry help target'" );
                return;
            }
            String firstSubArg = targetArgs[0].toLowerCase();
            String secondSubArg = targetArgs[1].toLowerCase();
            
            // TODO add user feedback for success or failure conditions
            
            if ( firstSubArg.equals( "all" ) ) {
                if ( secondSubArg.equals( "entities" ) ) 
                    target = new AllEntitiesTarget();
                else if ( secondSubArg.equals( "monsters" ) ) 
                    target = new AllMonstersTarget();
                else if ( secondSubArg.equals( "npcs" ) ) 
                    target = new AllNPCsTarget();
                else if ( secondSubArg.equals( "players" ) ) 
                    target = new AllPlayersTarget();
            }
            else if ( firstSubArg.equals( "mobtype" ) ) {
                EntityType type = EntityType.valueOf( secondSubArg );
                if ( type != null )
                    target = new MobTypeTarget( type );
            }
            else if ( firstSubArg.equals( "named" ) ) {
                if ( secondSubArg.equals( "player" ) ) { 
                    try {
                    target = new NamedPlayerTarget( 
                            Arrays.stream( Bukkit.getOfflinePlayers() )
                                  .filter( p -> p.getName().equalsIgnoreCase( targetArgs[2] ) )
                                  .findAny().orElse( Bukkit.getOfflinePlayer( UUID.fromString( targetArgs[2] ) ) )
                                  .getUniqueId() );
                    } catch (IllegalArgumentException e) {
                        Util.sendMessage( sender, S.ERROR, "No player called:- ",targetArgs[2], " was found." );
                    }                  
                }
                else if ( secondSubArg.equals( "npc" ) ) {

                    for ( NPC npc : Sentries.registry ) {
                        if ( npc.getName().equalsIgnoreCase( targetArgs[2] ) ) {
                            target = new NamedNPCTarget( npc.getUniqueId() );
                            break;
                        }
                        else if ( npc.getUniqueId().toString().equals( targetArgs[2] ) ) {
                            target = new NamedNPCTarget( UUID.fromString( targetArgs[2] ) );
                            break;
                        }
                    }
                }
            }
            
            if ( target == null )
                Util.sendMessage( sender, "The intended target was not recognised" );
            else if ( S.ADD.equals( subCommand ) && inst.targets.add( target ) )
                Util.sendMessage( sender, "Target Added" );
            else if ( S.REMOVE.equals( subCommand ) && inst.targets.remove( target ) )
                Util.sendMessage( sender, "Target Removed" );            
        }                
    }

    @Override
    public String getShortHelp() { return "set targets to attack"; }

    @Override
    public String getLongHelp() {
        
        if ( targetCommandHelp == null ) {

            StringJoiner joiner = new StringJoiner( System.lineSeparator() ).add( "" );

            joiner.add( String.join( "", "do ", Col.GOLD, "/sentry ", S.TARGET, " <add|remove|list|clearall> <TargetType>", 
                                                Col.RESET, " to configure targets for a sentry to attack. " ) );
            joiner.add( String.join( "", Col.BOLD, "Targets can be overridden by ignores.", Col.RESET ) );
            joiner.add( String.join( "", "  use ", Col.GOLD, S.ADD, Col.RESET, " to add <TargetType> as a target" ) );
            joiner.add( String.join( "", "  use ", Col.GOLD, S.REMOVE, Col.RESET, " to remove <TargetType> as a target" ) );
            joiner.add( String.join( "", "  use ", Col.GOLD, S.LIST, Col.RESET, " to display current list of targets" ) );
            joiner.add( String.join( "", "  use ", Col.GOLD, S.CLEARALL, Col.RESET, " to clear the ALL the current targets" ) );
            joiner.add( S.HELP_ADD_REMOVE_TYPES );
            joiner.add( String.join( "", Col.GOLD, "  All:Entities ", Col.RESET, "to target anything that moves.") );
            joiner.add( String.join( "", Col.GOLD, "  All:Monsters ", Col.RESET, "to target all hostile mobs.") );
            joiner.add( String.join( "", Col.GOLD, "  All:NPCs ", Col.RESET, "to target all Citizens NPC's.") );
            joiner.add( String.join( "", Col.GOLD, "  All:Players ", Col.RESET, "to target all (human) Players.") );
//            joiner.add( String.join( "", Col.GOLD, "", Col.RESET, "") );
            joiner.add( Util.getAdditionalTargets() );

            targetCommandHelp = joiner.toString();
        }       
        return targetCommandHelp;
    }

    @Override
    public String getPerm() { return S.PERM_TARGET; }
}
