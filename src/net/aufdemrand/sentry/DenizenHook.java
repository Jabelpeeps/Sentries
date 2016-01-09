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

public class DenizenHook {

	static boolean DenizenActive = false;
	static Plugin DenizenPlugin;
	static Sentry SentryPlugin;

	public static boolean SentryDeath( Set<Player> _myDamamgers, NPC npc ){
		
		if ( !DenizenActive ) return false;

		try {
			if ( npc == null ) return false;
			
			net.aufdemrand.denizen.Denizen denizen = (Denizen) DenizenPlugin;

			NpcdeathTrigger npcd = denizen	.getTriggerRegistry()
							.get( NpcdeathTrigger.class );
							
			NpcdeathTriggerOwner npcdo = denizen	.getTriggerRegistry()
								.get( NpcdeathTriggerOwner.class );

			boolean a = npcd.Die( _myDamamgers, npc );
			boolean c = npcdo.Die( npc );
			
			return ( a || c );
			
		} catch ( Exception e ) {
			e.printStackTrace();
			return false;
		}
	}

	static void setupDenizenHook()  {

		DenizenHook hook = new DenizenHook();

		hook.new NpcdeathTriggerOwner().activate().as( "Npcdeathowner" );
		hook.new NpcdeathTrigger().activate().as( "Npcdeath" );

		DieCommand dc = hook.new DieCommand();
		LiveCommand lc = hook.new LiveCommand();

		dc.activate().as( "die" ).withOptions( "die", 0 );
		lc.activate().as( "live" ).withOptions( "live", 0 );

		DenizenActive  =true;
	}

	public static void DenizenAction( NPC npc, String action, org.bukkit.OfflinePlayer player ){
	    
		if ( DenizenActive ){
		    
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
		    
			LivingEntity ent = (LivingEntity) ( (BukkitScriptEntryData) theEntry.entryData ).getNPC()
			                                                                                .getEntity();

			SentryInstance inst = ( (BukkitScriptEntryData) theEntry.entryData ).getNPC()
			                                                                    .getCitizen()
			                                                                    .getTrait( SentryTrait.class )
			                                                                    .getInstance();

			if ( ent != null ){
			    
				if ( ( (BukkitScriptEntryData) theEntry.entryData ) .getNPC()
				                                                    .getCitizen()
				                                                    .hasTrait( SentryTrait.class ) ){
					
					boolean deaggro = false;

					for ( String arg : theEntry.getArguments() ){
						if ( arg.equalsIgnoreCase( "peace" ) ) deaggro = true;
					}

					String db = "RISE! " + ( (BukkitScriptEntryData) theEntry.entryData ).getNPC().getName() + "!";
					
					if ( deaggro ) db += " ..And fight no more!";
					
					dB.log( db );

					if ( inst != null ){
						inst.sentryStatus = net.aufdemrand.sentry.SentryInstance.Status.isLOOKING;
						
						if ( deaggro ) inst.setTarget( null, false );
					}
				}
			} else {
				throw new CommandExecutionException( "Entity not found" );
			}
		}
		
		@Override
		public void parseArgs( ScriptEntry arg0 ) throws InvalidArgumentsException {
            // TODO Auto-generated method stub
		}
	}

	private class DieCommand extends AbstractCommand {

		@Override
		public void execute( ScriptEntry theEntry ) throws CommandExecutionException {
		    
			LivingEntity ent = (LivingEntity) ( (BukkitScriptEntryData) theEntry.entryData ).getNPC()
			                                                                                .getEntity();

			SentryInstance inst = ( (BukkitScriptEntryData) theEntry.entryData ).getNPC()
					                                                            .getCitizen()
					                                                            .getTrait( SentryTrait.class )
					                                                            .getInstance();

			if ( inst != null ){
				dB.log( "Goodbye, cruel world... " );
				inst.die( false, org.bukkit.event.entity.EntityDamageEvent.DamageCause.CUSTOM );
			}
			else if ( ent != null ){
				ent.remove();
			}
			else	{
				throw new CommandExecutionException( "Entity not found" );
			}
		}

		@Override
		public void parseArgs( ScriptEntry arg0 ) throws InvalidArgumentsException {
			// TODO Auto-generated method stub
		}
	}

	private class NpcdeathTriggerOwner extends AbstractTrigger{

		@Override
		public void onEnable() {
			// TODO Auto-generated method stub
		}

		public boolean Die( NPC npc ) {

			// Check if NPC has triggers and they are enabled.
			if ( !npc.hasTrait( TriggerTrait.class ) 
			  || !npc.getTrait( TriggerTrait.class ).isEnabled( name ) ) return false;

			dNPC theDenizen = dNPC.mirrorCitizensNPC( npc );

	//		dB.echoDebug( DebugElement.Header, "Parsing NPCDeath/Owner Trigger." );

			String owner = npc.getTrait( net.citizensnpcs.api.trait.trait.Owner.class ).getOwner();

			dPlayer thePlayer = net.aufdemrand.denizen.objects.dPlayer.valueOf( owner );

			if ( thePlayer == null ) {
	//			dB.echoDebug( DebugElement.Header, "Owner not found!" );
				return false;
			}

			InteractScriptContainer script = theDenizen.getInteractScriptQuietly( thePlayer, this.getClass() );

			return	parse( theDenizen, thePlayer, script );
		}
	}

	private class NpcdeathTrigger extends net.aufdemrand.denizen.scripts.triggers.AbstractTrigger{

		@Override
		public void onEnable() {
			// TODO Auto-generated method stub
		}

		public  boolean Die( Set<Player> _myDamamgers, NPC npc ) {

			// Check if NPC has triggers and they are enabled.
			if ( !npc.hasTrait( TriggerTrait.class ) 
			  || !npc.getTrait( TriggerTrait.class ).isEnabled( name ) ) return false;

			dNPC theDenizen = dNPC.mirrorCitizensNPC( npc );

	//		dB.echoDebug( DebugElement.Header, "Parsing NPCDeath/Killers Trigger" );

			boolean founone = false;

			for ( Player thePlayer : _myDamamgers ){

				if ( thePlayer != null 
				  && thePlayer.getLocation().distance( npc.getEntity().getLocation() ) > 300) {
				      
	//				dB.echoDebug( DebugElement.Header, thePlayer.getName()+ " is to far away." );
					continue;
				}

				InteractScriptContainer script = theDenizen.getInteractScriptQuietly( 
				                                dPlayer.mirrorBukkitPlayer( thePlayer ), this.getClass() );

				if ( parse( theDenizen, dPlayer.mirrorBukkitPlayer( thePlayer ), script) ) founone = true;
			}
			return founone;
		}
	}
}
