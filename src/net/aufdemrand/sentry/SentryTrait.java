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
	private SentryInstance inst;
	private boolean isToggled = true;

	public SentryTrait() {
		// super-constructor sets private final String 'name' with argument given.
		super( "sentry" );
		plugin = (Sentry) Bukkit.getServer().getPluginManager().getPlugin( "Sentry" );
	}

	@SuppressWarnings("unchecked")
	@Override
	public void load( DataKey key ) throws NPCLoadException {
		// plugin.debug( npc.getName() + " Load" );
		
		ensureInst();

		if ( key.keyExists( "traits" ) ) 
			key = key.getRelative( "traits" );

		isToggled = key.getBoolean( "toggled", isToggled() );
		
		// replaced the repeated uses of 'plugin.getConfig()' with a local variable
		FileConfiguration cfg = plugin.getConfig();
		
		inst.Retaliate	= key.getBoolean( "Retaliate"
						, cfg.getBoolean( "DefaultOptions.Retaliate", true ) );
		inst.Invincible	= key.getBoolean( "Invincinble"
						, cfg.getBoolean( "DefaultOptions.Invincible", false ) );
		inst.DropInventory = key.getBoolean( "DropInventory"
						   , cfg.getBoolean( "DefaultOptions.Drops", false ) );
		inst.LuckyHits 	= key.getBoolean( "CriticalHits"
						, cfg.getBoolean( "DefaultOptions.Criticals", true ) );
						
		inst.sentryHealth = key.getDouble( "Health", cfg.getInt( "DefaultStats.Health", 20 ) );
		inst.sentryRange = key.getInt( "Range", cfg.getInt( "DefaultStats.Range", 10 ) );
		
		inst.RespawnDelaySeconds = key.getInt( "RespawnDelay"
						     , cfg.getInt( "DefaultStats.Respawn", 10 ) );
		inst.sentrySpeed = (float) key.getDouble( "Speed"
							, cfg.getDouble( "DefaultStats.Speed", 1.0 ) );
		inst.sentryWeight = key.getDouble( "Weight"
						 , cfg.getDouble( "DefaultStats.Weight", 1.0 ) );
						 
		inst.Armor 	= key.getInt( "Armor", cfg.getInt( "DefaultStats.Armor", 0 ) );
		inst.Strength	= key.getInt( "Strength"
					    , cfg.getInt( "DefaultStats.Strength", 1 ) );
		inst.FollowDistance = key.getInt( "FollowDistance"
						, cfg.getInt( "DefaultStats.FollowDistance", 4 ) );
		inst.guardTarget = key.getString( "GuardTarget", null );
		
		inst.GreetingMessage = key.getString( "Greeting", cfg.getString( 
			"DefaultTexts.Greeting", "'" + ChatColor.COLOR_CHAR + "b<NPC> says Welcome, <PLAYER>'") );
			
		inst.WarningMessage = key.getString( "Warning", cfg.getString(
			"DefaultTexts.Warning", "'" + ChatColor.COLOR_CHAR + "c<NPC> says Halt! Come no closer!'") );
		
		inst.WarningRange = key.getInt( "WarningRange"
					      , cfg.getInt( "DefaultStats.WarningRange", 0 ) );
		inst.AttackRateSeconds = key.getDouble( "AttackRate"
						       , cfg.getDouble( "DefaultStats.AttackRate", 2.0) );
		inst.HealRate 	= key.getDouble( "HealRate"
						, cfg.getDouble( "DefaultStats.HealRate", 0.0 ) );
		inst.NightVision = key.getInt( "NightVision"
					     , cfg.getInt( "DefaultStats.NightVision", 16 ) );
		inst.KillsDropInventory = key.getBoolean( "KillDrops"
							, cfg.getBoolean( "DefaultOptions.KillDrops", true ) );
        	inst.IgnoreLOS 	= key.getBoolean( "IgnoreLOS"
        					, cfg.getBoolean( "DefaultOptions.IgnoreLOS", false ) );
        					
		inst.MountID 	= key.getInt( "MountID", (int) -1 );
		inst.Targetable = key.getBoolean( "Targetable"
						, cfg.getBoolean( "DefaultOptions.Targetable", true ) );

		if ( key.keyExists( "Spawn" ) ){
			try {
				inst.Spawn = new Location( 
							plugin.getServer().getWorld( key.getString( "Spawn.world" ) )
							, key.getDouble( "Spawn.x" )
							, key.getDouble( "Spawn.y" )
							, key.getDouble( "Spawn.z" )
							, (float) key.getDouble( "Spawn.yaw" )
							, (float) key.getDouble("Spawn.pitch") );
			} catch (Exception e) {
				e.printStackTrace();
				inst.Spawn = null;
			}
			if ( inst.Spawn.getWorld() == null ) 
				inst.Spawn = null;
		}
		if ( inst.guardTarget != null && inst.guardTarget.isEmpty() ) 
			inst.guardTarget = null;
		
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
			if( !inst.validTargets.contains( string.toUpperCase() ) ){
				inst.validTargets.add( string.toUpperCase() );
			}
		}
		for ( String string : ignoretemp ) {
			if( !inst.ignoreTargets.contains( string.toUpperCase() ) ){
				inst.ignoreTargets.add( string.toUpperCase() );
			}
		}
		
		inst.loaded = true;
		inst.processTargets();
	}

	public SentryInstance getInstance(){
		return inst;
	}

	@Override
	public void onSpawn() {
		plugin.debug( npc.getName() + ":" + npc.getId() + " onSpawn" );
		ensureInst();

		if ( !inst.loaded ){
			try {
				//  plugin.debug( npc.getName() + " onSpawn call load" );
				
				load( new net.citizensnpcs.api.util.MemoryDataKey() );
			} catch (NPCLoadException e) {
			}
		}

		if ( !plugin.GroupsChecked ) plugin.doGroups(); // lazy checking for lazy vault.

		inst.initialize();
	}

	private void ensureInst(){
		if ( inst == null ) {
			inst = new SentryInstance( plugin );
			inst.myNPC = npc;
			inst.myTrait = this;
		}
	}

	@Override
	public void onRemove() {

		//	plugin = (Sentry) Bukkit.getPluginManager().getPlugin("Sentry");

		if ( inst != null ){
			//	plugin.getServer().broadcastMessage("onRemove");
			inst.cancelRunnable();
		}

		plugin.debug( npc.getName() + " onRemove" );

		inst = null;
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
		if( inst != null ){
			inst.isRespawnable = System.currentTimeMillis() 
							+ inst.RespawnDelaySeconds * 1000;
			inst.sentryStatus = Status.isDEAD;
			inst.dismount();
		}
	}

	@Override
	public void save( DataKey key ) {
		
		if ( inst == null ) return;
		
		key.setBoolean( "toggled", isToggled );
		key.setBoolean( "Retaliate", inst.Retaliate );
		key.setBoolean( "Invincinble", inst.Invincible );
		key.setBoolean( "DropInventory", inst.DropInventory );
		key.setBoolean( "KillDrops", inst.KillsDropInventory );
		key.setBoolean( "Targetable", inst.Targetable );

		key.setInt( "MountID", inst.MountID );

		key.setBoolean( "CriticalHits", inst.LuckyHits );
        	key.setBoolean( "IgnoreLOS", inst.IgnoreLOS );
		key.setRaw( "Targets", inst.validTargets );
		key.setRaw( "Ignores", inst.ignoreTargets );

		if ( inst.Spawn != null ){
			key.setDouble( "Spawn.x", inst.Spawn.getX() );
			key.setDouble( "Spawn.y", inst.Spawn.getY() );
			key.setDouble( "Spawn.z", inst.Spawn.getZ() );
			key.setString( "Spawn.world", inst.Spawn.getWorld().getName() );
			key.setDouble( "Spawn.yaw", inst.Spawn.getYaw() );
			key.setDouble( "Spawn.pitch", inst.Spawn.getPitch() );
		}

		key.setDouble( "Health", inst.sentryHealth );
		key.setInt( "Range", inst.sentryRange );
		key.setInt( "RespawnDelay", inst.RespawnDelaySeconds );
		key.setDouble( "Speed", (double) inst.sentrySpeed );
		key.setDouble( "Weight", inst.sentryWeight );
		key.setDouble( "HealRate", inst.HealRate );
		key.setInt( "Armor", inst.Armor );
		key.setInt( "Strength", inst.Strength );
		key.setInt( "WarningRange", inst.WarningRange );
		key.setDouble( "AttackRate", inst.AttackRateSeconds );
		key.setInt( "NightVision", inst.NightVision );
		key.setInt( "FollowDistance", inst.FollowDistance );

		if ( inst.guardTarget != null ) key.setString( "GuardTarget", inst.guardTarget );
		else if ( key.keyExists( "GuardTarget" ) ) key.removeKey( "GuardTarget" );

		key.setString( "Warning",inst.WarningMessage );
		key.setString( "Greeting",inst.GreetingMessage );
	}

	@Override
	public void onCopy() {
		plugin.debug( npc.getName() + ":" + npc.getId() + " onCopy" );
		
		if ( inst != null ){
			// name given to annonymous runnable for clarity, and to prevent possible memory leaks.
			final Runnable cloneInstance = new Runnable(){
				//the new npc is not in the new location immediately.
				@Override public void run(){
					inst.Spawn = npc.getEntity().getLocation().clone();
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
