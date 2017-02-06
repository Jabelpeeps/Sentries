package org.jabelpeeps.sentries;

import net.citizensnpcs.api.ai.AttackStrategy;


public interface SentryAttack extends AttackStrategy {

    int getDamage();

    double getApproxRange();
    
    default String getName() {
        return ( this instanceof AttackType ) ? ((AttackType) this).name() 
                                              : this.getClass().getSimpleName();
    };
}
