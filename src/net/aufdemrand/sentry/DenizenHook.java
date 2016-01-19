package net.aufdemrand.sentry;

import java.util.Set;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.aufdemrand.denizen.BukkitScriptEntryData;
import net.aufdemrand.denizen.Denizen;
import net.aufdemrand.denizen.npc.traits.TriggerTrait;
import net.aufdemrand.denizen.objects.dNPC;
import net.aufdemrand.denizen.objects.dPlayer;
import net.aufdemrand.denizen.scripts.containers.core.InteractScriptContainer;
import net.aufdemrand.denizen.scripts.triggers.AbstractTrigger;
import net.aufdemrand.denizen.utilities.debugging.dB;
import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.citizensnpcs.api.npc.NPC;

/**
 * Class containing:-
 * <ul>
 *  <li>static variables<ul>
 *  	<li>boolean denizenActive</li>
 *  	<li>Plugin denizenPlugin</li>
 *  	<li>Sentry sentryPlugin</li>
 *  </ul></li>
 *  <li>static methods<ul>
 *  	<li>void {@link #setupDenizenHook()}</li>
 *  	<li>boolean {@link #sentryDeath()}</li>
 *  	<li>void {@link #denizenAction()}</li>
 *  </ul></li>
 *  <li>private class definitions<ul>
 *  	<li>{@link LiveCommand} extends AbstractCommand</li>
 *  	<li>{@link DieCommand} extends AbstractCommand</li>
 *  	<li>{@link NpcdeathTriggerOwner} extends AbstractTrigger</li>
 *  	<li>{@link NpcdeathTrigger} extends AbstractTrigger</li>
 *  </ul></li>
 *  </ul>
 *  TODO add further details to above.
 */
public class DenizenHook {

	static boolean denizenActive = false;
	static Plugin denizenPlugin;
	static Sentry sentryPlugin;
	
	@SuppressWarnings({ "synthetic-access" })
	static void setupDenizenHook() {

		DenizenHook hook = new DenizenHook();

		hook.new NpcdeathTriggerOwner().activate().as( "Npcdeathowner" );
		hook.new NpcdeathTrigger().activate().as( "Npcdeath" );

		hook.new DieCommand().activate().as( "die" ).withOptions( "die", 0 );
		hook.new LiveCommand().activate().as( "live" ).withOptions( "live", 0 );

		denizenActive  = true;
	}

	public static boolean sentryDeath( Set<Player> _myDamamgers, NPC npc ) {
		
		if ( denizenActive ) {

			try {
				if ( npc == null ) return false;
				
				Denizen denizen = (Denizen) denizenPlugin;
	
				NpcdeathTrigger npcd = denizen.getTriggerRegistry()
											  .get( NpcdeathTrigger.class );
								
				NpcdeathTriggerOwner npcdo = denizen.getTriggerRegistry()
													.get( NpcdeathTriggerOwner.class );
	
				boolean a = npcd.die( _myDamamgers, npc );
				boolean c = npcdo.die( npc );
				
				return ( a || c );
				
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public static void denizenAction( NPC npc, String action, org.bukkit.OfflinePlayer player ) {
	    
		if ( denizenActive ) {
		    
			dNPC dnpc = dNPC.mirrorCitizensNPC( npc );
			
			if ( dnpc != null ) {
				try {
					dnpc.action( action, dPlayer.mirrorBukkitPlayer( player ) );
				} catch ( Exception e ) {
				    e.printStackTrace();
				}
			}
		}
	}

	private class LiveCommand extends AbstractCommand {

		@Override
		public void execute( ScriptEntry theEntry ) throws CommandExecutionException {
			
		    dNPC npc = ((BukkitScriptEntryData) theEntry.entryData).getNPC();
		    
			LivingEntity ent = (LivingEntity) npc.getEntity();

			SentryInstance inst = npc.getCitizen().getTrait( SentryTrait.class ).getInstance();

			if ( ent != null ) {
			    
				if ( npc.getCitizen().hasTrait( SentryTrait.class ) ) {
					
					boolean deaggro = false;

					for ( String arg : theEntry.getArguments() ) {
						if ( arg.equalsIgnoreCase( "peace" ) ) 
							deaggro = true;
					}
					String db = "RISE! " + npc.getName() + "!";
					
					if ( deaggro ) db += " ..And fight no more!";
					
					dB.log( db );

					if ( inst != null ) {
						inst.sentryStatus = net.aufdemrand.sentry.SentryStatus.isLOOKING;
						
						if ( deaggro ) inst.setTarget( null, false );
					}
				}
			} else {
				throw new CommandExecutionException( "Entity not found" );
			}
		}
		
		@Override public void parseArgs( ScriptEntry arg0 ) throws InvalidArgumentsException { }
	}

	private class DieCommand extends AbstractCommand {

		@Override
		public void execute( ScriptEntry theEntry ) throws CommandExecutionException {
			
			dNPC npc = ((BukkitScriptEntryData) theEntry.entryData).getNPC();
		    
			LivingEntity ent = (LivingEntity) npc.getEntity();

			SentryInstance inst = npc.getCitizen().getTrait( SentryTrait.class ).getInstance();

			if ( inst != null ) {
				dB.log( "Goodbye, cruel world... " );
				inst.die( false, org.bukkit.event.entity.EntityDamageEvent.DamageCause.CUSTOM );
			}
			else if ( ent != null ) {
				ent.remove();
			}
			else	{
				throw new CommandExecutionException( "Entity not found" );
			}
		}

		@Override public void parseArgs( ScriptEntry arg0 ) throws InvalidArgumentsException {}
	}

	/**
	 * Sub-class of AbstractTrigger from Denizen's API.
	 * <p>
	 * Contains one method definition:- boolean die( NPC npc )
	 */
	private class NpcdeathTriggerOwner extends AbstractTrigger {

		public boolean die( NPC npc ) {

			// Check if NPC has triggers and they are enabled.
			if ( !npc.hasTrait( TriggerTrait.class ) 
			  || !npc.getTrait( TriggerTrait.class ).isEnabled( name ) ) return false;

			dNPC theDenizen = dNPC.mirrorCitizensNPC( npc );

			String owner = npc.getTrait( net.citizensnpcs.api.trait.trait.Owner.class ).getOwner();

			dPlayer thePlayer = net.aufdemrand.denizen.objects.dPlayer.valueOf( owner );

			if ( thePlayer == null ) {
				return false;
			}

			InteractScriptContainer script = theDenizen.getInteractScriptQuietly( thePlayer, this.getClass() );

			return	parse( theDenizen, thePlayer, script );
		}
		
		@Override public void onEnable() {}
	}
	
	/**
	 * Sub-class of AbstractTrigger from Denizen's API.
	 * <p>
	 * Contains one method definition:- boolean die( Set<Player> _myDamagers, NPC npc )
	 */
	private class NpcdeathTrigger extends AbstractTrigger {

		public boolean die( Set<Player> _myDamamgers, NPC npc ) {

			// Check if NPC has triggers and they are enabled, returning if not.
			if ( !npc.hasTrait( TriggerTrait.class ) // first check the trait exists to avoid NPE on next line.
			  || !npc.getTrait( TriggerTrait.class ).isEnabled( name ) ) 
					return false;
 
			dNPC theDenizen = dNPC.mirrorCitizensNPC( npc );
			boolean foundOne = false;

			for ( Player thePlayer : _myDamamgers ) {

				// jump to next iteration if thePlayer is too far away.
				if ( thePlayer != null 
				  && thePlayer.getLocation().distance( npc.getEntity().getLocation() ) > 300 ) 
						continue;
				
				InteractScriptContainer script = theDenizen.getInteractScriptQuietly( 
				                                dPlayer.mirrorBukkitPlayer( thePlayer ), this.getClass() );

				if ( parse( theDenizen, dPlayer.mirrorBukkitPlayer( thePlayer ), script ) ) 
						foundOne = true;
			}
			return foundOne;
		}

		@Override public void onEnable() {}
	}
}
