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

import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

final public class NPCUtils {

	protected NPCUtils() {}
	
	public static void sayAsNPC(Player player, String npcName, String message) {
		player.sendMessage("§a[NPC] §f" + npcName + "§a: " + message);
	}
	
	public static boolean isNear(Player player, String npcName) {
		return isNear(player, npcName, null);
	}
	
	public static boolean isNear(Player player, String[] npcNames) {
		return isNear(player, npcNames, null);
	}
	
	public static boolean isNear(Player player, String npcName, String messageOnNotFound) {
		return isNear(player, new String[] {npcName}, messageOnNotFound);
	}
	
	public static boolean isNear(Player player, String[] npcNames, String messageOnNotFound) {
		for(Entity entity : player.getNearbyEntities(6, 4, 6)) {
		    if(entity instanceof HumanEntity)
		    {
		    	HumanEntity humanEntity = (HumanEntity) entity;
		    	for (String name : npcNames) {
		    		if (humanEntity.getName().substring(2).equals(name)) return true;
		    	}
		    }
		}
		if (messageOnNotFound != null) player.sendMessage(messageOnNotFound);
		return false;
	}
	
}
