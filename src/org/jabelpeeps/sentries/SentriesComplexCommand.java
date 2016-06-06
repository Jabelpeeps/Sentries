package org.jabelpeeps.sentries;

import org.bukkit.command.CommandSender;

import net.citizensnpcs.api.npc.NPC;

public interface SentriesComplexCommand extends SentriesCommand {
    
    boolean call( CommandSender sender, // the sender of the command
                  NPC npc,              // the select NPC                 
                  String npcName,       // the NPC's name
                  SentryTrait inst,     // the NPC's SentryTrait
                  int nextArg,          // the next argument to be parsed
                  String... args );     // the arguments array.
  
}
