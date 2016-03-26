package net.aufdemrand.sentry;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.api.util.MemoryDataKey;
import net.citizensnpcs.trait.Toggleable;


public class SentryTrait extends Trait implements Toggleable {

	private Sentry sentry;
	private SentryInstance inst;
	private boolean isToggled = true;

	// Traits are required to have a no-argument constructor.
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
//			inst.myTrait = this;  // unused? 
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void load( DataKey key ) throws NPCLoadException {

		if ( Sentry.debug ) Sentry.debugLog( npc.getName() + ":" + npc.getId() + " load() start" );
		ensureInst();

		if ( key.keyExists( "traits" ) ) 
			key = key.getRelative( "traits" );
		
		isToggled = key.getBoolean( "toggled", isToggled() );
		
		inst.iWillRetaliate		= key.getBoolean( S.RETALIATE, sentry.defaultBooleans.get( S.RETALIATE ) );
		inst.invincible			= key.getBoolean( S.INVINCIBLE, sentry.defaultBooleans.get( S.INVINCIBLE ) );
		inst.dropInventory 		= key.getBoolean( S.DROP_INVENTORY, sentry.defaultBooleans.get( S.DROP_INVENTORY ) );
		inst.acceptsCriticals 	= key.getBoolean( S.CRITICAL_HITS, sentry.defaultBooleans.get( S.CRITICAL_HITS ) );
		inst.killsDropInventory = key.getBoolean( S.KILLS_DROP, sentry.defaultBooleans.get( S.KILLS_DROP ) );
        inst.ignoreLOS 			= key.getBoolean( "IgnoreLOS", false );
        inst.targetable 		= key.getBoolean( S.TARGETABLE, sentry.defaultBooleans.get( S.TARGETABLE ) );
				
		inst.armorValue 	= key.getInt( S.ARMOR, sentry.defaultIntegers.get( S.ARMOR ) );
		inst.strength		= key.getInt( S.STRENGTH, sentry.defaultIntegers.get( S.STRENGTH ) );
		inst.sentryRange 	= key.getInt( S.RANGE, sentry.defaultIntegers.get( S.RANGE ) );
		inst.respawnDelay 	= key.getInt( S.RESPAWN_DELAY, sentry.defaultIntegers.get( S.RESPAWN_DELAY ) );
		inst.followDistance = key.getInt( S.FOLLOW_DISTANCE, sentry.defaultIntegers.get( S.FOLLOW_DISTANCE ) );
		inst.warningRange 	= key.getInt( S.WARNING_RANGE, sentry.defaultIntegers.get( S.WARNING_RANGE ) );
		inst.nightVision 	= key.getInt( S.NIGHT_VISION, sentry.defaultIntegers.get( S.NIGHT_VISION ) );
		inst.mountID 		= key.getInt( S.MOUNTID, -1 );
		
		inst.sentrySpeed 	= (float) key.getDouble( S.SPEED, sentry.defaultDoubles.get( S.SPEED ) );
		inst.sentryWeight 	= key.getDouble( S.WEIGHT, sentry.defaultDoubles.get( S.WEIGHT ) );
		inst.sentryMaxHealth 	= key.getDouble( S.HEALTH, sentry.defaultDoubles.get( S.HEALTH ) );
		inst.attackRate 	= key.getDouble( S.ATTACK_RATE, sentry.defaultDoubles.get( S.ATTACK_RATE ) );
		inst.healRate 		= key.getDouble( S.HEALRATE, sentry.defaultDoubles.get( S.HEALRATE ) );
		
		inst.guardTarget = key.getString( S.GUARD_TARGET, null );		
		inst.greetingMsg = key.getString( S.GREETING, sentry.defaultGreeting );
		inst.warningMsg = key.getString( S.WARNING, sentry.defaultWarning );
		
		if ( key.keyExists( "Spawn" ) ) {
			try {
				inst.spawnLocation = new Location( sentry.getServer().getWorld( key.getString( "Spawn.world" ) ),
												   key.getDouble( "Spawn.x" ),
												   key.getDouble( "Spawn.y" ),
												   key.getDouble( "Spawn.z" ),
												   (float) key.getDouble( "Spawn.yaw" ),
												   (float) key.getDouble("Spawn.pitch") );
				
				if ( inst.spawnLocation.getWorld() == null ) 
						inst.spawnLocation = null;
			} catch (Exception e) {
				e.printStackTrace();
				inst.spawnLocation = null;
			}
		}
		if ( inst.guardTarget != null && inst.guardTarget.isEmpty() ) 
			inst.guardTarget = null;
		
		if ( key.getRaw( S.TARGETS ) != null ) 
			inst.validTargets.addAll( (Set<String>) key.getRaw( S.TARGETS ) );
		else 
			inst.validTargets.addAll( sentry.defaultTargets );

		if ( key.getRaw( S.IGNORES ) != null ) 
			inst.ignoreTargets.addAll( (Set<String>) key.getRaw( S.IGNORES ) );
		else 
			inst.ignoreTargets.addAll( sentry.defaultIgnores );
		
		inst.loaded = true;
		inst.processTargets();
		
		if ( Sentry.debug ) Sentry.debugLog( npc.getName() + ":" + npc.getId() + " load() end" );
	}

	public SentryInstance getInstance() {
		return inst;
	}

	@Override
	public void onSpawn() {
		if ( Sentry.debug ) Sentry.debugLog( npc.getName() + ":" + npc.getId() + " onSpawn()" );
		
		ensureInst();

		if ( !inst.loaded ) {
			try {
				load( new MemoryDataKey() );
				
			} catch ( NPCLoadException e ) {}
		}
		inst.initialize();
	}

	@Override
	public void onRemove() {

		if ( inst != null ) {
			inst.cancelRunnable();
		}
		if ( Sentry.debug ) Sentry.debugLog( npc.getName() + " onRemove()" );

		inst = null;
		isToggled = false;
	}

	@Override
	public void onAttach() {
		
		if ( Sentry.debug ) Sentry.debugLog( npc.getName() + ":" + npc.getId() + " onAttach()" );
		isToggled = true;
	}

	@Override
	public void onDespawn() {
		
		if ( Sentry.debug ) Sentry.debugLog( npc.getName() + ":" + npc.getId() + " onDespawn()" );
		
		if ( inst != null ) {
			inst.isRespawnable = System.currentTimeMillis() + inst.respawnDelay * 1000;
			inst.myStatus = SentryStatus.isDEAD;
			inst.dismount();
		}
	}

	@Override
	public void save( DataKey key ) {

		if ( Sentry.debug ) Sentry.debugLog( npc.getName() + ":" + npc.getId() + " save()" );
		if ( inst == null ) return;
		
		key.setBoolean( "toggled", isToggled );
		key.setBoolean( S.RETALIATE, inst.iWillRetaliate );
		key.setBoolean( S.INVINCIBLE, inst.invincible );
		key.setBoolean( S.DROP_INVENTORY, inst.dropInventory );
		key.setBoolean( S.KILLS_DROP, inst.killsDropInventory );
		key.setBoolean( S.TARGETABLE, inst.targetable );
		key.setInt( S.MOUNTID, inst.mountID );
		key.setBoolean( S.CRITICAL_HITS, inst.acceptsCriticals );
        key.setBoolean( "IgnoreLOS", inst.ignoreLOS );
		key.setRaw( S.TARGETS, inst.validTargets );
		key.setRaw( S.IGNORES, inst.ignoreTargets );

		if ( inst.spawnLocation != null ) {
			key.setDouble( "Spawn.x", inst.spawnLocation.getX() );
			key.setDouble( "Spawn.y", inst.spawnLocation.getY() );
			key.setDouble( "Spawn.z", inst.spawnLocation.getZ() );
			key.setString( "Spawn.world", inst.spawnLocation.getWorld().getName() );
			key.setDouble( "Spawn.yaw", inst.spawnLocation.getYaw() );
			key.setDouble( "Spawn.pitch", inst.spawnLocation.getPitch() );
		}

		key.setDouble( S.HEALTH, inst.sentryMaxHealth );
		key.setInt( S.RANGE, inst.sentryRange );
		key.setInt( S.RESPAWN_DELAY, inst.respawnDelay );
		key.setDouble( S.SPEED, inst.sentrySpeed );
		key.setDouble( S.WEIGHT, inst.sentryWeight );
		key.setDouble( S.HEALRATE, inst.healRate );
		key.setInt( S.ARMOR, inst.armorValue );
		key.setInt( S.STRENGTH, inst.strength );
		key.setInt( S.WARNING_RANGE, inst.warningRange );
		key.setDouble( S.ATTACK_RATE, inst.attackRate );
		key.setInt( S.NIGHT_VISION, inst.nightVision );
		key.setInt( S.FOLLOW_DISTANCE, inst.followDistance );

		if ( inst.guardTarget != null ) 
				key.setString( S.GUARD_TARGET, inst.guardTarget );
		
		else if ( key.keyExists( S.GUARD_TARGET ) ) 
				key.removeKey( S.GUARD_TARGET );

		key.setString( S.WARNING, inst.warningMsg );
		key.setString( S.GREETING, inst.greetingMsg );
	}

	@SuppressWarnings("synthetic-access")
	@Override
	public void onCopy() {

		if ( Sentry.debug ) Sentry.debugLog( npc.getName() + ":" + npc.getId() + " onCopy()" );
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
