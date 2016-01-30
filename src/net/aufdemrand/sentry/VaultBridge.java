package net.aufdemrand.sentry;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.permission.Permission;

public class VaultBridge implements PluginBridge {
	
	private String activationMsg = ""; 
	static Permission perms = null;
	private String[] groups;
	
	VaultBridge() {}
	
	@Override
	public boolean activate() {
		
		RegisteredServiceProvider<Permission> permissionProvider = 
							Sentry.getSentry().getServer().getServicesManager().getRegistration( Permission.class );
		
		if ( permissionProvider != null ) {
			perms = permissionProvider.getProvider();
		
			if ( perms.hasGroupSupport() ) {
				
				groups = perms.getGroups();
				
				if ( groups.length > 0 ) {
					activationMsg = "Sucessfully interfaced with Vault: " + groups.length 
												  + " groups found. The GROUP: target will function.";
					return true;
				}
				activationMsg = "Vault integration: No permission groups found. "
											+ "The GROUP: target will not function.";
			} 
			else { activationMsg = "Vault integration: Permissions Provider does not support groups. "
											+ "The GROUP: target will not function."; 
			}	
		}
		else { activationMsg = "Vault integration: No Permissions Provider is registered. "
											+ "The GROUP target will not function.";
		}
		perms = null;
		return false;
	}

	@Override
	public String getActivationMessage() {
		return activationMsg;
	}

	@Override
	public boolean isTarget( LivingEntity entity, SentryInstance inst ) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isIgnored( LivingEntity entity, SentryInstance inst ) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void refreshAllLists() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void refreshLists( SentryInstance inst ) {
		// TODO Auto-generated method stub
		
	}

}
