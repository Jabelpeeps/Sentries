package org.jabelpeeps.sentries;

import java.text.DecimalFormat;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.Owner;

public abstract class CommandHandler {

    public static Pattern colon = Pattern.compile( ":" );

    /**
     * Convenience method to check perms on Command usage. The method includes
     * informing the player if they lack the required perms.
     * 
     * @param command
     *            - The perm node to be checked.
     * @param player
     *            - The sender of the command.
     * @return true - if the player has the required permission.
     */
    private static boolean checkCommandPerm( String command, CommandSender player ) {

        if ( player.hasPermission( command ) ) return true;

        player.sendMessage( S.Col.RED.concat( S.ERROR_NO_COMMAND_PERM ) );
        return false;
    }

    /**
     * Check that the String[] args contains enough arguments, directing the
     * player on how to get help if false.
     * 
     * @param number
     *            - the number of required args
     * @param args
     *            - the argument array
     * @param player
     *            - the player who entered the command.
     * @return true - args.length >= number
     */
    private static boolean enoughArgs( int number, String[] args, CommandSender player ) {

        if ( args.length >= number ) return true;

        player.sendMessage( S.Col.RED.concat( S.GET_COMMAND_HELP ) );
        return false;
    }

    /**
     * Concatenates the supplied String[] starting at the position indicated.
     * 
     * @param startFrom
     *            - the starting position (zero-based)
     * @param args
     *            - the String[] to be joined
     * @return - the resulting String.
     */
    private static String joinArgs( int startFrom, String[] args ) {

        StringJoiner joiner = new StringJoiner( " " );

        for ( int i = startFrom; i < args.length; i++ ) {
            joiner.add( args[i] );
        }
        return joiner.toString();
    }

    // ------------------------------------------------------------------------------------
    /**
     * The only accessible method of this class. It parses the arguments and
     * responds accordingly.
     * 
     * @param player
     * @param inargs
     * @param sentry
     * @return - true if the command has been successfully handled.
     */
    static boolean call( CommandSender player, String[] inargs, Sentries sentry ) {

        if ( !enoughArgs( 1, inargs, player ) ) return true;

        // ----------------------------------- help command -----------------
        if ( S.HELP.equalsIgnoreCase( inargs[0] ) ) {

            if ( inargs.length > 1 ) {

                if ( checkCommandPerm( S.PERM_TARGET, player ) && S.TARGET.equalsIgnoreCase( inargs[1] ) ) {
                    player.sendMessage( S.targetCommandHelp() );
                }
                else if ( checkCommandPerm( S.PERM_IGNORE, player ) && S.IGNORE.equalsIgnoreCase( inargs[1] ) ) {
                    player.sendMessage( S.ignoreCommandHelp() );
                }
                else if ( checkCommandPerm( S.PERM_EQUIP, player ) && S.EQUIP.equalsIgnoreCase( inargs[1] ) ) {
                    player.sendMessage( S.equipCommandHelp() );
                }
                else if ( checkCommandPerm( S.PERM_GUARD, player ) && S.GUARD.equalsIgnoreCase( inargs[1] ) ) {
                    player.sendMessage( S.guardCommandHelp() );
                }
                else player.sendMessage( S.ERROR_NO_MORE_HELP );
            }
            else {
                player.sendMessage( String.join( "", System.lineSeparator(), S.Col.GOLD, "---------- Sentries Commands ----------", S.Col.RESET  ) );

                if ( checkCommandPerm( S.PERM_TARGET, player ) )
                    player.sendMessage( String.join( " ", S.Col.GOLD, "/sentry target ...", S.Col.RESET, "set targets to attack." ) );
                if ( checkCommandPerm( S.PERM_IGNORE, player ) )
                    player.sendMessage( String.join( " ", S.Col.GOLD, "/sentry ignore ...", S.Col.RESET, "set targets to ignore." ) );
                if ( checkCommandPerm( S.PERM_EQUIP, player ) )
                    player.sendMessage( String.join( " ", S.Col.GOLD, "/sentry equip ...", S.Col.RESET, "set the equipment a sentry is using" ) );
                if ( checkCommandPerm( S.PERM_GUARD, player ) )
                    player.sendMessage( String.join( " ", S.Col.GOLD, "/sentry guard ...", S.Col.RESET, "tell the sentry what to guard" ) );

                if ( checkCommandPerm( S.PERM_SPEED, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry speed [0-1.5]" );
                    player.sendMessage( "  Sets speed of the Sentries when attacking" );
                }
                if ( checkCommandPerm( S.PERM_HEALTH, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry health [1-2000000]" );
                    player.sendMessage( "  Sets the Sentries's Health" );
                }
                if ( checkCommandPerm( S.PERM_ARMOUR, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry armor [0-2000000]" );
                    player.sendMessage( "  Sets the Sentries's Armor" );
                }
                if ( checkCommandPerm( S.PERM_STRENGTH, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry strength [0-2000000]" );
                    player.sendMessage( "  Sets the Sentries's Strength" );
                }
                if ( checkCommandPerm( S.PERM_ATTACK_RATE, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry attackrate [0.0-30.0]" );
                    player.sendMessage( "  Sets the time between the Sentries's projectile attacks" );
                }
                if ( checkCommandPerm( S.PERM_HEAL_RATE, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry healrate [0.0-300.0]" );
                    player.sendMessage( "  Sets the frequency the sentry will heal 1 point. 0 to disable." );
                }
                if ( checkCommandPerm( S.PERM_RANGE, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry range [1-100]" );
                    player.sendMessage( "  Sets the Sentries's detection range" );
                }
                if ( checkCommandPerm( S.PERM_WARNING_RANGE, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry warningrange [0-50]" );
                    player.sendMessage( "  How far beyond the detection range, that the Sentries will warn targets." );
                }
                if ( checkCommandPerm( S.PERM_NIGHTVISION, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry nightvision [0-16] " );
                    player.sendMessage( "  0 = See nothing, 16 = See everything. " );
                }
                if ( checkCommandPerm( S.PERM_RESPAWN_DELAY, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry respawn [-1-2000000]" );
                    player.sendMessage( "  Sets the number of seconds after death the Sentries will respawn." );
                }
                if ( checkCommandPerm( S.PERM_FOLLOW_DIST, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry follow [0-32]" );
                    player.sendMessage( "  Sets the number of block away a bodyguard will follow. Default is 4" );
                }
                if ( checkCommandPerm( S.PERM_INVINCIBLE, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry invincible" );
                    player.sendMessage( "  Toggle the Sentries to take no damage or knockback." );
                }
                if ( checkCommandPerm( S.PERM_RETALIATE, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry retaliate" );
                    player.sendMessage( "  Toggle the Sentries to always attack an attacker." );
                }
                if ( checkCommandPerm( S.PERM_CRITICAL_HITS, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry criticalhits" );
                    player.sendMessage( "  Toggle the Sentries to take critical hits and misses" );
                }
                if ( checkCommandPerm( S.PERM_DROPS, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry drops" );
                    player.sendMessage( "  Toggle the Sentries to drop equipped items on death" );
                }
                if ( checkCommandPerm( S.PERM_KILLDROPS, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry killdrops" );
                    player.sendMessage( "  Toggle whether or not the sentry's victims drop items and exp" );
                }
                if ( checkCommandPerm( S.PERM_MOUNT, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry mount" );
                    player.sendMessage( "  Toggle whether or not the sentry rides a mount" );
                }
                if ( checkCommandPerm( S.PERM_TARGETABLE, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry targetable" );
                    player.sendMessage( "  Toggle whether or not the sentry is attacked by hostile mobs" );
                }
                if ( checkCommandPerm( S.PERM_SPAWN, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry spawn" );
                    player.sendMessage( "  Set the sentry to respawn at its current location" );
                }
                if ( checkCommandPerm( S.PERM_WARNING, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry warning <text to use>" );
                    player.sendMessage( "  Change the warning text. <NPC> and <PLAYER> can be used as placeholders" );
                }
                if ( checkCommandPerm( S.PERM_GREETING, player ) ) {
                    player.sendMessage( S.Col.GOLD + "/sentry greeting <text to use>" );
                    player.sendMessage( "  Change the greeting text. <NPC> and <PLAYER> can be used as placeholders" );
                }
                if ( checkCommandPerm( S.PERM_INFO, player ) )
                    player.sendMessage( String.join( "", S.Col.GOLD, "/sentry info", S.Col.RESET, " - View all attributes of a sentry NPC" ) );
                
                if ( player instanceof ConsoleCommandSender )
                    player.sendMessage( String.join( "", S.Col.GOLD, "/sentry debug", S.Col.RESET, " - toggles the debug display on the console",
                            System.lineSeparator(), S.Col.RED, "Reduces performance! DO NOT enable unless you need it!" ) );

                if ( checkCommandPerm( S.PERM_RELOAD, player ) )
                    player.sendMessage( String.join( "", S.Col.GOLD, "/sentry reload", S.Col.RESET, " - Reloads the config file" ) );
                
                player.sendMessage( S.mainHelpOutro() );
            }
            return true;
        }
        // ------------------------------------- Debug Command --------------
        if (    player instanceof ConsoleCommandSender 
                && "debug".equalsIgnoreCase( inargs[0] ) ) {

            Sentries.debug = !Sentries.debug;
            player.sendMessage( S.Col.GREEN + "Debug is now: " + (Sentries.debug ? S.ON : S.OFF) );
            return true;
        }
        // -------------------------------------- Reload Command ------------
        if ( "reload".equalsIgnoreCase( inargs[0] ) ) {

            if ( checkCommandPerm( S.PERM_RELOAD, player ) ) {

                sentry.reloadMyConfig();
                player.sendMessage( S.Col.GREEN + "reloaded Sentries's config.yml file" );
            }
            return true;
        }

        // the remaining commands all deal with npc's
        // -------------------------------------------------------------------------------------
        // did player specify an integer as the first argument?
        int npcid = Util.string2Int( inargs[0] );

        // if a positive number was found, the next argument to parse will be at position 1, otherwise 0.
        int nextArg = (npcid > 0) ? 1 : 0;

        if ( !enoughArgs( 1 + nextArg, inargs, player ) ) return true;

        NPC thisNPC;
        // check to see whether the value saved is an npc ID, and save a reference if so.
        if ( npcid == -1 ) {

            thisNPC = CitizensAPI.getDefaultNPCSelector().getSelected( player );

            if ( thisNPC == null ) {
                player.sendMessage( S.ERROR.concat( S.ERROR_NO_NPC ) );
                return true;
            }
            npcid = thisNPC.getId();
        }
        else {
            thisNPC = CitizensAPI.getNPCRegistry().getById( npcid );

            if ( thisNPC == null ) {
                player.sendMessage( String.join( "", S.ERROR, S.ERROR_ID_INVALID, String.valueOf( npcid ) ) );
                return true;
            }
        }
        // We are now sure that thisNPC is valid, and that npcid contains its id.
        if ( !thisNPC.hasTrait( SentryTrait.class ) ) {
            player.sendMessage( S.ERROR.concat( S.ERROR_NOT_SENTRY ) );
            return true;
        }
        // OK, we have a sentry to modify.

        // We need to check that the player sending the command has the authority to use it.
        if (    player instanceof Player
                && !player.isOp()
                && !CitizensAPI.getNPCRegistry().isNPC( (Entity) player ) ) {

            // TODO consider changing this section to allow admins to modify other players' npcs.

            if ( !thisNPC.getTrait( Owner.class ).getOwner().equalsIgnoreCase( player.getName() ) ) {
                // player is not owner of the npc

                if ( !((Player) player).hasPermission( S.PERM_CITS_ADMIN ) ) {
                    // player is not an admin either.

                    player.sendMessage( S.Col.RED.concat( "You must be the owner of this Sentries to execute commands." ) );
                    return true;
                }
                if ( !thisNPC.getTrait( Owner.class ).getOwner().equalsIgnoreCase( "server" ) ) {
                    // not server-owned NPC

                    player.sendMessage( S.Col.RED.concat( "You, or the server, must be the owner of this Sentries to execute commands." ) );
                    return true;
                }
            }
        }

        // We now know that player is either the owner, op'ed, or an admin with a server-owned npc. 
        SentryTrait inst = thisNPC.getTrait( SentryTrait.class );
        String npcName = thisNPC.getName();

        // hold the state of the third argument (if it holds a boolean value) in a field for later use. 
        // This is held as an object not a primitive to allow for a third state - 'null'.
        Boolean set = null;

        if ( inargs.length == 2 + nextArg ) {
            if (    S.TRUE.equalsIgnoreCase( inargs[1 + nextArg] )
                    || S.ON.equalsIgnoreCase( inargs[1 + nextArg] ) )
                set = true;
            if (    S.FALSE.equalsIgnoreCase( inargs[1 + nextArg] )
                    || S.OFF.equalsIgnoreCase( inargs[1 + nextArg] ) )
                set = false;
        }
        // ------------------------------------- spawn command --------------
        if ( S.SPAWN.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.SPAWN, player ) ) {

                if ( thisNPC.getEntity() == null ) 
                    player.sendMessage( S.Col.RED.concat( "Cannot set spawn while a sentry is dead" ) );
                else {
                    inst.spawnLocation = thisNPC.getEntity().getLocation();
                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "will respawn at its present location" ) );
                }
            }
            return true;
        }
        // ------------------------------------ invincible command ------------
        if ( S.INVINCIBLE.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_INVINCIBLE, player ) ) {

                inst.invincible = (set == null) ? !inst.invincible : set;

                player.sendMessage( String.join( " ", S.Col.GREEN, npcName, inst.invincible ? "is now INVINCIBLE" : "now takes damage" ) );
            }
            return true;
        }
        // --------------------------------------- retaliate command --------------
        if ( S.RETALIATE.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_RETALIATE, player ) ) {

                inst.iWillRetaliate = (set == null) ? !inst.iWillRetaliate : set;

                player.sendMessage( String.join( " ", S.Col.GREEN, npcName, inst.iWillRetaliate ? "will retalitate against all attackers"
                                                                                                : "will not retaliate when attacked" ) );
            }
            return true;
        }
        // ---------------------------------------- criticals command -------------
        if ( S.CRITICAL_HITS.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_CRITICAL_HITS, player ) ) {

                inst.acceptsCriticals = (set == null) ? !inst.acceptsCriticals : set;

                player.sendMessage( String.join( " ", S.Col.GREEN, npcName, inst.acceptsCriticals ? "will take critical hits"
                                                                                                  : "will take normal damage" ) );
            }
            return true;
        }
        // ------------------------------------------ drops command ----------------
        if ( S.DROPS.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_DROPS, player ) ) {

                inst.dropInventory = (set == null) ? !inst.dropInventory : set;

                player.sendMessage( String.join( " ", S.Col.GREEN, npcName, inst.dropInventory ? "will drop items"
                                                                                               : "will not drop items" ) );
            }
            return true;
        }
        // ---------------------------------------- killdrops command -------------
        if ( S.KILLS_DROP.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_KILLDROPS, player ) ) {

                inst.killsDropInventory = (set == null) ? !inst.killsDropInventory : set;

                player.sendMessage( String.join( "", S.Col.GREEN, npcName, inst.killsDropInventory ? "'s kills will drop items or exp"
                                                                                                   : "'s kills will not drop items or exp" ) );
            }
            return true;
        }
        // -----------------------------------------targetable command ------------
        if ( S.TARGETABLE.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_TARGETABLE, player ) ) {

                inst.targetable = (set == null) ? !inst.targetable : set;

                thisNPC.data().set( NPC.TARGETABLE_METADATA, inst.targetable );

                player.sendMessage( String.join( " ", S.Col.GREEN, npcName, inst.targetable ? "will be targeted by mobs"
                                                                                            : "will not be targeted by mobs" ) );
            }
            return true;
        }
        // -----------------------------------------mount command -----------
        if ( S.MOUNT.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_MOUNT, player ) ) {

                set = (set == null) ? !inst.hasMount() : set;

                if ( set ) {
                    inst.mount();
                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "is now Mounted" ) );
                }
                else {
                    if ( inst.hasMount() )
                        Util.removeMount( inst.mountID );

                    inst.mountID = -1;
                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "is no longer Mounted" ) );
                }
            }
            return true;
        }
        // -----------------------------------------guard command -------------

        // TODO add help text for this command.
        if ( S.GUARD.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_GUARD, player ) ) {

                if ( inargs.length > nextArg + 1 ) {
                    
                    if ( S.CLEAR.equalsIgnoreCase( inargs[nextArg + 1] ) ) {
                        inst.findGuardEntity( null, false );
                    }
                    else {
                        boolean localonly = false;
                        boolean playersonly = false;
                        int start = 1;
                        boolean ok = false;
    
                        if ( inargs[nextArg + 1].equalsIgnoreCase( "-p" ) ) {
                            start = 2;
                            playersonly = true;
                        }
    
                        if ( inargs[nextArg + 1].equalsIgnoreCase( "-l" ) ) {
                            start = 2;
                            localonly = true;
                        }
    
                        String arg = joinArgs( start + nextArg, inargs );
    
                        if ( !playersonly ) ok = inst.findGuardEntity( arg, false );   
                        if ( !localonly ) ok = inst.findGuardEntity( arg, true );
    
                        if ( ok )
                            player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "is now guarding", arg ) );
                        else
                            player.sendMessage( String.join( " ", S.Col.RED, npcName, "could not find", arg ) );
                        return true;
                    }
                }
                if ( inst.guardeeName == null )
                    player.sendMessage( S.Col.GREEN.concat( "Guarding: My Surroundings" ) );
                else if ( inst.guardeeEntity == null )
                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "is configured to guard", inst.guardeeName, "but cannot find them at the moment" ) );
                else
                    player.sendMessage( String.join( " ", S.Col.BLUE, "Guarding:", inst.guardeeEntity.getName() ) );

            }
            return true;
        }
        // ----------------------------------------------follow command -------------------
        if ( S.FOLLOW.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_FOLLOW_DIST, player ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    player.sendMessage( String.join( "", S.Col.GOLD, npcName, "'s Follow Distance is ", String.valueOf( inst.followDistance ) ) );
                    player.sendMessage( S.Col.GOLD.concat( "Usage: /sentry follow [#]. Default is 4. " ) );
                }
                else {
                    int dist = Util.string2Int( inargs[nextArg + 1] );
                    if ( dist < 0 ) {
                        player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( dist > 32 ) dist = 32;
                    
                    inst.followDistance = dist * dist;
                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "follow distance set to", String.valueOf( dist ) ) );
                }
            }
            return true;
        }
        // --------------------------------------------health command -------------------
        if ( S.HEALTH.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_HEALTH, player ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    player.sendMessage( String.join( "", S.Col.GOLD, npcName, "'s Health is ", String.valueOf( inst.sentryMaxHealth ) ) );
                    player.sendMessage( S.Col.GOLD.concat( "Usage: /sentry health [#]   note: Typically players have 20 HPs when fully healed" ) );
                }
                else {
                    int HPs = Util.string2Int( inargs[nextArg + 1] );
                    if ( HPs < 1 ) {
                        player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( HPs > 2000000 ) HPs = 2000000;
                    
                    inst.sentryMaxHealth = HPs;
                    inst.setHealth( HPs );
                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "health set to", String.valueOf( HPs ) ) );
                }
            }
            return true;
        }
        // -------------------------------------------armour command-----------------------
        if (    S.ARMOUR.equalsIgnoreCase( inargs[nextArg] )
                || S.ARMOR.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_ARMOUR, player ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    player.sendMessage( String.join( "", S.Col.GOLD, npcName, "'s Armour is ", String.valueOf( inst.armourValue ) ) );
                    player.sendMessage( S.Col.GOLD.concat( "Usage: /sentry armour [#] " ) );
                }
                else {
                    int armour = Util.string2Int( inargs[nextArg + 1] );
                    if ( armour < 0 ) {
                        player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( armour > 2000000 ) armour = 2000000;
                    
                    inst.armourValue = armour;
                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "armour set to", String.valueOf( armour ) ) );
                }
            }
            return true;
        }
        // --------------------------------------------strength command --------------
        if ( S.STRENGTH.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_STRENGTH, player ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    player.sendMessage( S.Col.GOLD + npcName + "'s Strength is " + inst.strength );
                    player.sendMessage( S.Col.GOLD + "Usage: /sentry strength # " );
                    player.sendMessage( S.Col.GOLD + "Note: At strength 0 the Sentries will do no damamge. " );
                }
                else {
                    int strength = Util.string2Int( inargs[nextArg + 1] );
                    if ( strength < 0 ) {
                        player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( strength > 2000000 ) strength = 2000000;
                    
                    inst.strength = strength;
                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "strength set to", String.valueOf( strength ) ) );
                }
            }
            return true;
        }
        // ----------------------------------------nightvision command---------
        // TODO add help text for this command
        if ( S.NIGHT_VISION.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_NIGHTVISION, player ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    player.sendMessage( S.Col.GOLD + npcName + "'s Night Vision is " + inst.nightVision );
                    player.sendMessage( S.Col.GOLD + "Usage: /sentry nightvision [0-16] " );
                    player.sendMessage( S.Col.GOLD + "Usage: 0 = See nothing, 16 = See everything. " );
                }
                else {
                    int vision = Util.string2Int( inargs[nextArg + 1] );
                    if ( vision < 0 ) {
                        player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }
                    if ( vision > 16 ) vision = 16;
                    
                    inst.nightVision = vision;
                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "Night Vision set to", String.valueOf( vision ) ) );
                }
            }
            return true;
        }
        // ------------------------------------respawn command------------------------
        if ( S.RESPAWN.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_RESPAWN_DELAY, player ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    S.respawnCommandMessage( inst.respawnDelay, thisNPC, player );

                    player.sendMessage( S.Col.GOLD + "Usage: /sentry respawn [-1 - 2000000] " );
                    player.sendMessage( S.Col.GOLD + "Usage: set to 0 to prevent automatic respawn" );
                    player.sendMessage( S.Col.GOLD + "Usage: set to -1 to *permanently* delete the Sentry on death." );
                }
                else {
                    int respawn = Util.string2Int( inargs[nextArg + 1] );
                    if ( respawn < -1 ) {
                        player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( respawn > 2000000 ) respawn = 2000000;
                    
                    inst.respawnDelay = respawn;
                    S.respawnCommandMessage( inst.respawnDelay, thisNPC, player );
                }
            }
            return true;
        }
        // ------------------------------------speed command--------------------------
        if ( S.SPEED.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_SPEED, player ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    player.sendMessage( S.Col.GOLD + npcName + "'s Speed is " + inst.sentrySpeed );
                    player.sendMessage( S.Col.GOLD + "Usage: /sentry speed [0.0 - 2.0]" );
                }
                else {
                    float speed = Util.string2Float( inargs[nextArg + 1] );
                    if ( speed < 0.0 ) {
                        player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( speed > 2.0 ) speed = 2.0f;
                    
                    inst.sentrySpeed = speed;
                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "speed set to", String.valueOf( speed ) ) );
                }
            }
            return true;
        }
        // -----------------------------------attackrate command ---------
        if ( S.ATTACK_RATE.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_ATTACK_RATE, player ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    player.sendMessage( String.join( "", S.Col.GOLD, npcName, "'s Projectile Attack Rate is ", 
                                        String.valueOf( inst.attackRate), " seconds between shots." ) );
                    player.sendMessage( S.Col.GOLD + "Usage: /sentry attackrate [0.0 - 30.0]" );
                }
                else {
                    double attackrate = Util.string2Double( inargs[nextArg + 1] );
                    if ( attackrate < 0.0 ) {
                        player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( attackrate > 30.0 ) attackrate = 30.0;
                    
                    inst.attackRate = attackrate;
                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "Projectile Attack Rate set to", String.valueOf( attackrate ) ) );
                }
            }
            return true;
        }
        // ----------------------------------------healrate command-----------------
        if ( S.HEALRATE.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_HEAL_RATE, player ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    player.sendMessage( S.Col.GOLD + npcName + "'s Heal Rate is " + inst.healRate + "s" );
                    player.sendMessage( S.Col.GOLD + "Usage: /sentry healrate [0.0 - 300.0]" );
                    player.sendMessage( S.Col.GOLD + "Usage: Set to 0 to disable healing" );
                }
                else {
                    double healrate = Util.string2Double( inargs[nextArg + 1] );
                    if ( healrate < 0.0 ) {
                        player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( healrate > 300.0 ) healrate = 300.0;
                    
                    inst.healRate = healrate;
                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "Heal Rate set to", String.valueOf( healrate ) ) );
                }
            }
            return true;
        }
        // --------------------------------------range command-----------------
        if ( S.RANGE.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_RANGE, player ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    player.sendMessage( S.Col.GOLD + npcName + "'s Range is " + inst.sentryRange );
                    player.sendMessage( S.Col.GOLD + "Usage: /sentry range [1 - 100]" );
                }
                else {
                    int range = Util.string2Int( inargs[nextArg + 1] );
                    if ( range < 1 ) {
                        player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( range > 100 ) range = 100;
                    
                    inst.sentryRange = range;
                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "range set to", String.valueOf( range ) ) );
                }
            }
            return true;
        }
        // --------------------------------------warningrange command----------
        if ( S.WARNING_RANGE.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_WARNING_RANGE, player ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    player.sendMessage( S.Col.GOLD + npcName + "'s Warning Range is " + inst.warningRange );
                    player.sendMessage( S.Col.GOLD + "Usage: /sentry warningrangee [0 - 50]" );
                }
                else {
                    int range = Util.string2Int( inargs[nextArg + 1] );
                    if ( range < 0 ) {
                        player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( range > 50 ) range = 50;
                    
                    inst.warningRange = range;
                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "warning range set to", String.valueOf( range ) ) );
                }
            }
            return true;
        }
        // -------------------------------------------------equip command-------------
        if ( S.EQUIP.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_EQUIP, player ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    player.sendMessage( String.join( "", S.ERROR, "More arguments needed.") );
                    player.sendMessage( S.equipCommandHelp() );
                    return true;
                }
                
                EntityType type = thisNPC.getEntity().getType();
                // TODO figure out why zombies and skele's are not included here.
                if ( type == EntityType.ENDERMAN || type == EntityType.PLAYER ) {

                    if ( S.CLEARALL.equalsIgnoreCase( inargs[nextArg + 1] ) ) {

                        inst.equip( null );
                        player.sendMessage( String.join( "", S.Col.YELLOW, npcName, "'s equipment cleared" ) );
                    }
                    else if ( S.CLEAR.equalsIgnoreCase( inargs[nextArg + 1] ) ) {

                        for ( Entry<String, Integer> each : Sentries.equipmentSlots.entrySet() )

                            if ( each.getKey().equalsIgnoreCase( inargs[nextArg + 2] ) ) {
                                
                                thisNPC.getTrait( Equipment.class ).set( each.getValue(), null );                                
                                player.sendMessage( String.join( "", S.Col.GREEN, "removed ", npcName, "'s ", inargs[nextArg +2] ) );
                            }
                    }
                    else {
                        Material mat = Material.matchMaterial( joinArgs( nextArg + 1, inargs ) );

                        if ( mat == null ) {
                            player.sendMessage( S.Col.RED.concat( "Could not equip: item name not recognised" ) );
                            return true;
                        }

                        ItemStack item = new ItemStack( mat );

                        if ( inst.equip( item ) )
                            player.sendMessage( String.join( " ", S.Col.GREEN, "equipped", mat.toString(), "on", npcName ) );
                        else
                            player.sendMessage( S.Col.RED.concat( "Could not equip: invalid mob type?" ) );
                    }
                }
                else player.sendMessage( S.Col.RED.concat( "Could not equip: must be Player or Enderman type" ) );
            }
            return true;
        }
        // ----------------------------------------warning command-----------
        if ( S.WARNING.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_WARNING, player ) ) {

                if ( inargs.length >= 2 + nextArg ) {

                    String str = Util.sanitiseString( joinArgs( 1 + nextArg, inargs ) );

                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "Warning message set to", S.Col.RESET, 
                                                        ChatColor.translateAlternateColorCodes( '&', str ) ) );
                    inst.warningMsg = str;
                }
                else {
                    player.sendMessage( String.join( "", S.Col.GOLD, npcName, "'s Warning Message is: ", S.Col.RESET, 
                                                        ChatColor.translateAlternateColorCodes( '&', inst.warningMsg ) ) );

                    player.sendMessage( S.Col.GOLD.concat( "Usage: /sentry warning 'The Text to use'" ) );
                }
            }
            return true;
        }
        // ----------------------------------------greeting command----------
        if ( S.GREETING.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_GREETING, player ) ) {

                if ( inargs.length >= 2 + nextArg ) {

                    String str = Util.sanitiseString( joinArgs( 1 + nextArg, inargs ) );

                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "Greeting message set to", S.Col.RESET,
                                                        ChatColor.translateAlternateColorCodes( '&', str ) ) );
                    inst.greetingMsg = str;
                }
                else {
                    player.sendMessage( String.join( "", S.Col.GOLD, npcName, "'s Greeting Message is: ", S.Col.RESET,
                                                        ChatColor.translateAlternateColorCodes( '&', inst.greetingMsg ) ) );

                    player.sendMessage( S.Col.GOLD.concat( "Usage: /sentry greeting 'The Text to use'" ) );
                }
            }
            return true;
        }
        // ---------------------------------------------info command-----------
        if ( S.INFO.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_INFO, player ) ) {

                StringJoiner joiner = new StringJoiner( System.lineSeparator() );

                joiner.add( String.join( "", S.Col.GOLD, "------- Sentries Info for ", npcName, " (npcid - ",
                                            String.valueOf( thisNPC.getId() ), ") ", "------" ) );
                
                joiner.add( String.join( "", 
                        S.Col.RED, "[HP]:", S.Col.WHITE, String.valueOf( inst.getHealth() ), "/", String.valueOf( inst.sentryMaxHealth ),
                        S.Col.RED, " [AP]:", S.Col.WHITE, String.valueOf( inst.getArmor() ),
                        S.Col.RED, " [STR]:", S.Col.WHITE, String.valueOf( inst.getStrength() ),
                        S.Col.RED, " [SPD]:", S.Col.WHITE, new DecimalFormat( "#.0" ).format( inst.getSpeed() ),
                        S.Col.RED, " [RNG]:", S.Col.WHITE, String.valueOf( inst.sentryRange ),
                        S.Col.RED, " [ATK]:", S.Col.WHITE, String.valueOf( inst.attackRate ),
                        S.Col.RED, " [VIS]:", S.Col.WHITE, String.valueOf( inst.nightVision ),
                        S.Col.RED, " [HEAL]:", S.Col.WHITE, String.valueOf( inst.healRate ),
                        S.Col.RED, " [WARN]:", S.Col.WHITE, String.valueOf( inst.warningRange ),
                        S.Col.RED, " [FOL]:", S.Col.WHITE, String.valueOf( Math.sqrt( inst.followDistance ) ) ) );

                joiner.add( String.join( "", S.Col.GREEN, "Invincible: ", String.valueOf( inst.invincible ), 
                                                        "  Retaliate: ", String.valueOf( inst.iWillRetaliate ) ) );
                joiner.add( String.join( "", S.Col.GREEN, "Drops Items: ", String.valueOf( inst.dropInventory ), 
                                                        "  Critical Hits: ", String.valueOf( inst.acceptsCriticals ) ) );
                joiner.add( String.join( "", S.Col.GREEN, "Kills Drop Items: ", String.valueOf( inst.killsDropInventory ), 
                                                        "  Respawn Delay: ", String.valueOf( inst.respawnDelay ), "secs" ) );
                joiner.add( String.join( "", S.Col.BLUE, "Currently: ", inst.getNPC().isSpawned() ? "Spawned" 
                                                                                                  : "Not Spawned" ) );
                joiner.add( String.join( "", S.Col.BLUE, "Status: ", inst.myStatus.toString() ) );

                if ( inst.attackTarget == null )
                    joiner.add( S.Col.BLUE.concat( "Current Target: None" ) );
                else
                    joiner.add( String.join( "", S.Col.BLUE, "Current Target: ", inst.attackTarget.getName() ) );

                if ( inst.guardeeEntity == null )
                    joiner.add( S.Col.BLUE.concat( "Guarding: My Surroundings" ) );
                else
                    joiner.add( String.join( "", S.Col.BLUE, "Guarding: ", inst.guardeeEntity.getName() ) );

                player.sendMessage( joiner.toString() );
            }
            return true;
        }
        // ---------------------------------------------target command---------
        if ( S.TARGET.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_TARGET, player ) ) {

                if ( inargs.length <= nextArg + 1 ) {
                    player.sendMessage( S.targetCommandHelp() );
                    return true;
                }
                if ( S.LIST.equals( inargs[nextArg + 1] ) ) {
                    player.sendMessage( String.join( "", S.Col.GREEN, "Targets: ", inst.validTargets.toString() ) );
                    return true;
                }
                if ( S.CLEAR.equals( inargs[nextArg + 1] ) ) {
                    inst.validTargets.clear();
                    inst.targetFlags = 0;
                    player.sendMessage( String.join( "", S.Col.GREEN, npcName, ": ALL Targets cleared" ) );
                    return true;
                }
                if ( inargs.length > 2 + nextArg ) {
                    player.sendMessage( parseTargetOrIgnore( inargs, nextArg, npcName, inst, true ) );
                    return true;
                }                
            }
        }
        // ----------------------------------------------------ignore command-----------
        if ( S.IGNORE.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_IGNORE, player ) ) {

                if ( inargs.length <= nextArg + 1 ) {
                    player.sendMessage( S.ignoreCommandHelp() );
                    return true;
                }                    
                if ( S.LIST.equals( inargs[nextArg + 1] ) ) {
                    player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "Current Ignores:", inst.ignoreTargets.toString() ) );
                    return true;
                }
                if ( S.CLEAR.equals( inargs[nextArg + 1] ) ) {
                    inst.ignoreTargets.clear();
                    inst.ignoreFlags = 0;
                    inst.clearTarget();
                    player.sendMessage( String.join( "", S.Col.GREEN, npcName, ": ALL Ignores cleared" ) );
                    return true;
                }
                if ( inargs.length > 2 + nextArg ) {
                    player.sendMessage( parseTargetOrIgnore( inargs, nextArg, npcName, inst, false ) );
                    return true;
                }
            }
        }
        return false;
    }

    private static String parseTargetOrIgnore( String[] inargs, int nextArg,  String npcName, SentryTrait inst, boolean forTargets ) {

        String[] typeArgs = new String[inargs.length - (2 + nextArg)];
        System.arraycopy( inargs, 2 + nextArg, typeArgs, 0, inargs.length - (2 + nextArg) );

        if ( Sentries.debug ) Sentries.debugLog( "Target types list is:- " + joinArgs( 0, typeArgs ) );

        StringJoiner joiner = new StringJoiner( System.lineSeparator() );
        Set<String> setOfTargets = forTargets ? inst.validTargets : inst.ignoreTargets;

        if ( S.ADD.equalsIgnoreCase( inargs[nextArg + 1] ) ) {

            for ( String arg : typeArgs ) {
                String[] args = colon.split( arg, 2 );

                if ( args.length > 1 ) {

                    boolean messageSent = false, opSucceeded = false;

                    plugins: 
                    for ( PluginBridge each : Sentries.activePlugins.values() ) {
                        if ( each.getPrefix().equalsIgnoreCase( args[0] ) ) {

                            joiner.add( each.add( arg, inst, forTargets ) );
                            messageSent = true;
                            break plugins;
                        }
                    }
                    if ( setOfTargets.add( arg.toUpperCase() ) )
                        opSucceeded = true;

                    if ( !messageSent ) {
                        if ( opSucceeded )
                            joiner.add( String.join( " ", S.Col.GREEN, npcName, forTargets ? "Target added. Now targeting:-"
                                                                                           : "Ignore added. Now ignoring:-",
                                                                                           setOfTargets.toString() ) );
                        else
                            joiner.add( String.join( " ", S.Col.GREEN, arg, S.ALLREADY_ON_LIST, forTargets ? S.TARGETS : S.IGNORES ) );
                    }
                }
            }
        }
        else if ( S.REMOVE.equalsIgnoreCase( inargs[nextArg + 1] ) ) {

            for ( String arg : typeArgs ) {
                String[] args = colon.split( arg, 2 );
                if ( args.length > 1 ) {

                    boolean messageSent = false, opSucceeded = false;

                    plugins: 
                    for ( PluginBridge each : Sentries.activePlugins.values() ) {
                        if ( each.getPrefix().equalsIgnoreCase( args[0] ) ) {

                            joiner.add( each.remove( arg, inst, forTargets ) );
                            messageSent = true;
                            break plugins;
                        }
                    }
                    if ( setOfTargets.remove( arg.toUpperCase() ) )
                        opSucceeded = true;

                    if ( !messageSent ) {
                        if ( opSucceeded )
                            joiner.add( String.join( " ", S.Col.GREEN, npcName, forTargets ? "Target removed. Now targeting:-"
                                                                                           : "Ignore removed. Now ignoring:-",
                                                                                           setOfTargets.toString() ) );
                        else
                            joiner.add( String.join( " ", S.Col.GREEN, arg, S.NOT_FOUND_ON_LIST, forTargets ? S.TARGETS : S.IGNORES ) );
                    }
                }
            }
        }

        if ( joiner.toString() == "" ) {
            joiner.add( "Arguments not recognised. Try '/sentry help'" );
        }
        else {
            inst.processTargetStrings( false );
            inst.clearTarget();
        }
        return joiner.toString();
    }
}
