package org.jabelpeeps.sentries.commands;

import java.util.StringJoiner;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.Sentries;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import lombok.Getter;


public class InfoCommand implements SentriesComplexCommand {
    @Getter final String shortHelp = "view the attributes of a sentry";
    @Getter final String longHelp = "Displays a summary of all the configurable settings for a sentry.";
    @Getter final String perm = S.PERM_INFO;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {

        StringJoiner joiner = new StringJoiner( System.lineSeparator() );

        joiner.add( Utils.join( Col.GOLD, "------- Sentries Info for ", npcName, " (npcid - ",
                                    String.valueOf( inst.getNPC().getId() ), ") ------" ) );
        
        joiner.add( String.join( "", 
                Col.RED, "[Health]:", Col.WHITE, Utils.formatDbl( inst.getHealth() ), "/", String.valueOf( inst.maxHealth ),
                Col.RED, " [Armour]:", Col.WHITE, 
                Sentries.useNewArmourCalc ? String.valueOf( Math.abs( inst.armour ) ) + "%"
                                          : String.valueOf( Math.abs( inst.armour ) ), inst.armour < 0 ? "(C)" : "",
                Col.RED, " [Strength]:", Col.WHITE, String.valueOf( inst.strength ), inst.strengthFromWeapon ? "(C)" : "",
                Col.RED, " [Speed]:", Col.WHITE, Utils.formatDbl( inst.getSpeed() ),
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
                Col.GREEN, "  Respawn Delay: ", Col.WHITE, String.valueOf( inst.respawnDelay ), " secs" ) );
        
        joiner.add( Utils.join( Col.BLUE, "Status: ", Col.WHITE, inst.getMyStatus().toString() ) );

        if ( inst.attackTarget == null )
            joiner.add( Utils.join( Col.BLUE, "Currently Targetting: ", Col.WHITE, "nothing" ) );
        else
            joiner.add( Utils.join( Col.BLUE, "Currently Targetting: ", Col.WHITE, inst.attackTarget.getName() ) );

        if ( inst.guardeeEntity != null )
            joiner.add( Utils.join( Col.BLUE, "Guarding: ", Col.WHITE, inst.guardeeEntity.getName() ) );          
        else if ( inst.guardeeName != null && !inst.guardeeName.isEmpty() )
            joiner.add( Utils.join( Col.BLUE, npcName, " is configured to guard ", Col.WHITE, inst.guardeeName, 
                                                                  Col.BLUE, " but cannot find them at the moment." ) );
        else joiner.add( Utils.join( Col.BLUE, "Guarding: ", Col.WHITE, "my spawnpoint" ) );

        sender.sendMessage( joiner.toString() );
    }
}
