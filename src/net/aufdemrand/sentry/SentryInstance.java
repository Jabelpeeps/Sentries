package net.aufdemrand.sentry;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
/////////////////////////
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Horse;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Spider;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import net.aufdemrand.denizen.npc.traits.HealthTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.GoalController;
import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.NavigatorParameters;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.PlayerAnimation;
//Version Specifics
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPotion;
import net.minecraft.server.v1_8_R3.PacketPlayOutAnimation;


public class SentryInstance {

	Sentry sentry;
	
	Location _projTargetLostLoc;
	Location spawnLocation = null;
	
	int strength = 1;
	int armorValue = 0;
	int epCount = 0;
	int nightVision = 16;
	int respawnDelay = 10;
	int sentryRange = 10;
	int followDistance  = 16;
	int mountID = -1;
	int warningRange = 0;

	float sentrySpeed =  1.0f;
	
	double attackRate = 2.0;
	double healRate = 0.0;
	double sentryWeight = 1.0;
	double sentryHealth = 20.0;

	boolean killsDropInventory = true;
	boolean dropInventory = false;
	boolean targetable = true;
	boolean invincible = false;
	boolean loaded = false;
	boolean acceptsCriticals = true;
	boolean iWillRetaliate = true;
    boolean ignoreLOS;
    boolean mountCreated = false;
	
	private GiveUpStuckAction giveup = new GiveUpStuckAction( this );

	public String greetingMsg = "&a<NPC> says: Welcome, <PLAYER>!";
	public String warningMsg = "&a<NPC> says: Halt! Come no further!";
	
	private Map<Player, Long> warningsGiven = new HashMap<Player, Long>();
	private Set<Player> _myDamamgers = new HashSet<Player>();

	public LivingEntity guardEntity = null;
	public LivingEntity meleeTarget;
	public String guardTarget = null;

	PacketPlayOutAnimation healAnimation = null;
	
	public List<String> ignoreTargets = new ArrayList<String>();
	public List<String> validTargets = new ArrayList<String>();

	public Set<String> _ignoreTargets = new HashSet<String>();
	public Set<String> _validTargets = new HashSet<String>();

	// TODO why are we saving four instances of the system time?
	long isRespawnable = System.currentTimeMillis();	
	long oktoFire = System.currentTimeMillis();
	long oktoheal = System.currentTimeMillis();
	long oktoreasses= System.currentTimeMillis();
	long okToTakedamage = 0;
	
	public List<PotionEffect> weaponSpecialEffects = null;
	ItemStack potiontype = null;
	public LivingEntity projectileTarget;
	Random random = new Random();
	
	public SentryStatus myStatus = SentryStatus.isDYING;
	public AttackType myAttacks;
	public SentryTrait myTrait;
	public NPC myNPC = null;

	private int taskID = 0;
	
	public SentryInstance( Sentry plugin ) {
		sentry = plugin;
		isRespawnable = System.currentTimeMillis();
	}
	
	public void initialize() {

		// check for illegal values
		if ( sentryWeight <= 0 ) 		sentryWeight = 1.0;
		if ( attackRate > 30 )			attackRate = 30.0;
		if ( sentryHealth < 0 )			sentryHealth = 0;
		if ( sentryRange < 1 )			sentryRange = 1;
		if ( sentryRange > 200 )		sentryRange = 200;
		if ( sentryWeight <= 0 )		sentryWeight =  1.0;
		if ( respawnDelay < -1 )		respawnDelay = -1;
		if ( spawnLocation == null ) 	spawnLocation = getMyEntity().getLocation();
		
		// Allow Denizen to handle the sentry's health if it is active.
		if ( Sentry.denizenActive ) {
			if ( myNPC.hasTrait( HealthTrait.class ) ) myNPC.removeTrait( HealthTrait.class );
		}

		// disable citizens respawning, because Sentry doesn't always raise EntityDeath
		myNPC.data().set( "respawn-delay", -1 );

		setHealth( sentryHealth );

		_myDamamgers.clear();
		myStatus = SentryStatus.isLOOKING;
		
		faceForward();

		healAnimation = new PacketPlayOutAnimation( ((CraftEntity) getMyEntity()).getHandle(), 6);

		//	Packet derp = new net.minecraft.server.Packet15Place();
		
		if ( guardTarget == null ) 
			myNPC.teleport( spawnLocation, TeleportCause.PLUGIN ); //it should be there... but maybe not if the position was saved elsewhere.

		float pf = myNPC.getNavigator().getDefaultParameters().range();

		if ( pf < sentryRange + 5 ) {
			pf = sentryRange + 5;
		}

		myNPC.data().set( NPC.DEFAULT_PROTECTED_METADATA, false );
		myNPC.data().set( NPC.TARGETABLE_METADATA, this.targetable );
		
		Navigator navigator = myNPC.getNavigator();

		navigator.getDefaultParameters().range( pf );
		navigator.getDefaultParameters().stationaryTicks( 5 * 20 );
		navigator.getDefaultParameters().useNewPathfinder( false );
		
		// TODO why is this disabled?
		//	myNPC.getNavigator().getDefaultParameters().stuckAction(new BodyguardTeleportStuckAction(this, this.plugin));

		// plugin.getServer().broadcastMessage("NPC GUARDING!");

		if ( getMyEntity() instanceof Creeper )
			navigator.getDefaultParameters().attackStrategy( new CreeperAttackStrategy() );
		else if ( getMyEntity() instanceof Spider )
			navigator.getDefaultParameters().attackStrategy( new SpiderAttackStrategy( sentry ) );
		
		processTargets();

		if ( taskID == 0 ) {
			taskID = sentry.getServer().getScheduler()
									   .scheduleSyncRepeatingTask( sentry, 
											   					   new SentryLogic(), 
											   					   40 + myNPC.getId(),  
											   					   sentry.logicTicks );
		}
	}

	public void cancelRunnable() {
		if ( taskID != 0 ) 
			sentry.getServer().getScheduler().cancelTask( taskID );
	}

	public boolean hasTargetType( int type ) {
		return ( targets & type ) == type;
	}
	public boolean hasIgnoreType( int type ) {
		return ( ignores & type ) == type;
	}

	// TODO replace deprecated methods with suggested alternatives.
	@SuppressWarnings("deprecation")
	public boolean isIgnored( LivingEntity aTarget ) {
		
		if ( aTarget == guardEntity ) return true;
		if ( ignores == 0 ) return false;
		if ( hasIgnoreType( all ) ) return true;

		if ( CitizensAPI.getNPCRegistry().isNPC( aTarget ) ) {

			if ( hasIgnoreType( npcs ) ) 
				return true;

			NPC npc = CitizensAPI.getNPCRegistry().getNPC( aTarget );

			if ( npc != null ) {

				String name = npc.getName();

				if ( hasIgnoreType( namednpcs ) && containsIgnore( "NPC:" + name ) )
						return true;

				if ( hasIgnoreType( permGroups ) ) {

					// TODO why are we checked the perms on an NPC?
					
					String[] groups1 = Sentry.perms.getPlayerGroups(aTarget.getWorld(),name); // world perms
					String[] groups2 = Sentry.perms.getPlayerGroups((World)null,name); //global perms

					if (groups1 !=null){
						for (int i = 0; i < groups1.length; i++) {
							//	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found world1 group " + groups1[i] + " on " + name);
							if (this.containsIgnore("GROUP:" + groups1[i]))	
								return true;
						}
					}

					if ( groups2 !=null){
						for (int i = 0; i < groups2.length; i++) {
							//	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found global group " + groups2[i] + " on " + name);
							if (this.containsIgnore("GROUP:" + groups2[i]))		
								return true;
						}
					}
				}
			}
		} else if ( aTarget instanceof Player ) {

			if ( hasIgnoreType( players ) ) 
				return true;
			else {
				Player player = (Player) aTarget;
				String name = player.getName();

				if ( hasIgnoreType( namedplayers ) && containsIgnore( "PLAYER:" + name ) ) 
					return true;

				if ( hasIgnoreType( owner ) && name.equalsIgnoreCase( myNPC.getTrait( Owner.class ).getOwner() ) ) 
					return true;

				if ( hasIgnoreType( permGroups ) ) {

					// deprecated method calls in the Vault API removed.
					String[] groups = Sentry.perms.getPlayerGroups( aTarget.getWorld().getName(), player ); // get world perms
					
					if ( groups != null ) {
						
			//			for ( int i = 0; i < groups.length; i++ ) 
						for ( String each : groups )
							if ( containsIgnore( "GROUP:" + each ) )	
								return true;
					}
					groups = Sentry.perms.getPlayerGroups( (String) null, player ); // get global perms

					if ( groups != null ) {
						
			//			for ( int i = 0; i < groups.length; i++ )
						for ( String each : groups )
							if ( containsIgnore( "GROUP:" + each ) )
								return true;
					}
				}

				if ( Sentry.townyActive && hasIgnoreType( towny ) ) {
					
					String[] info = TownyUtil.getResidentTownyInfo( player );

					if ( info[1] != null 
						&& containsIgnore( "TOWN:" + info[1] ) )	
								return true;

					if ( info[0] != null 
						&& containsIgnore( "NATION:" + info[0] ) )	
								return true;
				}

				if ( Sentry.factionsActive && hasIgnoreType( faction ) ) {
					
					String factionName = FactionsUtils.getFactionsTag( player );
					
					if ( factionName != null 
						&& containsIgnore( "FACTION:" + factionName ) )
								return true;
				}
				
				if ( Sentry.warActive && hasIgnoreType( war ) ) {
					
					String team = WarUtils.getWarTeam( player );
					
					if ( team != null
						&& containsIgnore( "WARTEAM:" + team ) )
								return true;
				}
				
				// TODO add boolean in Sentry to record this is active (and add to config saving and loading)
				if ( hasIgnoreType( mcTeams ) ) {
					
					String team = sentry.getMCTeamName( player );

					if ( team != null 
						&& containsIgnore( "TEAM:" + team ) )	
								return true;
				}
				
				if ( Sentry.clansActive && hasIgnoreType( clans ) ) {
					
					String clan = sentry.getClan( player );

					if ( clan != null 
						&& containsIgnore( "CLAN:" + clan ) )
								return true;
				}
			}
		}


		else if ( aTarget instanceof Monster && hasIgnoreType( monsters ) ) 
						return true;

		else if ( aTarget instanceof LivingEntity 
				&& hasIgnoreType( namedentities ) 
				&& containsIgnore( "ENTITY:" + aTarget.getType() ) )	
						return true;

		return false;
	}

	public boolean isTarget( LivingEntity aTarget ) {

		if ( targets == 0 || targets == events ) return false;

		if ( hasTargetType( all ) ) return true;

		//Check if target
		if ( aTarget instanceof Player && !CitizensAPI.getNPCRegistry().isNPC( aTarget ) ) {

			if ( hasTargetType( players ) ) return true;

			else {
				String name = ((Player) aTarget).getName();

				if ( hasTargetType( namedplayers ) && containsTarget( "PLAYER:" + name ) ) return true;

				if ( containsTarget( "ENTITY:OWNER" ) && name.equalsIgnoreCase( myNPC.getTrait( Owner.class ).getOwner() ) ) return true;

				if ( hasTargetType( permGroups ) ) {

					String[] groups1 = Sentry.perms.getPlayerGroups(aTarget.getWorld(),name); // world perms
					String[] groups2 = Sentry.perms.getPlayerGroups((World)null,name); //global perms

					if ( groups1 != null ) {
						for ( String each : groups1 )
					//	for ( int i = 0; i < groups1.length; i++ ) {
	
							if ( containsTarget( "GROUP:" + each ) ) return true;
					}

					if ( groups2 != null) {
						for ( String each : groups2 )
				//		for (int i = 0; i < groups2.length; i++) {
							
							if ( containsTarget( "GROUP:" + each ) ) return true;
					}
				}

				if ( Sentry.townyActive 
						&& ( hasTargetType( towny ) || ( hasTargetType( townyenemies ) ) ) ) {
					
					String[] info = TownyUtil.getResidentTownyInfo( (Player) aTarget );

					if ( hasTargetType( towny ) && info[1] != null 
						&& containsTarget( "TOWN:" + info[1] ) )
								return true;

					if ( info[0] != null ) {
						
						if ( hasTargetType( towny ) && containsTarget( "NATION:" + info[0] ) )
								return true;

						if ( hasTargetType( townyenemies ) )
							for ( String each : NationsEnemies ) 
								if ( TownyUtil.isNationEnemy( each, info[0] ) )	
									return true;
					}
				}

				if ( Sentry.factionsActive 
						&& ( hasTargetType( faction ) || hasTargetType( factionEnemies ) ) ) {
					
					String factionName = FactionsUtils.getFactionsTag((Player)aTarget);

					if ( factionName != null ) {
						
						if ( containsTarget( "FACTION:" + factionName ) )
								return true;

						if ( hasTargetType( factionEnemies ) ) 
							for ( String each : FactionEnemies ) 
								if ( FactionsUtils.isFactionEnemy( getMyEntity().getWorld()
																				.getName(), each, factionName) ) 
										return true;						
					}	
				}

				if ( Sentry.warActive && hasTargetType( war ) ) {
					
					String team = WarUtils.getWarTeam( (Player) aTarget );

					if ( team != null && containsTarget( "WARTEAM:" + team ) ) 
						return true;
				}
				if ( hasTargetType(mcTeams) ) {
					
					String team = sentry.getMCTeamName( (Player) aTarget );

					if ( team != null && containsTarget( "TEAM:" + team ) ) 
						return true;
				}
				if ( Sentry.clansActive && hasTargetType( clans ) ) {
					
					String clan = sentry.getClan( (Player) aTarget );

					if ( clan != null && containsTarget( "CLAN:" + clan ) ) 
						return true;
				}
			}
		}

		else if ( CitizensAPI.getNPCRegistry().isNPC( aTarget ) ) {

			if ( hasTargetType( npcs ) ) return true;

			NPC npc = CitizensAPI.getNPCRegistry().getNPC( aTarget );

			String name = npc.getName();

			if ( hasTargetType( namednpcs ) && containsTarget( "NPC:" + name ) ) 
					return true;

			if ( hasTargetType( permGroups ) ) {

				String[] groups1 = Sentry.perms.getPlayerGroups(aTarget.getWorld(),name); // world perms
				String[] groups2 = Sentry.perms.getPlayerGroups((World)null,name); //global perms
				//		String[] groups3 = plugin.perms.getPlayerGroups(aTarget.getWorld().getName(),name); // world perms
				//	String[] groups4 = plugin.perms.getPlayerGroups((Player)aTarget); // world perms

				if ( groups1 != null ) {
					
					for ( String each : groups1 )
			//		for ( int i = 0; i < groups1.length; i++ )
						if ( containsTarget( "GROUP:" + each ) )
							return true;
				}

				if ( groups2 != null ) {
					
					for ( String each : groups2 )
			//		for ( int i = 0; i < groups2.length; i++ ) 
						if ( containsTarget( "GROUP:" + each ) )		
							return true;
				}
			}
		}
		else if ( aTarget instanceof Monster && hasTargetType( monsters ) )
					return true;

		else if ( aTarget instanceof LivingEntity 
				&& hasTargetType( namedentities ) 
				&& containsTarget( "ENTITY:" + aTarget.getType() ) ) 
					return true;
		
		return false;
	}


	public boolean containsIgnore( String theTarget ) {
		return _ignoreTargets.contains( theTarget.toUpperCase().intern() );
	}

	public boolean containsTarget( String theTarget ) {
		return _validTargets.contains( theTarget.toUpperCase().intern() );
	}

	public void deactivate() {
		sentry.getServer().getScheduler().cancelTask( taskID );
	}

	public void die( boolean runscripts, EntityDamageEvent.DamageCause cause ) {
		
		LivingEntity myEntity = getMyEntity();
		
		if 	(  myStatus == SentryStatus.isDYING 
			|| myStatus == SentryStatus.isDEAD 
			|| !( myEntity instanceof LivingEntity ) ) 
					return;

		myStatus = SentryStatus.isDYING;

		setTarget( null, false );
		//		myNPC.getTrait(Waypoints.class).getCurrentProvider().setPaused(true);

		boolean handled = false;

		if ( runscripts && Sentry.denizenActive ) {
			handled = DenizenHook.sentryDeath( _myDamamgers, myNPC );
		}
		if ( handled ) return;

		if ( Sentry.denizenActive ) {
			try {
				Entity killer = myEntity.getKiller();
				
				if ( killer == null ) {
					//might have been a projectile.
					EntityDamageEvent ev = myEntity.getLastDamageCause();
					if 	(  ev != null 
						&& ev instanceof EntityDamageByEntityEvent ) {
								killer = ((EntityDamageByEntityEvent) ev).getDamager();
					}
				}
				DenizenHook.denizenAction( myNPC, "death", null );
				DenizenHook.denizenAction( myNPC, "death by" + cause.toString().replace( " ", "_" ), null );


				if ( killer != null ) {

					if 	(  killer instanceof Projectile 
						&& ( (Projectile) killer).getShooter() != null
                        && ( (Projectile) killer).getShooter() instanceof Entity )
                        	killer = (Entity) ((Projectile) killer).getShooter();

					sentry.debug( "Running Denizen actions for " + myNPC.getName() + " with killer: " + killer.toString() );

					if ( killer instanceof OfflinePlayer ) {
						DenizenHook.denizenAction( myNPC, "death by player", (OfflinePlayer) killer );
					}
					else {
						DenizenHook.denizenAction( myNPC, "death by entity", null );
						DenizenHook.denizenAction( myNPC, "death by " + killer.getType().toString(), null );
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		myStatus = SentryStatus.isDEAD;
		
		if ( this.dropInventory )  
			myEntity.getLocation().getWorld()
								  .spawn( myEntity.getLocation(), 
										  ExperienceOrb.class )
								  .setExperience( sentry.sentryEXP );

		List<ItemStack> items = new LinkedList<ItemStack>();

		if ( myEntity instanceof HumanEntity ) {

			PlayerInventory inventory = ((HumanEntity) myEntity).getInventory();
			
			for ( ItemStack is : inventory.getArmorContents() ) {
				
				if ( is.getType() != null ) 
					items.add( is );
			}

			ItemStack is = inventory.getItemInHand();
			
			if ( is.getType() != null ) items.add( is );

			inventory.clear();
			inventory.setArmorContents( null );
			inventory.setItemInHand( null );
		}

		if ( items.isEmpty() ) 
			myEntity.playEffect( EntityEffect.DEATH );
		else 
			myEntity.playEffect( EntityEffect.HURT );

		if ( !dropInventory ) items.clear();

		for ( ItemStack is : items ) 
			myEntity.getWorld().dropItemNaturally( myEntity.getLocation(), is );

		if ( sentry.dieLikePlayers ) {
			myEntity.setHealth( 0 );
		}
		else {
			EntityDeathEvent ed = new EntityDeathEvent( myEntity, items );

			sentry.getServer().getPluginManager().callEvent( ed );
			//citizens will despawn it.
		}

		if ( respawnDelay == -1 ) {
			
			cancelRunnable();
			
			if ( isMounted() ) 
				Util.removeMount( mountID );
			
			myNPC.destroy();	
		} 
		else 
			isRespawnable = System.currentTimeMillis() + respawnDelay * 1000;
	}

	void faceEntity( Entity from, Entity at ) {

		if ( from.getWorld() != at.getWorld() )	return;
		
		Location fromLoc = from.getLocation();
		Location atLoc = at.getLocation();

		double xDiff = atLoc.getX() - fromLoc.getX();
		double yDiff = atLoc.getY() - fromLoc.getY();
		double zDiff = atLoc.getZ() - fromLoc.getZ();

		double distanceXZ = Math.sqrt( xDiff * xDiff + zDiff * zDiff );
		double distanceY = Math.sqrt( distanceXZ * distanceXZ + yDiff * yDiff );

		double yaw = Math.acos( xDiff / distanceXZ ) * 180 / Math.PI;
		double pitch = ( Math.acos( yDiff / distanceY ) * 180 / Math.PI ) - 90;
		
		if ( zDiff < 0.0 ) {
			yaw = yaw + ( Math.abs( 180 - yaw ) * 2 );
		}

		NMS.look( from, (float) yaw - 90, (float) pitch );

	}

	private void faceForward() {
		NMS.look( getMyEntity(), getMyEntity().getLocation().getYaw(), 0 );
	}

	private void faceAlignWithVehicle() {
		NMS.look( getMyEntity(), getMyEntity().getVehicle().getLocation().getYaw(), 0 );
	}

	public LivingEntity findTarget( Integer Range ) {
		
		LivingEntity myEntity = getMyEntity();		
		Range += warningRange;
		List<Entity> EntitiesWithinRange = myEntity.getNearbyEntities( Range, Range, Range );
		LivingEntity theTarget = null;
		Double distanceToBeat = 99999.0;

		for ( Entity aTarget : EntitiesWithinRange ) {
			if (!(aTarget instanceof LivingEntity) ) continue;

			// find closest target

			if ( !isIgnored( (LivingEntity) aTarget ) && isTarget( (LivingEntity) aTarget) ) {

				// can i see it?
				double lightLevel = aTarget.getLocation().getBlock().getLightLevel();
				
				// sneaking cut light in half
				if ( aTarget instanceof Player && ( (Player) aTarget ).isSneaking() )
						lightLevel /= 2;

				// too dark?
				if ( lightLevel >= ( 16 - nightVision ) ) {

					double dist = aTarget.getLocation().distance( myEntity.getLocation() );

					if ( hasLOS( aTarget ) ) {

						if  (  warningRange > 0 
							&& myStatus == SentryStatus.isLOOKING 
							&& aTarget instanceof Player 
							&& dist > ( Range - warningRange ) 
							&& !CitizensAPI.getNPCRegistry().isNPC( aTarget ) 
							&& !warningMsg.isEmpty() ) {

							if  (  !warningsGiven.containsKey( aTarget ) 
								|| System.currentTimeMillis() > warningsGiven.get( aTarget ) + 60 * 1000 ) {
								
								((Player) aTarget).sendMessage( getWarningMessage( (Player) aTarget ) );
								
								if ( !getNavigator().isNavigating() )
									faceEntity( myEntity, aTarget );
								
								warningsGiven.put( (Player) aTarget, System.currentTimeMillis() );
							}

						}
						else if	( dist < distanceToBeat ) {
							distanceToBeat = dist;
							theTarget = (LivingEntity) aTarget;
						}
					}
				}
			}
			else if (  warningRange > 0 
					&& myStatus == SentryStatus.isLOOKING 
					&& aTarget instanceof Player 
					&& !CitizensAPI.getNPCRegistry().isNPC( aTarget ) 
					&& !greetingMsg.isEmpty() ) {
					
					if  (  myEntity.hasLineOfSight( aTarget ) 
						&&  (  !warningsGiven.containsKey( aTarget ) 
							|| System.currentTimeMillis() > warningsGiven.get( aTarget ) + 60 * 1000 ) ) {
							
								((Player) aTarget).sendMessage( getGreetingMEssage( (Player) aTarget ) );
								faceEntity( myEntity, aTarget );
								
								warningsGiven.put( (Player) aTarget, System.currentTimeMillis() );
					}
			}
		}

		if ( theTarget != null ) {
			return theTarget;
		}
		return null;
	}

	public void draw( boolean on ) {
		((CraftLivingEntity) getMyEntity()).getHandle().b( on ); // TODO: 1.8 UPDATE - IS THIS CORRECT?
	}
	
	public void Fire( LivingEntity theTarget ) {
		
		LivingEntity myEntity = getMyEntity();
		Class<? extends Projectile> myProjectile = myAttacks.getProjectile();
		Effect effect = null;
		
		double v = 34;
		double g = 20;

		boolean ballistics = true;
		
		if ( myProjectile == Arrow.class ) {
			effect = Effect.BOW_FIRE;	
		} 
		else if  ( myProjectile == SmallFireball.class 
				|| myProjectile == Fireball.class 
				|| myProjectile == WitherSkull.class) {
			effect = Effect.BLAZE_SHOOT;
			ballistics = false;
		}
		else if ( myProjectile == ThrownPotion.class ) {
			v = 21;
			g = 20;
		}
		else {
			v = 17.75;
			g = 13.5;
		}

		// calc shooting spot.
		Location loc = Util.getFireSource( myEntity, theTarget );
		Location targetsHeart = theTarget.getLocation();
		
		targetsHeart = targetsHeart.add(0, .33, 0);
		Vector test = targetsHeart.clone().subtract( loc ).toVector();

		double elev = test.getY();
		Double testAngle = Util.launchAngle( loc, targetsHeart, v, elev, g );

		if ( testAngle == null && clearTargets() ) return;

		double hangtime = Util.hangtime( testAngle, v, elev, g );
		Vector targetVelocity = theTarget.getLocation().subtract( _projTargetLostLoc ).toVector();

		targetVelocity.multiply( 20 / sentry.logicTicks );
		
		Location to = Util.leadLocation( targetsHeart, targetVelocity, hangtime );
		Vector victor = to.clone().subtract( loc ).toVector();

		double dist = Math.sqrt( Math.pow( victor.getX(), 2 ) + Math.pow( victor.getZ(), 2 ) );
		elev = victor.getY();
		
		if ( dist == 0 ) return;

		if ( !hasLOS( theTarget ) && clearTargets() ) return;
		
		switch ( myAttacks.lightningLevel ) {
		
			case ( 1 ):
				swingPlayerArm( myEntity );
				to.getWorld().strikeLightningEffect( to );
				theTarget.damage( getStrength(), myEntity );
				return;
			case ( 2 ):
				swingPlayerArm( myEntity );
				to.getWorld().strikeLightning( to );
				return;
			case ( 3 ):
				swingPlayerArm( myEntity );
				to.getWorld().strikeLightningEffect( to );
				theTarget.setHealth( 0 );
				return;
			default:
		}
		
		if ( dist > sentryRange && clearTargets() ) return;	
		
		else if ( ballistics ) {
			Double launchAngle = Util.launchAngle(loc, to, v, elev, g);
			
			if ( launchAngle == null && clearTargets() ) return;
			
			// Apply angle
			victor.setY( Math.tan( launchAngle ) * dist );
			Vector noise = Vector.getRandom();
			
			// normalize vector
			victor = Util.normalizeVector( victor );

			noise = noise.multiply( 1 / 10.0 );

			// victor = victor.add(noise);

			if ( myProjectile == Arrow.class || myProjectile == ThrownPotion.class )  
				v = v + ( 1.188 * Math.pow( hangtime, 2 ) );
			else 
				v = v + ( 0.5 * Math.pow( hangtime, 2 ) );

			v = v + (random.nextDouble() - 0.8 ) / 2;

			// apply power
			victor = victor.multiply( v / 20.0 );

			// Shoot!
			// Projectile theArrow
			// =getMyEntity().launchProjectile(myProjectile);
		}
		else {
			Projectile theArrow;

			if ( myProjectile == ThrownPotion.class ) {
				net.minecraft.server.v1_8_R3.World nmsWorld = ((CraftWorld) myEntity.getWorld()).getHandle();
				EntityPotion ent = new EntityPotion( nmsWorld
													, loc.getX()
													, loc.getY()
													, loc.getZ()
													, CraftItemStack.asNMSCopy( potiontype ) );
				nmsWorld.addEntity( ent );
				theArrow = (Projectile) ent.getBukkitEntity();
			}
			else if ( myProjectile == EnderPearl.class ) 
				theArrow = myEntity.launchProjectile( myProjectile );
			else 
				theArrow = myEntity.getWorld().spawn( loc, myProjectile );

			if ( myProjectile == Fireball.class || myProjectile == WitherSkull.class ) {
				victor = victor.multiply( 1 / 1000000000 );
			}
			else if ( myProjectile == SmallFireball.class ) {
				
				victor = victor.multiply( 1 / 1000000000 );
				( (SmallFireball) theArrow ).setIsIncendiary( myAttacks.incendiary );
				
				if ( !myAttacks.incendiary ) {
					( (SmallFireball) theArrow ).setFireTicks( 0 );
					( (SmallFireball) theArrow ).setYield( 0 );
				}
			}
			
			//TODO why are we counting enderpearls?
			else if ( myProjectile == EnderPearl.class ) {
				epCount++;
				if ( epCount > Integer.MAX_VALUE-1 ) 
					epCount = 0;
				sentry.debug(epCount + "");
			}

			sentry.arrows.add( theArrow );
			theArrow.setShooter( myEntity );
			theArrow.setVelocity( victor );
		}

		// OK we're shooting
		if ( effect != null )
			myEntity.getWorld().playEffect( myEntity.getLocation(), effect, null );

		if ( myProjectile == Arrow.class ) {
			draw( false );
		}
		else swingPlayerArm( myEntity );
	}

	private void swingPlayerArm( LivingEntity myEntity ) {
		if ( myEntity instanceof Player )	{
			PlayerAnimation.ARM_SWING.play( (Player) myEntity, 64 );
		}
	}
	public int getArmor(){

		double mod = 0;
		
		if ( getMyEntity() instanceof Player ) {
			for ( ItemStack is : ((Player) getMyEntity()).getInventory().getArmorContents() ) {
				if ( sentry.armorBuffs.containsKey( is.getType() ) ) 
					mod += sentry.armorBuffs.get( is.getType() );
			}
		}

		return (int) ( armorValue + mod );
	}

	String getGreetingMEssage( Player player ) {
		String str = greetingMsg.replace( "<NPC>", myNPC.getName() ).replace( "<PLAYER>", player.getName() );
		return ChatColor.translateAlternateColorCodes('&', str);
	}

	public LivingEntity getGuardTarget() {
		return guardEntity;
	}

	public double getHealth() {
		if ( myNPC == null || getMyEntity() == null ) return 0;
		
		return  ( (CraftLivingEntity) getMyEntity() ).getHealth();
	}

	public float getSpeed() {
		
		if ( !myNPC.isSpawned() ) return sentrySpeed;
		
		LivingEntity myEntity = getMyEntity();
		double mod = 0;
		
		if ( myEntity instanceof Player ) {
			for ( ItemStack stack : ((Player) myEntity).getInventory().getArmorContents() ) {
				if ( sentry.speedBuffs.containsKey( stack.getType() ) ) 
					mod += sentry.speedBuffs.get( stack.getType() );
			}
		}
		return (float) ( sentrySpeed + mod ) * ( myEntity.isInsideVehicle() ? 2 
																			: 1 );
	}

	public int getStrength(){
		
		double mod = 0;

		if  (  getMyEntity() instanceof Player 
			&& sentry.strengthBuffs.containsKey( ((Player) getMyEntity()).getInventory().getItemInHand().getType() ) ) {
				
				mod += sentry.strengthBuffs.get( ((Player) getMyEntity()).getInventory().getItemInHand().getType() );
		}
		return (int) ( strength + mod );
	}

	String getWarningMessage( Player player ) {
		
		String str =  warningMsg.replace( "<NPC>", myNPC.getName() ).replace( "<PLAYER>", player.getName() );
		
		return ChatColor.translateAlternateColorCodes( '&', str );
	}
	
	static Set<AttackType> pyros = EnumSet.of( AttackType.pyro1, AttackType.pyro2, AttackType.pyro3 );
	
	public boolean isPyromancer() {
		return pyros.contains( myAttacks );
	}

	public boolean isPyromancer1() {
		return ( myAttacks == AttackType.pyro1 ) ;
	}

	static Set<AttackType> stormCallers = EnumSet.of( AttackType.sc1, AttackType.sc2, AttackType.sc3 );
	
	public boolean isStormcaller() {
		return stormCallers.contains( myAttacks ) ;
	}

	public boolean isWarlock1() {
		return ( myAttacks == AttackType.warlock1 ) ;
	}

	public boolean isWitchDoctor() {
		return ( myAttacks == AttackType.witchdoctor ) ;
	}

	public void onDamage( EntityDamageByEntityEvent event ) {

		if ( myStatus == SentryStatus.isDYING ) return;

		if ( myNPC == null || !myNPC.isSpawned() ) return;

		if ( guardTarget != null && guardEntity == null ) return; //dont take damage when bodyguard target isnt around.

		if ( System.currentTimeMillis() < okToTakedamage + 500 ) return;
		
		okToTakedamage = System.currentTimeMillis();

		event.getEntity().setLastDamageCause( event );
		
		if ( invincible ) return;

		NPC npc = myNPC;
		LivingEntity attacker = null;

		Entity damager = event.getDamager();

		// Find the attacker
		if  (  damager instanceof Projectile 
			&& ((Projectile) damager).getShooter() instanceof LivingEntity ) 
					attacker = (LivingEntity) ((Projectile) damager).getShooter();
			
		else if ( damager instanceof LivingEntity ) 
					attacker = (LivingEntity) damager;

		if ( sentry.ignoreListIsInvincible && isIgnored( attacker ) ) return;

		if  (  attacker != null 
			&& iWillRetaliate 
			&&  (  !(damager instanceof Projectile) 
				|| CitizensAPI.getNPCRegistry().getNPC( attacker ) == null ) ) {

					setTarget( attacker, true );			
		}
		
		Hits hit = Hits.Hit;

		double damage = event.getDamage();
		
		if ( acceptsCriticals ) {
			
			hit = Hits.getHit();

			damage = Math.round( damage * hit.damageModifier );
		}

		int arm = getArmor();

		if ( damage > 0 ) {

			if ( attacker != null ) {
				// knockback
				npc.getEntity().setVelocity( attacker.getLocation()
													 .getDirection()
													 .multiply( 1.0 / ( sentryWeight + ( arm / 5 ) ) ) );
			}

			// Apply armor
			damage -= arm;

			// there was damage before armor.
			if ( damage <= 0 ) {
				npc.getEntity().getWorld().playEffect( npc.getEntity().getLocation(), Effect.ZOMBIE_CHEW_IRON_DOOR, 1 );
				hit = Hits.Block;
			}
		}

		if ( attacker instanceof Player && !CitizensAPI.getNPCRegistry().isNPC( attacker ) ) {

			_myDamamgers.add( (Player) attacker );
			
			String msg = hit.message;

			if ( msg != null && !msg.isEmpty() ) {
				((Player) attacker).sendMessage( Util.format( msg, 
															  npc, 
															  attacker, 
															  ((Player) attacker).getItemInHand().getType(), 
															  damage + "" ) );
			}
		}

		if ( damage > 0 ) {
			npc.getEntity().playEffect( EntityEffect.HURT );

			// is he dead?
			if ( getHealth() - damage <= 0 ) {

				//set the killer
				if ( damager instanceof HumanEntity ) 
					((CraftLivingEntity) getMyEntity()).getHandle().killer 
											= (EntityHuman) ((CraftLivingEntity) damager).getHandle();

				die( true, event.getCause() );

			}
			else getMyEntity().damage( damage );
		}
	}

	public void onEnvironmentDamage( EntityDamageEvent event ) {

		if ( myStatus == SentryStatus.isDYING ) return;

		if ( !myNPC.isSpawned() || invincible ) return;

		if ( guardTarget != null && guardEntity == null ) return; //dont take damage when bodyguard target isnt around.

		if ( System.currentTimeMillis() <  okToTakedamage + 500 ) return;
		
		okToTakedamage = System.currentTimeMillis();
		
		LivingEntity myEntity = getMyEntity();

		myEntity.setLastDamageCause( event );

		double finaldamage = event.getDamage();
		DamageCause cause = event.getCause();

		if ( cause == DamageCause.CONTACT || cause == DamageCause.BLOCK_EXPLOSION ) {
			finaldamage -= getArmor();
		}

		if ( finaldamage > 0 ) {
			myEntity.playEffect( EntityEffect.HURT );

			if ( cause == DamageCause.FIRE ) {
				
				Navigator navigator = getNavigator();
				
				if ( !navigator.isNavigating() ) 
					navigator.setTarget( myEntity.getLocation().add( random.nextInt( 2 ) - 1,
																	 0, 
																	 random.nextInt( 2 ) - 1 ) );
			}

			if ( getHealth() - finaldamage <= 0 ) 
				die( true, cause );
			else 
				myEntity.damage( finaldamage );
		}
	}

//  @EventHandler
//	public void onRightClick(NPCRightClickEvent event) {}

	static final int all = 1;
	static final int players = 2;
	static final int npcs = 4;
	static final int monsters = 8;
	static final int events = 16;
	static final int namedentities = 32;
	static final int namedplayers = 64;
	static final int namednpcs = 128;
	static final int faction = 256;
	static final int towny = 512;
	static final int war = 1024;
	static final int permGroups = 2048;
	static final int owner = 4096;
	static final int clans = 8192;
	static final int townyenemies = 16384;
	static final int factionEnemies = 16384*2;
	static final int mcTeams = 16384*4;

	private int targets = 0;
	private int ignores = 0;

	List<String> NationsEnemies = new ArrayList<String>();
	List<String> FactionEnemies = new ArrayList<String>();

	public void processTargets(){
		try {

			targets = 0;
			ignores = 0;
			_ignoreTargets.clear();
			_validTargets.clear();
			NationsEnemies.clear();
			FactionEnemies.clear();

			for ( String target: validTargets ) {
				
				if      ( target.contains( "ENTITY:ALL" ) ) targets |= all;
				else if ( target.contains( "ENTITY:MONSTER" ) ) targets |= monsters;
				else if ( target.contains( "ENTITY:PLAYER" ) ) targets |= players;
				else if ( target.contains( "ENTITY:NPC" ) ) targets |= npcs;
				else {
					_validTargets.add( target );
					if 		( target.contains( "NPC:" ) ) targets |= namednpcs;
					else if ( Sentry.perms != null && Sentry.perms.isEnabled() && target.contains( "GROUP:" ) ) 
									targets |= permGroups;
					else if ( target.contains( "EVENT:" ) ) targets |= events;
					else if ( target.contains( "PLAYER:" ) ) targets |= namedplayers;
					else if ( target.contains( "ENTITY:" ) ) targets |= namedentities;
					else if ( Sentry.factionsActive && target.contains( "FACTION:" ) ) targets |= faction;
					else if ( Sentry.factionsActive && target.contains( "FACTIONENEMIES:" ) ) {
									targets |= factionEnemies;
									FactionEnemies.add( target.split( ":" )[1] );
					}
					else if ( Sentry.townyActive && target.contains( "TOWN:") ) targets |= towny;
					else if ( Sentry.townyActive && target.contains( "NATIONENEMIES:") ) {
									targets |= townyenemies;
									NationsEnemies.add( target.split( ":" )[1] );
					}
					else if ( Sentry.townyActive && target.contains( "NATION:" ) )  targets |= towny;
					else if ( Sentry.warActive && target.contains( "WARTEAM:" ) )  targets |= war;
					else if ( target.contains("TEAM:") )  targets |= mcTeams;
					else if ( Sentry.clansActive && target.contains( "CLAN:" ) )  targets |= clans;
				}
			// end of 1st for loop
			}
			for ( String ignore : ignoreTargets ) {
				if 		( ignore.contains( "ENTITY:ALL" ) ) ignores |= all;
				else if ( ignore.contains( "ENTITY:MONSTER" ) ) ignores |= monsters;
				else if ( ignore.contains( "ENTITY:PLAYER" ) ) ignores |= players;
				else if ( ignore.contains( "ENTITY:NPC" ) ) ignores |= npcs;
				else if ( ignore.contains( "ENTITY:OWNER" ) ) ignores |= owner;
				else {
					_ignoreTargets.add( ignore );
					if 		( Sentry.perms != null && Sentry.perms.isEnabled() && ignore.contains( "GROUP:" ) ) 
									ignores |= permGroups;
					else if ( ignore.contains( "NPC:" ) ) ignores |= namednpcs;
					else if ( ignore.contains( "PLAYER:" ) ) ignores |= namedplayers;
					else if ( ignore.contains( "ENTITY:" ) ) ignores |= namedentities;
					else if ( Sentry.factionsActive && ignore.contains( "FACTION:" ) ) ignores |= faction;
					else if ( Sentry.townyActive && ignore.contains( "TOWN:" ) ) ignores |= towny;
					else if ( Sentry.townyActive && ignore.contains( "NATION:" ) ) ignores |= towny;
					else if ( Sentry.warActive && ignore.contains( "TEAM:" ) )  ignores |= war;
					else if ( Sentry.clansActive && ignore.contains( "CLAN:" ) )  ignores |= clans;
				}
			// end of 2nd for loop
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	private class SentryLogic implements Runnable {

		@SuppressWarnings("synthetic-access")
		@Override
		public void run() {
			
			LivingEntity myEntity = getMyEntity();
			
			if ( myEntity == null ) myStatus = SentryStatus.isDEAD; 

			if ( UpdateWeapon() ) {
				// ranged weapon equipped
				if ( meleeTarget != null ) {
					sentry.debug( myNPC.getName() + " Switched to ranged" );
					
					// TODO do we really hve to run this method twice?
					setTarget( null, false );
					setTarget( meleeTarget, ( myStatus == SentryStatus.isRETALIATING ) );
				}
			}
			else {
				// melee weapon equipped
				if ( projectileTarget != null ) {
					sentry.debug( myNPC.getName() + " Switched to melee" );
					boolean ret = ( myStatus == SentryStatus.isRETALIATING );
					LivingEntity derp = projectileTarget;
					setTarget(null, false);
					setTarget(derp, ret);
				}
			}

			if ( myStatus != SentryStatus.isDEAD &&  healRate > 0 ) {
				if ( System.currentTimeMillis() > oktoheal ) {
					if  (  getHealth() < sentryHealth 
						&& myStatus !=  SentryStatus.isDEAD 
						&& myStatus != SentryStatus.isDYING) {
						
							double heal = 1;
							
							if ( healRate < 0.5 ) heal = ( 0.5 / healRate );
	
							setHealth( getHealth() + heal );
	
							if ( healAnimation != null ) NMS.sendPacketsNearby( null, myEntity.getLocation(), healAnimation );
	
							if ( getHealth() >= sentryHealth ) _myDamamgers.clear(); //healed to full, forget attackers

					}
					oktoheal = (long) ( System.currentTimeMillis() + healRate * 1000 );
				}

			}

			if  (  myNPC.isSpawned() 
				&& !myEntity.isInsideVehicle()
				&& isMounted() 
				&& isMyChunkLoaded() ) 
						mount();

			if  (  myStatus == SentryStatus.isDEAD 
				&& System.currentTimeMillis() > isRespawnable 
				&& respawnDelay > 0 
				&& spawnLocation.getWorld().isChunkLoaded( spawnLocation.getBlockX() >> 4, spawnLocation.getBlockZ() >> 4 ) ) {

					sentry.debug( "respawning" + myNPC.getName() );
					
					if ( guardEntity == null ) {
						myNPC.spawn( spawnLocation.clone() );
						//	myNPC.teleport(Spawn,org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
					} else {
						myNPC.spawn( guardEntity.getLocation().add( 2, 0, 2 ) );
						//	myNPC.teleport(guardEntity.getLocation().add(2, 0, 2),org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
					}
					return;
			}
			else if  (  (  myStatus == SentryStatus.isHOSTILE 
						|| myStatus == SentryStatus.isRETALIATING ) 
					&& myNPC.isSpawned() ) {

				if ( !isMyChunkLoaded() && clearTargets() ) return;

				if  (  targets > 0 
					&& myStatus == SentryStatus.isHOSTILE 
					&& System.currentTimeMillis() > oktoreasses ) {
						LivingEntity target = findTarget( sentryRange );
						setTarget( target, false );
						oktoreasses = System.currentTimeMillis() + 3000;
				}

				if  (  projectileTarget != null 
					&& !projectileTarget.isDead() 
					&& projectileTarget.getWorld() == myEntity.getLocation().getWorld() ) {
					
						if (_projTargetLostLoc == null)
							_projTargetLostLoc = projectileTarget.getLocation();
	
						if ( !getNavigator().isNavigating() )
							faceEntity( myEntity, projectileTarget );
	
						draw( true );
	
						if ( System.currentTimeMillis() > oktoFire ) {

							oktoFire = (long) (System.currentTimeMillis() + attackRate * 1000.0 );
							Fire( projectileTarget );
						}
						if ( projectileTarget != null )
							_projTargetLostLoc = projectileTarget.getLocation();
	
						return; 
				}

				else if ( meleeTarget != null && !meleeTarget.isDead() ) {

					if ( isMounted() ) 
						faceEntity( myEntity, meleeTarget );

					if ( meleeTarget.getWorld() != myEntity.getLocation().getWorld() ) {
						clearTargets();
					}
					else {
						double dist = meleeTarget.getLocation().distance( myEntity.getLocation() );
						//block if in range
						draw( dist < 3 );
						// Did it get away?
						if ( dist > sentryRange ) clearTargets();
					}
				}
				else clearTargets();

			}

			else if ( myStatus == SentryStatus.isLOOKING && myNPC.isSpawned() ) {

				if ( myEntity.isInsideVehicle() == true ) faceAlignWithVehicle(); 


				if ( guardEntity instanceof Player && !((Player) guardEntity).isOnline() ) 
						guardEntity = null;
			
				if ( guardTarget != null && guardEntity == null ) {
					setGuardTarget( guardTarget, false );
				}

				// TODO why is this block repeated twice?
				if ( guardTarget != null && guardEntity == null ) {
					setGuardTarget( guardTarget, true );
				}

				if ( guardEntity != null ) {

					Location npcLoc = myEntity.getLocation();

					if ( guardEntity.getLocation().getWorld() != npcLoc.getWorld() || !isMyChunkLoaded() ) {
						
						if ( Util.CanWarp( guardEntity, myNPC ) ) {
							myNPC.despawn();
							myNPC.spawn( guardEntity.getLocation().add( 1, 0, 1 ) );
						}
						else {
							((Player) guardEntity).sendMessage( myNPC.getName() + " cannot follow you to " 
																				+ guardEntity.getWorld().getName() );
							guardEntity = null;
						}
					}
					else {
						Navigator navigator = getNavigator();

						double dist = npcLoc.distanceSquared( guardEntity.getLocation() );
						sentry.debug( myNPC.getName() + dist + navigator.isNavigating() + " " + navigator.getEntityTarget() + " " );
						
						if ( dist > 1024 ) {
							myNPC.teleport( guardEntity.getLocation().add( 1,0,1 ), TeleportCause.PLUGIN );
						}
						else if ( dist > followDistance && !navigator.isNavigating() ) {
							navigator.setTarget( (Entity) guardEntity, false );
							navigator.getLocalParameters().stationaryTicks( 3 * 20 );
						}
						else if ( dist < followDistance && navigator.isNavigating() ) {
							navigator.cancelNavigation();
						}
					}
				}

				LivingEntity target = null;

				if ( targets > 0 ) {
					target = findTarget( sentryRange );
				}

				if ( target != null ) {
					oktoreasses = System.currentTimeMillis() + 3000;
					setTarget( target, false );
				}
			}
		}
	}


	boolean isMyChunkLoaded() {
		if ( getMyEntity() == null ) return false;
		
		Location npcLoc = getMyEntity().getLocation();
		return npcLoc.getWorld().isChunkLoaded( npcLoc.getBlockX() >> 4, npcLoc.getBlockZ() >> 4 );
	}

	/** 
	 *  
	 */
	public boolean setGuardTarget( String name, boolean onlyCheckAllPlayers ) {

		if ( myNPC == null )
			return false;

		if ( name == null ) {
			guardEntity = null;
			guardTarget = null;

			return clearTargets();
		}

		if ( onlyCheckAllPlayers ) {
			
			for ( Player player : sentry.getServer().getOnlinePlayers() ) {
				
				if ( name.equals( player.getName() ) ) {
					
					guardEntity = player;
					guardTarget = player.getName();

					return clearTargets();
				}
			}
		} else {
			String ename;
			
			for ( Entity each : getMyEntity().getNearbyEntities( sentryRange, sentryRange / 2, sentryRange ) ) {

				if ( each instanceof Player )
					ename = ((Player) each).getName();
				
				else if ( each instanceof LivingEntity ) 
					ename = ((LivingEntity) each).getCustomName();
				
				// if the entity for this loop doesn't have a name, let's see if the next one does...
				else continue;
					
				// name found! now is it the name we are looking for?
				if ( name.equals( ename ) ) {
					
					guardEntity = (LivingEntity) each;
					guardTarget = ename;

					return clearTargets();
				}
			}
		}
		return false;
	}

	public void setHealth( double health ) {
		
		if ( myNPC == null ) return;
		
		LivingEntity myEntity = getMyEntity();
		
		if ( myEntity == null ) return;
		
		if ( ( (CraftLivingEntity) myEntity ).getMaxHealth() != sentryHealth )
			myEntity.setMaxHealth( sentryHealth );
		
		if ( health > sentryHealth ) health = sentryHealth;

		myEntity.setHealth( health );
	}

    /** 
     * @return - true to indicate a ranged attack 
     * <br>    - false for a melee attack 
     */
	public boolean UpdateWeapon() {
		Material weapon = Material.AIR;

		ItemStack is = null;

		LivingEntity myEntity = getMyEntity();
		
		if ( myEntity instanceof HumanEntity ) {
			is = ((HumanEntity) myEntity).getInventory().getItemInHand();
			weapon = is.getType();
			
			myAttacks = AttackType.find( weapon );
			
			if ( myAttacks != AttackType.witchdoctor ) 
					is.setDurability( (short) 0 );
		} 
		else if ( myEntity instanceof Skeleton ) myAttacks = AttackType.archer;
		else if ( myEntity instanceof Ghast ) myAttacks = AttackType.pyro3;
		else if ( myEntity instanceof Snowman ) myAttacks = AttackType.magi;
		else if ( myEntity instanceof Wither ) myAttacks = AttackType.warlock2;
		else if ( myEntity instanceof Witch ) myAttacks = AttackType.witchdoctor;
		else if  ( myEntity instanceof Blaze 
				|| myEntity instanceof EnderDragon ) myAttacks = AttackType.pyro2;

		weaponSpecialEffects = sentry.weaponEffects.get( weapon );
		
		if ( myAttacks == AttackType.witchdoctor ) {
			if ( is == null ) {
				is = new ItemStack( Material.POTION, 1, (short) 16396 );
			}
			potiontype = is;
		}
		return ( myAttacks.getProjectile() != null );
	}
	
	/** short convenience method to reduce repetition - calls setTarget( null, false )
	 * @return true - to allow calling from 'if' clauses (when && in second position with the first condition) */
	private boolean clearTargets() {
		setTarget( null, false );
		return true;
	}
	
	public void setTarget( LivingEntity theEntity, boolean isretaliation ) {
		
		LivingEntity myEntity = getMyEntity();

		if ( myEntity == null || theEntity == myEntity ) return; 
		
		if ( guardTarget != null && guardEntity == null ) theEntity = null; //dont go aggro when bodyguard target isnt around.

		if ( theEntity == null ) {
			
			sentry.debug( myNPC.getName() + "- Set Target Null" );
			// this gets called while npc is dead, reset things.
			myStatus = SentryStatus.isLOOKING;
			projectileTarget = null;
			meleeTarget = null;
			_projTargetLostLoc = null;
		}

		if ( myNPC == null || !myNPC.isSpawned() ) return;
		
		GoalController goalController = getGoalController();

		if ( theEntity == null ) {
			// no hostile target

			draw( false );
			
			Navigator navigator = getNavigator();

			if ( guardEntity == null ) {
				//not a guard
				navigator.cancelNavigation();

				faceForward();

				if ( goalController.isPaused() )
					goalController.setPaused( false );
				
			} else {
				goalController.setPaused( true );
				//	if (!myNPC.getTrait(Waypoints.class).getCurrentProvider().isPaused())  myNPC.getTrait(Waypoints.class).getCurrentProvider().setPaused(true);
				
				if  (  navigator.getEntityTarget() == null 
					||  (  navigator.getEntityTarget() != null 
						&& navigator.getEntityTarget().getTarget() != guardEntity ) ) {

					if ( guardEntity.getLocation().getWorld() != myEntity.getLocation().getWorld() ) {
						myNPC.despawn();
						myNPC.spawn( guardEntity.getLocation().add( 1, 0, 1 ) );
						return;
					}

					navigator.setTarget( (Entity) guardEntity, false );

					navigator.getLocalParameters().stationaryTicks( 3 * 20 );
				}
			} 
			return;
		}

		if ( theEntity == guardEntity )	return; 

		if ( isretaliation ) 
			myStatus = SentryStatus.isRETALIATING;
		else 
			myStatus = SentryStatus.isHOSTILE;


		if ( !getNavigator().isNavigating() ) 
			faceEntity( myEntity, theEntity );

		if ( UpdateWeapon() ) {
			// ranged attack
			sentry.debug( myNPC.getName() + "- Set Target projectile" );
			projectileTarget = theEntity;
			meleeTarget = null;
		}
		else {
			// melee Attack
			sentry.debug( myNPC.getName() + "- Set Target melee" );
			meleeTarget = theEntity;
			projectileTarget = null;
			
			Navigator navigator = getNavigator();
			
			if  (  navigator.getEntityTarget() != null 
				&& navigator.getEntityTarget().getTarget() == theEntity ) 
						return; 
			
			if ( !goalController.isPaused() )
						goalController.setPaused( true );
			
			navigator.setTarget( (Entity) theEntity, true );
			navigator.getLocalParameters().speedModifier( getSpeed() );
			navigator.getLocalParameters().stuckAction( giveup );
			navigator.getLocalParameters().stationaryTicks( 5 * 20 );
		}
	}

	private Navigator getNavigator() {
		return ifMountedGetMount().getNavigator();
	}

	protected GoalController getGoalController() {
		return ifMountedGetMount().getDefaultGoalController();
	}
	
	private NPC ifMountedGetMount() {
		
		NPC npc = getMountNPC();
		
		if ( npc == null || !npc.isSpawned() ) 
			npc = myNPC;
		
		return npc;
	}

	public void dismount() {
		//get off and despawn the horse.
		if 	(  myNPC.isSpawned() 
			&& getMyEntity().isInsideVehicle() ) {
				
				NPC mount = getMountNPC();
				
				if ( mount != null ) {
					getMyEntity().getVehicle().setPassenger( null );
					mount.despawn( DespawnReason.PLUGIN );
				}
		}
	}

	public void mount(){
		if ( myNPC.isSpawned() ) {
			
			LivingEntity myEntity = getMyEntity();
			
			if ( myEntity.isInsideVehicle() ) 
				myEntity.getVehicle().setPassenger( null );
			
			NPC mount = getMountNPC();

			if  (  mount == null 
				||  (  !mount.isSpawned() 
					&& !mountCreated ) ) {
				
					mount = createMount();
			}

			if ( mount != null ) {
				mountCreated = true;
				
				if ( !mount.isSpawned() ) return; //dead mount
				
				mount.data().set( NPC.DEFAULT_PROTECTED_METADATA, false );
				
				NavigatorParameters params = mount.getNavigator().getDefaultParameters();
				
				params.attackStrategy( new MountAttackStrategy() );
				params.useNewPathfinder( false );
				params.speedModifier( myNPC.getNavigator().getDefaultParameters().speedModifier() * 2 );
				params.range( myNPC.getNavigator().getDefaultParameters().range() + 5 );
				
				((CraftLivingEntity) mount.getEntity()).setCustomNameVisible( false );
				mount.getEntity().setPassenger( null );
				mount.getEntity().setPassenger( myEntity );
			}
			else mountID = -1;
		}
	}

	public  NPC createMount() {
		sentry.debug( "Creating mount for " + myNPC.getName() );

		if ( myNPC.isSpawned() ) {
			
			if ( getMyEntity() == null ) 
				Sentry.logger.info( "why is this spawned but bukkit entity is null???" );
			
			NPC mount = null;

			if ( isMounted() ) {
				mount =	CitizensAPI.getNPCRegistry().getById( mountID );

				if ( mount != null ) 
					mount.despawn();
				else 
					Sentry.logger.info( "Cannot find mount NPC " + mountID );
			}
			else {
				mount = CitizensAPI.getNPCRegistry().createNPC( EntityType.HORSE, myNPC.getName() + "_Mount" );
				mount.getTrait( MobType.class ).setType( EntityType.HORSE );
			}

			if ( mount == null ) {
				Sentry.logger.info( "Cannot create mount NPC!" );
			}
			else {
				mount.spawn( getMyEntity().getLocation() );
				
				mount.getTrait( Owner.class ).setOwner( myNPC.getTrait( Owner.class ).getOwner() );
				
				( (Horse) mount.getEntity() ).getInventory().setSaddle( new ItemStack( Material.SADDLE ) );
	
				mountID = mount.getId();
	
				return mount;
			}
		}
		return null;
	}

	public boolean hasLOS( Entity other ) {
		
		if ( !myNPC.isSpawned() ) return false;
        if ( ignoreLOS ) return true;
        
		return getMyEntity().hasLineOfSight( other );
	}

	public LivingEntity getMyEntity() {
		if 	(  myNPC == null 
			|| myNPC.getEntity() == null 
			|| myNPC.getEntity().isDead() ) 
				return null;
		
		if ( !( myNPC.getEntity() instanceof LivingEntity ) ) {
			Sentry.logger.info("Sentry " + myNPC.getName() + " is not a living entity! Errors inbound....");
			return null;
		}
		return (LivingEntity) myNPC.getEntity();
	}

	protected NPC getMountNPC() {
		if ( isMounted() && CitizensAPI.hasImplementation() ) {

			return CitizensAPI.getNPCRegistry().getById( mountID );
		}
		return null;
	}
	
	public boolean isMounted() {
		return mountID >= 0;
	}
}
