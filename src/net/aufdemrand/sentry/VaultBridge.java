package net.aufdemrand.sentry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.permission.Permission;

public class VaultBridge extends PluginBridge {
	
	Map<SentryInstance, Set<String>> friends = new HashMap<SentryInstance, Set<String>>();
	Map<SentryInstance, Set<String>> enemies = new HashMap<SentryInstance, Set<String>>();
	
	private String activationMsg = ""; 
	static Permission perms = null;
	
	VaultBridge( int flag ) {
		super( flag );
	}
	
	@Override
	public boolean activate() {
		
		RegisteredServiceProvider<Permission> permissionProvider = 
							Sentry.getSentry().getServer().getServicesManager().getRegistration( Permission.class );
		
		if ( permissionProvider != null ) {
			perms = permissionProvider.getProvider();
		
			if ( perms.hasGroupSupport() ) {
				
				String[] groups = perms.getGroups();
				
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
	public boolean isIgnoring( LivingEntity entity, SentryInstance inst ) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void refreshLists( SentryInstance inst ) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean addTarget( String target, SentryInstance inst ) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addIgnore( String target, SentryInstance inst ) {
		// TODO Auto-generated method stub
		return false;
	}
	
	static boolean checkGroups4Targets( World world, OfflinePlayer player, SentryInstance inst ) {
		
		// check world permission groups & then global permission groups if needed.
		return checkGroups4Targets( perms.getPlayerGroups( world.getName(), player ), inst )
			|| checkGroups4Targets( perms.getPlayerGroups( (String) null, player ), inst );
	}
	
	private static boolean checkGroups4Targets( String[] groups, SentryInstance inst ) {
		
		if ( groups != null ) {
			for ( String each : groups )
				if ( inst.targetsContain( "GROUP:" + each ) )	
					return true;
		}
		return false;
	}
	
	static boolean checkGroups4Ignores( World world, OfflinePlayer player, SentryInstance inst ) {
		
		// check world permission groups & then global permission groups if needed.
		return checkGroups4Ignores( perms.getPlayerGroups( world.getName(), player ), inst ) 
			|| checkGroups4Ignores( perms.getPlayerGroups( (String) null, player ), inst );
	}
	
	private static boolean checkGroups4Ignores( String[] groups, SentryInstance inst ) {
		
		if ( groups != null ) {
			for ( String each : groups )
				if ( inst.ignoresContain( "GROUP:" + each ) )	
					return true;
		}
		return false;
	}

	@Override
	String getCommandText() {
		return "GROUP";
	}

	@Override
	String getCommandHelp() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
