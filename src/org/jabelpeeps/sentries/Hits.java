package org.jabelpeeps.sentries;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import org.bukkit.configuration.file.FileConfiguration;

enum Hits {
    Crit3( 2.0, 2, "&c*** You DISEMBOWEL <NPC> with your <ITEM> for <AMOUNT> damage" ), 
    Crit2( 1.75, 4, "&6*** You MAIM <NPC> with your <ITEM> for <AMOUNT> damage" ), 
    Crit1( 1.50, 6, "&e*** <NPC> sustains \"a mere flesh wound\" for <AMOUNT> damage" ), 
    Glance( 0.5, 4, "&f*** Your paltry blow does only <AMOUNT> damage to <NPC>" ), 
    Miss( 0, 4, "&7*** You MISSED! <NPC> thumbs their nose at you!" ), 
    Hit( 1.0, 0, "" ),             // represents a standard unmodified hit.
    Block( 0, 0, "&7*** <NPC> skillfully parries your attack!" );

    double damageModifier;
    private int percentChance;
    private int culmulativeChance;
    String message;

    static boolean useCriticalHits;
    static Random rand = new Random();
    private static Set<Hits> randomisedHits = EnumSet.of( Hits.Crit3,
                                                          Hits.Crit2, 
                                                          Hits.Crit1, 
                                                          Hits.Glance, 
                                                          Hits.Miss );

    Hits( double mod, int chance, String msg ) {
        damageModifier = mod;
        percentChance = chance;
        message = msg;
    }

    static Hits getHit() {
        if ( useCriticalHits ) {

            int chance = rand.nextInt( 100 );

            for ( Hits each : randomisedHits ) {
                if ( chance < each.culmulativeChance )
                    return each;
            }
        }
        return Hits.Hit;
    }

    static void makeChanceMap() {
        int total = 0;

        for ( Hits each : randomisedHits ) {
            total += each.percentChance;
            each.culmulativeChance = total;
        }
    }

    static void loadConfig( FileConfiguration config ) {

        useCriticalHits = config.getBoolean( "UseCriticalHits" );

        if ( !useCriticalHits )
            return;

        if ( config.getBoolean( "UseCustomMessages" ) ) {
            for ( Hits each : values() )
                each.message = config.getString( "GlobalTexts." + each.name() );
        }
        Hits.Crit3.percentChance = config.getInt( "HitChances.Crit3" );
        Hits.Crit2.percentChance = config.getInt( "HitChances.Crit2" );
        Hits.Crit1.percentChance = config.getInt( "HitChances.Crit1" );
        Hits.Glance.percentChance = config.getInt( "HitChances.Glance" );
        Hits.Miss.percentChance = config.getInt( "HitChances.Miss" );

        makeChanceMap();
    }
}
