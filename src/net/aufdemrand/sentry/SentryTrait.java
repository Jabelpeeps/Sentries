package net.aufdemrand.sentry;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.api.util.MemoryDataKey;
import net.citizensnpcs.trait.Toggleable;


public class SentryTrait extends Trait implements Toggleable {

	private Sentry sentry;
	private SentryInstance inst;
	private boolean isToggled = true;

	public SentryTrait() {
		// super-constructor sets private final String 'name' with argument given.
		super( "sentry" );
		
		sentry = (Sentry) Bukkit.getServer().getPluginManager().getPlugin( "Sentry" );
	}
	
	/**
	 * Instantiates an instance of SentryInstance.class if one is not currently referenced in 
	 * the inst field of this SentryTrait instance.  Also sets up the mutual links between the two
	 * objects.  
	 */
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
		
		inst.iWillRetaliate		= key.getBoolean( "Retaliate", sentry.defaultBooleans.get( "Retaliate" ) );
		inst.invincible			= key.getBoolean( "Invincinble", sentry.defaultBooleans.get( "Invincible" ) );
		inst.dropInventory 		= key.getBoolean( "DropInventory", sentry.defaultBooleans.get( "Drops" ) );
		inst.acceptsCriticals 	= key.getBoolean( "CriticalHits", sentry.defaultBooleans.get( "Criticals" ) );
		inst.killsDropInventory = key.getBoolean( "KillDrops", sentry.defaultBooleans.get( "KillDrops" ) );
        inst.ignoreLOS 			= key.getBoolean( "IgnoreLOS", false );
        inst.targetable 		= key.getBoolean( "Targetable", sentry.defaultBooleans.get( "Targetable" ) );
				
		inst.armorValue 	= key.getInt( "Armor", sentry.defaultIntegers.get( "Armor" ) );
		inst.strength		= key.getInt( "Strength", sentry.defaultIntegers.get( "Strength" ) );
		inst.sentryRange 	= key.getInt( "Range", sentry.defaultIntegers.get( "Range" ) );
		inst.respawnDelay 	= key.getInt( "RespawnDelay", sentry.defaultIntegers.get( "Respawn" ) );
		inst.followDistance = key.getInt( "FollowDistance", sentry.defaultIntegers.get( "FollowDistance" ) );
		inst.warningRange 	= key.getInt( "WarningRange", sentry.defaultIntegers.get( "WarningRange" ) );
		inst.nightVision 	= key.getInt( "NightVision", sentry.defaultIntegers.get( "NightVision" ) );
		inst.mountID 		= key.getInt( "MountID", -1 );
		
		inst.sentrySpeed 	= (float) key.getDouble( "Speed", sentry.defaultDoubles.get( "Speed" ) );
		inst.sentryWeight 	= key.getDouble( "Weight", sentry.defaultDoubles.get( "Weight" ) );
		inst.sentryHealth 	= key.getDouble( "Health", sentry.defaultDoubles.get( "Health" ) );
		inst.attackRate 	= key.getDouble( "AttackRate", sentry.defaultDoubles.get( "AttackRate" ) );
		inst.healRate 		= key.getDouble( "HealRate", sentry.defaultDoubles.get( "HealRate" ) );
		
		inst.guardTarget = key.getString( "GuardTarget", null );		
		inst.greetingMsg = key.getString( "Greeting", sentry.defaultGreeting );
		inst.warningMsg = key.getString( "Warning", sentry.defaultWarning );
		
		if ( key.keyExists( "Spawn" ) ) {
			try {
				inst.spawnLocation = new Location( sentry.getServer().getWorld( key.getString( "Spawn.world" ) ),
												   key.getDouble( "Spawn.x" ),
												   key.getDouble( "Spawn.y" ),
												   key.getDouble( "Spawn.z" ),
												   (float) key.getDouble( "Spawn.yaw" ),
												   (float) key.getDouble("Spawn.pitch") );
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

		FileConfiguration cfg = sentry.getConfig();
		
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

	public SentryInstance getInstance() {
		return inst;
	}

	@Override
	public void onSpawn() {
		sentry.debug( npc.getName() + ":" + npc.getId() + " onSpawn" );
		
		ensureInst();

		if ( !inst.loaded ) {
			try {
				
				load( new MemoryDataKey() );
				
			} catch ( NPCLoadException e ) {}
		}

		if ( !sentry.groupsChecked ) sentry.doGroups(); // lazy checking for lazy vault.

		inst.initialize();
	}

	@Override
	public void onRemove() {

		if ( inst != null ) {
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
			inst.myStatus = SentryStatus.isDEAD;
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
		key.setBoolean( "CriticalHits", inst.acceptsCriticals );
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

	@SuppressWarnings("synthetic-access")
	@Override
	public void onCopy() {
		
		if ( inst != null ) {
			
			final Runnable cloneInstance = new Runnable() {
				
				//the new npc is not in the new location immediately.
				@Override public void run() {
					inst.spawnLocation = npc.getEntity().getLocation(); // .clone();
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
