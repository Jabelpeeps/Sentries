package org.jabelpeeps.sentries;

import java.text.DecimalFormat;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.commands.SentriesCommand;

import net.citizensnpcs.api.ai.NavigatorParameters;
import net.citizensnpcs.api.npc.NPC;

/**
 * An abstract collection of useful static methods.
 */
public abstract class Utils {
    final static double angle = Math.toRadians( 45 ); 
    final static double cos = Math.cos( angle );   
    final static double sin = Math.sin( angle );
    
    public static double sqr( double d ) { return d * d; }
    
    /** 
     * Calculate the maximum range that a ballistic projectile can be fired on given speed and gravity.
     *
     * @param speed: projectile velocity
     * @param gravity: force of gravity, positive is down
     * @param initial_height: distance above flat terrain
     *
     * @return the maximum range
     */
     public static double getRange( double v, double g, double d ) {
         return ( v * cos / g ) * ( v * sin + Math.sqrt( sqr( v ) * sqr( sin ) + 2 * g * d ) );
     }

     /** 
      * Solve firing angles for a ballistic projectile with speed and gravity to hit a fixed position.
     *
     * @param myLoc - point projectile will fire from
     * @param v - scalar speed of projectile
     * @param targetLoc - point projectile is trying to hit
     * @param g - force of gravity, positive down
    
     *
     * @return the low-angle Vector to hit the target.
     */
     public static Vector getFiringVector( Vector myLoc, double v, Vector targetLoc, double g ) {
    
         Vector diff = targetLoc.subtract( myLoc );
         Vector diffXZ = new Vector( diff.getX(), 0.0, diff.getZ() );
         double groundDist = diffXZ.length();
         diffXZ.normalize();
    
         double speed2 = sqr( v );
         double y = diff.getY();
    
         double root = sqr( speed2 ) - g * ( g * sqr( groundDist ) + 2 * y * speed2 );
    
         // No solution
         if ( root < 0 ) return null;
    
         double lowAng = Math.atan2( speed2 - Math.sqrt( root ), g * groundDist );
    
         return diffXZ.multiply( Math.cos( lowAng ) ).multiply( v ).setY( Math.sin( lowAng ) * v );
     }

     public static void copyNavParams( NavigatorParameters from, NavigatorParameters to ) {
         to.attackRange( from.attackRange() );
         to.attackDelayTicks( from.attackDelayTicks() );
     }
     
    public static void removeMount( int npcid ) {

        NPC npc = Sentries.registry.getById( npcid );

        if ( npc != null ) {
            if ( npc.getEntity() != null ) {
                npc.getEntity().setPassenger( null );
            }
            npc.destroy();
        }
    }

    /**
     * checks a player's permissions for having a bodyguard in the specified world.
     * (the sentry.bodyguard.<world-name> permission.)
     * 
     * @param Entity
     *            entity this is checked to be an instance of Player, and then has perms checked
     * @param worldname
     *            a String identifying the world to be checked.
     * 
     * @returns true if player has permission "sentry.bodyguard.xxx" for the current world.
     */
    public static boolean CanWarp( Entity entity, String worldname ) {

        if ( entity instanceof Player ) {

            Player player = (Player) entity;
            String worldPerm = S.PERM_BODYGUARD + worldname;
            
            if ( player.hasPermission( S.PERM_BODYGUARD + "*" ) ) {
                // all players have "*" perm by default.
               
                if (    player.isPermissionSet( worldPerm )
                        && !player.hasPermission( worldPerm ) ) {
                    // denied in this world.
                    return false;
                }
                return true;
            }
            if ( player.hasPermission( worldPerm ) ) {
                // no "*"" but specifically allowed this world.
                return true;
            }
        }
        return false;
    }

    /**
     * Reformat the supplied String, replacing the tags <NPC>, <PLAYER>, <ITEM>,
     * & <AMOUNT> with the names of the objects supplied as arguments, and
     * translating any colour codes.
     * 
     * The method will return immediately if 'input' is null, and will remove
     * the tags related to any other arguments that are null objects.
     */
    public static String format( String input, NPC npc, CommandSender player, Material item, String amount ) {

        if ( input == null ) return "";

        input = input.replace( "<NPC>", (npc == null) ? "" : npc.getName() );
        input = input.replace( "<PLAYER>", (player == null) ? "" : player.getName() );
        input = input.replace( "<ITEM>", (item == null) ? "" : Utils.getLocalItemName( item ) );
        input = input.replace( "<AMOUNT>", (amount == null) ? "" : amount );

        return input;
    }
    
    static Pattern initialDoubleQuote = Pattern.compile( "^\"" );
    static Pattern endDoubleQuote = Pattern.compile( "\"$" );
    static Pattern initialSingleQuote = Pattern.compile( "^'" );
    static Pattern endSingleQuote = Pattern.compile( "'$" );

    /**
     * Convenience method that removes single and double quotes from the ends of
     * the supplied string.
     * 
     * @param input
     *            - the string to be parsed
     * @return - the string without quotes
     */
    public static String removeQuotes( String input ) {

        input = initialDoubleQuote.matcher( input ).replaceAll( "" );
        input = endDoubleQuote.matcher( input ).replaceAll( "" );
        input = initialSingleQuote.matcher( input ).replaceAll( "" );
        input = endSingleQuote.matcher( input ).replaceAll( "" );

        return input;
    }
    
    /**
     * Returns the name of the material or item matching the supplied ID, or
     * "Hand".
     * 
     * @param int
     *            MatID the ID to be named.
     */
    static String getLocalItemName( Material mat ) {

        if ( mat == null || mat == Material.AIR )
            return "Hand";

        return mat.name();
    }

    /**
     * method to convert String values to int's.
     * <p>
     * It catches any NumberFormatExceptions and returns Integer.MIN_VALUE instead.
     * 
     * @param value
     *            - the string to be converted
     * @return the int value represented by the string.
     */
    public static int string2Int( String value ) {
        try {
            return Integer.parseInt( value );
        } catch ( NumberFormatException e ) {
            return Integer.MIN_VALUE;
        }
    }

    /**
     * method to convert String values to floats.
     * <p>
     * It catches any NumberFormatExceptions and returns Float.MIN_VALUE instead.
     * 
     * @param value
     *            - the string to be converted
     * @return the float value represented by the string.
     */
    public static float string2Float( String value ) {
        try {
            return Float.parseFloat( value );
        } catch ( NumberFormatException e ) {
            return Float.MIN_VALUE;
        }
    }

    /**
     * method to convert String values to doubles.
     * <p>
     * It catches any NumberFormatExceptions and returns Double.MIN_VALUE instead.
     * 
     * @param value
     *            - the string to be converted
     * @return the double value represented by the string.
     */
    public static double string2Double( String value ) {
        try {
            return Double.parseDouble( value );
        } catch ( NumberFormatException e ) {
            return Double.MIN_VALUE;
        }
    }

    public static SentryTrait getSentryTrait( Entity ent ) {
    
        if ( ent != null && ent instanceof LivingEntity ) {
            return getSentryTrait( Sentries.registry.getNPC( ent ) );
        }
        return null;
    }

    static SentryTrait getSentryTrait( NPC npc ) {
    
        if ( npc != null && npc.hasTrait( SentryTrait.class ) ) {
            return npc.getTrait( SentryTrait.class );
        }
        return null;
    }
    
    /**
     * Returns the shooter of a projectile, if possible. (or the source of primed TNT)
     * @param damager - the entity that did some damage.
     * @return damager - if it is already an instanceof living entity. 
     *         The shooter or source - if damager is a projectile or primed TNT.
     *         otherwise - null.
     */
    static Entity getArcher( Entity damager ) {
        
        if ( damager instanceof LivingEntity ) return damager;
        
        if ( damager instanceof Projectile ) {
            ProjectileSource source = ((Projectile) damager).getShooter();

            if ( source instanceof Entity ) return (Entity) source;
        }  
        else if ( damager instanceof TNTPrimed ) 
            return ((TNTPrimed) damager).getSource();
        
        return null;
    }
    
    /**
     * Concatenates the supplied String[] starting at the position indicated.
     * 
     * @param startFrom
     *            - the starting position (zero-based)
     * @param args
     *            - the String[] to be joined
     * @param delimiter
     *            - a string to be inserted between the joined array's values.
     * @return - the resulting String.
     */
    public static String joinArgs( int startFrom, String[] args, String delimiter ) {

        StringJoiner joiner = new StringJoiner( delimiter );
    
        for ( int i = startFrom; i < args.length; i++ ) {
            joiner.add( args[i] );
        }
        return joiner.toString();
    }
    
    /**
     * Concatenates the supplied String[] starting at the position indicated. 
     * Inserts a space between the values of the array.
     * 
     * @param startFrom
     *            - the starting position (zero-based)
     * @param args
     *            - the String[] to be joined
     * @return - the resulting String.
     */
    public static String joinArgs( int startFrom, String[] args ) {
        return joinArgs( startFrom, args, " " );
    }
    
    /**
     * Convenience method to send messages. Before sending it performs a null-check
     * on sender, and then calls String.join() on the String args, with a delimiter of ""
     */
    public static void sendMessage( CommandSender sender, String...strings ) {
        if ( sender != null ) 
            sender.sendMessage( String.join( "", strings ) );
        else if ( Sentries.debug )
            Sentries.debugLog( String.join( "", strings ));
    }

    public static Pattern colon = Pattern.compile( ":" );
    
    /**
     * Static method to iterate over the activated PluginBridges, polling each one for command
     * help text.
     * 
     * @return - the concatenated help Strings
     */
    public static String getAdditionalTargets() {
        
        if ( !Sentries.activePlugins.isEmpty() ) {
            StringJoiner joiner = new StringJoiner( System.lineSeparator() );
            joiner.add( "These additional options are available:- " ); 
            
            SentriesCommand command = CommandHandler.getCommand( S.EVENT );
            if ( command != CommandHandler.nullCommand ) {
                joiner.add( String.join( "", Col.GOLD, "/sentry ", S.EVENT, Col.WHITE, command.getShortHelp() ) );
            }
            
            Sentries.activePlugins.parallelStream()
                                  .filter( p -> p instanceof PluginTargetBridge )
                                  .forEach( p -> joiner.add( ((PluginTargetBridge) p).getCommandHelp() ) );
            
            return joiner.toString();
        }
        return "";
    }

    private static DecimalFormat df = new DecimalFormat( "0.0#" );
    
    public static String prettifyLocation( Location loc ) {
        return String.join( " ", "World:", loc.getWorld().getName(), "at X:", df.format( loc.getX() ), 
                           " Y:", df.format(  loc.getY() ), " Z:", df.format(  loc.getZ() ) );
    }
}
