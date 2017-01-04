package org.jabelpeeps.sentries.commands;

import java.text.DecimalFormat;
import java.util.StringJoiner;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;


public class InfoCommand implements SentriesComplexCommand {

    private static DecimalFormat df = new DecimalFormat( "0.0" );
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {

        StringJoiner joiner = new StringJoiner( System.lineSeparator() );

        joiner.add( String.join( "", Col.GOLD, "------- Sentries Info for ", npcName, " (npcid - ",
                                    String.valueOf( inst.getNPC().getId() ), ") ", "------" ) );
        
        joiner.add( String.join( "", 
                Col.RED, "[Health]:", Col.WHITE, String.valueOf( inst.getHealth() ), "/", String.valueOf( inst.maxHealth ),
                Col.RED, " [Armour]:", Col.WHITE, String.valueOf( inst.armour ),
                Col.RED, " [Strength]:", Col.WHITE, String.valueOf( inst.strength ),
                Col.RED, " [Speed]:", Col.WHITE, df.format( inst.getSpeed() ),
                Col.RED, " [AttackRange]:", Col.WHITE, String.valueOf( inst.range ),
                Col.RED, " [AttackRate]:", Col.WHITE, String.valueOf( inst.attackRate ),
                Col.RED, " [NightVision]:", Col.WHITE, String.valueOf( inst.nightVision ),
                Col.RED, " [HealRate]:", Col.WHITE, String.valueOf( inst.healRate ),
                Col.RED, " [VoiceRange]:", Col.WHITE, String.valueOf( inst.voiceRange ),
                Col.RED, " [FollowDistance]:", Col.WHITE, String.valueOf( Math.sqrt( inst.followDistance ) ) ) );

        joiner.add( String.join( "", 
                Col.GREEN, "Invincible: ", Col.WHITE, String.valueOf( inst.invincible ), 
                Col.GREEN, "  Retaliate: ", Col.WHITE, String.valueOf( inst.iRetaliate ),
                Col.GREEN, "  Drops Items: ", Col.WHITE, String.valueOf( inst.dropInventory ), 
                Col.GREEN, "  Critical Hits: ", Col.WHITE, String.valueOf( inst.acceptsCriticals ),
                Col.GREEN, "  Kills Drop Items: ", Col.WHITE, String.valueOf( inst.killsDrop ), 
                Col.GREEN, "  Respawn Delay: ", Col.WHITE, String.valueOf( inst.respawnDelay ), "secs" ) );
        
        joiner.add( String.join( "", Col.BLUE, "Status: ", inst.myStatus.toString() ) );

        if ( inst.attackTarget == null )
            joiner.add( Col.BLUE.concat( "Currently Targetting: nothing" ) );
        else
            joiner.add( String.join( "", Col.BLUE, "Currently Targetting: ", inst.attackTarget.getName() ) );

        if ( inst.guardeeEntity != null )
            joiner.add( String.join( "", Col.BLUE, "Guarding: ", inst.guardeeEntity.getName() ) );          
        else if ( inst.guardeeName != null && !inst.guardeeName.isEmpty() )
            joiner.add( String.join( "", Col.BLUE, npcName, " is configured to guard ", inst.guardeeName, 
                                                                       " but cannot find them at the moment." ) );
        else joiner.add( Col.BLUE.concat( "Guarding: my spawnpoint" ) );

        sender.sendMessage( joiner.toString() );
    }

    @Override
    public String getShortHelp() { return "view the attributes of a sentry"; }
    @Override
    public String getLongHelp() { return "Displays a summary of all the configurable settings for a sentry."; }
    @Override
    public String getPerm() { return S.PERM_INFO; }
}
