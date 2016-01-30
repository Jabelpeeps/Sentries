package net.aufdemrand.sentry;

enum SentryStatus {
	isDEAD {
		@Override
		boolean update( SentryInstance inst ) {
			
			if  (  System.currentTimeMillis() > inst.isRespawnable 
				&& inst.respawnDelay > 0 
				&& inst.spawnLocation.getWorld().isChunkLoaded( inst.spawnLocation.getBlockX() >> 4,
																inst.spawnLocation.getBlockZ() >> 4 ) ) {

						if ( Sentry.debug ) Sentry.debugLog( "respawning" + inst.myNPC.getName() );
						
						if ( inst.guardEntity == null ) 
							inst.myNPC.spawn( inst.spawnLocation.clone() );
						else 
							inst.myNPC.spawn( inst.guardEntity.getLocation().add( 2, 0, 2 ) );
						
						return true;
			}
			return false;
		}
	},
	isDYING {
		@Override
		boolean update( SentryInstance inst ) {
			// TODO add code to manage drops and other cleaning up tasks.
			return false;
		}
	},
	isHOSTILE {
		@Override
		boolean update( SentryInstance inst ) {
			// TODO Auto-generated method stub
			return false;
		}
	},
	isLOOKING {
		@Override
		boolean update( SentryInstance inst ) {
			// TODO Auto-generated method stub
			return false;
		}
	},
	isRETALIATING {
		@Override
		boolean update( SentryInstance inst ) {
			// TODO Auto-generated method stub
			return false;
		}
		
		// unused?
//	},
//	isSTUCK {
//		@Override
//		boolean update( SentryInstance inst ) {
//			// TODO Auto-generated method stub
//			return false;
//		}
		
		//  unused ?
//	},
//	isWAITING {
//		@Override
//		boolean update( SentryInstance inst  ) {
//			// TODO Auto-generated method stub
//			return false;
//		}

	};
	
	abstract boolean update( SentryInstance inst );
}