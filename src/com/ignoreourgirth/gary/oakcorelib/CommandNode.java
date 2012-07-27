
package com.ignoreourgirth.gary.oakcorelib;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.ignoreourgirth.gary.oakcorelib.CommandPreprocessor.OnCommand;

class CommandNode {

	private static final String accessDeniedMessage = ChatColor.RED + "Access denied. You do not have the required permissions.";
	
	private boolean hasChildren;
	private boolean hasPlayerParam;
	private boolean isClickCommand;
	private String fullCommandName;
	private String nodeName;
	private int nodeDepth;
	private int optionalParams;
	private Method method;
	private Class<?>[] paramTypes;
	private String[] paramLabels;
	private HashSet<String> myPermissions;
	private HashSet<String> inheritedPermissions;
	private ArrayList<String> orderedChildNames;
	private HashMap<String, CommandNode> children;

	private Object executor;
	
	protected CommandNode(int depth, String name, String completeCommandName) {
		nodeDepth = depth;
		nodeName = name;
		fullCommandName = completeCommandName;
		hasChildren = false;
		orderedChildNames = new ArrayList<String>();
		children = new HashMap<String, CommandNode>();
		myPermissions = new HashSet<String>();
		inheritedPermissions = new HashSet<String>();
		paramTypes = new Class<?>[] {};
		paramLabels = new String[] {};
	}
	
	protected int getDepth() {
		return nodeDepth;
	}
	
	protected String getNodeName() {
		return nodeName;
	}
	
	protected CommandNode getChild(String name) {
		return children.get(name.toLowerCase());
	}
	
	protected boolean hasMethod() {
		return (method != null);
	}
	
	protected void addPermission(String newPermission) {
		if (!myPermissions.contains(newPermission)) {
			myPermissions.add(newPermission);
			for (Entry<String, CommandNode> entry : children.entrySet()) {
			    entry.getValue().inheritPermission(newPermission);
			}
		}
		
	}
	
	protected void removePermission(String newPermission) {
		if (myPermissions.contains(newPermission)) {
			myPermissions.remove(newPermission);
			for (Entry<String, CommandNode> entry : children.entrySet()) {
			    entry.getValue().disinheritPermission(newPermission);
			}
		}
		
	}
	
	protected void inheritPermission(String newPermission) {
		if (!inheritedPermissions.contains(newPermission)) {
			inheritedPermissions.add(newPermission);
			for (Entry<String, CommandNode> entry : children.entrySet()) {
			    entry.getValue().inheritPermission(newPermission);
			}
		}
	}
	
	protected void disinheritPermission(String newPermission) {
		if (inheritedPermissions.contains(newPermission)) {
			inheritedPermissions.remove(newPermission);
			for (Entry<String, CommandNode> entry : children.entrySet()) {
			    entry.getValue().disinheritPermission(newPermission);
			}
		}
	}
	
	protected void addChild(CommandNode child) {
		hasChildren = true;
		orderedChildNames.add(child.getNodeName());
		children.put(child.getNodeName().toLowerCase(), child);
	}
	
	protected void setMethod(Object object, Method javaMethod) {
		
		executor = object; method = javaMethod;
		
		OnCommand cmd = method.getAnnotation(OnCommand.class);
    	isClickCommand = cmd.clickCommand();
    	optionalParams = cmd.optionals();
    	
    	int paramIndex = 0;
    	ArrayList<Class<?>> paramTypeList = new ArrayList<Class<?>>();
    	for (Class<?> type : method.getParameterTypes()) {
    		if (paramIndex == 0 && Player.class.isAssignableFrom(type)) {
    			hasPlayerParam = true;
    		} else if ((!hasPlayerParam && isClickCommand && paramIndex==0) ||
    				(hasPlayerParam && isClickCommand && paramIndex==1)
    				) {
    			if (!Location.class.isAssignableFrom(type)) {
        			OakCoreLib.plugin.getLogger().log(Level.SEVERE, " (" + 
        					CommandPreprocessor.class.getSimpleName() + 
        					"): Incorrect params for click command method "
        					+ method.getName() + " in " + executor.getClass().getName() + ". " +
        					"Expected Location type to satisfy ClickCommand requirements." + 
        					" Found " + type.getName() + ".");
        			return;
    			}
    		} else if (CommandPreprocessor.allowedTypes.contains(type)) {
    			paramTypeList.add(type);
    		} else {
    			OakCoreLib.plugin.getLogger().log(Level.SEVERE, " (" + 
    					CommandPreprocessor.class.getSimpleName() + 
    					"): Unsupported parameter type for method "
    					+ method.getName() + " in " + executor.getClass().getName() + ". " +
    					"Expected primitive type or String. Found " + type.getName() + ".");
    			return;
    		}
    		paramIndex++;
    	}
    	if (isClickCommand && 
    			((paramIndex == 0 && !hasPlayerParam) || (paramIndex == 1 && hasPlayerParam))) {
			OakCoreLib.plugin.getLogger().log(Level.SEVERE, " (" + 
					CommandPreprocessor.class.getSimpleName() + 
					"): Missing location param for click command method "
					+ method.getName() + " in " + executor.getClass().getName() + ". " +
					"Expected Location param to satisfy ClickCommand requirements.");
			return;
    	}
    	paramTypes = paramTypeList.toArray(new Class<?>[paramTypeList.size()]);

    	
    	if (cmd.labels().length() > 0) {
    		String[] labels = cmd.labels().split(",");
    		if (labels.length > paramTypes.length) {
    			OakCoreLib.plugin.getLogger().log(Level.SEVERE, " (" + 
    					CommandPreprocessor.class.getSimpleName() + 
    					"): Too many labels specified for " + method.getName() + 
    					" in " + executor.getClass().getName() + ". " +
    					"Expected one label per parameter, excluding CommandSender");
    			return;
    		}
    		for (int i = 0; i < labels.length; i++)
    			labels[i] = labels[i].trim();
    		paramLabels = labels;
    	}


	}
	
	protected void run(Player player, String[] args) {
		if (!checkInheritedPermissions(player)) return;
		if (!checkSelfPermissions(player)) return;
		ArrayList<Object> finalArguments = new ArrayList<Object>();
		if (hasPlayerParam) finalArguments.add(player);
		if (paramTypes.length == args.length - optionalParams) {
			for (int i=0; i< paramTypes.length; i++) {
				if (i < args.length) {
					Object nextObject = CommandPreprocessor.castToType(args[i], paramTypes[i]); 
					if (nextObject != null) {
						finalArguments.add(nextObject);
					} else {
						player.sendMessage(ChatColor.RED + "Expected " + 
							CommandPreprocessor.readableTypes.get(paramTypes[i]).toLowerCase() + 
							" as argument " + i + ".");
						return;
					}
				} else {
					finalArguments.add(null);
				}
			}
			if (isClickCommand) {
				CommandPreprocessor.setClickCommandForPlayer(player, this, finalArguments);
				player.sendMessage(ChatColor.GOLD + "Command set. Click target location.");
				return;
			}
			try {
				method.invoke(executor, finalArguments.toArray(new Object[finalArguments.size()]));
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				OakCoreLib.plugin.getLogger().log(Level.SEVERE, e.getMessage());
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				OakCoreLib.plugin.getLogger().log(Level.SEVERE, e.getMessage());
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				OakCoreLib.plugin.getLogger().log(Level.SEVERE, "");
				OakCoreLib.plugin.getLogger().log(Level.SEVERE, "Command Error [" + 
				executor.getClass().getSimpleName() + "] Player:" + player.getName() + " -> " + fullCommandName);
			}
		} else {
			if (args.length == 0) {
				showUsageText(player, true);
			} else {
				if (paramTypes.length == 1) {
					player.sendMessage(ChatColor.RED + "Wrong number of arguments. Expected 1 argument.");
				} else {
					player.sendMessage(ChatColor.RED + "Wrong number of arguments. " +
							"Expected " + paramTypes.length + " arguments.");
				}
			}
		}
	}
	
	protected void runClickCommand(ArrayList<Object>args, Location location) {
		if (hasPlayerParam) {
			args.add(1,location);
		} else {
			args.add(0,location);
		}
		try {
			method.invoke(executor, args.toArray(new Object[args.size()]));
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			OakCoreLib.plugin.getLogger().log(Level.SEVERE, e.getMessage());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			OakCoreLib.plugin.getLogger().log(Level.SEVERE, e.getMessage());
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			OakCoreLib.plugin.getLogger().log(Level.SEVERE, "");
			OakCoreLib.plugin.getLogger().log(Level.SEVERE, "Click Command Error [" + 
			executor.getClass().getSimpleName() + "] -> " + fullCommandName);
		}
	}
	
	protected boolean checkSelfPermissions(Player player) {
		Iterator<String> iterator = myPermissions.iterator();
		while (iterator.hasNext()) {
			if (!OakCoreLib.getPermission().has(player, iterator.next())) {
				player.sendMessage(accessDeniedMessage); 
				return false;
			}
		}
		return true;
	}
	
	protected boolean checkInheritedPermissions(Player player) {
		Iterator<String> iterator = inheritedPermissions.iterator();
		while (iterator.hasNext()) {
			if (!OakCoreLib.getPermission().has(player, iterator.next())) {
				player.sendMessage(accessDeniedMessage); 
				return false;
			}
		}
		return true;
	}
	
	protected void showUsageText(Player player, boolean skipPermissionCheck) {
		if (!skipPermissionCheck) {
			if (!checkInheritedPermissions(player)) return;
			if (!checkSelfPermissions(player)) return;
		}
		StringBuilder message = new StringBuilder(ChatColor.WHITE.toString());
		message.append(ChatColor.BOLD.toString());
		message.append("Command Help");
		message.append(ChatColor.RESET.toString());
		message.append(ChatColor.WHITE.toString());
		message.append(" (");
		message.append(ChatColor.GREEN.toString());
		message.append(fullCommandName);
		message.append(ChatColor.WHITE.toString());
		message.append(")");
		player.sendMessage(message.toString());
		
		message = new StringBuilder();
		if (paramTypes.length > 0) {
			message.append(ChatColor.WHITE.toString());
			message.append("Arguments: ");
			message.append(ChatColor.LIGHT_PURPLE.toString());
			for (int i=0; i < paramTypes.length; i++) {
				if (i < paramLabels.length) {
					message.append(paramLabels[i]);
				} else {
					message.append(CommandPreprocessor.readableTypes.get(paramTypes[i]));
				}
				if (i+1 < paramTypes.length) message.append(" ");	
			}
		}
		
		if (hasChildren) {
			Iterator<String> iterator = orderedChildNames.iterator();
			if (paramTypes.length > 0) message.append("   ");
			message.append(ChatColor.WHITE.toString());
			message.append("SubCommands: ");
			message.append(ChatColor.GOLD.toString());
			while (iterator.hasNext()) {
				CommandNode child = children.get(iterator.next());
				if (child.checkSelfPermissions(player)) {
					message.append(child.getNodeName());
					if (iterator.hasNext()) {
						message.append(ChatColor.WHITE.toString());
						message.append(", ");
						message.append(ChatColor.GOLD.toString());
					}
				}
			}
		}
		
		player.sendMessage(message.toString());
	}
}
