package org.jabelpeeps.sentries.commands;

import java.text.DecimalFormat;
import java.util.StringJoiner;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;


public class InfoCommand implements SentriesComplexCommand {

    private String helpTxt;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {

        StringJoiner joiner = new StringJoiner( System.lineSeparator() );

        joiner.add( String.join( "", Col.GOLD, "------- Sentries Info for ", npcName, " (npcid - ",
                                    String.valueOf( inst.getNPC().getId() ), ") ", "------" ) );
        
        joiner.add( String.join( "", 
                Col.RED, "[Health]:", Col.WHITE, String.valueOf( inst.getHealth() ), "/", String.valueOf( inst.maxHealth ),
                Col.RED, " [Armour]:", Col.WHITE, String.valueOf( inst.getArmor() ),
                Col.RED, " [Strength]:", Col.WHITE, String.valueOf( inst.getStrength() ),
                Col.RED, " [Speed]:", Col.WHITE, new DecimalFormat( "#.0" ).format( inst.getSpeed() ),
                Col.RED, " [Range]:", Col.WHITE, String.valueOf( inst.range ),
                Col.RED, " [ArrowRate]:", Col.WHITE, String.valueOf( inst.arrowRate ),
                Col.RED, " [NightVision]:", Col.WHITE, String.valueOf( inst.nightVision ),
                Col.RED, " [HealRate]:", Col.WHITE, String.valueOf( inst.healRate ),
                Col.RED, " [VoiceRange]:", Col.WHITE, String.valueOf( inst.voiceRange ),
                Col.RED, " [FollowDistance]:", Col.WHITE, String.valueOf( Math.sqrt( inst.followDistance ) ) ) );

        joiner.add( String.join( "", S.Col.GREEN, "Invincible: ", String.valueOf( inst.invincible ), 
                                                "  Retaliate: ", String.valueOf( inst.iRetaliate ) ) );
        joiner.add( String.join( "", S.Col.GREEN, "Drops Items: ", String.valueOf( inst.dropInventory ), 
                                                "  Critical Hits: ", String.valueOf( inst.acceptsCriticals ) ) );
        joiner.add( String.join( "", S.Col.GREEN, "Kills Drop Items: ", String.valueOf( inst.killsDrop ), 
                                                "  Respawn Delay: ", String.valueOf( inst.respawnDelay ), "secs" ) );
        joiner.add( String.join( "", S.Col.BLUE, "Status: ", inst.myStatus.toString() ) );

        if ( inst.attackTarget == null )
            joiner.add( Col.BLUE.concat( "Current Target: None" ) );
        else
            joiner.add( String.join( "", Col.BLUE, "Current Target: ", inst.attackTarget.getName() ) );

        if ( inst.guardeeEntity == null )
            joiner.add( Col.BLUE.concat( "Guarding: My Surroundings" ) );
        else
            joiner.add( String.join( "", Col.BLUE, "Guarding: ", inst.guardeeEntity.getName() ) );

        sender.sendMessage( joiner.toString() );
    }

    @Override
    public String getShortHelp() { return "view the attributes of a sentry"; }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = "Displays a summary of all the configurable settings for a sentry.";
        }
        return helpTxt;
    }

    @Override
    public String getPerm() { return S.PERM_INFO; }
}
