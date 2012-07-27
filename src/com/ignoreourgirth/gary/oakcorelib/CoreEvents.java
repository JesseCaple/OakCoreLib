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

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;

class CoreEvents implements Listener {

	protected CoreEvents() {}
	
	@EventHandler (priority = EventPriority.LOW)
	public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (event.isCancelled()) return;
		if (CommandPreprocessor.sendCommand(event.getPlayer(), event.getMessage())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler (priority = EventPriority.LOWEST)
	public void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (CommandPreprocessor.clickCommand.containsKey(player)) {
			boolean leftClick = (event.getAction() == Action.LEFT_CLICK_BLOCK);
			boolean rightClick = (event.getAction() == Action.RIGHT_CLICK_BLOCK);
			boolean click = (leftClick || rightClick);
			if (click) {
				Location location = event.getClickedBlock().getLocation();
				ArrayList<Object> args = CommandPreprocessor.clickCommandArgs.get(player);
				CommandNode node = CommandPreprocessor.clickCommand.get(player);
				CommandPreprocessor.clearClickCommandForPlayer(player);
				node.runClickCommand(args, location);
				event.setCancelled(true);
			}	
		}
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (event.isCancelled()) return;
		if (event.getSpawnReason() == SpawnReason.SPAWNER) {
			OakCoreLib.mobSpawnerMobs.add(event.getEntity());
		}
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onEntityDeath(EntityDeathEvent event) {
		if (OakCoreLib.mobSpawnerMobs.contains(event.getEntity())) 
				OakCoreLib.mobSpawnerMobs.remove(event.getEntity());
	}
	
}
