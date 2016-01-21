package net.aufdemrand.sentry;

import org.bukkit.entity.LivingEntity;

enum SentryStatus {
	isDEAD {
		@Override
		LivingEntity update() {
			// TODO Auto-generated method stub
			return null;
		}
	},
	isDYING {
		@Override
		LivingEntity update() {
			// TODO Auto-generated method stub
			return null;
		}
	},
	isHOSTILE {
		@Override
		LivingEntity update() {
			// TODO Auto-generated method stub
			return null;
		}
	},
	isLOOKING {
		@Override
		LivingEntity update() {
			// TODO Auto-generated method stub
			return null;
		}
	},
	isRETALIATING {
		@Override
		LivingEntity update() {
			// TODO Auto-generated method stub
			return null;
		}
	},
	isSTUCK {
		@Override
		LivingEntity update() {
			// TODO Auto-generated method stub
			return null;
		}
	},
	isWWAITING {
		@Override
		LivingEntity update() {
			// TODO Auto-generated method stub
			return null;
		}
	};
	
	abstract LivingEntity update();
}