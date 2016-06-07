package org.jabelpeeps.sentries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.commands.CriticalsCommand;
import org.jabelpeeps.sentries.commands.DropsCommand;
import org.jabelpeeps.sentries.commands.EquipCommand;
import org.jabelpeeps.sentries.commands.FollowDistanceCommand;
import org.jabelpeeps.sentries.commands.GreetingCommand;
import org.jabelpeeps.sentries.commands.GuardCommand;
import org.jabelpeeps.sentries.commands.IgnoreCommand;
import org.jabelpeeps.sentries.commands.InfoCommand;
import org.jabelpeeps.sentries.commands.InvincibleCommand;
import org.jabelpeeps.sentries.commands.KillsDropCommand;
import org.jabelpeeps.sentries.commands.MobsAttackCommand;
import org.jabelpeeps.sentries.commands.MountCommand;
import org.jabelpeeps.sentries.commands.RetaliateCommand;
import org.jabelpeeps.sentries.commands.SetSpawnCommand;
import org.jabelpeeps.sentries.commands.TargetComand;
import org.jabelpeeps.sentries.commands.WarningCommand;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;

public class CommandHandler implements CommandExecutor, TabCompleter {
    
    public static Pattern colon = Pattern.compile( ":" );
    private static Map<String, SentriesCommand> commandMap = new TreeMap<>();

    static {
        commandMap.put( S.TARGET,       new TargetComand() );
        commandMap.put( S.IGNORE,       new IgnoreCommand() );
        commandMap.put( S.EQUIP,        new EquipCommand() );
        commandMap.put( S.GUARD,        new GuardCommand() );
        commandMap.put( S.SET_SPAWN,    new SetSpawnCommand() );
        commandMap.put( S.INVINCIBLE,   new InvincibleCommand() );
        commandMap.put( S.RETALIATE,    new RetaliateCommand() );
        commandMap.put( S.CRITICALS,    new CriticalsCommand() );
        commandMap.put( S.DROPS,        new DropsCommand() );
        commandMap.put( S.KILLSDROP,    new KillsDropCommand() ); 
        commandMap.put( S.MOBS_ATTACK,  new MobsAttackCommand() );
        commandMap.put( S.MOUNT,        new MountCommand() );
        commandMap.put( S.INFO,         new InfoCommand() );
        commandMap.put( S.GREETING,     new GreetingCommand() );
        commandMap.put( S.WARNING,      new WarningCommand() );
        commandMap.put( S.FOLLOW,       new FollowDistanceCommand() );
    }
    
    /**
     * Adds a command option to the Sentries command structure as a second 
     * level sub-command (i.e. an option immediately following '/sentry')
     * 
     * @param name - the name of the sub-command that will need to be used by users to access it.
     * @param command - either a {@link#SentriesComplexCommand} or a {@link#SentriesToggleCommand} object.
     */
    public static void addCommand( String name, SentriesCommand command ) {
        commandMap.put( name, command );
    }
    
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
    public static boolean checkCommandPerm( String command, CommandSender player ) {

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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        
        List<String> tabs = new ArrayList<>( commandMap.keySet() );
        
        tabs.removeIf( new Predicate<String>() {
            @Override
            public boolean test( String t ) {
                return !t.startsWith( args[args.length - 1] );
            }            
        });
        
        return tabs;       
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command com, String label, String[] inargs) {
        
        if ( !enoughArgs( 1, inargs, sender ) ) return true;

        // ----------------------------------- help command -----------------
        if ( S.HELP.equalsIgnoreCase( inargs[0] ) ) {

            if ( inargs.length > 1 ) {
                
                SentriesCommand command = commandMap.get( inargs[1].toLowerCase() );
                
                if ( command != null && checkCommandPerm( command.getPerm(), sender ) )               
                    sender.sendMessage( command.getLongHelp() );
                else 
                    sender.sendMessage( S.ERROR_UNKNOWN_COMMAND );
                return true;
            }
            
            sender.sendMessage( String.join( "", S.Col.GOLD, "---------- Sentries Commands ----------", S.Col.RESET  ) );

            for ( Entry<String, SentriesCommand> each : commandMap.entrySet() ) {
                
                SentriesCommand command = each.getValue();
                
                if ( checkCommandPerm( command.getPerm(), sender ) ) {
                    sender.sendMessage( String.join( "", S.Col.GOLD, "/sentry ", each.getKey(), " ... ", S.Col.RESET, command.getShortHelp() ) );
                }
            }

            if ( checkCommandPerm( S.PERM_SPEED, sender ) ) {
                sender.sendMessage( S.Col.GOLD + "/sentry speed [0-1.5]" );
                sender.sendMessage( "  Sets speed of the Sentries when attacking" );
            }
            if ( checkCommandPerm( S.PERM_HEALTH, sender ) ) {
                sender.sendMessage( S.Col.GOLD + "/sentry health [1-2000000]" );
                sender.sendMessage( "  Sets the Sentries's Health" );
            }
            if ( checkCommandPerm( S.PERM_ARMOUR, sender ) ) {
                sender.sendMessage( S.Col.GOLD + "/sentry armor [0-2000000]" );
                sender.sendMessage( "  Sets the Sentries's Armor" );
            }
            if ( checkCommandPerm( S.PERM_STRENGTH, sender ) ) {
                sender.sendMessage( S.Col.GOLD + "/sentry strength [0-2000000]" );
                sender.sendMessage( "  Sets the Sentries's Strength" );
            }
            if ( checkCommandPerm( S.PERM_ATTACK_RATE, sender ) ) {
                sender.sendMessage( S.Col.GOLD + "/sentry attackrate [0.0-30.0]" );
                sender.sendMessage( "  Sets the time between the Sentries's projectile attacks" );
            }
            if ( checkCommandPerm( S.PERM_HEAL_RATE, sender ) ) {
                sender.sendMessage( S.Col.GOLD + "/sentry healrate [0.0-300.0]" );
                sender.sendMessage( "  Sets the frequency the sentry will heal 1 point. 0 to disable." );
            }
            if ( checkCommandPerm( S.PERM_RANGE, sender ) ) {
                sender.sendMessage( S.Col.GOLD + "/sentry range [1-100]" );
                sender.sendMessage( "  Sets the Sentries's detection range" );
            }
            if ( checkCommandPerm( S.PERM_WARNING_RANGE, sender ) ) {
                sender.sendMessage( S.Col.GOLD + "/sentry warningrange [0-50]" );
                sender.sendMessage( "  How far beyond the detection range, that the Sentries will warn targets." );
            }
            if ( checkCommandPerm( S.PERM_NIGHTVISION, sender ) ) {
                sender.sendMessage( S.Col.GOLD + "/sentry nightvision [0-16] " );
                sender.sendMessage( "  0 = See nothing, 16 = See everything. " );
            }
            if ( checkCommandPerm( S.PERM_RESPAWN_DELAY, sender ) ) {
                sender.sendMessage( S.Col.GOLD + "/sentry respawn [-1-2000000]" );
                sender.sendMessage( "  Sets the number of seconds after death the Sentries will respawn." );
            }
            if ( checkCommandPerm( S.PERM_FOLLOW_DIST, sender ) ) {
                sender.sendMessage( S.Col.GOLD + "/sentry follow [0-32]" );
                sender.sendMessage( "  Sets the number of block away a bodyguard will follow. Default is 4" );
            }
            
            if ( sender instanceof ConsoleCommandSender )
                sender.sendMessage( String.join( "", S.Col.GOLD, "/sentry debug", S.Col.RESET, " - toggles the debug display on the console",
                        System.lineSeparator(), S.Col.RED, "Reduces performance! DO NOT enable unless you need it!" ) );

            if ( checkCommandPerm( S.PERM_RELOAD, sender ) )
                sender.sendMessage( String.join( "", S.Col.GOLD, "/sentry reload", S.Col.RESET, " - Reloads the config file" ) );
            
            sender.sendMessage( S.mainHelpOutro() );
            return true;
        }
        // ------------------------------------- Debug Command --------------
        if (    sender instanceof ConsoleCommandSender 
                && "debug".equalsIgnoreCase( inargs[0] ) ) {

            Sentries.debug = !Sentries.debug;
            sender.sendMessage( S.Col.GREEN + "Debug is now: " + (Sentries.debug ? S.ON : S.OFF) );
            return true;
        }
        // -------------------------------------- Reload Command ------------
        if ( S.RELOAD.equalsIgnoreCase( inargs[0] ) ) {

            if ( checkCommandPerm( S.PERM_RELOAD, sender ) ) {

                ((Sentries) Bukkit.getPluginManager().getPlugin( "Sentries" )).reloadMyConfig();
                sender.sendMessage( S.Col.GREEN + "reloaded Sentries's config.yml file" );
            }
            return true;
        }

        // the remaining commands all deal with npc's
        // -------------------------------------------------------------------------------------
        
        // did player specify an integer as the first argument?
        int npcid = Util.string2Int( inargs[0] );

        // if a positive number was found, the next argument to parse will be at position 1, otherwise 0.
        int nextArg = (npcid > 0) ? 1 : 0;

        if ( !enoughArgs( 1 + nextArg, inargs, sender ) ) return true;

        NPC thisNPC;
        // check to see whether the value saved is an npc ID, and save a reference if so.
        if ( npcid == -1 ) {

            thisNPC = CitizensAPI.getDefaultNPCSelector().getSelected( sender );

            if ( thisNPC == null ) {
                sender.sendMessage( S.ERROR.concat( S.ERROR_NO_NPC ) );
                return true;
            }
            npcid = thisNPC.getId();
        }
        else {
            thisNPC = CitizensAPI.getNPCRegistry().getById( npcid );

            if ( thisNPC == null ) {
                sender.sendMessage( String.join( "", S.ERROR, S.ERROR_ID_INVALID, String.valueOf( npcid ) ) );
                return true;
            }
        }
        // We are now sure that thisNPC is valid, and that npcid contains its id.
        if ( !thisNPC.hasTrait( SentryTrait.class ) ) {
            sender.sendMessage( S.ERROR.concat( S.ERROR_NOT_SENTRY ) );
            return true;
        }
        // OK, we have a sentry to modify.

        // We need to check that the player sending the command has the authority to use it.
        if (    sender instanceof Player
                && !sender.isOp()
                && !CitizensAPI.getNPCRegistry().isNPC( (Entity) sender ) ) {

            // TODO consider changing this section to allow admins to modify other players' npcs.

            if ( !thisNPC.getTrait( Owner.class ).getOwner().equalsIgnoreCase( sender.getName() ) ) {
                // player is not owner of the npc

                if ( !((Player) sender).hasPermission( S.PERM_CITS_ADMIN ) ) {
                    // player is not an admin either.

                    sender.sendMessage( S.Col.RED.concat( "You must be the owner of this Sentries to execute commands." ) );
                    return true;
                }
                if ( !thisNPC.getTrait( Owner.class ).getOwner().equalsIgnoreCase( "server" ) ) {
                    // not server-owned NPC

                    sender.sendMessage( S.Col.RED.concat( "You, or the server, must be the owner of this Sentries to execute commands." ) );
                    return true;
                }
            }
        }

        // We now know that player is either the owner, op'ed, or an admin with a server-owned npc. 
        SentryTrait inst = thisNPC.getTrait( SentryTrait.class );
        String npcName = thisNPC.getName();

        
        // ------------------------------ handle commands from separate classes -----    
        SentriesCommand command = commandMap.get( inargs[nextArg].toLowerCase() );

        if ( command != null ) {            
            if ( checkCommandPerm( command.getPerm(), sender ) ) {
                
                if ( command instanceof SentriesComplexCommand )
                    ((SentriesComplexCommand) command).call( sender, npcName, inst, nextArg, inargs ); 
                
                else if ( command instanceof SentriesToggleCommand ) {
                    
                    // This is held as an object not a primitive to allow for a third state - 'null'.
                    Boolean set = null;
                    
                    if ( inargs.length > nextArg + 2 ) {
                        sender.sendMessage( String.join( "", S.ERROR, "Too many arguments given.", Col.RESET, "This command accepts 1 argument (at most)." ) );
                        return true;
                    }
                    else if ( inargs.length == 2 + nextArg ) {
                        String arg = inargs[1 + nextArg];
                        if (    S.TRUE.equalsIgnoreCase( arg )
                                || S.ON.equalsIgnoreCase( arg ) )
                            set = true;
                        if (    S.FALSE.equalsIgnoreCase( arg )
                                || S.OFF.equalsIgnoreCase( arg ) )
                            set = false;
                    }                   
                    ((SentriesToggleCommand) command).call( sender, npcName, inst, set );
                }
                
                else if ( command instanceof SentriesNumberCommand ) {
                    
                    if ( inargs.length > nextArg + 2 )
                        sender.sendMessage( String.join( "", S.ERROR, "Too many arguments given.", Col.RESET, "Only one number value can be processesd." ) );
                    else {
                        String number = null;
                        if ( inargs.length == nextArg + 2 ) number = inargs[nextArg + 1];
                        
                        ((SentriesNumberCommand) command).call( sender, npcName, inst, number );                    
                    }
                }
            }
            return true;
        }

        // --------------------------------------------health command -------------------
        if ( S.HEALTH.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_HEALTH, sender ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    sender.sendMessage( String.join( "", S.Col.GOLD, npcName, "'s Health is ", String.valueOf( inst.sentryMaxHealth ) ) );
                    sender.sendMessage( S.Col.GOLD.concat( "Usage: /sentry health [#]   note: Typically players have 20 HPs when fully healed" ) );
                }
                else {
                    int HPs = Util.string2Int( inargs[nextArg + 1] );
                    if ( HPs < 1 ) {
                        sender.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( HPs > 2000000 ) HPs = 2000000;
                    
                    inst.sentryMaxHealth = HPs;
                    inst.setHealth( HPs );
                    sender.sendMessage( String.join( " ", S.Col.GREEN, npcName, "health set to", String.valueOf( HPs ) ) );
                }
            }
            return true;
        }
        // -------------------------------------------armour command-----------------------
        if (    S.ARMOUR.equalsIgnoreCase( inargs[nextArg] )
                || S.ARMOR.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_ARMOUR, sender ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    sender.sendMessage( String.join( "", S.Col.GOLD, npcName, "'s Armour is ", String.valueOf( inst.armour ) ) );
                    sender.sendMessage( S.Col.GOLD.concat( "Usage: /sentry armour [#] " ) );
                }
                else {
                    int armour = Util.string2Int( inargs[nextArg + 1] );
                    if ( armour < 0 ) {
                        sender.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( armour > 2000000 ) armour = 2000000;
                    
                    inst.armour = armour;
                    sender.sendMessage( String.join( " ", S.Col.GREEN, npcName, "armour set to", String.valueOf( armour ) ) );
                }
            }
            return true;
        }
        // --------------------------------------------strength command --------------
        if ( S.STRENGTH.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_STRENGTH, sender ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    sender.sendMessage( S.Col.GOLD + npcName + "'s Strength is " + inst.strength );
                    sender.sendMessage( S.Col.GOLD + "Usage: /sentry strength # " );
                    sender.sendMessage( S.Col.GOLD + "Note: At strength 0 the Sentries will do no damamge. " );
                }
                else {
                    int strength = Util.string2Int( inargs[nextArg + 1] );
                    if ( strength < 0 ) {
                        sender.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( strength > 2000000 ) strength = 2000000;
                    
                    inst.strength = strength;
                    sender.sendMessage( String.join( " ", S.Col.GREEN, npcName, "strength set to", String.valueOf( strength ) ) );
                }
            }
            return true;
        }
        // ----------------------------------------nightvision command---------
        // TODO add help text for this command
        if ( S.NIGHT_VISION.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_NIGHTVISION, sender ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    sender.sendMessage( S.Col.GOLD + npcName + "'s Night Vision is " + inst.nightVision );
                    sender.sendMessage( S.Col.GOLD + "Usage: /sentry nightvision [0-16] " );
                    sender.sendMessage( S.Col.GOLD + "Usage: 0 = See nothing, 16 = See everything. " );
                }
                else {
                    int vision = Util.string2Int( inargs[nextArg + 1] );
                    if ( vision < 0 ) {
                        sender.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }
                    if ( vision > 16 ) vision = 16;
                    
                    inst.nightVision = vision;
                    sender.sendMessage( String.join( " ", S.Col.GREEN, npcName, "Night Vision set to", String.valueOf( vision ) ) );
                }
            }
            return true;
        }
        // ------------------------------------respawn command------------------------
        if ( S.RESPAWN.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_RESPAWN_DELAY, sender ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    S.respawnCommandMessage( inst.respawnDelay, thisNPC, sender );

                    sender.sendMessage( S.Col.GOLD + "Usage: /sentry respawn [-1 - 2000000] " );
                    sender.sendMessage( S.Col.GOLD + "Usage: set to 0 to prevent automatic respawn" );
                    sender.sendMessage( S.Col.GOLD + "Usage: set to -1 to *permanently* delete the Sentry on death." );
                }
                else {
                    int respawn = Util.string2Int( inargs[nextArg + 1] );
                    if ( respawn < -1 ) {
                        sender.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( respawn > 2000000 ) respawn = 2000000;
                    
                    inst.respawnDelay = respawn;
                    S.respawnCommandMessage( inst.respawnDelay, thisNPC, sender );
                }
            }
            return true;
        }
        // ------------------------------------speed command--------------------------
        if ( S.SPEED.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_SPEED, sender ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    sender.sendMessage( S.Col.GOLD + npcName + "'s Speed is " + inst.speed );
                    sender.sendMessage( S.Col.GOLD + "Usage: /sentry speed [0.0 - 2.0]" );
                }
                else {
                    float speed = Util.string2Float( inargs[nextArg + 1] );
                    if ( speed < 0.0 ) {
                        sender.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( speed > 2.0 ) speed = 2.0f;
                    
                    inst.speed = speed;
                    sender.sendMessage( String.join( " ", S.Col.GREEN, npcName, "speed set to", String.valueOf( speed ) ) );
                }
            }
            return true;
        }
        // -----------------------------------attackrate command ---------
        if ( S.ATTACK_RATE.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_ATTACK_RATE, sender ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    sender.sendMessage( String.join( "", S.Col.GOLD, npcName, "'s Projectile Attack Rate is ", 
                                        String.valueOf( inst.attackRate), " seconds between shots." ) );
                    sender.sendMessage( S.Col.GOLD + "Usage: /sentry attackrate [0.0 - 30.0]" );
                }
                else {
                    double attackrate = Util.string2Double( inargs[nextArg + 1] );
                    if ( attackrate < 0.0 ) {
                        sender.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( attackrate > 30.0 ) attackrate = 30.0;
                    
                    inst.attackRate = attackrate;
                    sender.sendMessage( String.join( " ", S.Col.GREEN, npcName, "Projectile Attack Rate set to", String.valueOf( attackrate ) ) );
                }
            }
            return true;
        }
        // ----------------------------------------healrate command-----------------
        if ( S.HEALRATE.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_HEAL_RATE, sender ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    sender.sendMessage( S.Col.GOLD + npcName + "'s Heal Rate is " + inst.healRate + "s" );
                    sender.sendMessage( S.Col.GOLD + "Usage: /sentry healrate [0.0 - 300.0]" );
                    sender.sendMessage( S.Col.GOLD + "Usage: Set to 0 to disable healing" );
                }
                else {
                    double healrate = Util.string2Double( inargs[nextArg + 1] );
                    if ( healrate < 0.0 ) {
                        sender.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( healrate > 300.0 ) healrate = 300.0;
                    
                    inst.healRate = healrate;
                    sender.sendMessage( String.join( " ", S.Col.GREEN, npcName, "Heal Rate set to", String.valueOf( healrate ) ) );
                }
            }
            return true;
        }
        // --------------------------------------range command-----------------
        if ( S.RANGE.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_RANGE, sender ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    sender.sendMessage( S.Col.GOLD + npcName + "'s Range is " + inst.sentryRange );
                    sender.sendMessage( S.Col.GOLD + "Usage: /sentry range [1 - 100]" );
                }
                else {
                    int range = Util.string2Int( inargs[nextArg + 1] );
                    if ( range < 1 ) {
                        sender.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( range > 100 ) range = 100;
                    
                    inst.sentryRange = range;
                    sender.sendMessage( String.join( " ", S.Col.GREEN, npcName, "range set to", String.valueOf( range ) ) );
                }
            }
            return true;
        }
        // --------------------------------------warningrange command----------
        if ( S.WARNING_RANGE.equalsIgnoreCase( inargs[nextArg] ) ) {

            if ( checkCommandPerm( S.PERM_WARNING_RANGE, sender ) ) {

                if ( inargs.length <= 1 + nextArg ) {
                    sender.sendMessage( S.Col.GOLD + npcName + "'s Warning Range is " + inst.warningRange );
                    sender.sendMessage( S.Col.GOLD + "Usage: /sentry warningrangee [0 - 50]" );
                }
                else {
                    int range = Util.string2Int( inargs[nextArg + 1] );
                    if ( range < 0 ) {
                        sender.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
                        return true;
                    }

                    if ( range > 50 ) range = 50;
                    
                    inst.warningRange = range;
                    sender.sendMessage( String.join( " ", S.Col.GREEN, npcName, "warning range set to", String.valueOf( range ) ) );
                }
            }
            return true;
        }
        
        return false;
    }

    public static String parseTargetOrIgnore( String[] inargs, int nextArg,  String npcName, SentryTrait inst, boolean forTargets ) {

        String[] typeArgs = new String[inargs.length - (2 + nextArg)];
        System.arraycopy( inargs, 2 + nextArg, typeArgs, 0, inargs.length - (2 + nextArg) );

        if ( Sentries.debug ) Sentries.debugLog( "Target types list is:- " + Util.joinArgs( 0, typeArgs ) );

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
