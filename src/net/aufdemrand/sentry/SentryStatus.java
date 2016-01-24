package net.aufdemrand.sentry;

import net.citizensnpcs.api.npc.NPC;

enum SentryStatus {
	isDEAD {
		@Override
		boolean update( NPC npc ) {
			// TODO add code to control respawning
			return false;
		}
	},
	isDYING {
		@Override
		boolean update( NPC npc ) {
			// TODO add code to manage drops and other cleaning up tasks.
			return false;
		}
	},
	isHOSTILE {
		@Override
		boolean update( NPC npc ) {
			// TODO Auto-generated method stub
			return false;
		}
	},
	isLOOKING {
		@Override
		boolean update( NPC npc ) {
			// TODO Auto-generated method stub
			return false;
		}
	},
	isRETALIATING {
		@Override
		boolean update( NPC npc ) {
			// TODO Auto-generated method stub
			return false;
		}
	},
	isSTUCK {
		@Override
		boolean update( NPC npc ) {
			// TODO Auto-generated method stub
			return false;
		}
	},
	isWWAITING {
		@Override
		boolean update( NPC npc ) {
			// TODO Auto-generated method stub
			return false;
		}
	};
	
	abstract boolean update( NPC npc );
}