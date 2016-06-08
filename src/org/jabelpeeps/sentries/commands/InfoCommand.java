package org.jabelpeeps.sentries.commands;

import java.text.DecimalFormat;
import java.util.StringJoiner;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.SentryTrait;


public class InfoCommand implements SentriesComplexCommand {

    private String helpTxt;
    
    @Override
    public boolean call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {

        StringJoiner joiner = new StringJoiner( System.lineSeparator() );

        joiner.add( String.join( "", S.Col.GOLD, "------- Sentries Info for ", npcName, " (npcid - ",
                                    String.valueOf( inst.getNPC().getId() ), ") ", "------" ) );
        
        joiner.add( String.join( "", 
                S.Col.RED, "[Health]:", S.Col.WHITE, String.valueOf( inst.getHealth() ), "/", String.valueOf( inst.maxHealth ),
                S.Col.RED, " [Armour]:", S.Col.WHITE, String.valueOf( inst.getArmor() ),
                S.Col.RED, " [Strength]:", S.Col.WHITE, String.valueOf( inst.getStrength() ),
                S.Col.RED, " [Speed]:", S.Col.WHITE, new DecimalFormat( "#.0" ).format( inst.getSpeed() ),
                S.Col.RED, " [Range]:", S.Col.WHITE, String.valueOf( inst.range ),
                S.Col.RED, " [ArrowRate]:", S.Col.WHITE, String.valueOf( inst.arrowRate ),
                S.Col.RED, " [NightVision]:", S.Col.WHITE, String.valueOf( inst.nightVision ),
                S.Col.RED, " [HealRate]:", S.Col.WHITE, String.valueOf( inst.healRate ),
                S.Col.RED, " [VoiceRange]:", S.Col.WHITE, String.valueOf( inst.voiceRange ),
                S.Col.RED, " [FollowDistance]:", S.Col.WHITE, String.valueOf( Math.sqrt( inst.followDistance ) ) ) );

        joiner.add( String.join( "", S.Col.GREEN, "Invincible: ", String.valueOf( inst.invincible ), 
                                                "  Retaliate: ", String.valueOf( inst.iRetaliate ) ) );
        joiner.add( String.join( "", S.Col.GREEN, "Drops Items: ", String.valueOf( inst.dropInventory ), 
                                                "  Critical Hits: ", String.valueOf( inst.acceptsCriticals ) ) );
        joiner.add( String.join( "", S.Col.GREEN, "Kills Drop Items: ", String.valueOf( inst.killsDrop ), 
                                                "  Respawn Delay: ", String.valueOf( inst.respawnDelay ), "secs" ) );
        joiner.add( String.join( "", S.Col.BLUE, "Status: ", inst.myStatus.toString() ) );

        if ( inst.attackTarget == null )
            joiner.add( S.Col.BLUE.concat( "Current Target: None" ) );
        else
            joiner.add( String.join( "", S.Col.BLUE, "Current Target: ", inst.attackTarget.getName() ) );

        if ( inst.guardeeEntity == null )
            joiner.add( S.Col.BLUE.concat( "Guarding: My Surroundings" ) );
        else
            joiner.add( String.join( "", S.Col.BLUE, "Guarding: ", inst.guardeeEntity.getName() ) );

        sender.sendMessage( joiner.toString() );
    
        return true;
    }

    @Override
    public String getShortHelp() {
        return "view the attributes of a sentry";
    }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = "Displays a summary of all the configurable settings for a sentry.";
        }
        return helpTxt;
    }

    @Override
    public String getPerm() {
        return S.PERM_INFO;
    }
}
