package net.aufdemrand.sentry;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.api.util.MemoryDataKey;
import net.citizensnpcs.trait.Toggleable;


public class SentryTrait extends Trait implements Toggleable {

	private Sentry sentry;
	SentryInstance inst;
	private boolean isToggled = true;

	public SentryTrait() {
		// super-constructor sets private final String 'name' with argument given.
		super( "sentry" );
		sentry = (Sentry) Bukkit.getServer().getPluginManager().getPlugin( "Sentry" );
	}
	
	private void ensureInst() {
		
		if ( inst == null ) {
			inst = new SentryInstance( sentry );
			inst.myNPC = npc;
			inst.myTrait = this;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void load( DataKey key ) throws NPCLoadException {
		
		ensureInst();

		if ( key.keyExists( "traits" ) ) 
			key = key.getRelative( "traits" );

		isToggled = key.getBoolean( "toggled", isToggled() );
		
		// replace the repeated uses of 'plugin.getConfig()' with a local variable
		FileConfiguration cfg = sentry.getConfig();
		
		// TODO change these lines to use the config values that have already been saved in Sentry instance.
		inst.iWillRetaliate	= key.getBoolean( "Retaliate", cfg.getBoolean( "DefaultOptions.Retaliate", true ) );
		inst.invincible	= key.getBoolean( "Invincinble", cfg.getBoolean( "DefaultOptions.Invincible", false ) );
		inst.dropInventory = key.getBoolean( "DropInventory", cfg.getBoolean( "DefaultOptions.Drops", false ) );
		inst.luckyHits 	= key.getBoolean( "CriticalHits", cfg.getBoolean( "DefaultOptions.Criticals", true ) );
						
		inst.sentryHealth = key.getDouble( "Health", cfg.getInt( "DefaultStats.Health", 20 ) );
		inst.sentryRange = key.getInt( "Range", cfg.getInt( "DefaultStats.Range", 10 ) );
		
		inst.respawnDelay = key.getInt( "RespawnDelay", cfg.getInt( "DefaultStats.Respawn", 10 ) );
		inst.sentrySpeed = (float) key.getDouble( "Speed", cfg.getDouble( "DefaultStats.Speed", 1.0 ) );
		inst.sentryWeight = key.getDouble( "Weight", cfg.getDouble( "DefaultStats.Weight", 1.0 ) );
						 
		inst.armorValue 	= key.getInt( "Armor", cfg.getInt( "DefaultStats.Armor", 0 ) );
		inst.strength	= key.getInt( "Strength", cfg.getInt( "DefaultStats.Strength", 1 ) );
		inst.followDistance = key.getInt( "FollowDistance", cfg.getInt( "DefaultStats.FollowDistance", 4 ) );
		inst.guardTarget = key.getString( "GuardTarget", null );
		
		inst.greetingMsg = key.getString( "Greeting", cfg.getString( 
				"DefaultTexts.Greeting", "'" + ChatColor.COLOR_CHAR + "b<NPC> says Welcome, <PLAYER>'") );
			
		inst.warningMsg = key.getString( "Warning", cfg.getString(
				"DefaultTexts.Warning", "'" + ChatColor.COLOR_CHAR + "c<NPC> says Halt! Come no closer!'") );
		
		inst.warningRange = key.getInt( "WarningRange", cfg.getInt( "DefaultStats.WarningRange", 0 ) );
		inst.attackRate = key.getDouble( "AttackRate", cfg.getDouble( "DefaultStats.AttackRate", 2.0) );
		inst.healRate 	= key.getDouble( "HealRate", cfg.getDouble( "DefaultStats.HealRate", 0.0 ) );
		inst.nightVision = key.getInt( "NightVision", cfg.getInt( "DefaultStats.NightVision", 16 ) );
		inst.killsDropInventory = key.getBoolean( "KillDrops", cfg.getBoolean( "DefaultOptions.KillDrops", true ) );
        inst.ignoreLOS 	= key.getBoolean( "IgnoreLOS", cfg.getBoolean( "DefaultOptions.IgnoreLOS", false ) );
        					
		inst.mountID 	= key.getInt( "MountID", -1 );
		inst.targetable = key.getBoolean( "Targetable", cfg.getBoolean( "DefaultOptions.Targetable", true ) );

		if ( key.keyExists( "Spawn" ) ) {
			try {
				inst.spawnLocation = new Location( 
							sentry.getServer().getWorld( key.getString( "Spawn.world" ) )
														, key.getDouble( "Spawn.x" )
														, key.getDouble( "Spawn.y" )
														, key.getDouble( "Spawn.z" )
														, (float) key.getDouble( "Spawn.yaw" )
														, (float) key.getDouble("Spawn.pitch") );
			} catch (Exception e) {
				e.printStackTrace();
				inst.spawnLocation = null;
			}
			if ( inst.spawnLocation.getWorld() == null ) 
				inst.spawnLocation = null;
		}
		if ( inst.guardTarget != null && inst.guardTarget.isEmpty() ) 
			inst.guardTarget = null;
		
		// TODO confirm that leaving these fields uninitialised has no bad effects
		List<String> targettemp; // = new ArrayList<String>();
		List<String> ignoretemp; // = new ArrayList<String>();

		if ( key.getRaw( "Targets" ) != null ) 
			targettemp = (List<String>) key.getRaw( "Targets" );
		else 
			targettemp = cfg.getStringList( "DefaultTargets" );

		if ( key.getRaw( "Ignores" ) != null ) 
			ignoretemp = (List<String>) key.getRaw( "Ignores" );
		else 
			ignoretemp = cfg.getStringList( "DefaultIgnores" );

		// TODO find out why these checks are needed.  i.e. why aren't the targets lists using Sets?
		for ( String string : targettemp ) {
			if( !inst.validTargets.contains( string.toUpperCase() ) ) 
				inst.validTargets.add( string.toUpperCase() );
		}
		
		for ( String string : ignoretemp ) {
			if( !inst.ignoreTargets.contains( string.toUpperCase() ) ) 
				inst.ignoreTargets.add( string.toUpperCase() );
		}
		
		inst.loaded = true;
		inst.processTargets();
	}

	public SentryInstance getInstance(){
		return inst;
	}

	@Override
	public void onSpawn() {
//		sentry.debug( npc.getName() + ":" + npc.getId() + " onSpawn" );
		
		ensureInst();

		if ( !inst.loaded ) {
			try {
				//  plugin.debug( npc.getName() + " onSpawn call load" );
				
				load( new MemoryDataKey() );
				
			} catch ( NPCLoadException e ) {}
		}

		if ( !sentry.groupsChecked ) sentry.doGroups(); // lazy checking for lazy vault.

		inst.initialize();
	}

	@Override
	public void onRemove() {

		// plugin = (Sentry) Bukkit.getPluginManager().getPlugin("Sentry");

		if ( inst != null ){
			//	plugin.getServer().broadcastMessage("onRemove");
			inst.cancelRunnable();
		}

		sentry.debug( npc.getName() + " onRemove" );

		inst = null;
		isToggled = false;
	}

	@Override
	public void onAttach() {
		
		sentry.debug( npc.getName() + ":" + npc.getId() + " onAttach" );
		isToggled = true;
	}

	@Override
	public void onDespawn() {
		
		sentry.debug( npc.getName() + ":" + npc.getId() + " onDespawn" );
		
		if ( inst != null ) {
			inst.isRespawnable = System.currentTimeMillis() + inst.respawnDelay * 1000;
			inst.sentryStatus = SentryStatus.isDEAD;
			inst.dismount();
		}
	}

	@Override
	public void save( DataKey key ) {
		
		if ( inst == null ) return;
		
		key.setBoolean( "toggled", isToggled );
		key.setBoolean( "Retaliate", inst.iWillRetaliate );
		key.setBoolean( "Invincinble", inst.invincible );
		key.setBoolean( "DropInventory", inst.dropInventory );
		key.setBoolean( "KillDrops", inst.killsDropInventory );
		key.setBoolean( "Targetable", inst.targetable );
		key.setInt( "MountID", inst.mountID );
		key.setBoolean( "CriticalHits", inst.luckyHits );
        key.setBoolean( "IgnoreLOS", inst.ignoreLOS );
		key.setRaw( "Targets", inst.validTargets );
		key.setRaw( "Ignores", inst.ignoreTargets );

		if ( inst.spawnLocation != null ){
			key.setDouble( "Spawn.x", inst.spawnLocation.getX() );
			key.setDouble( "Spawn.y", inst.spawnLocation.getY() );
			key.setDouble( "Spawn.z", inst.spawnLocation.getZ() );
			key.setString( "Spawn.world", inst.spawnLocation.getWorld().getName() );
			key.setDouble( "Spawn.yaw", inst.spawnLocation.getYaw() );
			key.setDouble( "Spawn.pitch", inst.spawnLocation.getPitch() );
		}

		key.setDouble( "Health", inst.sentryHealth );
		key.setInt( "Range", inst.sentryRange );
		key.setInt( "RespawnDelay", inst.respawnDelay );
		key.setDouble( "Speed", inst.sentrySpeed );
		key.setDouble( "Weight", inst.sentryWeight );
		key.setDouble( "HealRate", inst.healRate );
		key.setInt( "Armor", inst.armorValue );
		key.setInt( "Strength", inst.strength );
		key.setInt( "WarningRange", inst.warningRange );
		key.setDouble( "AttackRate", inst.attackRate );
		key.setInt( "NightVision", inst.nightVision );
		key.setInt( "FollowDistance", inst.followDistance );

		if ( inst.guardTarget != null ) 
			key.setString( "GuardTarget", inst.guardTarget );
		
		else if ( key.keyExists( "GuardTarget" ) ) 
				key.removeKey( "GuardTarget" );

		key.setString( "Warning",inst.warningMsg );
		key.setString( "Greeting",inst.greetingMsg );
	}

	@Override
	public void onCopy() {
		// sentry.debug( npc.getName() + ":" + npc.getId() + " onCopy" );
		
		if ( inst != null ) {
			
			// name given to anonymous runnable for clarity, and to prevent possible memory leaks.
			final Runnable cloneInstance = new Runnable() {
				
				//the new npc is not in the new location immediately.
				@Override public void run() {
					inst.spawnLocation = npc.getEntity().getLocation().clone();
				}
			};
			sentry.getServer().getScheduler().scheduleSyncDelayedTask( sentry, cloneInstance, 10 );
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
