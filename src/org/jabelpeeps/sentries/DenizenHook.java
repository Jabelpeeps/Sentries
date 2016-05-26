
package org.jabelpeeps.sentries;

import java.util.Set;
import java.util.logging.Level;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

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
import net.citizensnpcs.api.trait.trait.Owner;

public class DenizenHook {

    static Denizen denizenPlugin;
    static Sentries sentryPlugin;
    static boolean sentryHealthByDenizen = false;
    static boolean npcDeathTriggerActive = false;
    static boolean npcDeathTriggerOwnerActive = false;
    static boolean dieCommandActive = false;
    static boolean liveCommandActive = false;

    DenizenHook() {}

    DenizenHook( Denizen denizen, Sentries sentry ) {
        denizenPlugin = denizen;
        sentryPlugin = sentry;

        setupHooks( this );
    }

    static void setupHooks() {

        DenizenHook hook = new DenizenHook();
        setupHooks( hook );
    }

    static void setupHooks( DenizenHook hook ) {

        ConfigurationSection config = sentryPlugin.getConfig().getConfigurationSection( "DenizenIntegration" );
        
        if ( config.getBoolean( "SentryHealthByDenizen", true ) ) {
            sentryHealthByDenizen = true;
            Sentries.logger.log( Level.INFO, "SentryHealthByDenizen configured to be enabled." );
        }
        if ( config.getBoolean( "NpcDeathTrigger", true ) ) {
            hook.new NpcdeathTrigger().activate().as( "Npcdeath" );
            Sentries.logger.log( Level.INFO, "NPCDeathTrigger registered sucessfully with Denizen" );
            npcDeathTriggerActive = true;
        }
        if ( config.getBoolean( "NpcDeathTriggerOwner", true ) ) {
            hook.new NpcdeathTriggerOwner().activate().as( "Npcdeathowner" );
            Sentries.logger.log( Level.INFO, "NPCDeathTriggerOwner registered sucessfully with Denizen" );
            npcDeathTriggerOwnerActive = true;
        }
        if ( config.getBoolean( "DieCommand", true ) ) {
            hook.new DieCommand().activate().as( "die" ).withOptions( "die", 0 );
            Sentries.logger.log( Level.INFO, "DIE command registered sucessfully with Denizen" );
            dieCommandActive = true;
        }
        if ( config.getBoolean( "LiveCommand", true ) ) {
            hook.new LiveCommand().activate().as( "live" ).withOptions( "live", 0 );
            Sentries.logger.log( Level.INFO, "LIVE command registered sucessfully with Denizen" );
            liveCommandActive = true;
        }
    }

    static boolean sentryDeath( Set<Player> _myDamamgers, NPC npc ) {

        if ( npc == null )
            return false;

        boolean triggerA = false;
        boolean triggerB = false;

        if ( npcDeathTriggerActive ) {
            triggerA = denizenPlugin.getTriggerRegistry().get( NpcdeathTrigger.class ).die( _myDamamgers, npc );
        }
        if ( npcDeathTriggerOwnerActive ) {
            triggerB = denizenPlugin.getTriggerRegistry().get( NpcdeathTriggerOwner.class ).die( npc );
        }
        return (triggerA || triggerB);
    }

    public static void denizenAction( NPC npc, String action, OfflinePlayer player ) {

        dNPC dnpc = dNPC.mirrorCitizensNPC( npc );

        if ( dnpc != null ) {
            dnpc.action( action, dPlayer.mirrorBukkitPlayer( player ) );
        }
    }

    private class LiveCommand extends AbstractCommand {

        public LiveCommand() {}

        @Override
        public void execute( ScriptEntry theEntry ) throws CommandExecutionException {

            dNPC npc = ((BukkitScriptEntryData) theEntry.entryData).getNPC();

            LivingEntity ent = (LivingEntity) npc.getEntity();

            if ( ent != null ) {

                if ( npc.getCitizen().hasTrait( SentryTrait.class ) ) {

                    SentryTrait inst = npc.getCitizen() .getTrait( SentryTrait.class );

                    boolean deaggro = false;

                    for ( String arg : theEntry.getArguments() ) {
                        if ( arg.equalsIgnoreCase( "peace" ) )
                            deaggro = true;
                    }
                    String db = "RISE! " + npc.getName() + "!";

                    if ( deaggro )
                        db += " ..And fight no more!";

                    dB.log( db );

                    if ( inst != null ) {
                        inst.myStatus = SentryStatus.isFOLLOWING;

                        if ( deaggro )
                            inst.clearTarget();
                    }
                }
            }
            else {
                throw new CommandExecutionException( "Entity not found" );
            }
        }

        @Override
        public void parseArgs( ScriptEntry arg0 ) throws InvalidArgumentsException {}
    }

    private class DieCommand extends AbstractCommand {

        public DieCommand() {}

        @Override
        public void execute( ScriptEntry theEntry ) throws CommandExecutionException {

            dNPC npc = ((BukkitScriptEntryData) theEntry.entryData).getNPC();

            LivingEntity ent = (LivingEntity) npc.getEntity();

            SentryTrait inst = npc.getCitizen().getTrait( SentryTrait.class );

            if ( inst != null ) {
                dB.log( "Goodbye, cruel world... " );
                inst.die( false, EntityDamageEvent.DamageCause.CUSTOM );
            }
            else if ( ent != null ) {
                ent.remove();
            }
            else {
                throw new CommandExecutionException( "Entity not found" );
            }
        }

        @Override
        public void parseArgs( ScriptEntry arg0 ) throws InvalidArgumentsException {}
    }

    /**
     * Sub-class of AbstractTrigger from Denizen's API.
     * <p>
     * Contains one method definition:- boolean die( NPC npc )
     */
    private class NpcdeathTriggerOwner extends AbstractTrigger {

        public NpcdeathTriggerOwner() {}

        public boolean die( NPC npc ) {

            // Check if NPC has triggers and they are enabled.
            if (    !npc.hasTrait( TriggerTrait.class )
                    || !npc.getTrait( TriggerTrait.class ).isEnabled( name ) )
                return false;

            dPlayer thePlayer = dPlayer.valueOf( npc.getTrait( Owner.class ).getOwner() );

            if ( thePlayer == null ) return false;

            dNPC theDenizen = dNPC.mirrorCitizensNPC( npc );

            InteractScriptContainer script = theDenizen.getInteractScriptQuietly( thePlayer, this.getClass() );

            return parse( theDenizen, thePlayer, script );
        }

        @Override
        public void onEnable() {}
    }

    /**
     * Sub-class of AbstractTrigger from Denizen's API.
     * <p>
     * Contains one method definition:- boolean die( Set<Player> _myDamagers,
     * NPC npc )
     */
    private class NpcdeathTrigger extends AbstractTrigger {

        public NpcdeathTrigger() {}

        public boolean die( Set<Player> _myDamamgers, NPC npc ) {

            // Check if NPC has triggers and they are enabled, returning if not.
            if ( !npc.hasTrait( TriggerTrait.class ) // first check the trait
                                                     // exists to avoid NPE on
                                                     // next line.
                    || !npc.getTrait( TriggerTrait.class ).isEnabled( name ) )
                return false;

            dNPC theDenizen = dNPC.mirrorCitizensNPC( npc );
            boolean foundOne = false;

            for ( Player thePlayer : _myDamamgers ) {

                // jump to next iteration if thePlayer is too far away.
                if (    thePlayer != null 
                        && thePlayer.getLocation().distance( npc.getEntity().getLocation() ) > 300 )
                    continue;

                InteractScriptContainer script = theDenizen.getInteractScriptQuietly(
                                dPlayer.mirrorBukkitPlayer( thePlayer ),
                                this.getClass() );

                if ( parse( theDenizen, dPlayer.mirrorBukkitPlayer( thePlayer ), script ) )
                    foundOne = true;
            }
            return foundOne;
        }

        @Override
        public void onEnable() {}
    }
}
