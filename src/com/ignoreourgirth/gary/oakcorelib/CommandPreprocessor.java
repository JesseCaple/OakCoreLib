/*******************************************************************************
 * Copyright (c) 2012 GaryMthrfkinOak (Jesse Caple).
 * 
 *     This is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 *     This software is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty
 *     of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *     See the GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this software.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ignoreourgirth.gary.oakcorelib;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.entity.Player;

public class CommandPreprocessor {
	
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface OnCommand{
		String value();
		String labels() default "";
		int optionals() default 0;
		boolean clickCommand() default false;
	}
	
	protected static HashSet<Class<?>> allowedTypes;
	protected static HashMap<Class<?>, String> readableTypes;
	protected static HashMap<Player, CommandNode> clickCommand;
	protected static HashMap<Player, ArrayList<Object>> clickCommandArgs;
	private static HashMap<String, CommandNode> baseNodes;
	
	protected CommandPreprocessor() {}
	
	static {
		allowedTypes = new HashSet<Class<?>>(); 
		readableTypes = new HashMap<Class<?>, String>();
		clickCommand = new HashMap<Player, CommandNode>();
		clickCommandArgs = new HashMap<Player, ArrayList<Object>>();
		allowedTypes.add(String.class); readableTypes.put(String.class, "TEXT");
		allowedTypes.add(byte.class); readableTypes.put(byte.class, "SMALL_NUMBER");
		allowedTypes.add(short.class); readableTypes.put(short.class, "SMALL_NUMBER");
		allowedTypes.add(int.class); readableTypes.put(int.class, "NUMBER");
		allowedTypes.add(long.class); readableTypes.put(long.class, "NUMBER");
		allowedTypes.add(float.class); readableTypes.put(float.class, "DECIMAL");
		allowedTypes.add(double.class); readableTypes.put(double.class, "DECIMAL");
		allowedTypes.add(boolean.class); readableTypes.put(boolean.class, "TRUE|FALSE");
		allowedTypes.add(char.class); readableTypes.put(char.class, "CHARACTER");
		allowedTypes.add(Byte.class); readableTypes.put(Byte.class, "SMALL_NUMBER");
		allowedTypes.add(Short.class); readableTypes.put(Short.class, "SMALL_NUMBER");
		allowedTypes.add(Integer.class); readableTypes.put(Integer.class, "NUMBER");
		allowedTypes.add(Long.class); readableTypes.put(Long.class, "NUMBER");
		allowedTypes.add(Float.class); readableTypes.put(Float.class, "DECIMAL");
		allowedTypes.add(Double.class); readableTypes.put(Double.class, "DECIMAL");
		allowedTypes.add(Boolean.class); readableTypes.put(Boolean.class, "TRUE|FALSE");
		allowedTypes.add(Character.class); readableTypes.put(Character.class, "CHARACTER");
		baseNodes = new HashMap<String, CommandNode>();
	}
	
	public static void addExecutor(Object executor) {
        for (Method nextMethod : executor.getClass().getDeclaredMethods()) {
            if (nextMethod.isAnnotationPresent(OnCommand.class)) {
            	String fullPath = nextMethod.getAnnotation(OnCommand.class).value();
            	createNodePath(fullPath).setMethod(executor, nextMethod);
            }
        };
	}
	
	public static void addPermission(String commandPath, String permission) {
		createNodePath(commandPath).addPermission(permission);
	}
	
	public static void removePermission(String commandPath, String permission) {
		createNodePath(commandPath).removePermission(permission);
	}
	
	protected static void setClickCommandForPlayer(Player player, CommandNode node, ArrayList<Object> args) {
		clickCommand.put(player, node);
		clickCommandArgs.put(player, args);
	}
	
	protected static void clearClickCommandForPlayer(Player player) {
		clickCommand.remove(player);
		clickCommandArgs.remove(player);
	}
	
	private static CommandNode createNodePath(String commandPath) {
		String[] split = commandPath.split("\\.");
		StringBuilder currentPath = new StringBuilder(split[0]);
    	CommandNode parent = baseNodes.get(split[0]);
    	if (parent == null) {
    		parent = new CommandNode(0, split[0], split[0]);
    		baseNodes.put(split[0], parent);
    	}
    	for (int i = 1; i < split.length; i++) {
    		currentPath.append('.');
    		currentPath.append(split[i]);
    		CommandNode currentNode = parent.getChild(split[i]);
        	if (currentNode == null) {
        		currentNode = new CommandNode(i, split[i], currentPath.toString());
        		parent.addChild(currentNode);
        	}
        	parent = currentNode;
    	}
    	return parent;
	}
	
	protected static Object castToType(String string, Class<?> type) {
		
		try {
			if (string != null) {
				if (type == byte.class || type == Byte.class) {
					return Byte.parseByte(string);
				} else if (type == short.class || type == Short.class) {
					return Short.parseShort(string);
				} else if (type == int.class || type == Integer.class) {
					return Integer.parseInt(string);
				} else if (type == long.class || type == Long.class) {
					return Long.parseLong(string);
				} else if (type == float.class || type == Float.class) {
					return Float.parseFloat(string);
				} else if (type == double.class || type == Double.class) {
					return Double.parseDouble(string);
				} else if (type ==  boolean.class || type == Boolean.class) {
					return Boolean.parseBoolean(string);
				} else if (type == String.class) {
					return string;
				} else if (type == char.class || type == Character.class) {
					if (string.length() == 1) return string.charAt(0);
				}
			} else {
				if (type == byte.class || type == Byte.class) {
					Byte byteObject = 0;
					return byteObject;
				} else if (type == short.class || type == Short.class) {
					Short shortObject = 0;
					return shortObject;
				} else if (type == int.class || type == Integer.class) {
					Integer intObject = 0;
					return intObject;
				} else if (type == long.class || type == Long.class) {
					Long longObject = 0l;
					return longObject;
				} else if (type == float.class || type == Float.class) {
					Float floatObject = 0f;
					return floatObject;
				} else if (type == double.class || type == Double.class) {
					Double doubleObject = 0d;
					return doubleObject;
				} else if (type ==  boolean.class || type == Boolean.class) {
					Boolean boolObject = false;
					return boolObject;
				} else if (type == String.class) {
					String stringObject = new String();
					return stringObject;
				} else if (type == char.class || type == Character.class) {
					Character charObject = ' ';
					return charObject;
				}
			}

			return null;
		} catch (Throwable e) {
			return null;
		}
	}
	
	protected static boolean sendCommand(Player player, String command) {
		String[] split = command.substring(1).split("\\s");
		CommandNode baseNode = baseNodes.get(split[0]);
		if (baseNode == null) return false;

    	CommandNode parent = baseNode;
    	for (int i = 1; i < split.length; i++) {
    		CommandNode child = parent.getChild(split[i]);
        	if (child == null) break;
        	parent = child;
    	}

    	if (parent.hasMethod()) {
    		int startAt = parent.getDepth() + 1;
    		String[] subArgs = new String[split.length - startAt];
			for (int i = startAt; i < split.length; i++) {
				subArgs[i - startAt] = split[i];
			}
			parent.run(player, subArgs);
    	} else {
    		parent.showUsageText(player, false);
    	}
    	return true;
	}
	
}
