package net.aufdemrand.sentry;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.bukkit.entity.Egg;
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
import org.bukkit.entity.Snowball;
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

	enum Hittype {
		block, disembowel, glance, injure, main, miss, normal
	}

	private Set<Player> _myDamamgers = new HashSet<Player>();

	private Location _projTargetLostLoc;
	
	Location spawnLocation = null;
	
	int strength = 1;
	int armorValue = 0;
	int lightningLevel = 0;
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
	double sentryHealth = 20;

	boolean killsDropInventory = true;
	boolean dropInventory = false;
	boolean targetable = true;
	boolean inciendary = false;
	boolean invincible = false;
	boolean lightning = false;
	boolean loaded = false;
	boolean luckyHits = true;
	boolean iWillRetaliate = true;
    boolean ignoreLOS;
    boolean mountCreated = false;
	
	private GiveUpStuckAction giveup = new GiveUpStuckAction( this );

	public String greetingMsg = "&a<NPC> says: Welcome, <PLAYER>!";
	public String warningMsg = "&a<NPC> says: Halt! Come no further!";
	
	public LivingEntity guardEntity = null;;
	public String guardTarget = null;

	PacketPlayOutAnimation healAnimation = null;
	
	public List<String> ignoreTargets = new ArrayList<String>();
	public List<String> validTargets = new ArrayList<String>();

	public Set<String> _ignoreTargets = new HashSet<String>();
	public Set<String> _validTargets = new HashSet<String>();

	public LivingEntity meleeTarget;
	public NPC myNPC = null;
	
	private Class<? extends Projectile> myProjectile;
	
	public SentryTrait myTrait;
	
	// TODO why are we saving four instantces of the system time?
	long isRespawnable = System.currentTimeMillis();	
	long oktoFire = System.currentTimeMillis();
	long oktoheal = System.currentTimeMillis();
	long oktoreasses= System.currentTimeMillis();
	long okToTakedamage = 0;
	

	Sentry sentry;
	public List<PotionEffect> potionEffects = null;
	ItemStack potiontype = null;
	public LivingEntity projectileTarget;
	Random random = new Random();
	
	public SentryStatus sentryStatus = SentryStatus.isDYING;
	

	private int taskID = 0;
	private Map<Player, Long> Warnings = new HashMap<Player, Long>();

	
	public SentryInstance( Sentry plugin ) {
		sentry = plugin;
		isRespawnable = System.currentTimeMillis();
	}
	
	public void initialize() {

		// check for illegal values
		if ( sentryWeight <= 0 ) 		sentryWeight = 1.0;
		if ( attackRate > 30)	attackRate = 30.0;
		if ( sentryHealth < 0 )			sentryHealth = 0;
		if ( sentryRange < 1 )			sentryRange = 1;
		if ( sentryRange > 200 )		sentryRange = 200;
		if ( sentryWeight <= 0 )		sentryWeight =  1.0;
		if ( respawnDelay < -1 )	respawnDelay = -1;
		if ( spawnLocation == null ) 	spawnLocation = getMyEntity().getLocation();
		
		if ( Sentry.denizenActive ) {
			if ( myNPC.hasTrait( HealthTrait.class ) ) myNPC.removeTrait( HealthTrait.class );
		}

		// disable citizens respawning, because Sentry doesn't always raise EntityDeath
		myNPC.data().set( "respawn-delay",-1 );

		setHealth( sentryHealth );

		_myDamamgers.clear();

		sentryStatus = SentryStatus.isLOOKING;
		faceForward();

		healAnimation = new PacketPlayOutAnimation( ((CraftEntity)getMyEntity()).getHandle(), 6);

		//	Packet derp = new net.minecraft.server.Packet15Place();

		if ( guardTarget == null ) 
			myNPC.teleport( spawnLocation, TeleportCause.PLUGIN ); //it should be there... but maybe not if the position was saved elsewhere.

		float pf = myNPC.getNavigator().getDefaultParameters().range();

		if ( pf < sentryRange + 5 ) {
			pf = sentryRange + 5;
		}

		myNPC.data().set( NPC.DEFAULT_PROTECTED_METADATA, false );
		myNPC.data().set( NPC.TARGETABLE_METADATA, this.targetable );

		myNPC.getNavigator().getDefaultParameters().range( pf );
		myNPC.getNavigator().getDefaultParameters().stationaryTicks( 5 * 20 );
		myNPC.getNavigator().getDefaultParameters().useNewPathfinder( false );
		//	myNPC.getNavigator().getDefaultParameters().stuckAction(new BodyguardTeleportStuckAction(this, this.plugin));

		// plugin.getServer().broadcastMessage("NPC GUARDING!");

		if ( getMyEntity() instanceof Creeper )
			myNPC.getNavigator().getDefaultParameters().attackStrategy(new CreeperAttackStrategy());
		else if ( getMyEntity() instanceof Spider )
			myNPC.getNavigator().getDefaultParameters().attackStrategy(new SpiderAttackStrategy( sentry ));
		
		processTargets();

		if ( taskID == 0 ) {
			taskID = sentry.getServer().getScheduler().scheduleSyncRepeatingTask(sentry, new SentryLogic(), 40 + this.myNPC.getId(),  sentry.logicTicks);
		}
	//	mountCreated = false;
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
		
		if 	(  sentryStatus == SentryStatus.isDYING 
			|| sentryStatus == SentryStatus.isDEAD 
			|| !( getMyEntity() instanceof LivingEntity ) ) 
					return;

		sentryStatus = SentryStatus.isDYING;

		setTarget( null, false );
		//		myNPC.getTrait(Waypoints.class).getCurrentProvider().setPaused(true);

		boolean handled = false;

		if ( runscripts && Sentry.denizenActive ) {
			handled = DenizenHook.sentryDeath( _myDamamgers, myNPC );
		}
		if ( handled ) return;

		if ( Sentry.denizenActive ) {
			try {
				Entity killer = getMyEntity().getKiller();
				
				if ( killer == null ) {
					//might have been a projectile.
					EntityDamageEvent ev = getMyEntity().getLastDamageCause();
					if 	(  ev != null 
						&& ev instanceof EntityDamageByEntityEvent) {
							killer = ( (EntityDamageByEntityEvent) ev).getDamager();
					}
				}
				DenizenHook.denizenAction( myNPC, "death", null );
				DenizenHook.denizenAction( myNPC, "death by" + cause.toString().replace( " " ,"_" ), null );


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

		sentryStatus = SentryStatus.isDEAD;

		if ( this.dropInventory )  
			getMyEntity().getLocation().getWorld()
									   .spawn( getMyEntity().getLocation(), ExperienceOrb.class )
									   .setExperience( sentry.sentryEXP );

		List<ItemStack> items = new java.util.LinkedList<ItemStack>();

		if ( getMyEntity() instanceof HumanEntity ) {
			//get drop inventory.
			for ( ItemStack is : ( (HumanEntity) getMyEntity()).getInventory().getArmorContents() ) {
				if ( is.getTypeId() > 0 ) 
					items.add( is );
			}

			ItemStack is = ( (HumanEntity) getMyEntity()).getInventory().getItemInHand();
			
			if ( is.getTypeId() > 0 ) items.add( is );

			((HumanEntity) getMyEntity()).getInventory().clear();
			((HumanEntity) getMyEntity()).getInventory().setArmorContents( null );
			((HumanEntity) getMyEntity()).getInventory().setItemInHand( null );
		}

		if ( items.isEmpty() ) 
			getMyEntity().playEffect( EntityEffect.DEATH );
		else 
			getMyEntity().playEffect( EntityEffect.HURT );

		if ( !dropInventory ) items.clear();

		for ( ItemStack is : items ) 
			getMyEntity().getWorld().dropItemNaturally( getMyEntity().getLocation(), is );

		if ( sentry.dieLikePlayers ) {
			getMyEntity().setHealth( 0 );
		}
		else {
			EntityDeathEvent ed = new EntityDeathEvent( getMyEntity(), items );

			sentry.getServer().getPluginManager().callEvent( ed );
			//citizens will despawn it.
		}

		if ( respawnDelay == -1 ) {
			
			cancelRunnable();
			if ( isMounted() ) Util.removeMount( mountID );
			myNPC.destroy();
			return;
		} else {
			isRespawnable = System.currentTimeMillis() + respawnDelay * 1000;
		}
	}


	private void faceEntity( Entity from, Entity at ) {

		if ( from.getWorld() != at.getWorld() )	return;
		
		Location loc = from.getLocation();

		double xDiff = at.getLocation().getX() - loc.getX();
		double yDiff = at.getLocation().getY() - loc.getY();
		double zDiff = at.getLocation().getZ() - loc.getZ();

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
		Entity v = getMyEntity().getVehicle();
		NMS.look( getMyEntity(), v.getLocation().getYaw(), 0 );
	}

	public LivingEntity findTarget( Integer Range ) {
		
		Range += warningRange;
		List<Entity> EntitiesWithinRange = getMyEntity().getNearbyEntities( Range, Range, Range );
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

					double dist = aTarget.getLocation().distance( getMyEntity().getLocation() );

					if ( hasLOS( aTarget ) ) {

						if  (  warningRange > 0 
							&& sentryStatus == SentryStatus.isLOOKING 
							&& aTarget instanceof Player 
							&& dist > ( Range - warningRange ) 
							&& !CitizensAPI.getNPCRegistry().isNPC( aTarget ) 
							&& !warningMsg.isEmpty() ) {

							if  (  !Warnings.containsKey( aTarget ) 
								|| System.currentTimeMillis() > Warnings.get( aTarget ) + 60 * 1000 ) {
								
								((Player) aTarget).sendMessage( getWarningMessage( (Player) aTarget ) );
								
								if ( !getNavigator().isNavigating() )
									faceEntity( getMyEntity(), aTarget );
								
								Warnings.put( (Player) aTarget, System.currentTimeMillis() );
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
					&& sentryStatus == SentryStatus.isLOOKING 
					&& aTarget instanceof Player 
					&& !CitizensAPI.getNPCRegistry().isNPC( aTarget ) 
					&& !greetingMsg.isEmpty() ) {
					
					if  (  getMyEntity().hasLineOfSight( aTarget ) 
						&&  (  !Warnings.containsKey( aTarget ) 
							|| System.currentTimeMillis() > Warnings.get( aTarget ) + 60 * 1000 ) ) {
							
								((Player) aTarget).sendMessage( getGreetingMEssage( (Player) aTarget ) );
								faceEntity( getMyEntity(), aTarget );
								Warnings.put( (Player) aTarget, System.currentTimeMillis() );
					}
				
			}
		}

		if ( theTarget != null ) {
			// plugin.getServer().broadcastMessage("Targeting: " +
			// theTarget.toString());
			return theTarget;
		}

		return null;
	}


	public void draw( boolean on ) {
		( (CraftLivingEntity) getMyEntity() ).getHandle().b( on ); // TODO: 1.8 UPDATE - IS THIS CORRECT?
	}
	
	public void Fire(LivingEntity theEntity) {
		
		double v = 34;
		double g = 20;

		Effect effect = null;

		boolean ballistics = true;

		if (myProjectile == Arrow.class) {
			effect = Effect.BOW_FIRE;
		} else if (myProjectile == SmallFireball.class || myProjectile == Fireball.class || myProjectile == WitherSkull.class) {
			effect = Effect.BLAZE_SHOOT;
			ballistics =false;
		}
		else if (myProjectile == ThrownPotion.class){
			v = 21;
			g = 20;
		}
		else {
			v = 17.75;
			g = 13.5;
		}

		if ( lightning ) {
			ballistics = false;
			effect =null;
		}

		// calc shooting spot.
		Location loc = Util.getFireSource( getMyEntity(), theEntity );
		Location targetsHeart = theEntity.getLocation();
		
		targetsHeart = targetsHeart.add(0, .33, 0);
		Vector test = targetsHeart.clone().subtract( loc ).toVector();

		double elev = test.getY();
		Double testAngle = Util.launchAngle( loc, targetsHeart, v, elev, g );

		if ( testAngle == null && clearTargets() ) return;

		double hangtime = Util.hangtime( testAngle, v, elev, g );
		Vector targetVelocity = theEntity.getLocation().subtract( _projTargetLostLoc ).toVector();

		targetVelocity.multiply( 20 / sentry.logicTicks );
		
		Location to = Util.leadLocation( targetsHeart, targetVelocity, hangtime );
		Vector victor = to.clone().subtract( loc ).toVector();

		double dist = Math.sqrt( Math.pow( victor.getX(), 2 ) + Math.pow( victor.getZ(), 2 ) );
		elev = victor.getY();
		
		if ( dist == 0 ) return;

		if ( !hasLOS( theEntity ) && clearTargets() ) return;
			
		if ( ballistics ) {
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
		else if ( dist > sentryRange && clearTargets() ) return;

		if ( lightning ) {
			if ( lightningLevel == 2 ) {
				to.getWorld().strikeLightning( to );
			}
			else if ( lightningLevel == 1 ) {
				to.getWorld().strikeLightningEffect( to );
				theEntity.damage( getStrength(), getMyEntity() );
			}
			else if ( lightningLevel == 3 ) {
				to.getWorld().strikeLightningEffect( to );
				theEntity.setHealth( 0 );
			}
		}
		else {
			Projectile theArrow;

			if ( myProjectile == ThrownPotion.class ) {
				net.minecraft.server.v1_8_R3.World nmsWorld = ((CraftWorld)getMyEntity().getWorld()).getHandle();
				EntityPotion ent = new EntityPotion(nmsWorld
													, loc.getX()
													, loc.getY()
													, loc.getZ()
													, CraftItemStack.asNMSCopy( potiontype ) );
				nmsWorld.addEntity( ent );
				theArrow = (Projectile) ent.getBukkitEntity();
			}
			else if ( myProjectile == EnderPearl.class ) 
				theArrow = getMyEntity().launchProjectile( myProjectile );
			else 
				theArrow = getMyEntity().getWorld().spawn( loc, myProjectile );

			if ( myProjectile == Fireball.class || myProjectile == WitherSkull.class ) {
				victor = victor.multiply( 1 / 1000000000 );
			}
			else if ( myProjectile == SmallFireball.class ) {
				
				victor = victor.multiply( 1 / 1000000000 );
				( (SmallFireball) theArrow ).setIsIncendiary( inciendary );
				
				if ( !inciendary ) {
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
			theArrow.setShooter( getMyEntity() );
			theArrow.setVelocity( victor );
		}

		// OK we're shooting
		if ( effect != null )
			getMyEntity().getWorld().playEffect( getMyEntity().getLocation(), effect, null );

		if ( myProjectile == Arrow.class ) {
			draw( false );
		}
		else if ( getMyEntity() instanceof Player )	{
				PlayerAnimation.ARM_SWING.play( (Player) getMyEntity(), 64 );
		}
	}

	public int getArmor(){

		double mod = 0;
		if ( getMyEntity() instanceof Player){
			for (ItemStack is:((Player)getMyEntity()).getInventory().getArmorContents()){
				if (sentry.armorBuffs.containsKey(is.getTypeId())) mod += sentry.armorBuffs.get(is.getTypeId());
			}
		}

		return (int) (armorValue + mod);
	}

	String getGreetingMEssage( Player player ) {
		String str = greetingMsg.replace( "<NPC>", myNPC.getName() ).replace( "<PLAYER>", player.getName() );
		return ChatColor.translateAlternateColorCodes('&', str);
	}

	public LivingEntity getGuardTarget() {
		return guardEntity;
	}

	public double getHealth(){
		if ( myNPC == null || getMyEntity() == null ) return 0;
		
		return  ( (CraftLivingEntity) getMyEntity() ).getHealth();
	}

	public float getSpeed() {
		
		if ( !myNPC.isSpawned() ) return sentrySpeed;
		
		double mod = 0;
		if ( getMyEntity() instanceof Player ) {
			for ( ItemStack is : ((Player) getMyEntity()).getInventory().getArmorContents() ) {
				if ( sentry.speedBuffs.containsKey( is.getTypeId() ) ) 
					mod += sentry.speedBuffs.get( is.getTypeId() );
			}
		}
		return (float) (sentrySpeed + mod) * ( getMyEntity().isInsideVehicle() ? 2 : 1 );
	}
	
	public String getStats() {
		
		DecimalFormat df = new DecimalFormat( "#.0" );
		double h = getHealth();

		return  ChatColor.RED + "[HP]:" + ChatColor.WHITE + h + "/" + sentryHealth + 
				ChatColor.RED + " [AP]:" + ChatColor.WHITE + getArmor() +
				ChatColor.RED + " [STR]:" + ChatColor.WHITE + getStrength() + 
				ChatColor.RED + " [SPD]:" + ChatColor.WHITE + df.format( getSpeed() ) +
				ChatColor.RED + " [RNG]:" + ChatColor.WHITE + sentryRange + 
				ChatColor.RED + " [ATK]:" + ChatColor.WHITE + attackRate + 
				ChatColor.RED + " [VIS]:" + ChatColor.WHITE + nightVision +
				ChatColor.RED + " [HEAL]:" + ChatColor.WHITE + healRate + 
				ChatColor.RED + " [WARN]:" + ChatColor.WHITE + warningRange + 
				ChatColor.RED + " [FOL]:" + ChatColor.WHITE + Math.sqrt( followDistance );
	}

	public int getStrength(){
		
		double mod = 0;

		if  (  getMyEntity() instanceof Player 
			&& sentry.strengthBuffs.containsKey( ((Player) getMyEntity()).getInventory().getItemInHand().getTypeId() ) ) {
				
				mod += sentry.strengthBuffs.get( ((Player)getMyEntity()).getInventory().getItemInHand().getTypeId() );
		}
		return (int) (strength + mod);
	}

	String getWarningMessage( Player player ) {
		
		String str =  warningMsg.replace( "<NPC>", myNPC.getName() ).replace( "<PLAYER>", player.getName() );
		
		return ChatColor.translateAlternateColorCodes( '&', str );
	}
	
	public boolean isPyromancer() {
		return ( myProjectile == Fireball.class || myProjectile == SmallFireball.class ) ;
	}

	public boolean isPyromancer1() {
		return ( !inciendary && myProjectile == SmallFireball.class ) ;
	}

	public boolean isPyromancer2() {
		return ( inciendary && myProjectile == SmallFireball.class ) ;
	}

	public boolean isPyromancer3() {
		return ( myProjectile == Fireball.class ) ;
	}

	public boolean isStormcaller() {
		return ( lightning ) ;
	}

	public boolean isWarlock1() {
		return ( myProjectile == EnderPearl.class ) ;
	}

	public boolean isWitchDoctor() {
		return ( myProjectile == ThrownPotion.class) ;
	}


	public void onDamage( EntityDamageByEntityEvent event ) {

		if ( sentryStatus == SentryStatus.isDYING ) return;

		if ( myNPC == null || !myNPC.isSpawned() ) return;

		if ( guardTarget != null && guardEntity == null ) return; //dont take damage when bodyguard target isnt around.

		if ( System.currentTimeMillis() < okToTakedamage + 500 ) return;
		
		okToTakedamage = System.currentTimeMillis();

		event.getEntity().setLastDamageCause( event );

		NPC npc = myNPC;

		LivingEntity attacker = null;

		Hittype hit = Hittype.normal;

		double finaldamage = event.getDamage();

		// Find the attacker
		if (event.getDamager() instanceof Projectile) {
			if (((Projectile) event.getDamager()).getShooter() instanceof LivingEntity) {
				attacker = (LivingEntity) ((Projectile) event.getDamager()).getShooter();
			}
		} else if (event.getDamager() instanceof LivingEntity) {
			attacker = (LivingEntity) event.getDamager();
		}

		if (invincible)
			return;

		if(sentry.ignoreInvincibility ){
			if(isIgnored(attacker)) return;
		}

		// can i kill it? lets go kill it.
		if (attacker != null) {
			if (this.iWillRetaliate) {
				if ( !(event.getDamager() instanceof Projectile) || (CitizensAPI.getNPCRegistry().getNPC(attacker) == null)) {
					// only retaliate to players or non-projectiles. Prevents stray sentry arrows from causing retaliation.

					setTarget(attacker, true);

				}
			}
		}

		if (luckyHits) {
			// Calculate crits
			double damagemodifer = event.getDamage();

			int luckeyhit = random.nextInt(100);

			if (luckeyhit < sentry.crit3Chance) {
				damagemodifer = damagemodifer * 2.00;
				hit = Hittype.disembowel;
			} else if (luckeyhit < sentry.crit3Chance + sentry.crit2Chance) {
				damagemodifer = damagemodifer * 1.75;
				hit = Hittype.main;
			} else if (luckeyhit < sentry.crit3Chance + sentry.crit2Chance + sentry.crit1Chance) {
				damagemodifer = damagemodifer * 1.50;
				hit = Hittype.injure;
			} else if (luckeyhit <  sentry.crit3Chance + sentry.crit2Chance + sentry.crit1Chance + sentry.glanceChance) {
				damagemodifer = damagemodifer * 0.50;
				hit = Hittype.glance;
			} else if (luckeyhit < sentry.crit3Chance + sentry.crit2Chance + sentry.crit1Chance + sentry.glanceChance + sentry.missChance) {
				damagemodifer = 0;
				hit = Hittype.miss;
			}

			finaldamage = Math.round(damagemodifer);
		}

		int arm = getArmor();

		if (finaldamage > 0) {

			if (attacker != null) {
				// knockback
				npc.getEntity().setVelocity( attacker.getLocation().getDirection().multiply(1.0 / (sentryWeight + (arm/5))));
			}

			// Apply armor
			finaldamage -= arm;

			// there was damamge before armor.
			if (finaldamage <= 0){
				npc.getEntity().getWorld().playEffect(npc.getEntity().getLocation(), Effect.ZOMBIE_CHEW_IRON_DOOR,1);
				hit = Hittype.block;
			}
		}

		if (attacker instanceof Player && !CitizensAPI.getNPCRegistry().isNPC(attacker)) {

			_myDamamgers.add((Player) attacker);
			String msg = null;
			// Messages
			switch (hit) {
			case normal:
				msg = sentry.hitMessage;
				break;
			case miss:
				msg = sentry.missMessage;
				break;
			case block:
				msg = sentry.blockMessage;
				break;
			case main:
				msg = sentry.crit2Message;
				break;
			case disembowel:
				msg = sentry.crit3Message;
				break;
			case injure:
				msg = sentry.crit1Message;
				break;
			case glance:
				msg = sentry.glanceMessage;
				break;
			}

			if(msg!=null && msg.isEmpty() == false){
				((Player) attacker).sendMessage(Util.format(msg, npc, attacker, ((Player) attacker).getItemInHand().getTypeId(), finaldamage+""));
			}
		}

		if (finaldamage > 0) {
			npc.getEntity().playEffect(EntityEffect.HURT);

			// is he dead?
			if (getHealth() - finaldamage <= 0) {

				//set the killer
				if ( event.getDamager() instanceof HumanEntity ) 
					( (CraftLivingEntity)getMyEntity() ).getHandle().killer 
								= (EntityHuman) ((CraftLivingEntity) event.getDamager()).getHandle();

				die( true, event.getCause() );

			}
			else getMyEntity().damage(finaldamage);
		}
	}

	public void onEnvironmentDamage(EntityDamageEvent event){

		if(sentryStatus == SentryStatus.isDYING) return;

		if (!myNPC.isSpawned() || invincible) return;

		if (guardTarget != null && guardEntity == null) return; //dont take damage when bodyguard target isnt around.

		if (System.currentTimeMillis() <  okToTakedamage + 500) return;
		okToTakedamage = System.currentTimeMillis();

		getMyEntity().setLastDamageCause(event);

		double finaldamage = event.getDamage();

		if (event.getCause() == DamageCause.CONTACT || event.getCause() == DamageCause.BLOCK_EXPLOSION){
			finaldamage -= getArmor();
		}

		if (finaldamage > 0 ){
			getMyEntity().playEffect(EntityEffect.HURT);

			if (event.getCause() == DamageCause.FIRE){
				if (!getNavigator().isNavigating()){
					getNavigator().setTarget(getMyEntity().getLocation().add(random.nextInt(2)-1, 0, random.nextInt(2)-1));
				}
			}

			if (getHealth() - finaldamage <= 0) {

				die(true, event.getCause());

				// plugin.getServer().broadcastMessage("Dead!");
			}
			else {
				getMyEntity().damage(finaldamage);

			}
		}


	}

//  @EventHandler
//	public void onRightClick(NPCRightClickEvent event) {}

	final int all = 1;
	final int players = 2;
	final int npcs = 4;
	final int monsters = 8;
	final int events = 16;
	final int namedentities = 32;
	final int namedplayers = 64;
	final int namednpcs = 128;
	final int faction = 256;
	final int towny = 512;
	final int war = 1024;
	final int permGroups = 2048;
	final int owner = 4096;
	final int clans = 8192;
	final int townyenemies = 16384;
	final int factionEnemies = 16384*2;
	final int mcTeams = 16384*4;

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

			for (String t: validTargets){
				if (t.contains("ENTITY:ALL")) targets |= all;
				else	if(t.contains("ENTITY:MONSTER")) targets |= monsters;
				else	if(t.contains("ENTITY:PLAYER")) targets |= players;
				else	if(t.contains("ENTITY:NPC")) targets |= npcs;
				else{
					_validTargets.add(t);
					if(t.contains("NPC:")) targets |= namednpcs;
					else if (Sentry.perms!=null && Sentry.perms.isEnabled() && t.contains("GROUP:")) targets |= permGroups;
					else if (t.contains("EVENT:"))  targets |= events;
					else	if(t.contains("PLAYER:")) targets |= namedplayers;
					else	if(t.contains("ENTITY:")) targets |= namedentities;
					else	if (Sentry.factionsActive && t.contains("FACTION:")) targets |= faction;
					else	if (Sentry.factionsActive && t.contains("FACTIONENEMIES:")){
						targets |= factionEnemies;
						FactionEnemies.add(t.split(":")[1]);
					}
					else	if (Sentry.townyActive && t.contains("TOWN:")) targets |= towny;
					else	if (Sentry.townyActive && t.contains("NATIONENEMIES:")) {
						targets |= townyenemies;
						NationsEnemies.add(t.split(":")[1]);
					}
					else	if (Sentry.townyActive && t.contains("NATION:"))  targets |= towny;
					else	if (Sentry.warActive && t.contains("WARTEAM:"))  targets |= war;
					else	if (t.contains("TEAM:"))  targets |= mcTeams;
					else	if (Sentry.clansActive && t.contains("CLAN:"))  targets |= clans;
				}
			}
			for (String t: ignoreTargets){
				if(t.contains("ENTITY:ALL")) ignores |= all;
				else	if(t.contains("ENTITY:MONSTER")) ignores |= monsters;
				else	if(t.contains("ENTITY:PLAYER")) ignores |= players;
				else	if(t.contains("ENTITY:NPC")) ignores |= npcs;
				else	if(t.contains("ENTITY:OWNER")) ignores |= owner;
				else{
					_ignoreTargets.add(t);
					if (Sentry.perms!=null && Sentry.perms.isEnabled() && t.contains("GROUP:")) ignores |= permGroups;
					else	if(t.contains("NPC:")) ignores |= namednpcs;
					else	if(t.contains("PLAYER:")) ignores |= namedplayers;
					else	if(t.contains("ENTITY:")) ignores |= namedentities;
					else	if (Sentry.factionsActive && t.contains("FACTION:")) ignores |= faction;
					else	if (Sentry.townyActive && t.contains("TOWN:")) ignores |= towny;
					else	if (Sentry.townyActive && t.contains("NATION:"))  ignores |= towny;
					else	if (Sentry.warActive && t.contains("TEAM:"))  ignores |= war;
					else	if (Sentry.clansActive && t.contains("CLAN:"))  ignores |= clans;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	private class SentryLogic implements Runnable {

		@Override
		public void run() {
			// plugin.getServer().broadcastMessage("tick " + (myNPC ==null) +
			if (getMyEntity() == null ) sentryStatus = SentryStatus.isDEAD; // incase it dies in a way im not handling.....

			if (UpdateWeapon()){
				//ranged
				if(meleeTarget !=null) {
					sentry.debug(myNPC.getName() + " Switched to ranged");
					LivingEntity derp = meleeTarget;
					boolean ret = sentryStatus == SentryStatus.isRETALIATING;
					setTarget(null, false);
					setTarget(derp, ret);
				}
			}
			else{
				//melee
				if(projectileTarget != null) {
					sentry.debug(myNPC.getName() + " Switched to melee");
					boolean ret = ( sentryStatus == SentryStatus.isRETALIATING );
					LivingEntity derp = projectileTarget;
					setTarget(null, false);
					setTarget(derp, ret);
				}
			}

			if (sentryStatus != SentryStatus.isDEAD &&  healRate > 0) {
				if(System.currentTimeMillis() > oktoheal ){
					if (getHealth() < sentryHealth && sentryStatus !=  SentryStatus.isDEAD && sentryStatus != SentryStatus.isDYING) {
						double heal = 1;
						if (healRate <0.5) heal = (0.5 / healRate);


						setHealth(getHealth() + heal);


						if (healAnimation!=null) NMS.sendPacketsNearby(null, getMyEntity().getLocation(),healAnimation);

						if (getHealth() >= sentryHealth) _myDamamgers.clear(); //healed to full, forget attackers

					}
					oktoheal = (long) (System.currentTimeMillis() + healRate * 1000);
				}

			}

			if(myNPC.isSpawned() && getMyEntity().isInsideVehicle() == false && isMounted() && isMyChunkLoaded()) mount();

			if (sentryStatus == SentryStatus.isDEAD 
					&& System.currentTimeMillis() > isRespawnable 
					&& respawnDelay > 0 & spawnLocation.getWorld().isChunkLoaded( spawnLocation.getBlockX() >> 4, spawnLocation.getBlockZ()>>4)) {
				// Respawn

				sentry.debug("respawning" + myNPC.getName());
				if (guardEntity == null) {
					myNPC.spawn(spawnLocation.clone());
					//	myNPC.teleport(Spawn,org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
				} else {
					myNPC.spawn(guardEntity.getLocation().add(2, 0, 2));
					//	myNPC.teleport(guardEntity.getLocation().add(2, 0, 2),org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
				}
				return;
			}
			else if ((sentryStatus == SentryStatus.isHOSTILE || sentryStatus == SentryStatus.isRETALIATING) && myNPC.isSpawned()) {

				if (!isMyChunkLoaded()){
					setTarget(null, false);
					return;
				}

				if (targets >0 && sentryStatus == SentryStatus.isHOSTILE && System.currentTimeMillis() > oktoreasses) {
					LivingEntity target = findTarget(sentryRange);
					setTarget(target, false);
					oktoreasses = System.currentTimeMillis() + 3000;
				}

				if (projectileTarget != null 
						&& !projectileTarget.isDead() 
						&& projectileTarget.getWorld() == getMyEntity().getLocation().getWorld() ) {
					
					if (_projTargetLostLoc == null)
						_projTargetLostLoc = projectileTarget.getLocation();

					if (!getNavigator().isNavigating())	faceEntity(getMyEntity(), projectileTarget);

					draw(true);

					if (System.currentTimeMillis() > oktoFire) {
						// Fire!
						oktoFire = (long) (System.currentTimeMillis() + attackRate * 1000.0);
						Fire(projectileTarget);
					}
					if (projectileTarget != null)
						_projTargetLostLoc = projectileTarget.getLocation();

					return; // keep at it
				}

				else if (meleeTarget != null && !meleeTarget.isDead()) {

					if (isMounted()) faceEntity(getMyEntity(), meleeTarget);

					if (meleeTarget.getWorld() == getMyEntity().getLocation().getWorld()) {
						double dist=  meleeTarget.getLocation().distance(getMyEntity().getLocation());
						//block if in range
						draw(dist < 3);
						// Did it get away?
						if(dist > sentryRange) {
							// it got away...
							setTarget(null, false);
						}
					}
					else {
						setTarget(null, false);
					}

				}

				else {
					// target died or null
					setTarget(null, false);
				}

			}

			else if (sentryStatus == SentryStatus.isLOOKING && myNPC.isSpawned()) {

				if(getMyEntity().isInsideVehicle() == true) faceAlignWithVehicle(); //sync the rider with the vehicle.


				if (guardEntity instanceof Player){
					if (((Player)guardEntity).isOnline() == false){
						guardEntity = null;
					}
				}

				if (guardTarget != null && guardEntity == null) {
					// daddy? where are u?
					setGuardTarget(guardTarget, false);
				}

				if (guardTarget != null && guardEntity == null) {
					// daddy? where are u?
					setGuardTarget(guardTarget, true);
				}

				if (guardEntity !=null){

					Location npcLoc = getMyEntity().getLocation();

					if (guardEntity.getLocation().getWorld() != npcLoc.getWorld() || !isMyChunkLoaded()){
						if(Util.CanWarp(guardEntity, myNPC)){
							myNPC.despawn();
							myNPC.spawn((guardEntity.getLocation().add(1, 0, 1)));
						}
						else {
							((Player) guardEntity).sendMessage(myNPC.getName() + " cannot follow you to " + guardEntity.getWorld().getName());
							guardEntity = null;
						}

					}
					else{
						double dist = npcLoc.distanceSquared(guardEntity.getLocation());
						sentry.debug(myNPC.getName() + dist + getNavigator().isNavigating() + " " +getNavigator().getEntityTarget() + " " );
						if(dist > 1024) {
							myNPC.teleport(guardEntity.getLocation().add(1,0,1),TeleportCause.PLUGIN);
						}
						else if(dist > followDistance && !getNavigator().isNavigating()) {
							getNavigator().setTarget((Entity)guardEntity, false);
							getNavigator().getLocalParameters().stationaryTicks(3*20);
						}
						else if (dist < followDistance && getNavigator().isNavigating()) {
							getNavigator().cancelNavigation();
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

	public boolean setGuardTarget(String name, boolean forcePlayer) {

		if ( myNPC == null )
			return false;

		if ( name == null ) {
			guardEntity = null;
			guardTarget = null;

			return clearTargets();
		}

		if ( !forcePlayer ) {

			List<Entity> EntitiesWithinRange = getMyEntity().getNearbyEntities( sentryRange, sentryRange, sentryRange );

			for ( Entity aTarget : EntitiesWithinRange ) {

				if ( aTarget instanceof Player ) {
					//chesk for players
					if ( ( (Player) aTarget ).getName().equals( name ) ) {
						
						guardEntity = (LivingEntity) aTarget;
						guardTarget = ((Player) aTarget).getName();

						return clearTargets();
					}
				}
				else if ( aTarget instanceof LivingEntity ) {
					//check for named mobs.
					String ename = ( (LivingEntity) aTarget ).getCustomName();
					
					if ( ename != null && ename.equals( name ) ) {
						
						guardEntity = (LivingEntity) aTarget;
						guardTarget = ename;

						return clearTargets();
					}
				}

			}
		}
		else {

			for ( Player player : sentry.getServer().getOnlinePlayers() ) {
				
				if ( player.getName().equals( name ) ) {
					guardEntity = player;
					guardTarget = player.getName();

					return clearTargets();
				}
			}
		}
		return false;
	}

	public void setHealth( double health ) {
		
		if ( myNPC == null ) return;
		if ( getMyEntity() == null ) return;
		
		if ( ( (CraftLivingEntity)getMyEntity() ).getMaxHealth() != sentryHealth )
				getMyEntity().setMaxHealth( sentryHealth );
		
		if ( health > sentryHealth ) health = sentryHealth;

		getMyEntity().setHealth( health );
	}

    /** 
     * @return - true to indicate a ranged attack 
     * <br>    - false for a melee attack */
	public boolean UpdateWeapon() {
		int weapon = 0;

		ItemStack is = null;

		if ( getMyEntity() instanceof HumanEntity ) {
			is = ((HumanEntity) getMyEntity()).getInventory().getItemInHand();
			weapon = is.getTypeId();
			
			if ( weapon != sentry.witchdoctor ) 
				is.setDurability( (short) 0 );
		}

		lightning = false;
		lightningLevel = 0;
		inciendary = false;
		potionEffects = sentry.weaponEffects.get( weapon );

		myProjectile = null;

		if ( weapon == sentry.archer || getMyEntity() instanceof Skeleton ) {
			myProjectile = Arrow.class;
		}
		else if ( weapon ==  sentry.pyro3 || getMyEntity() instanceof Ghast) {
			myProjectile = Fireball.class;
		}
		else if ( weapon ==  sentry.pyro2 || getMyEntity() instanceof Blaze || getMyEntity() instanceof EnderDragon ) {
			myProjectile = SmallFireball.class;
			inciendary = true;
		}
		else if ( weapon ==  sentry.pyro1 ) {
			myProjectile = SmallFireball.class;
			inciendary =false;
		}
		else if ( weapon == sentry.magi || getMyEntity() instanceof Snowman){
			myProjectile = Snowball.class;
		}
		else if ( weapon == sentry.warlock1 ) {
			myProjectile = EnderPearl.class;
		}
		else if ( weapon == sentry.warlock2 || getMyEntity() instanceof Wither){
			myProjectile = WitherSkull.class;
		}
		else if ( weapon == sentry.warlock3 ) {
			myProjectile = WitherSkull.class;
		}
		else if ( weapon == sentry.bombardier ) {
			myProjectile = Egg.class;
		}
		else if ( weapon == sentry.witchdoctor || getMyEntity() instanceof Witch ) {
			if ( is == null ) {
				is = new ItemStack( 373, 1, (short) 16396 );
			}
			myProjectile = ThrownPotion.class;
			potiontype = is;
		}
		else if ( weapon == sentry.sc1 ) {
			myProjectile = ThrownPotion.class;
			lightning = true;
			lightningLevel = 1;
		}
		else if ( weapon == sentry.sc2 ) {
			myProjectile = ThrownPotion.class;
			lightning = true;
			lightningLevel = 2;
		}
		else if ( weapon == sentry.sc3 ) {
			myProjectile = ThrownPotion.class;
			lightning = true;
			lightningLevel = 3;
		}
		else return false; //melee

	return true; //ranged
	}
	
	
	public void setTarget( LivingEntity theEntity, boolean isretaliation ) {

		if ( getMyEntity() == null || theEntity == getMyEntity() ) return; 
		
		if ( guardTarget != null && guardEntity == null ) theEntity = null; //dont go aggro when bodyguard target isnt around.

		if ( theEntity == null ) {
			sentry.debug( myNPC.getName() + "- Set Target Null" );
			// this gets called while npc is dead, reset things.
			sentryStatus = SentryStatus.isLOOKING;
			projectileTarget = null;
			meleeTarget = null;
			_projTargetLostLoc = null;
		}

		if ( myNPC == null || !myNPC.isSpawned() ) return;

		if ( theEntity == null ) {
			// no hostile target

			draw( false );


			if ( guardEntity != null ) {

				getGoalController().setPaused( true );
				//	if (!myNPC.getTrait(Waypoints.class).getCurrentProvider().isPaused())  myNPC.getTrait(Waypoints.class).getCurrentProvider().setPaused(true);

				if  (  getNavigator().getEntityTarget() == null 
					||  (  getNavigator().getEntityTarget() != null 
						&& getNavigator().getEntityTarget().getTarget() != guardEntity ) ) {

					if ( guardEntity.getLocation().getWorld() != getMyEntity().getLocation().getWorld() ) {
						myNPC.despawn();
						myNPC.spawn( guardEntity.getLocation().add( 1, 0, 1 ) );
						return;
					}

					getNavigator().setTarget( (Entity)guardEntity, false );

					getNavigator().getLocalParameters().stationaryTicks( 3 * 20 );
				}
			} else {
				//not a guard
				getNavigator().cancelNavigation();

				faceForward();

				if ( getGoalController().isPaused() )
						getGoalController().setPaused( false );
			}
			return;
		}

		if ( theEntity == guardEntity )	return; 

		if ( isretaliation ) 
			sentryStatus = SentryStatus.isRETALIATING;
		else 
			sentryStatus = SentryStatus.isHOSTILE;


		if ( !getNavigator().isNavigating() ) 
			faceEntity( getMyEntity(), theEntity );

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
						return; //already attacking this, dummy.
			
			if ( !getGoalController().isPaused() )
						getGoalController().setPaused( true );
			
			navigator.setTarget( (Entity) theEntity, true );
			navigator.getLocalParameters().speedModifier( getSpeed() );
			navigator.getLocalParameters().stuckAction( giveup );
			navigator.getLocalParameters().stationaryTicks( 5 * 20 );
		}
	}

	protected Navigator getNavigator() {
		NPC npc = getMountNPC();
		
		if ( npc == null || !npc.isSpawned() ) 
			npc = myNPC;
		
		return npc.getNavigator();
	}

	protected GoalController getGoalController() {
		NPC npc = getMountNPC();
		
		if ( npc == null || !npc.isSpawned() ) 
			npc = myNPC;
		
		return npc.getDefaultGoalController();
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
			
			if ( getMyEntity().isInsideVehicle() ) 
				getMyEntity().getVehicle().setPassenger( null );
			
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
				mount.getEntity().setPassenger( getMyEntity() );
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
	
	/** short convenience method to reduce repetition - calls setTarget( null, false )
	 * @return true - to allow calling from 'if' clauses (when && in second position with the first condition) */
	private boolean clearTargets() {
		setTarget( null, false);
		return true;
	}
}
