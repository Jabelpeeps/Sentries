package org.jabelpeeps.sentry;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.aufdemrand.denizen.Denizen;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.trait.trait.Equipment;

public class Sentry extends JavaPlugin {

    public static boolean debug = true;

    static boolean dieLikePlayers = false;
    static boolean bodyguardsObeyProtection = true;
    static boolean ignoreListIsInvincible = true;

    static boolean denizenActive = false;
    static Map<String, PluginBridge> activePlugins = new HashMap<String, PluginBridge>();

    // Lists of various armour items that will be accepted, by default.
    public Set<Material> boots = EnumSet.of( Material.LEATHER_BOOTS,
            Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
            Material.DIAMOND_BOOTS, Material.GOLD_BOOTS );

    public Set<Material> chestplates = EnumSet.of( Material.LEATHER_CHESTPLATE,
            Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE,
            Material.DIAMOND_CHESTPLATE, Material.GOLD_CHESTPLATE );

    public Set<Material> helmets = EnumSet.of( Material.LEATHER_HELMET,
            Material.CHAINMAIL_HELMET, Material.IRON_HELMET,
            Material.DIAMOND_HELMET, Material.GOLD_HELMET, Material.PUMPKIN,
            Material.JACK_O_LANTERN );

    public Set<Material> leggings = EnumSet.of( Material.LEATHER_LEGGINGS,
            Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS,
            Material.DIAMOND_LEGGINGS, Material.GOLD_LEGGINGS );

    static Map<String, Integer> equipmentSlots = new HashMap<String, Integer>();

    Map<Material, Double> armorBuffs = new EnumMap<Material, Double>(
            Material.class );
    Map<Material, Double> speedBuffs = new EnumMap<Material, Double>(
            Material.class );
    Map<Material, Double> strengthBuffs = new EnumMap<Material, Double>(
            Material.class );
    Map<Material, List<PotionEffect>> weaponEffects = new EnumMap<Material, List<PotionEffect>>(
            Material.class );

    Map<String, Boolean> defaultBooleans = new HashMap<String, Boolean>();
    Map<String, Integer> defaultIntegers = new HashMap<String, Integer>();
    Map<String, Double> defaultDoubles = new HashMap<String, Double>();
    List<String> defaultTargets;
    List<String> defaultIgnores;
    String defaultGreeting = "";
    String defaultWarning = "";

    static int logicTicks = 10;
    static int sentryEXP = 5;

    public Queue<Projectile> arrows = new LinkedList<Projectile>();

    PluginManager pluginManager = getServer().getPluginManager();

    static Logger logger;
    static Plugin sentryPlugin;
    static DenizenHook denizenHook;

    @Override
    public void onEnable() {

        if ( sentryPlugin != null )
            return;

        sentryPlugin = this;
        logger = getLogger();

        if ( !checkPlugin( S.CITIZENS ) ) {
            logger.log( Level.SEVERE, S.ERROR_NO_CITIZENS );
            pluginManager.disablePlugin( this );
            return;
        }

        if ( checkPlugin( S.DENIZEN ) ) {

            String vers = pluginManager.getPlugin( S.DENIZEN ).getDescription()
                    .getVersion();

            if ( vers.startsWith( "0.9" ) ) {

                denizenHook = new DenizenHook(
                        (Denizen) pluginManager.getPlugin( S.DENIZEN ), this );
                denizenActive = DenizenHook.npcDeathTriggerActive
                        || DenizenHook.npcDeathTriggerOwnerActive
                        || DenizenHook.dieCommandActive
                        || DenizenHook.liveCommandActive;
            }
            else {
                logger.log( Level.WARNING, S.ERROR_WRONG_DENIZEN );
            }
        }
        equipmentSlots.put( "hand", 0 );
        equipmentSlots.put( "helmet", 1 );
        equipmentSlots.put( "chestplate", 2 );
        equipmentSlots.put( "leggings", 3 );
        equipmentSlots.put( "boots", 4 );

        int targetBitFlag = SentryInstance.bridges;

        for ( String each : getConfig().getStringList( "OtherPlugins" ) ) {

            if ( !checkPlugin( each ) )
                continue;

            PluginBridge bridge = null;

            try {
                @SuppressWarnings( "unchecked" )
                Class<? extends PluginBridge> clazz = (Class<? extends PluginBridge>) Class
                        .forName( S.PACKAGE + "pluginbridges." + each + "Bridge" );
                Constructor<? extends PluginBridge> constructor = clazz
                        .getDeclaredConstructor( int.class );

                bridge = constructor.newInstance( targetBitFlag );
            } catch ( ClassNotFoundException e ) {
                helpfulReflectionError( each );
                e.printStackTrace();
            } catch ( InstantiationException e ) {
                helpfulReflectionError( each );
                e.printStackTrace();
            } catch ( IllegalAccessException e ) {
                helpfulReflectionError( each );
                e.printStackTrace();
            } catch ( IllegalArgumentException e ) {
                helpfulReflectionError( each );
                e.printStackTrace();
            } catch ( InvocationTargetException e ) {
                helpfulReflectionError( each );
                e.printStackTrace();
            } catch ( NoSuchMethodException e ) {
                helpfulReflectionError( each );
                e.printStackTrace();
            } catch ( SecurityException e ) {
                helpfulReflectionError( each );
                e.printStackTrace();
            }

            if ( bridge == null )
                continue;
            if ( !bridge.activate() ) {
                logger.log( Level.INFO, bridge.getActivationMessage() );
                continue;
            }

            if ( debug )
                logger.log( Level.INFO,
                        each + " activated with bitFlag value of "
                                + bridge.getBitFlag() );
            logger.log( Level.INFO, bridge.getActivationMessage() );

            targetBitFlag *= 2;

            activePlugins.put( each, bridge );
        }

        CitizensAPI.getTraitFactory().registerTrait(
                TraitInfo.create( SentryTrait.class ).withName( "sentry" ) );

        pluginManager.registerEvents( new SentryListener( this ), this );

        final Runnable removeArrows = new Runnable() {

            @Override
            public void run() {

                while ( arrows.size() > 200 ) {
                    Projectile arrow = arrows.remove();

                    if ( arrow != null )
                        arrow.remove();
                }
            }
        };
        getServer().getScheduler().scheduleSyncRepeatingTask( this,
                removeArrows, 40, 20 * 120 );

        reloadMyConfig();
    }

    private void helpfulReflectionError( String name ) {
        logger.log( Level.WARNING, "Error loading PluginBridge for the value: '"
                + name + "' from config.yml. " );
    }

    void reloadMyConfig() {

        saveDefaultConfig();
        // load the contents of the config.yml from the disk.
        reloadConfig();

        FileConfiguration config = getConfig();

        loadIntoMaterialMap( config, "ArmorBuffs", armorBuffs );
        loadIntoMaterialMap( config, "StrengthBuffs", strengthBuffs );
        loadIntoMaterialMap( config, "SpeedBuffs", speedBuffs );

        loadPotions( config, "WeaponEffects", weaponEffects );

        loadIntoSet( config, "Helmets", helmets );
        loadIntoSet( config, "Chestplates", chestplates );
        loadIntoSet( config, "Leggings", leggings );
        loadIntoSet( config, "Boots", boots );

        AttackType.loadWeapons( config );
        Hits.loadConfig( config );

        dieLikePlayers = config.getBoolean( "Server.DieLikePlayers" );
        bodyguardsObeyProtection = config
                .getBoolean( "Server.BodyguardsObeyProtection", true );
        ignoreListIsInvincible = config
                .getBoolean( "Server.IgnoreListInvincibility", true );

        logicTicks = config.getInt( "Server.LogicTicks", 10 );
        sentryEXP = config.getInt( "Server.ExpValue", 5 );

        loadIntoStringMap( config, "DefaultOptions", defaultBooleans );
        loadIntoStringMap( config, "DefaultStats", defaultIntegers );
        loadIntoStringMap( config, "DefaultValues", defaultDoubles );
        defaultTargets = config.getStringList( S.DEFAULT_TARGETS );
        defaultIgnores = config.getStringList( S.DEFAULT_IGNORES );
        defaultWarning = config.getString( "DefaultTexts.Warning" );
        defaultGreeting = config.getString( "DefaultTexts.Greeting" );
    }

    /**
     * Sends a message to the logger associated with this plugin.
     * <p>
     * The caller should check the boolean Sentry.debug is true before calling -
     * to avoid the overhead of compiling the String if it is not needed.
     * 
     * @param s
     *            - the message to log.
     */
    public static void debugLog( String s ) {
        logger.info( s );
    }

    boolean equip( NPC npc, SentryInstance inst, ItemStack newEquipment ) {

        Equipment equipment = npc.getTrait( Equipment.class );
        if ( equipment == null )
            return false;
        // the npc's entity type does not support equipment.

        if ( newEquipment == null ) {

            for ( int i = 0; i < 5; i++ ) {

                if ( equipment.get( i ) != null
                        && equipment.get( i ).getType() != Material.AIR ) {
                    equipment.set( i, null );
                }
            }
            return true;
        }
        int slot = 0;
        Material type = newEquipment.getType();

        // First, determine the slot to edit
        if ( helmets.contains( type ) )
            slot = 1;
        else if ( chestplates.contains( type ) )
            slot = 2;
        else if ( leggings.contains( type ) )
            slot = 3;
        else if ( boots.contains( type ) )
            slot = 4;

        equipment.set( slot, newEquipment );

        if ( slot == 0 )
            inst.updateAttackType();

        return true;
    }

    public SentryInstance getSentryInstance( Entity ent ) {

        if ( ent != null && ent instanceof LivingEntity ) {
            return getSentryInstance(
                    CitizensAPI.getNPCRegistry().getNPC( ent ) );
        }
        return null;
    }

    public SentryInstance getSentryInstance( NPC npc ) {

        if ( npc != null && npc.hasTrait( SentryTrait.class ) ) {
            return npc.getTrait( SentryTrait.class ).getInstance();
        }
        return null;
    }

    @Override
    public boolean onCommand( CommandSender player, Command cmd,
            String cmdLabel, String[] inargs ) {

        return CommandHandler.call( player, inargs, this );
    }

    @Override
    public void onDisable() {

        logger.log( Level.INFO,
                " v" + getDescription().getVersion() + " disabled." );
        Bukkit.getServer().getScheduler().cancelTasks( this );
    }

    public void loadIntoSet( FileConfiguration config, String key,
            Set<Material> set ) {

        if ( config.getBoolean( "UseCustom" + key ) ) {

            List<String> strings = config.getStringList( key );

            if ( strings.size() > 0 ) {
                if ( debug )
                    debugLog( strings.toString() );
                set.clear();

                for ( String each : strings )
                    set.add( Util.getMaterial( each.trim() ) );
            }
        }
    }

    private void loadIntoMaterialMap( FileConfiguration config, String node,
            Map<Material, Double> map ) {
        map.clear();

        for ( String each : config.getStringList( node ) ) {
            if ( debug )
                debugLog( each );

            String[] args = each.trim().split( " " );

            if ( args.length != 2 )
                continue;

            double val = Util.string2Double( args[1] );
            Material item = Util.getMaterial( args[0] );

            if ( item != null && val > 0 && !map.containsKey( item ) ) {

                map.put( item, val );
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    private <T> void loadIntoStringMap( FileConfiguration config, String node,
            Map<String, T> map ) {

        map.clear();
        map.putAll( (Map<String, T>) config.getConfigurationSection( node )
                .getValues( false ) );
    }

    private void loadPotions( FileConfiguration config, String path,
            Map<Material, List<PotionEffect>> map ) {
        map.clear();

        for ( String each : config.getStringList( path ) ) {
            String[] args = each.trim().split( " " );

            if ( args.length < 2 )
                continue;

            Material item = Util.getMaterial( args[0] );

            if ( item == null )
                continue;

            List<PotionEffect> list = new ArrayList<PotionEffect>();

            for ( String string : args ) {

                PotionEffect val = getPotionEffect( string );

                if ( val != null )
                    list.add( val );
            }
            if ( !list.isEmpty() )
                map.put( item, list );
        }
    }

    private PotionEffect getPotionEffect( String string ) {
        if ( string == null )
            return null;

        String[] args = string.trim().split( ":" );

        int dur = 10;
        int amp = 1;

        PotionEffectType type = PotionEffectType.getByName( args[0] );

        if ( type == null )
            return null;

        if ( args.length > 1 ) {
            dur = Util.string2Int( args[1] );

            if ( dur < 0 )
                dur = 10;
        }

        if ( args.length > 2 ) {
            amp = Util.string2Int( args[2] );

            if ( amp < 0 )
                amp = 1;
        }
        return new PotionEffect( type, dur, amp );
    }

    /**
     * Check if a named plugin is loaded and enabled.
     * <p>
     * If returning false, this method also calls:-<br>
     * getLogger().log( Level.INFO, "Could not find or register with " + name )
     * 
     * @param name
     *            - the name of the plugin.
     * @return true - if it is loaded <strong>and</strong> enabled.
     */
    private boolean checkPlugin( String name ) {

        if ( S.SCORE.equals( name ) )
            return true;

        if ( pluginManager.getPlugin( name ) != null
                && pluginManager.getPlugin( name ).isEnabled() ) {

            if ( debug )
                debugLog( name + " found by bukkit/spigot." );

            return true;
        }
        logger.log( Level.INFO, S.ERROR_PLUGIN_NOT_FOUND.concat( name ) );

        return false;
    }

    /**
     * @return the current instance of Sentry, for calls that cannot be made
     *         statically.
     */
    public static Plugin getSentry() {
        return sentryPlugin;
    }
}
