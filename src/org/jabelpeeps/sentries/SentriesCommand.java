package org.jabelpeeps.sentries;

import org.bukkit.command.CommandSender;

import net.citizensnpcs.api.npc.NPC;

public interface SentriesCommand {
    
    boolean call( CommandSender sender, // the sender of the command
                  NPC npc,              // the select NPC                 
                  String npcName,       // the NPC's name
                  SentryTrait inst,     // the NPC's SentryTrait
                  Boolean set,          // is true if the true or on was supplied as an argument
                  int nextArg,          // the next argument to be parsed
                  String... args );     // the arguments array.
  
    String getShortHelp();
    
    String getLongHelp();
    
    String getPerm();

}
