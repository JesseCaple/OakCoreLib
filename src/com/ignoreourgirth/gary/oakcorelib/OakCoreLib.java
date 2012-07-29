/*******************************************************************************
 * Copyright (c) 2012 GaryMthrfkinOak (Jesse Caple).
 * 
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this software.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ignoreourgirth.gary.oakcorelib;

import java.security.acl.NotOwnerException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.logging.Level;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;

public class OakCoreLib extends JavaPlugin implements Listener {
	
	protected static Plugin plugin;
	protected static HashSet<LivingEntity> mobSpawnerMobs;
	
	private static boolean initDone;
	private static Connection dbConnection;
	private static XPMP xpmp;
	private static Economy plugin_Economy;
	private static Permission plugin_Permission;
	private static WorldGuardPlugin plugin_WorldGuard;
	
	public static final String bankPrefix = "!worldbank_";
	public static boolean intialized() {return initDone;}
	public static Economy getEconomy() {return plugin_Economy;}
	public static Permission getPermission() {return plugin_Permission;}
	public static WorldGuardPlugin getWorldGuard() {return plugin_WorldGuard;}
	public static XPMP getXPMP() {return xpmp;}
	
	public static boolean isFromMobSpawner(LivingEntity entity) 
	{return mobSpawnerMobs.contains(entity);}
	
	public static Connection getDB() {
		try {
			if (dbConnection == null || dbConnection.isClosed()) {
				FileConfiguration config = plugin.getConfig();
				try {
					dbConnection = DriverManager.getConnection(
							config.getString("database.url"),
							config.getString("database.user"),
							config.getString("database.password"));
				} catch (SQLException e) {
					plugin.getLogger().log(Level.SEVERE, e.getMessage());
				}
			}
			return dbConnection;
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, e.getMessage());
			plugin.getLogger().log(Level.SEVERE, "Severe database error!");
			return null;
		}
	}
	
	public static boolean canBuild(Player player, Location location) {
		// Towny has very little documentation...
		// I have no idea if this is the right way to do this.
		// Hopefully it's good enough. So help me god.
		
		// Operators can build anywhere.
		if (player.isOp()) return true;
		
		// See if WorldGuard thinks they can't build...
		ApplicableRegionSet regions = OakCoreLib.getWorldGuard()
				.getRegionManager(location.getWorld()).getApplicableRegions(location);
		LocalPlayer localPlayer = OakCoreLib.getWorldGuard().wrapPlayer(player);
		if (!regions.canBuild(localPlayer)) return false;
		
		
		// Okay, WorldGuard checks out. See what Towny thinks.
		
		
		//If they have admin status with towny, I'm going to say that's good enough for me.
		if (TownyUniverse.getPermissionSource().isTownyAdmin(player)) return true;
		
		//If this location isn't a part of a town, on my server at least, building is fine.
		Resident residentPlayer;
		TownBlock townBlock = TownyUniverse.getTownBlock(player.getLocation());
		if (townBlock == null) return true;	
		
		
		try {
			residentPlayer = TownyUniverse.getDataSource().getResident(player.getName());
		} catch (NotRegisteredException e) {
			// Oh for the love of god Towny. Nice lack of documentation.
			// I have no fucking clue... fuck it, you can't build. Sorry.
			return false;
		}
		
		
		//Okay, this location is in a town. Fuck.
		try {
			
			//If you are the owner of the town block...
			//I could care less about the flags you set, you can build.
			if (townBlock.isOwner(residentPlayer)) return true;
			
			// You are not the owner, so now we have to see what properties this location has.
			TownyPermission perm = townBlock.getPermissions();
			boolean outsidersBuild = perm.getOutsiderPerm(ActionType.BUILD);
			boolean allyBuild = perm.getAllyPerm(ActionType.BUILD);
			boolean residentsBuild = perm.getResidentPerm(ActionType.BUILD);
			
			
			// Anyone can build, hurray!
			if (outsidersBuild) return true;
			
			
			// If you don't have a town then you can't be an ally or resident.
			// We already know outsider's can't build. So, if you don't have a town you can't build.
			if (!residentPlayer.hasTown()) return false;
			
			// Is the player a resident of the town and the resident flag is set? Hopefully.
			Town playerTown = residentPlayer.getTown();
			Town locationTown = townBlock.getTown();
			if (playerTown == locationTown && residentsBuild) return true;
			
			// Last chance, are you an allied nation and does this location allow allies to build?
			if (playerTown != null) {
				if (locationTown.getNation() != null && playerTown.getNation() != null) {
					if (locationTown.getNation().hasAlly(playerTown.getNation()) && allyBuild) return true;
				}
			}
			
		} catch (NotRegisteredException ex) {
			plugin.getLogger().log(Level.SEVERE, ex.getMessage());
		}
		
		//NOPE! You can't build, you fucker.
		return false;
	}
	
	public void onEnable() {
		if (getConfig().getString("database.url") == null) {
			saveDefaultConfig();
			getLogger().log(Level.SEVERE, "Please complete configuration file.");
			getPluginLoader().disablePlugin(this);
			return;
		}
		
		mobSpawnerMobs = new HashSet<LivingEntity>();
		plugin = this;
		
		boolean hookSuccess = (setupEconomyHook() && setupPermissionHook() && 
				                   setupTownyHook()&& setupWorldGuardHook());
		if (!hookSuccess) {
			getLogger().log(Level.SEVERE, "Could not hook required plugins!");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		getDB();	
		
		PluginAdministration pluginAdministration = new PluginAdministration();
		getCommand("load").setExecutor(pluginAdministration);
		getCommand("unload").setExecutor(pluginAdministration);
		getCommand("toggle").setExecutor(pluginAdministration);
		getCommand("refresh").setExecutor(pluginAdministration);
		
		try {xpmp = new XPMP();}
		catch (NotOwnerException e) {plugin.getLogger().log(Level.SEVERE, e.getMessage());}
		
		getServer().getPluginManager().registerEvents(xpmp, this);
		getServer().getPluginManager().registerEvents(new CoreEvents(), this);
		getServer().getPluginManager().registerEvents(new ProtectedLocations(), this);
		getServer().getPluginManager().registerEvents(new DisplayItems(), this);
		getServer().getPluginManager().registerEvents(new ProximityDetection(), this);
		for (Player player : getServer().getOnlinePlayers()) {
			xpmp.loadDBData(player);
		}
		
		initDone = true;
	}
	
	public void onDisable() {
		for (Player player : getServer().getOnlinePlayers()) {
			xpmp.saveDBData(player);
		}
		try { 
			if (dbConnection != null) dbConnection.close();
		} catch (SQLException ex) {}
	}
	
    private boolean setupEconomyHook()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			plugin_Economy = economyProvider.getProvider();
		}
		return (economyProvider != null);
    }
    
    private boolean setupPermissionHook()
    {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
        	plugin_Permission = permissionProvider.getProvider();
        }
        return (permissionProvider != null);
    }
    
    private boolean setupTownyHook()
    {
		Plugin townyPlugin = this.getServer().getPluginManager().getPlugin("Towny");
        if (townyPlugin != null && townyPlugin.isEnabled() && townyPlugin.getClass().getName().equals("com.palmergames.bukkit.towny.Towny")) {
        	return true;
        } else {
            return false;
        }
    }
    
    private boolean setupWorldGuardHook()
    {
        Plugin wgPlugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if (wgPlugin != null && wgPlugin.isEnabled()) {
        	plugin_WorldGuard = (WorldGuardPlugin) wgPlugin;
        	return true;
        } else {
            return false;
        }
    }
	
}
