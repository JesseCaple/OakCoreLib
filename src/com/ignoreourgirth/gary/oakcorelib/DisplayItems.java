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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import com.google.common.collect.HashMultimap;

public class DisplayItems implements Listener {
	
	private static final long taskTicks = 2400L;
	private static int currentItemID;

	private static HashMap<Integer, ItemStack> idLookup_ItemStack;
	private static HashMap<Integer, Location> idLookup_Location;
	private static HashMap<Integer, Item> idLookup_Item;
	private static HashMap<Integer, Location[]> idLookup_Blocks;
	private static HashMap<Integer, Location> idLookup_Chunk;
	private static HashMap<Integer, Plugin> idLookup_Plugin;
	
	private static HashMultimap<Location, Integer> blockLocations;
	private static HashMultimap<Location, Integer> chunkLocations;
	private static HashMultimap<Plugin, Integer> registeredPlugins;
	
	static {
		idLookup_ItemStack = new HashMap<Integer, ItemStack>();
		idLookup_Location = new HashMap<Integer, Location>();
		idLookup_Item = new HashMap<Integer, Item>();
		idLookup_Blocks = new HashMap<Integer, Location[]>();
		idLookup_Chunk = new HashMap<Integer, Location>();
		idLookup_Plugin = new HashMap<Integer, Plugin>();
		blockLocations = HashMultimap.create();
		chunkLocations = HashMultimap.create();
		registeredPlugins = HashMultimap.create();
	}
	
	protected DisplayItems() {
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(OakCoreLib.plugin, new Runnable() {
    		public void run() {
    			
    			Iterator<Integer> idIterator = idLookup_ItemStack.keySet().iterator();
    			while (idIterator.hasNext()) {
    				refresh(idIterator.next());
    			}
    			
    			ArrayList<Plugin> toRemove = new ArrayList<Plugin>();
    			Iterator<Plugin> pluginIterator = registeredPlugins.keySet().iterator();
    			while (pluginIterator.hasNext()) {
    				Plugin nextPlugin = pluginIterator.next();
    				if (!nextPlugin.isEnabled()) {
    					toRemove.add(nextPlugin);
    				}
    			}
    			
    			Iterator<Plugin> removalIterator = toRemove.iterator();
    			while (removalIterator.hasNext()) {
    				removeAll(removalIterator.next());
    			}

    		}
    	}, taskTicks, taskTicks);
	}
	
	public static void removeAll(Plugin plugin) {
		if (!registeredPlugins.containsKey(plugin)) return;
		Iterator<Integer> idIterator = registeredPlugins.get(plugin).iterator();
		while (idIterator.hasNext()) {
			
			int ID = idIterator.next();
			Location[] blocks = idLookup_Blocks.get(ID);
			Location chunk = idLookup_Chunk.get(ID);
			
			clear(ID);
			
			if (chunk == null) return;
			
			idLookup_ItemStack.remove(ID);
			idLookup_Location.remove(ID);
			idLookup_Blocks.remove(ID);
			idLookup_Chunk.remove(ID);
			idLookup_Plugin.remove(ID);
			
			blockLocations.get(blocks[0]).remove(ID);
			blockLocations.get(blocks[1]).remove(ID);
			chunkLocations.get(chunk).remove(ID);
			
			ProtectedLocations.remove(blocks[0]);
			ProtectedLocations.remove(blocks[1]);
		}
		registeredPlugins.removeAll(plugin);
	}
	
	public static Integer newItem(ItemStack stack, Location location, Plugin plugin) {
		
		currentItemID++;
		
		Location blockA = location.getBlock().getLocation().clone();
		Location blockB = blockA.clone().add(0,1,0);
		Location chunk = blockA.getChunk().getBlock(0, 0, 0).getLocation().clone();
		Location centered = blockB.clone().add(.5,.2,.5);
		
		idLookup_ItemStack.put(currentItemID, new ItemStack(stack.getType(), stack.getAmount()));
		idLookup_Location.put(currentItemID, centered);
		idLookup_Blocks.put(currentItemID, new Location[] {blockA, blockB});
		idLookup_Chunk.put(currentItemID, chunk);
		idLookup_Plugin.put(currentItemID, plugin);
		
		blockLocations.get(blockA).add(currentItemID);
		blockLocations.get(blockB).add(currentItemID);
		chunkLocations.get(chunk).add(currentItemID);
		registeredPlugins.get(plugin).add(currentItemID);
		
		ProtectedLocations.add(blockA, true);
		ProtectedLocations.add(blockB, true);
		
		refresh(currentItemID);

		return currentItemID;
	}
	
	public static void removeItem(Integer ID) {
		
		Location[] blocks = idLookup_Blocks.get(ID);
		Location chunk = idLookup_Chunk.get(ID);
		Plugin plugin = idLookup_Plugin.get(ID);
		
		clear(ID);
		
		if (chunk == null) return;
		
		idLookup_ItemStack.remove(ID);
		idLookup_Location.remove(ID);
		idLookup_Blocks.remove(ID);
		idLookup_Chunk.remove(ID);
		idLookup_Plugin.remove(ID);
		
		blockLocations.get(blocks[0]).remove(ID);
		blockLocations.get(blocks[1]).remove(ID);
		chunkLocations.get(chunk).remove(ID);
		registeredPlugins.get(plugin).remove(ID);
		
		ProtectedLocations.remove(blocks[0]);
		ProtectedLocations.remove(blocks[1]);
	}
	
	public static void refresh(Integer ID) {
		clear(ID);
		ItemStack stack = idLookup_ItemStack.get(ID);
		Location location = idLookup_Location.get(ID);
		Item newItem = location.getWorld().dropItem(location, stack.clone());
		newItem.setVelocity(new Vector(0,0.01,0));
		newItem.setPickupDelay(Integer.MAX_VALUE);
		idLookup_Item.put(ID, newItem);
	}
	
	private static void clear(Integer ID) {
		Item item = idLookup_Item.get(ID);
		if (item != null) {
			item.remove();
			idLookup_Item.remove(ID);
		}
	}
	
	@EventHandler (priority=EventPriority.MONITOR, ignoreCancelled = true)
	public void onChunkLoad(ChunkLoadEvent event) {
		Location location =  event.getChunk().getBlock(0, 0, 0).getLocation();
		if (chunkLocations.containsKey(location)) {
			Iterator<Integer> iterator = chunkLocations.get(location).iterator();
			while (iterator.hasNext()) {
				refresh(iterator.next());
			}
		}
	}
	
	@EventHandler (priority=EventPriority.HIGHEST, ignoreCancelled = true)
	public void onChunkUnload(ChunkUnloadEvent event) {
		Location location =  event.getChunk().getBlock(0, 0, 0).getLocation();
		if (chunkLocations.containsKey(location)) {
			event.setCancelled(true);
		}
	}

}
