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
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

import com.google.common.collect.HashMultimap;

public class ProximityDetection implements Listener {

	private static HashMultimap<Location,Integer> regions;
	private static HashMultimap<Plugin,Integer> pluginIDs;
	private static HashMap<Integer,List<Location>> lookupA;
	private static HashMap<Integer,Plugin> lookupB;
	
	private static int nextID;
	
	protected ProximityDetection() {}
	
	static {
		regions = HashMultimap.create();
		pluginIDs = HashMultimap.create();
		lookupA = new HashMap<Integer,List<Location>>();
		lookupB = new HashMap<Integer,Plugin>();
		nextID = 0;
	}
	
	public static int add(Plugin plugin, Location base, int radius) {
		nextID++;
		World world = base.getWorld();
		ArrayList<Location> locationsAdded = new ArrayList<Location>(); 
		int baseX = (int) Math.round(base.getX());
		int baseY = (int) Math.round(base.getY());
		int baseZ = (int) Math.round(base.getZ());
		int minX = baseX - radius;
		int maxX = baseX + radius;
		int minY = baseY - radius;
		int maxY = baseY + radius;
		int minZ = baseZ - radius;
		int maxZ = baseZ + radius;
		for (int x=minX; x <= maxX; x++) {
			for (int y=minY; y <= maxY; y++) {
				for (int z=minZ; z <= maxZ; z++) {
					Location nextLocation = new Location(world, x, y, z);
					regions.put(nextLocation, nextID);
					locationsAdded.add(nextLocation);
				}
			}
		}
 		pluginIDs.put(plugin, nextID);
		lookupA.put(nextID, locationsAdded);
		lookupB.put(nextID, plugin);
		return nextID;
	}
	
	public static void removeAll(Plugin plugin) {
		Set<Integer> ids = pluginIDs.get(plugin);
		if (ids == null) return;
		Iterator<Integer> iteratorA = ids.iterator();
		while (iteratorA.hasNext()) {
			int ID = iteratorA.next();
			List<Location> regionLocation = lookupA.get(ID);
			Iterator<Location> iteratorB = regionLocation.iterator();
			while (iteratorB.hasNext()) {
				regions.get(iteratorB.next()).remove(ID);
			}
			lookupA.remove(ID);
			lookupB.remove(ID);
		}
		pluginIDs.removeAll(plugin);
	}
	
	public static void remove(int ID) {
		Plugin plugin = lookupB.get(ID);
		List<Location> regionLocation = lookupA.get(ID);
		Iterator<Location> iterator = regionLocation.iterator();
		while (iterator.hasNext()) {
			regions.get(iterator.next()).remove(ID);
		}
		lookupA.remove(ID);
		lookupB.remove(ID);
		pluginIDs.remove(plugin, ID);
	}
	
	
	@EventHandler (priority=EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		Location location = event.getTo().getBlock().getLocation();
		if (regions.containsKey(location)) {
			Set<Integer> idSet = regions.get(location);
			Iterator<Integer> iterator = idSet.iterator();
			while (iterator.hasNext()) {
				Bukkit.getServer().getPluginManager().callEvent(
						new ProximityEvent(iterator.next(), event.getPlayer()));
			}
		}
	}
	
}
