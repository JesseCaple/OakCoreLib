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

import com.google.common.collect.HashMultimap;

public class ProximityDetection implements Listener {

	HashMultimap<Location,Integer> regions;
	HashMap<Integer,List<Location>> lookup;
	int nextID;
	
	protected ProximityDetection() {
		regions = HashMultimap.create();
		lookup = new HashMap<Integer,List<Location>>();
		nextID = 0;
	}
	
	public int add(Location base, int radius) {
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
		lookup.put(nextID, locationsAdded);
		return nextID;
	}
	
	public void remove(int ID) {
		List<Location> regionLocation = lookup.get(ID);
		Iterator<Location> iterator = regionLocation.iterator();
		while (iterator.hasNext()) {
			regions.get(iterator.next()).remove(ID);
		}
		lookup.remove(ID);
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
