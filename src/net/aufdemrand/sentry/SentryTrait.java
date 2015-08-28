package net.aufdemrand.sentry;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import net.aufdemrand.sentry.SentryInstance.Status;
import net.citizensnpcs.api.exception.NPCLoadException;

import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.trait.Toggleable;


public class SentryTrait extends Trait implements Toggleable {

	private Sentry plugin = null;
	private SentryInstance thisInstance;
	private boolean isToggled = true;

	public SentryTrait() {
		super( "sentry" );
		plugin = (Sentry) Bukkit.getServer().getPluginManager().getPlugin( "Sentry" );
	}

	@SuppressWarnings("unchecked")
	@Override
	public void load( DataKey key ) throws NPCLoadException {
		
		plugin.debug( npc.getName() + " Load" );
		ensureInst();

		if ( key.keyExists( "traits" ) ) 
			key = key.getRelative( "traits" );

		isToggled = key.getBoolean( "toggled", isToggled() );
		
		// replaced the repeated uses of 'plugin.getConfig()' with a local variable
		FileConfiguration cfg = plugin.getConfig();
		
		thisInstance.Retaliate	= key.getBoolean( "Retaliate"
							, cfg.getBoolean( "DefaultOptions.Retaliate", true ) );
		thisInstance.Invincible	= key.getBoolean( "Invincinble"
							, cfg.getBoolean( "DefaultOptions.Invincible", false ) );
		thisInstance.DropInventory = key.getBoolean( "DropInventory"
							   , cfg.getBoolean( "DefaultOptions.Drops", false ) );
		thisInstance.LuckyHits 	= key.getBoolean( "CriticalHits"
							, cfg.getBoolean( "DefaultOptions.Criticals", true ) );
		thisInstance.sentryHealth = key.getDouble( "Health", cfg.getInt( "DefaultStats.Health", 20 ) );
		thisInstance.sentryRange = key.getInt( "Range", cfg.getInt( "DefaultStats.Range", 10 ) );
		thisInstance.RespawnDelaySeconds = key.getInt( "RespawnDelay"
								, cfg.getInt( "DefaultStats.Respawn", 10 ) );
		thisInstance.sentrySpeed = (float) key.getDouble( "Speed"
								, cfg.getDouble( "DefaultStats.Speed", 1.0 ) );
		thisInstance.sentryWeight = key.getDouble( "Weight"
							 , cfg.getDouble( "DefaultStats.Weight", 1.0 ) );
		thisInstance.Armor 	= key.getInt( "Armor", cfg.getInt( "DefaultStats.Armor", 0 ) );
		thisInstance.Strength	= key.getInt( "Strength"
							, cfg.getInt( "DefaultStats.Strength", 1 ) );
		thisInstance.FollowDistance = key.getInt( "FollowDistance"
							, cfg.getInt( "DefaultStats.FollowDistance", 4 ) );
		thisInstance.guardTarget = key.getString( "GuardTarget", null );
		thisInstance.GreetingMessage = key.getString( "Greeting", cfg.getString( 
			"DefaultTexts.Greeting", "'" + ChatColor.COLOR_CHAR + "b<NPC> says Welcome, <PLAYER>'") );
			
		thisInstance.WarningMessage = key.getString( "Warning", cfg.getString(
			"DefaultTexts.Warning", "'" + ChatColor.COLOR_CHAR + "c<NPC> says Halt! Come no closer!'") );
		
		thisInstance.WarningRange = key.getInt( "WarningRange"
							, cfg.getInt( "DefaultStats.WarningRange", 0 ) );
		thisInstance.AttackRateSeconds = key.getDouble( "AttackRate"
							       , cfg.getDouble( "DefaultStats.AttackRate", 2.0) );
		thisInstance.HealRate = key.getDouble( "HealRate"
							, cfg.getDouble( "DefaultStats.HealRate", 0.0 ) );
		thisInstance.NightVision = key.getInt( "NightVision"
							, cfg.getInt( "DefaultStats.NightVision", 16 ) );
		thisInstance.KillsDropInventory = key.getBoolean( "KillDrops"
								, cfg.getBoolean( "DefaultOptions.KillDrops"
											, true ) );
        	thisInstance.IgnoreLOS = key.getBoolean( "IgnoreLOS"
        						, cfg.getBoolean( "DefaultOptions.IgnoreLOS", false ) );
		thisInstance.MountID = key.getInt( "MountID", (int) -1 );
		thisInstance.Targetable = key.getBoolean( "Targetable"
							, cfg.getBoolean( "DefaultOptions.Targetable", true ) );

		if ( key.keyExists( "Spawn" ) ){
			try {
				thisInstance.Spawn = new Location( 
							plugin.getServer().getWorld( key.getString( "Spawn.world" ) )
							, key.getDouble( "Spawn.x" )
							, key.getDouble( "Spawn.y" )
							, key.getDouble( "Spawn.z" )
							, (float) key.getDouble( "Spawn.yaw" )
							, (float) key.getDouble("Spawn.pitch") );
			} catch (Exception e) {
				e.printStackTrace();
				thisInstance.Spawn = null;
			}
			if ( thisInstance.Spawn.getWorld() == null ) 
				thisInstance.Spawn = null;
		}
		if ( thisInstance.guardTarget != null && thisInstance.guardTarget.isEmpty() ) 
			thisInstance.guardTarget = null;
		
		// TODO consider why we are allocating new objects here, when they are going to be immediately 
		// replaced in the following if...else... blocks; the new'ed lists will be left for GC.
		List<String> targettemp = new ArrayList<String>();
		List<String> ignoretemp = new ArrayList<String>();

		if ( key.getRaw( "Targets" ) != null ) 
			targettemp = (List<String>) key.getRaw( "Targets" );
		else 
			targettemp = plugin.getConfig().getStringList( "DefaultTargets" );

		if ( key.getRaw( "Ignores" ) != null ) 
			ignoretemp = (List<String>) key.getRaw( "Ignores" );
		else 
			ignoretemp = plugin.getConfig().getStringList( "DefaultIgnores" );

		// TODO find out why these checks are needed.  i.e. why aren't the targets lists using Sets?
		for ( String string : targettemp ) {
			if( !thisInstance.validTargets.contains( string.toUpperCase() ) ){
				thisInstance.validTargets.add( string.toUpperCase() );
			}
		}
		for ( String string : ignoretemp ) {
			if( !thisInstance.ignoreTargets.contains( string.toUpperCase() ) ){
				thisInstance.ignoreTargets.add( string.toUpperCase() );
			}
		}
		
		thisInstance.loaded = true;
		thisInstance.processTargets();
	}

	public SentryInstance getInstance(){
		return thisInstance;
	}

	@Override
	public void onSpawn() {
		plugin.debug( npc.getName() + ":" + npc.getId() + " onSpawn" );
		ensureInst();

		if ( !thisInstance.loaded ){
			try {
				plugin.debug( npc.getName() + " onSpawn call load" );
				load( new net.citizensnpcs.api.util.MemoryDataKey() );
			} catch (NPCLoadException e) {
			}
		}

		if ( !plugin.GroupsChecked ) plugin.doGroups(); // lazy checking for lazy vault.

		thisInstance.initialize();
	}

	private void ensureInst(){
		if ( thisInstance == null ) {
			thisInstance = new SentryInstance( plugin );
			thisInstance.myNPC = npc;
			thisInstance.myTrait = this;
		}
	}

	@Override
	public void onRemove() {

		//	plugin = (Sentry) Bukkit.getPluginManager().getPlugin("Sentry");

		if ( thisInstance != null ){
			//	plugin.getServer().broadcastMessage("onRemove");
			thisInstance.cancelRunnable();
		}

		plugin.debug( npc.getName() + " onRemove" );

		thisInstance = null;
		isToggled = false;
	}

	@Override
	public void onAttach() {
		plugin.debug( npc.getName() + ":" + npc.getId() + " onAttach" );
		isToggled = true;
	}

	@Override
	public void onDespawn() {
		plugin.debug( npc.getName() + ":" + npc.getId() + " onDespawn" );
		if( thisInstance != null ){
			thisInstance.isRespawnable = System.currentTimeMillis() 
							+ thisInstance.RespawnDelaySeconds * 1000;
			thisInstance.sentryStatus = Status.isDEAD;
			thisInstance.dismount();
		}
	}

	@Override
	public void save( DataKey key ) {
		
		if ( thisInstance == null ) return;
		
		key.setBoolean( "toggled", isToggled );
		key.setBoolean( "Retaliate", thisInstance.Retaliate );
		key.setBoolean( "Invincinble", thisInstance.Invincible );
		key.setBoolean( "DropInventory", thisInstance.DropInventory );
		key.setBoolean( "KillDrops", thisInstance.KillsDropInventory );
		key.setBoolean( "Targetable", thisInstance.Targetable );

		key.setInt( "MountID", thisInstance.MountID );

		key.setBoolean( "CriticalHits", thisInstance.LuckyHits );
        	key.setBoolean( "IgnoreLOS", thisInstance.IgnoreLOS );
		key.setRaw( "Targets", thisInstance.validTargets );
		key.setRaw( "Ignores", thisInstance.ignoreTargets );

		if ( thisInstance.Spawn != null ){
			key.setDouble( "Spawn.x", thisInstance.Spawn.getX() );
			key.setDouble( "Spawn.y", thisInstance.Spawn.getY() );
			key.setDouble( "Spawn.z", thisInstance.Spawn.getZ() );
			key.setString( "Spawn.world", thisInstance.Spawn.getWorld().getName() );
			key.setDouble( "Spawn.yaw", thisInstance.Spawn.getYaw() );
			key.setDouble( "Spawn.pitch", thisInstance.Spawn.getPitch() );
		}

		key.setDouble( "Health", thisInstance.sentryHealth );
		key.setInt( "Range", thisInstance.sentryRange );
		key.setInt( "RespawnDelay", thisInstance.RespawnDelaySeconds );
		key.setDouble( "Speed", (double) thisInstance.sentrySpeed );
		key.setDouble( "Weight", thisInstance.sentryWeight );
		key.setDouble( "HealRate", thisInstance.HealRate );
		key.setInt( "Armor", thisInstance.Armor );
		key.setInt( "Strength", thisInstance.Strength );
		key.setInt( "WarningRange", thisInstance.WarningRange );
		key.setDouble( "AttackRate", thisInstance.AttackRateSeconds );
		key.setInt( "NightVision", thisInstance.NightVision );
		key.setInt( "FollowDistance", thisInstance.FollowDistance );

		if ( thisInstance.guardTarget != null ) key.setString( "GuardTarget", thisInstance.guardTarget );
		else if ( key.keyExists( "GuardTarget" ) ) key.removeKey( "GuardTarget" );

		key.setString( "Warning",thisInstance.WarningMessage );
		key.setString( "Greeting",thisInstance.GreetingMessage );
	}

	@Override
	public void onCopy() {
		plugin.debug( npc.getName() + ":" + npc.getId() + " onCopy" );
		
		if ( thisInstance != null ){
			// name given to annonymous runnable for clarity, and to prevent possible memory leaks.
			final Runnable cloneInstance = new Runnable(){
				//the new npc is not in the new location immediately.
				@Override public void run(){
					thisInstance.Spawn = npc.getEntity().getLocation().clone();
				}
			};
			plugin.getServer().getScheduler().scheduleSyncDelayedTask( plugin, cloneInstance, 10 );
		}
	}

	@Override
	public boolean toggle() {
		isToggled = !isToggled;
		return isToggled;
	}

	public boolean isToggled() {
		return isToggled;
	}

}
