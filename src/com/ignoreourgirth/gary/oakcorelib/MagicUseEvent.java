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

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MagicUseEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	
	private int spellID;
	private int level;
	private Player caster;
	private Entity entity;	
	private Location location;
	
	public int getSpellID() {return spellID;}
	public int getSpellLevel() {return level;}
	public Player getCaster() {return caster;}
	public Entity getEntity() {return entity;}
	public Location getLocation() {return location;}
	
    public MagicUseEvent(int id, int spellLevel, Player whoCasted, Entity targetEntity, Location targetLocation) {
    	spellID = id;
    	level = spellLevel;
    	caster = whoCasted;
        entity = targetEntity;
        location = targetLocation;
    }
 
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
 
    public static HandlerList getHandlerList() {
        return handlers;
    }

}
