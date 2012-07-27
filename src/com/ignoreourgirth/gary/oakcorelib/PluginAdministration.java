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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.UnknownDependencyException;

public class PluginAdministration implements CommandExecutor {

	private String pluginDirectory;
	private PluginManager pluginManager;
	private SimplePluginManager simplePluginManager;

	protected PluginAdministration() {
		pluginManager = Bukkit.getServer().getPluginManager();
		simplePluginManager = (SimplePluginManager) pluginManager;
		try {
			pluginDirectory = new File(".").getCanonicalPath() + "\\plugins\\";
			OakCoreLib.plugin.getLogger().info("OakPluginManager enabled.");
		} catch (IOException ex) {
			OakCoreLib.plugin.getLogger().log(Level.SEVERE, ex.getMessage());
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		String pluginName = getCorrectCase(stripExtension(unsplitString(args)));
		if (cmd.getName().equalsIgnoreCase("load")) {
			if (args.length < 1) {
				sender.sendMessage("§4Correct usage: /load Plugin Name");
				return true;
			} else {
				loadPlugin(sender, pluginName, false);
				return true;
			}
		} else if (cmd.getName().equalsIgnoreCase("unload")) {
			if (args.length < 1) {
				sender.sendMessage("§4Correct usage: /unload Plugin Name");
				return true;
			} else {
				unloadPlugin(sender, pluginName);
				return true;
			}
		} else if (cmd.getName().equalsIgnoreCase("toggle")) {
			if (args.length < 1) {
				sender.sendMessage("§4Correct usage: /toggle Plugin Name");
				return true;
			} else {
				togglePlugin(sender, pluginName);
				return true;
			}
		} else if (cmd.getName().equalsIgnoreCase("refresh")) {
			if (args.length < 1) {
				sender.sendMessage("§4Correct usage: /refresh Plugin Name");
				return true;
			} else {
				refreshPlugin(sender, pluginName);
				return true;
			}
		}
		return false;
	}

	private String unsplitString(String[] arguments) {
		StringBuilder builder = new StringBuilder("");
		for (String arg : arguments) {
			builder.append(arg);
			builder.append(" ");
		}
		if (builder.length() > 0) {
			builder.deleteCharAt(builder.length() - 1);
		}
		return builder.toString();
	}
	
	private String getCorrectCase(String pluginName) {
		Plugin[] plugins = pluginManager.getPlugins();
		for (Plugin plugin : plugins) {
			if (plugin.getName() != null) {
				if (plugin.getName().equalsIgnoreCase(pluginName)) {
					return plugin.getName();
				}
			}
		}
		return pluginName;
	}
	
	private String stripExtension(String input) {
		int inputLength = input.length();
		if (inputLength > 4) {
			String possibleFileExtension = input.substring(inputLength - 4).toLowerCase();
			if (possibleFileExtension.equals(".jar")) {
				return input.substring(0, inputLength - 4);
			}
		}
		return input;
	}
	
	private void togglePlugin(CommandSender sender, String name) {
		Plugin targetPlugin = pluginManager.getPlugin(name);
		if (targetPlugin != null) {
			if (targetPlugin.isEnabled()) {
				pluginManager.disablePlugin(targetPlugin);
				sender.sendMessage("§6Plugin disabled: " + name);
			} else {
				pluginManager.enablePlugin(targetPlugin);
				sender.sendMessage("§6Plugin enabled: " + name);
			}
		} else {
			sender.sendMessage("§4No such plugin: " + name);
		}
	}

	private void loadPlugin(CommandSender sender, String name, boolean supressMessages) {
		String jarPath = pluginDirectory + name + ".jar";
		File jarToLoad = new File(jarPath);
		if (jarToLoad.exists()) {
			try {
				Plugin targetPlugin = pluginManager.getPlugin(name);
				if (targetPlugin == null) {
					targetPlugin = pluginManager.loadPlugin(jarToLoad);
					pluginManager.enablePlugin(targetPlugin);
					if (!supressMessages) {sender.sendMessage("§6Plugin loaded: " + name + ".jar");}
				} else {
					if (!supressMessages) {sender.sendMessage("§4Plugin already loaded: " + name);}
				}
			} catch (UnknownDependencyException e) {
				if (!supressMessages) {sender.sendMessage("§4Error loading plugin. Unknown Dependency.");}
			} catch (InvalidPluginException e) {
				if (!supressMessages) {sender.sendMessage("§4Error loading plugin. Invalid Plugin.");}
			} catch (InvalidDescriptionException e) {
				if (!supressMessages) {sender.sendMessage("§4Error loading plugin. Invalid Description.");}
			}
		} else {
			if (!supressMessages) {sender.sendMessage("§4Can not find: " + name + ".jar");}
		}
	}
	
	private void unloadPlugin(CommandSender sender, String name) {
		Plugin targetPlugin = pluginManager.getPlugin(name);
		if (targetPlugin != null) {
			removePlugin(name);
			sender.sendMessage("§6Plugin unloaded: " + name);
		} else {
			sender.sendMessage("§4No such plugin: " + name);
		}
	}

	private void refreshPlugin(CommandSender sender, String name) {
		Plugin targetPlugin = pluginManager.getPlugin(name);
		if (targetPlugin != null) {
			removePlugin(name);
			loadPlugin(sender, name, true);
			sender.sendMessage("§6Plugin refreshed: " + name);
		} else {
			sender.sendMessage("§4No such plugin: " + name);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void removePlugin(String pluginName) {
		try {
			// Reflect and find the fields we need. Allow them to be accessed publicly.
			Class managerClass = simplePluginManager.getClass();
			Field pluginsField = managerClass.getDeclaredField("plugins");
			Field lookupNamesField = managerClass.getDeclaredField("lookupNames");
			Field commandMapField = managerClass.getDeclaredField("commandMap");
			pluginsField.setAccessible(true);
			lookupNamesField.setAccessible(true);
			commandMapField.setAccessible(true);
			SimpleCommandMap commandMap = (SimpleCommandMap) commandMapField.get(simplePluginManager);
			Class commandMapClass = commandMap.getClass();
			Field knownCommandsField = commandMapClass.getDeclaredField("knownCommands");
			knownCommandsField.setAccessible(true);
	
			// Grab the list of plugins, lookup names, and the command names.
			List<Plugin> plugins = (List<Plugin>) pluginsField.get(simplePluginManager);
			Map lookupNames = (Map) lookupNamesField.get(simplePluginManager);
			Map knownCommands = (Map) knownCommandsField.get(commandMap);
	
			// Iterate through the plugins to find the one we want to unload.
			for (int i = 0; i < plugins.size(); i++) {
				
				Plugin nextPlugin = plugins.get(i);
				
				if (!nextPlugin.getDescription().getName().equalsIgnoreCase(pluginName)) {continue;}
	
				// Time to wipe this plugin from the face of the earth.
				simplePluginManager.disablePlugin(nextPlugin);
				plugins.remove(nextPlugin);
				lookupNames.remove(pluginName);
				if (commandMap != null) {
					for (Iterator iterator = knownCommands.entrySet().iterator(); iterator.hasNext();) {
						Map.Entry entry = (Map.Entry) iterator.next();
	
						if ((entry.getValue() instanceof PluginCommand)) {
							PluginCommand c = (PluginCommand) entry.getValue();
							if (c.getPlugin() == nextPlugin) {
								c.unregister(commandMap);
								iterator.remove();
							}
						}
					}
				}
				
				try {
					List permissionlist = nextPlugin.getDescription().getPermissions();
					Iterator p = permissionlist.iterator();
					while (p.hasNext()) {
						simplePluginManager.removePermission(((Permission) p.next()).toString());
					}	
				} catch (NoSuchMethodError ex) {}
				
				// Now if we we were really good, this is where we would unload it's resources from memory. Nope.
			}
		} catch (SecurityException ex) {
		} catch (IllegalArgumentException ex) {
		} catch (NoSuchFieldException ex) {
		} catch (IllegalAccessException ex) {}
	}
}
