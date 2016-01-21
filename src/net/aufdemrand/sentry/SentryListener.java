package net.aufdemrand.sentry;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;


public class SentryListener implements Listener {

	public Sentry plugin;

	public SentryListener (Sentry sentry) {
		plugin = sentry;
	}

	@EventHandler
	public void kill (EntityDeathEvent event) {
		
		LivingEntity deceased = event.getEntity();
		if ( deceased == null ) return;

		//don't mess with player death.
		if ( deceased instanceof Player 
		  && !deceased.hasMetadata( "NPC" ) ) return;

		Entity killer = deceased.getKiller();
		if ( killer == null ){
			// death might have been caused by a projectile, or environmental harm.
			
			// let start by getting the specific damage event.
			EntityDamageEvent ev = deceased.getLastDamageCause();
			
			// test whether death caused by another entity.
			if ( ev != null && ev instanceof EntityDamageByEntityEvent) {
				
				// re-allocate killer to reference new entity
				killer = ((EntityDamageByEntityEvent) ev).getDamager();
				
				// check if new entity is a projectile, and was shot by a third entity.
				if ( killer instanceof Projectile 
				  && ((Projectile) killer).getShooter() instanceof Entity )
					
				  	// make killer reference the shooter
                    	killer = (Entity) ((Projectile) killer).getShooter();
                    			
                    		// TODO consider what should happen if last 'if' returns false
                    		// (e.g. it is possible killer references a projectile shot by a dispenser.)
			}
		}
		SentryInstance sentry = plugin.getSentry( killer );

		if ( sentry != null && !sentry.killsDropInventory ) {
			event.getDrops().clear();
			event.setDroppedExp( 0 );
		}
	}

	@EventHandler( ignoreCancelled = true )
	public void despawn( NPCDespawnEvent event ) {
		// don't despawn active bodyguards on chunk unload
		
		SentryInstance sentry = plugin.getSentry( event.getNPC() );
		
		if ( sentry != null 
		  && event.getReason() == DespawnReason.CHUNK_UNLOAD 
		  && sentry.guardEntity != null ) {
					event.setCancelled( true );
		}
	}

	@EventHandler( priority = EventPriority.HIGHEST )
	public void entteleportevent( EntityTeleportEvent event ) {
		
		SentryInstance sentry = plugin.getSentry( event.getEntity() );
		
		if ( sentry != null 
		  && sentry.epCount != 0 
		  && sentry.isWarlock1() ) {
					event.setCancelled( true );
		}
	}

	@EventHandler( priority = EventPriority.HIGHEST )
	public void entteleportevent( PlayerTeleportEvent event ) {
		
		SentryInstance sentry = plugin.getSentry( event.getPlayer() );
		
		if ( sentry != null 
		  && sentry.epCount != 0 
		  && sentry.isWarlock1() 
		  && event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL ) {
					event.setCancelled( true );
		}
	}

	@EventHandler( priority = EventPriority.MONITOR )
	public void projectilehit( ProjectileHitEvent event ) {
		
		Projectile projectile = event.getEntity();
		
		if ( projectile instanceof EnderPearl 
		  && projectile.getShooter() instanceof Entity ) {
		  	
			SentryInstance sentry = plugin.getSentry( (Entity) projectile.getShooter() );
			
			if ( sentry != null ) {
				
				sentry.epCount--;
				if ( sentry.epCount < 0 ) sentry.epCount = 0;
				
				projectile.getLocation()
					   .getWorld()
					   .playEffect( event.getEntity().getLocation()
				     		      , Effect.ENDER_SIGNAL, 1, 100 );
				//ender pearl from a sentry
			}
			return;
		}
		
		if ( projectile instanceof SmallFireball 
		  && projectile.getShooter() instanceof Entity ) {
			
			SentryInstance sentry = plugin.getSentry( (Entity) projectile.getShooter() );

			if ( sentry != null && sentry.isPyromancer1() ) {

				final Block block = projectile.getLocation().getBlock();
				
				final Runnable blockDamage = new Runnable() {
					@Override public void run(){
						
						for ( BlockFace face : BlockFace.values() ) {
							if ( block.getRelative( face ).getType() == Material.FIRE ) 
								block.getRelative( face ).setType( Material.AIR );
						}
						if ( block.getType() == Material.FIRE ) 
							block.setType( Material.AIR ); 
					}
				};
				plugin.getServer().getScheduler().scheduleSyncDelayedTask( plugin, blockDamage );
			}
		}
	}

//	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
//	public void tarsdfget(EntityTargetEvent event) {
//		SentryInstance inst = plugin.getSentry(event.getTarget());
//		if(inst!=null){
//			event.setCancelled(false); //inst.myNPC.data().get(NPC.DEFAULT_PROTECTED_METADATA, false));
//		}
//	}

	@EventHandler( priority = EventPriority.HIGHEST )
	public void EnvDamage( EntityDamageEvent event ) {

		if ( event instanceof EntityDamageByEntityEvent ) return;

		SentryInstance inst = plugin.getSentry( event.getEntity() );
		
		if ( inst != null ) {

			event.setCancelled( true );
	
			switch ( event.getCause() ) {
			
			case CONTACT: 	case DROWNING: 	case LAVA: 	case VOID:	case SUICIDE:
			case CUSTOM:  	case BLOCK_EXPLOSION: 	 case SUFFOCATION: 	case MAGIC:
				inst.onEnvironmentDamage( event );
				break;
			case LIGHTNING:
				if ( !inst.isStormcaller() ) 
					inst.onEnvironmentDamage( event );
				break;
			case FIRE: 	case FIRE_TICK:
				if ( !inst.isPyromancer() && !inst.isStormcaller() ) 
					inst.onEnvironmentDamage( event );
				break;
			case POISON:
				if ( !inst.isWitchDoctor() ) 
					inst.onEnvironmentDamage( event );
				break;
			case FALL:
				break;
			default:
				break;
			}
		}
	}

	@EventHandler( priority = EventPriority.HIGHEST ) 
	public void onDamage( EntityDamageByEntityEvent event ) {

		Entity damager = event.getDamager();
		// a duplicate reference, as the original may get changed to refer to a projectile shooter.
		Entity _damager = damager;
		Entity victim = event.getEntity();

		// following 'if' statements change damager to refer to the shooter of a projectile.
		// TODO figure out why there is an (apparently redundant) 'instanceof Entity' checks here.
		if ( damager instanceof Projectile 
		  && damager instanceof Entity ) {
			
			ProjectileSource source = ((Projectile) damager).getShooter();
			
			if ( source instanceof Entity )
				damager = (Entity) ((Projectile) damager).getShooter();
		}

		SentryInstance instDamager = plugin.getSentry( damager );
		SentryInstance instVictim = plugin.getSentry( victim );

		// plugin.debug( "start: from : " + damager + " to " + victim + " cancelled " + event.isCancelled()
		//			+ " damage " + event.getDamage() + " cause " + event.getCause() );

		if ( instDamager != null ) {

			//projectiles go through ignore targets.
			if ( _damager instanceof Projectile 
			  && victim instanceof LivingEntity 
			  && instDamager.isIgnored( (LivingEntity) victim ) ) {
				
				event.setCancelled( true );
				_damager.remove();
				
				Projectile newProjectile = 
					(Projectile) damager.getWorld()
							    		.spawnEntity( _damager.getLocation()
									          		  		  .add( _damager.getVelocity() )
									          		, _damager.getType() );
									 
				newProjectile.setVelocity( _damager.getVelocity() );
				newProjectile.setShooter( (LivingEntity) damager );
				newProjectile.setTicksLived( _damager.getTicksLived() );
				return;
			}

			// set damage dealt by a sentry
			event.setDamage( instDamager.getStrength() );

			// uncancel if not bodyguard.
			if ( instDamager.guardTarget == null 
			  || plugin.bodyguardsObeyProtection == false ) 
			  	event.setCancelled( false );

			// cancel if invulnerable non-sentry npc
			if ( instVictim == null ) {
				NPC npc = CitizensAPI.getNPCRegistry().getNPC( victim );
				if ( npc != null ) {
					event.setCancelled( npc.data()
							     		   .get( NPC.DEFAULT_PROTECTED_METADATA, true ) );
				}
			}
			// don't hurt guard target.
			if ( victim == instDamager.guardEntity ) event.setCancelled( true );

			// stop hitting yourself.
			if ( damager == victim ) event.setCancelled( true );

			// apply potion effects
			if ( instDamager.weaponSpecialEffects != null 
			  && event.isCancelled() == false ) {
				( (LivingEntity) victim ).addPotionEffects( instDamager.weaponSpecialEffects );
			}
			
			// warlock 1 should do no direct damage, except to other sentries which take no fall damage.
			if ( instDamager.isWarlock1() ) {
				if ( event.isCancelled() == false ) {
					if ( instVictim == null ) event.setCancelled( true ); 

					double h = instDamager.getStrength() + 3;
					double v = 7.7 * Math.sqrt( h ) + 0.2;
					if ( h <= 3 ) v -= 2;
					if ( v > 150 ) v = 150;

					victim.setVelocity( new Vector( 0, v / 20, 0 ) );
				}
			}
		}
		
		/* a boolean that is used in addition to the Event.cancelled status to decide whether
		 * to enter the final section of this method.  */
		boolean ok = false;

		if ( instVictim  != null ) {
			
			// stop hitting yourself.
			if ( damager == victim ) return;

			// innate protections
			if ( event.getCause() == DamageCause.LIGHTNING 
			  && instVictim.isStormcaller() ) 
					return;
			  
			if ( ( event.getCause() == DamageCause.FIRE 
			    || event.getCause() == DamageCause.FIRE_TICK ) 
			  && ( instVictim.isPyromancer() 
			    || instVictim.isStormcaller() ) ) 
					return;

			// only bodyguards obey pvp-protection
			if ( instVictim.guardTarget == null ) 
					event.setCancelled( false );

			// dont take damamge from guard entity.
			if ( damager == instVictim.guardEntity ) 
					event.setCancelled( true );

			if ( damager != null ) {
				NPC npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC( damager );
				
				if ( npc != null 
				  && npc.hasTrait( SentryTrait.class ) 
				  && instVictim.guardEntity != null 
				  && npc.getTrait( SentryTrait.class )
				  		.getInstance()
				  		.guardEntity == instVictim.guardEntity ) { 
					
			  		//don't take damage from co-guards.
					event.setCancelled( true );
				}
			}

			// process event
			if ( !event.isCancelled() ){
				ok = true;
				instVictim.onDamage( event );
			}

			//Damage to a sentry cannot be handled by the server. Always cancel the event here.
			event.setCancelled( true );
		}

		//process this event on each sentry to check for events that need a response.
		if ( ( event.isCancelled() == false || ok ) 
		  && damager != victim 
		  && event.getDamage() > 0 ){
		  	
			for ( NPC npc : CitizensAPI.getNPCRegistry() ) {
				SentryInstance inst = plugin.getSentry( npc );

				if ( inst == null 
				  || !npc.isSpawned() 
				  || npc.getEntity().getWorld() != victim.getWorld() ) {
				  	continue;   //not a sentry, or not this world, or dead.
				}
				
				if ( inst.guardEntity == victim 
				  && inst.iWillRetaliate 
				  && damager instanceof LivingEntity ) {
				  	inst.setTarget( (LivingEntity) damager, true );
				}
				
				// are u attacking mai horse?
				if ( inst.getMountNPC() != null 
				  && inst.getMountNPC().getEntity() == victim ) {
				  	
					if ( damager == inst.guardEntity ) 
						event.setCancelled( true );
					else if ( inst.iWillRetaliate 
							&& damager instanceof LivingEntity )  
						inst.setTarget( (LivingEntity) damager, true );
				}

				if ( ( inst.hasTargetType( 16 )  
				    && inst.myStatus == SentryStatus.isLOOKING 
				    && damager instanceof Player 
				    && CitizensAPI.getNPCRegistry().isNPC( damager ) == false ) 
				// is the event within range of the sentry?
				  && ( npc.getEntity().getLocation()
						  			  .distance( victim.getLocation() ) <= inst.sentryRange 
				    || npc.getEntity().getLocation()
					  				  .distance( damager.getLocation() ) <= inst.sentryRange ) 
				// is it too dark for the sentry to see?
				  && ( inst.nightVision >= damager.getLocation()
								  				  .getBlock()
								  				  .getLightLevel() 
				    || inst.nightVision  >= victim.getLocation()
				    				  			  .getBlock()
				    				  			  .getLightLevel() ) 
				// does the sentry have line-of-sight?
				  && ( inst.hasLOS( damager ) 
				    || inst.hasLOS( victim ) )
				// does the event correspond to configured triggers?
				  && ( ( !(victim instanceof Player) 
				      && inst.containsTarget( "event:pve" ) )
				    || ( victim instanceof Player 
				      && CitizensAPI.getNPCRegistry().isNPC( victim ) == false 
				      && inst.containsTarget( "event:pvp" ) ) 
				    || ( CitizensAPI.getNPCRegistry().isNPC( victim ) == true 
				      && inst.containsTarget( "event:pvnpc" ) ) 
				    || ( instVictim != null 
				      && inst.containsTarget( "event:pvsentry" ) ) )
				// is the damager on the sentry's ignore list?
				  && !inst.isIgnored( (LivingEntity) damager ) ) {
				  	// phew! we made it! the event is a valid trigger. Attack the aggressor!
				  	
				  		inst.setTarget( (LivingEntity) damager, true ); 
				  }
			}
		}
		return;
	}

	@EventHandler( ignoreCancelled = true )
	public void onEntityDeath( NPCDeathEvent event ) {
		
		final NPC hnpc = event.getNPC();
		
		//if the mount dies carry aggression over.
		for ( NPC each : CitizensAPI.getNPCRegistry() ) {
			
			final SentryInstance inst = plugin.getSentry( each );
			if ( inst == null 
			  || !each.isSpawned() 
			  || !inst.isMounted() ) 
			  	continue; //not a sentry, not spawned, or not mounted
			  	
			if ( hnpc.getId() == inst.mountID ) {
				///nooooo butterstuff!

				Entity killer = ( (LivingEntity) hnpc.getEntity() ).getKiller();
				if ( killer == null ) {
					//might have been a projectile.
					EntityDamageEvent ev = hnpc.getEntity().getLastDamageCause();
					
					if ( ev != null 
					  && ev instanceof EntityDamageByEntityEvent ) {
					  	
						killer = ((EntityDamageByEntityEvent) ev).getDamager();
						
						if ( killer instanceof Projectile 
						  && ((Projectile) killer).getShooter() instanceof Entity )
						  	killer = (Entity) ((Projectile) killer).getShooter();
					}
				}

				final LivingEntity perp = ( killer instanceof LivingEntity ) ? (LivingEntity) killer 
											 							     : null;

				if ( Sentry.denizenActive ) {
					DenizenHook.denizenAction( each
								 			 , "mount death"
								 			 , ( perp instanceof Player ) ? (Player) perp 
								 					 					  : null );
				}

				if ( perp == null || inst.isIgnored( perp ) ) return;
				
				// prepare a task to send to the scheduler.
				final Runnable getThePerp = new Runnable() {
					@Override
					public void run(){
						inst.setTarget( perp, true );
					}
				};
				//delay so the mount is gone.
				plugin.getServer().getScheduler().scheduleSyncDelayedTask( plugin, getThePerp, 2 );
				break;
			}
		}
	}


	@EventHandler
	public void onNPCRightClick( NPCRightClickEvent event ) {
		
		// get a sentry instance if one is attached to the npc.
		SentryInstance inst = plugin.getSentry( event.getNPC() );
		
		// stop here if not.
		if ( inst == null ) return;

		if ( inst.myNPC.getEntity() instanceof Horse 
		  && inst.guardEntity != event.getClicker() ) {
				
				event.setCancelled( true );
		}
	}
}

