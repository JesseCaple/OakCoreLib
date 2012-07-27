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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class XPMP implements Listener {
	
	private final int startingMaxMP = 100;
	private final int mpUpdateInterval = 4;
	private final int mpPerInterval = 2;
	
	protected Hashtable<Player, Integer> MP;
	protected Hashtable<Player, Integer> maxMP;
	protected Hashtable<Player, Integer> XPLevel;
	protected Hashtable<Player, Integer> additionalXP;
	protected Hashtable<Player, Integer> blockRequests;
	
	protected XPMP() throws NotOwnerException {
		MP = new Hashtable<Player, Integer>();
		maxMP = new Hashtable<Player, Integer>();
		XPLevel = new Hashtable<Player, Integer>();
		additionalXP = new Hashtable<Player, Integer>();
		blockRequests = new Hashtable<Player, Integer>();
		Bukkit.getScheduler().scheduleSyncRepeatingTask(OakCoreLib.plugin, new Runnable() {
			   public void run() {
				   giveMPTask();
			   }
		}, 60L, mpUpdateInterval * 20);
	}
	
	public void blockMPRegen(Player player) {
		if (!blockRequests.containsKey(player)) blockRequests.put(player, 0);
		blockRequests.put(player, blockRequests.get(player) + 1);
	}
	
	public void allowMPRegen(Player player) {
		if (!blockRequests.containsKey(player)) blockRequests.put(player, 0);
		if (blockRequests.get(player) > 0) blockRequests.put(player, blockRequests.get(player) - 1);
	}
	
	public void resetMPRegenBlocks() {
		blockRequests.clear();
	}
	
	public void setTotalXP(Player player, int value) {
		if (!XPLevel.containsKey(player)) loadDBData(player);
		ExperienceManager xpManager = new ExperienceManager(player);
		XPLevel.put(player, xpManager.getLevelForExp(value));
		additionalXP.put(player, value - xpManager.getXpForLevel(XPLevel.get(player)));
		updateMCGUIXPLevel(player);
		updateMCGUIMPBar(player);
		saveDBData(player);
	}
	
	public void addXP(Player player, int value) {
		if (!XPLevel.containsKey(player)) loadDBData(player);
		setTotalXP(player, getTotalXP(player) + value);
		saveDBData(player);
	}
	
	public int getTotalXP(Player player) {
		if (!XPLevel.containsKey(player)) loadDBData(player);
		ExperienceManager xpManager = new ExperienceManager(player);
		return xpManager.getXpForLevel(XPLevel.get(player)) + additionalXP.get(player);
	}
	
	public void setTotalMP(Player player, int value) {
		int maxMP = getMaxMP(player);
		if (value > maxMP) value = maxMP;
		MP.put(player, value);
		updateMCGUIMPBar(player);
		saveDBData(player);
	}
	
	public void setMaxMP(Player player, int value) {
		maxMP.put(player, value);
		updateMCGUIMPBar(player);
		saveDBData(player);
	}
	
	public void addMP(Player player, int value) {
		if (!MP.containsKey(player)) loadDBData(player);
		setTotalMP(player, getTotalMP(player) + value);
		saveDBData(player);
	}
	
	public int getTotalMP(Player player) {
		if (!MP.containsKey(player)) loadDBData(player);
		return MP.get(player);
	}
	
	public int getMaxMP(Player player) {
		if (!MP.containsKey(player)) loadDBData(player);
		return maxMP.get(player);
	}
	
	protected void updateMCGUIXPLevel(Player player) {
		ExperienceManager xpManager = new ExperienceManager(player);
		xpManager.changeExp(-1 * xpManager.getCurrentExp());
		xpManager.changeExp(xpManager.getXpForLevel(XPLevel.get(player)));
	}
	
	protected void updateMCGUIMPBar(Player player) {
		float mpPercent = (((float) MP.get(player)) / ((float) maxMP.get(player)));
		if (mpPercent >= 1) mpPercent = 0.999F;
		player.setExp(mpPercent);
	}
	
	private void giveMPTask() {
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			if (!MP.containsKey(player)) loadDBData(player);
			if (!blockRequests.containsKey(player)) blockRequests.put(player, 0);
			if (blockRequests.get(player) == 0) {
				int newMP = MP.get(player) + mpPerInterval;
				if (newMP > maxMP.get(player)) newMP = maxMP.get(player);
				MP.put(player, newMP);
				updateMCGUIMPBar(player);
			}
		}	
	}
	
	protected void saveDBData(Player player) {
		try {
			if (!XPLevel.containsKey(player)) loadDBData(player);
			ExperienceManager xpManager = new ExperienceManager(player);
			int totalXP =  xpManager.getXpForLevel(XPLevel.get(player)) + additionalXP.get(player);
			PreparedStatement statement = OakCoreLib.getDB().prepareStatement("" +
					"INSERT INTO oakxpmp(player, mp, maxmp, totalxp) VALUES (?, ?, ?, ?)" +
					"ON DUPLICATE KEY UPDATE mp=?, maxmp=?, totalxp=?");
			statement.setString(1, player.getName());
			statement.setInt(2, MP.get(player));
			statement.setInt(3, maxMP.get(player));
			statement.setInt(4, totalXP );
			statement.setInt(5, MP.get(player));
			statement.setInt(6, maxMP.get(player));
			statement.setInt(7, totalXP );
			statement.addBatch();
			statement.executeUpdate();
			statement.close();
		} catch (SQLException ex) {
			OakCoreLib.plugin.getLogger().log(Level.SEVERE, ex.getMessage());
		}
	}
	
	protected void loadDBData(Player player) {
		try {
			if (!XPLevel.containsKey(player)) {
				ExperienceManager xpManager = new ExperienceManager(player);
				int playerLevel = xpManager.getLevelForExp(xpManager.getCurrentExp());
				additionalXP.put(player, xpManager.getCurrentExp() -  xpManager.getXpForLevel(playerLevel));
				XPLevel.put(player, xpManager.getLevelForExp(xpManager.getCurrentExp()));
			}
			PreparedStatement statement = OakCoreLib.getDB().prepareStatement("SELECT mp, maxmp, totalXP FROM oakxpmp WHERE player=?");
			statement.setString(1, player.getName());
			ResultSet result = statement.executeQuery();
			int newXPValue = 0;
			if (result.next()) {
				MP.put(player, result.getInt(1));
				maxMP.put(player, result.getInt(2));
				newXPValue = result.getInt(3);
			} else {
				MP.put(player, 0);
				maxMP.put(player, startingMaxMP);
			}
			if (!XPLevel.containsKey(player)) loadDBData(player);
			ExperienceManager xpManager = new ExperienceManager(player);
			XPLevel.put(player, xpManager.getLevelForExp(newXPValue));
			additionalXP.put(player, newXPValue  - xpManager.getXpForLevel(XPLevel.get(player)));
			updateMCGUIXPLevel(player);
			updateMCGUIMPBar(player);
			result.close();
			statement.close();
		} catch (SQLException ex) {
			OakCoreLib.plugin.getLogger().log(Level.SEVERE, ex.getMessage());
		}
	}
	
	protected void clearPlayer(Player player) {
		if (MP.containsKey(player)) MP.remove(player);
		if (maxMP.containsKey(player)) maxMP.remove(player);
		if (XPLevel.containsKey(player)) XPLevel.remove(player);
		if (additionalXP.containsKey(player)) additionalXP.remove(player);
		if (blockRequests.containsKey(player)) blockRequests.remove(player);
	}
	
	@EventHandler
	public void onExpChange(PlayerExpChangeEvent event) {
		Player eventPlayer = event.getPlayer();
		ExperienceManager xpManager = new ExperienceManager(eventPlayer);
		int curXP = additionalXP.get(eventPlayer);
		int curLevel = XPLevel.get(eventPlayer);
		int newTotal = xpManager.getXpForLevel(curLevel) + curXP + event.getAmount();
		int newLevel = xpManager.getLevelForExp(newTotal);
		additionalXP.put(eventPlayer, newTotal - xpManager.getXpForLevel(newLevel));
		XPLevel.put(eventPlayer, newLevel);
		if (curLevel != newLevel) {
			updateMCGUIXPLevel(eventPlayer);
			eventPlayer.sendMessage("§7:: Level+");
		}
		updateMCGUIMPBar(eventPlayer);
		event.setAmount(0);
	}
	
	@EventHandler
	public void onEnchantItem(EnchantItemEvent event) {
		Player player = event.getEnchanter();
		if (player.getGameMode() == GameMode.SURVIVAL) {
			XPLevel.put(event.getEnchanter(), XPLevel.get(player) - event.getExpLevelCost());
			additionalXP.put(player, 0);
			updateMCGUIXPLevel(player);
			updateMCGUIMPBar(player);
			event.setExpLevelCost(0);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();
		ExperienceManager xpManager = new ExperienceManager(player);
		int droppedExp = (int) Math.round((xpManager.getXpForLevel(XPLevel.get(player)) + additionalXP.get(player)) * 0.95);
		event.setDroppedExp(droppedExp);
		additionalXP.put(player, 0);
		XPLevel.put(player, 0);
		MP.put(player, 0);
		updateMCGUIMPBar(player);
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		loadDBData(event.getPlayer());
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		saveDBData(event.getPlayer());
		clearPlayer(event.getPlayer());
	}
	
	@EventHandler
	public void onKick(PlayerKickEvent event) {
		saveDBData(event.getPlayer());
		clearPlayer(event.getPlayer());
	}
	
}
