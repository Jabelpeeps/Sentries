package net.aufdemrand.sentry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
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
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.milkbowl.vault.permission.Permission;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;


public class Sentry extends JavaPlugin {

	boolean debug = false;
	
	public boolean dieLikePlayers = false;
	public boolean bodyguardsObeyProtection = true;
	public boolean ignoreListIsInvincible = true;

	static Permission perms = null;
	
	public boolean groupsChecked = false;
	
	// Denizen, Factions, Towny, War & SimpleClans Support
	static boolean factionsActive = false;
	static boolean townyActive = false;
	static boolean warActive = false;
	static boolean clansActive = false;
	static boolean denizenActive = false;
	
	// fields to support the critical hits system
//	public int crit1Chance;
//	public int crit2Chance;
//	public int crit3Chance;
//	public int glanceChance;
//	public int missChance;
//	
	// Lists of various armour items that will be accepted 
//	public List<Integer> boots = new LinkedList<Integer>( Arrays.asList(301,305,309,313,317) );
	public Set<Material> boots = EnumSet.of( Material.LEATHER_BOOTS, 
											 Material.CHAINMAIL_BOOTS, 
											 Material.IRON_BOOTS, 
											 Material.DIAMOND_BOOTS, 
											 Material.GOLD_BOOTS );
	
//	public List<Integer> chestplates = new LinkedList<Integer>( Arrays.asList(299,303,307,311,315) );
	public Set<Material> chestplates = EnumSet.of( Material.LEATHER_CHESTPLATE, 
												   Material.CHAINMAIL_CHESTPLATE, 
												   Material.IRON_CHESTPLATE, 
												   Material.DIAMOND_CHESTPLATE, 
												   Material.GOLD_CHESTPLATE );
	
//	public List<Integer> helmets = new LinkedList<Integer>( Arrays.asList(298,302,306,310,314,91,86) );
	public Set<Material> helmets = EnumSet.of( Material.LEATHER_HELMET,
											   Material.CHAINMAIL_HELMET,
											   Material.IRON_HELMET, 
											   Material.DIAMOND_HELMET, 
											   Material.GOLD_HELMET, 
											   Material.PUMPKIN, 
											   Material.JACK_O_LANTERN );
	
//	public List<Integer> leggings = new LinkedList<Integer>( Arrays.asList(300,304,308,312,316) );
	public Set<Material> leggings = EnumSet.of( Material.LEATHER_LEGGINGS, 	
												Material.CHAINMAIL_LEGGINGS,
												Material.IRON_LEGGINGS,
												Material.DIAMOND_LEGGINGS,
												Material.GOLD_LEGGINGS );

	public Map<Material, Double> armorBuffs = new EnumMap<Material, Double>( Material.class );
	public Map<Material, Double> speedBuffs = new EnumMap<Material, Double>( Material.class );
	public Map<Material, Double> strengthBuffs = new EnumMap<Material, Double>( Material.class );
	public Map<Material, List<PotionEffect>> weaponEffects = new EnumMap<Material, List<PotionEffect>>( Material.class );

	public int logicTicks = 10;	
	public int sentryEXP = 5;
	
	public Queue<Projectile> arrows = new LinkedList<Projectile>(); 
	
	// saved in field for readability as the combination of calls is often called together.
	PluginManager pluginManager = getServer().getPluginManager();
	
	static Logger logger;
	
	
	@Override
	public void onEnable() {
		
		logger = getLogger();

		if ( !checkPlugin( "Citizens" ) ) {			
			logger.log( Level.SEVERE, "Sentry cannot be loaded without Citizens 2.0. Aborting." );
			pluginManager.disablePlugin( this );	
			return;
		}	

		try {
			if ( checkPlugin( "Denizen" ) ) {
				
				String vers = pluginManager.getPlugin( "Denizen" ).getDescription().getVersion();
				if ( vers.startsWith( "0.7" ) || vers.startsWith( "0.8" ) ) {
					logger.log( Level.WARNING, "Sentry is not compatible with Denizen .7 or .8" );
				}
				else if (  vers.startsWith("0.9" ) ) {
					DenizenHook.sentryPlugin = this;
					DenizenHook.denizenPlugin = pluginManager.getPlugin("Denizen");
					DenizenHook.setupDenizenHook();
					denizenActive = true;
					logger.log( Level.INFO, "NPCDeath Triggers and DIE/LIVE command registered sucessfully with Denizen" );
				}
				else {
					logger.log( Level.WARNING, "Unknown version of Denizen, Sentry was unable to register with it." );
				}
			}
		} catch ( NoClassDefFoundError e ) {
			logger.log( Level.WARNING, "An error occured attempting to register with Denizen " + e.getMessage() );
		} catch ( Exception e ) {
			logger.log( Level.WARNING, "An error occured attempting to register with Denizen " + e.getMessage() );
		}

		if ( checkPlugin( "Towny" ) ) {
			logger.log( Level.INFO, "Registered with Towny sucessfully. the TOWN: and NATION: targets will function" );
			townyActive = true;
		}

		if ( checkPlugin( "Factions" ) ) {
			logger.log( Level.INFO, "Registered with Factions sucessfully. the FACTION: target will function" );
			factionsActive = true;
		}
		
		if ( checkPlugin( "War" ) ) {
			logger.log( Level.INFO, "Registered with War sucessfully. The TEAM: target will function" );
			warActive = true;
		}

		if ( checkPlugin( "SimpleClans" ) ) {
			logger.log( Level.INFO,"Registered with SimpleClans sucessfully. The CLAN: target will function" );
			clansActive = true;
		}

		CitizensAPI.getTraitFactory().registerTrait( TraitInfo.create( SentryTrait.class ).withName( "sentry" ) );

		pluginManager.registerEvents( new SentryListener( this ), this);

		final Runnable removeArrows = new Runnable() {
				@Override
				public void run() {
					
					while ( arrows.size() > 200 ) {
						
						Projectile arrow = arrows.remove();
						
						if ( arrow != null ) arrow.remove();
					}
				}
		};
		
		getServer().getScheduler().scheduleSyncRepeatingTask( this, removeArrows, 40,  20 * 120 );

		reloadMyConfig();
	}
	
	void reloadMyConfig() {
		
		// create a default config.yml if it doesn't already exist.
		saveDefaultConfig();
		// load the contents of the config.yml from the disk.
		reloadConfig();
		
		loadmap( "ArmorBuffs", armorBuffs );
		loadmap( "StrengthBuffs", strengthBuffs );
		loadmap( "SpeedBuffs", speedBuffs );
		
		loadPotions( "WeaponEffects", weaponEffects );
		
		loadItemList( "Helmets", helmets );
		loadItemList( "Chestplates", chestplates );
		loadItemList( "Leggings", leggings );
		loadItemList( "Boots", boots );
		
		FileConfiguration config = getConfig();

		AttackType.loadWeapons( config );
		
		dieLikePlayers = config.getBoolean( "Server.DieLikePlayers", false );
		bodyguardsObeyProtection = config.getBoolean( "Server.BodyguardsObeyProtection", true );
		ignoreListIsInvincible =  config.getBoolean( "Server.IgnoreListInvincibility", true );
		
		logicTicks = 	config.getInt( "Server.LogicTicks", 10 );
		sentryEXP = 	config.getInt( "Server.ExpValue", 5 );
		
		Hits.loadConfig( config );
	}

	/**
	 * Sends a message to the logger associated with this plugin - after checking the value of boolean 'debug' (false = don't send).
	 * @param s - the message to log.
	 */
	public void debug( String s ){
		if ( debug ) logger.info( s );
	}

	public void doGroups() {
		if ( !setupPermissions() ) 
			logger.log( Level.WARNING,"Could not register with Vault!  the GROUP target will not function." );
		else {
			try {
				String[] groups = perms.getGroups();
				if ( groups.length == 0 ) {
					logger.log( Level.WARNING,"No permission groups found.  the GROUP target will not function.");
					perms = null;
				}
				else logger.log( Level.INFO,"Registered sucessfully with Vault: " + groups.length 
														+ " groups found. The GROUP: target will function" );

			} catch ( Exception e ) {
				logger.log( Level.WARNING,"Error getting groups.  the GROUP target will not function.");
				perms = null;
			}	
		}
		groupsChecked = true;
	}

	boolean equip( NPC npc, ItemStack newEquipment ) {
		
		Equipment equipment = npc.getTrait( Equipment.class );
		if ( equipment == null ) return false;
				// the npc's entity type does not support equipment.
		
		if ( newEquipment == null ) {
			
			for ( int i = 0; i < 5; i++ ) {
				
				if ( equipment.get( i ) != null 
				  && equipment.get( i ).getType() != Material.AIR ) {
	//				try {
						equipment.set( i , null );
	//				} catch ( Exception e ) { }   
				}
			}
			return true;
		}
		
		int slot = 0;
		Material type = newEquipment.getType();
		
		// First, determine the slot to edit
		if      ( helmets.contains( type ) ) slot = 1;
		else if ( chestplates.contains( type ) ) slot = 2;
		else if ( leggings.contains( type ) ) slot = 3;
		else if ( boots.contains( type ) ) slot = 4;	
	
	// Removed as unnecessary, ItemStack defaults to a stack size of 1 
	//	@SuppressWarnings("null")
	//	ItemStack clone = newEquipment.clone();
	//	clone.setAmount( 1 );

	//  Why do we have to check for exceptions here? 
		
	//	try {
			equipment.set( slot, newEquipment );
	//	} catch ( Exception e ) {
	//		return false;
	//	}
		return true;	
	}

	public  String getClan( Player player ) {
		if ( clansActive ) {
			try {
				Clan clan = SimpleClans.getInstance().getClanManager().getClanByPlayerName( player.getName() );
				
				if ( clan != null ) 
					return clan.getName();
				
			} catch ( Exception e ) {
				logger.info( "Error getting Clan " + e.getMessage() );
			}
		}
		return null;
	}
	
	/** New method with this name, now re-written to return Material values from the official enum. */
    static Material getMaterial( String materialName ) {
		
		if ( materialName == null ) return null;

		String[] args = materialName.toUpperCase().split( ":" );

		Material material = Material.getMaterial( args[0] );

		if ( material == null ) 
			throw new RuntimeException("Invalid Material name:" + materialName + ". Please check config.yml carefully.");
		
		return material;
	}
	
	public SentryInstance getSentry( Entity ent ) {
		
		if  (  ent != null 
			&& ent instanceof LivingEntity ) {
				return getSentry( CitizensAPI.getNPCRegistry().getNPC( ent ) );
		}			
		return null;
	}

	public SentryInstance getSentry( NPC npc ) {
		
		if ( npc != null 
		  && npc.hasTrait( SentryTrait.class ) ) {
				return npc.getTrait( SentryTrait.class ).getInstance();
		}
		return null;
	}

	// deprecated call to "getPlayerTeam" replaced
	public String getMCTeamName( Player player ) {
		
		Team team = getServer().getScoreboardManager().getMainScoreboard().getEntryTeam( player.getName() );
		
		if ( team != null ) {
			return team.getName();
		}
		return null;
	}

	@Override
	public boolean onCommand( CommandSender player, Command cmd, String cmdLabel, String[] inargs ) {
		
		return CommandHandler.call( player, inargs, this );
	}
	
	@Override
	public void onDisable() {

		logger.log( Level.INFO, " v" + getDescription().getVersion() + " disabled." );
		Bukkit.getServer().getScheduler().cancelTasks( this );
	}
	
	public void loadItemList( String key, Set<Material> set ) {
		
		if ( getConfig().getBoolean( "UseCustom" + key ) ) {
			
			List<String> strings = getConfig().getStringList( key );
	
			if ( strings.size() > 0 ) {
				
				set.clear();
			
				for ( String each : strings ) {
					set.add( getMaterial( each.trim() ) );
				}
			}
		}
	}

	private void loadmap( String node, Map<Material, Double> map ) {
		map.clear();
		
		for ( String each : getConfig().getStringList( node ) ) {
			String[] args = each.trim().split(" ");
			
			if ( args.length != 2 ) continue;

			double val = 0;

			try {
				val = Double.parseDouble( args[1] );
			} catch (Exception e) { }

			Material item = getMaterial( args[0] );

			if ( item != null 
			  && val != 0 
			  && !map.containsKey( item ) ) {
				
				map.put( item, val );
			}
		}
	}
	
	private void loadPotions( String path, Map<Material, List<PotionEffect>> map ) {
		map.clear();
		
		for ( String each : getConfig().getStringList( path ) ) {
			String[] args = each.trim().split(" ");

			if ( args.length < 2 ) continue;

			Material item  = getMaterial( args[0] );
			
			if ( item == null ) continue;

			List<PotionEffect> list = new ArrayList<PotionEffect>();

			for ( String string : args ) {
				
				PotionEffect val = getPotionEffect( string );
				
				if ( val != null ) list.add( val );
			}
			if ( !list.isEmpty() ) {
				
				map.put( item, list );
			}
		}
	}

	@SuppressWarnings("deprecation")
	private PotionEffect getPotionEffect( String string ) {
		if ( string == null ) return null;
		
		String[] args = string.trim().split(":");

		int dur = 10;
		int amp = 1;

		PotionEffectType type = PotionEffectType.getByName( args[0] );

		if ( type == null ) {
			// TODO this block appears to be using the deprecated method as a backup, let see if it's needed.
			logger.info( "getByName() in Sentry.getpot() returned null trying getById" );
			type = PotionEffectType.getById( Util.string2Int( args[0] ) );
		}
		if ( type == null ) 
			return null;
	//	else 
	//		getLogger().info( type.toString() + " found, watch out!" );

		if ( args.length > 1 ) {
			dur = Util.string2Int( args[1] );
			
			if ( dur < 0 ) dur = 10;
			
	//		try {
	//			dur = Integer.parseInt( args[1] );
	//		} catch ( NumberFormatException e ) { }
			
		}

		if ( args.length > 2 ) {
			amp = Util.string2Int( args[2] );
			
			if ( amp < 0 ) amp = 1; 
			
	//		try {
	//			amp = Integer.parseInt( args[2] );
	//		} catch ( NumberFormatException e ) { }
			
		}
		return new PotionEffect( type, dur, amp );
	}
	
	/**
	 * Check if a named plugin is loaded and enabled.
	 * <p>
	 * If returning false, this method also calls:-<br>
	 * getLogger().log( Level.INFO, "Could not find or register with " + name )
	 * 
	 * @param name - the name of the plugin.
	 * @return true - if it is loaded <strong>and</strong> enabled.
	 */
	private boolean checkPlugin( String name ) {
		if 	(  pluginManager.getPlugin( name ) != null 
			&& pluginManager.getPlugin( name ).isEnabled() == true ) {
				return true;
		}
		logger.log( Level.INFO, "Could not find or register with " + name );
		
		return false;
	}

	/**
	 * Store reference to the server's current instance of Vault in perms field.
	 * 
	 * @return true if a permission provider has been stored in 'perms'. 
	 * Otherwise (if Vault is not enabled, or if an exception if thrown) returns false.
	 */
	private boolean setupPermissions() {
		try {
			if ( checkPlugin( "Vault" ) ) {
			
				RegisteredServiceProvider<Permission> permissionProvider = 
																getServer().getServicesManager()
																		   .getRegistration( Permission.class );
				if ( permissionProvider != null ) {
					perms = permissionProvider.getProvider();
					return true;
				}
			}
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		return false;
	}
}

