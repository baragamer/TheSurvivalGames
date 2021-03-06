/**
 * Name: SGArena.java Edited: 7 December 2013
 *
 * @version 1.0.0
 */
package com.communitysurvivalgames.thesurvivalgames.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;

import com.communitysurvivalgames.thesurvivalgames.locale.I18N;
import com.communitysurvivalgames.thesurvivalgames.managers.SGApi;
import com.communitysurvivalgames.thesurvivalgames.multiworld.SGWorld;
import com.communitysurvivalgames.thesurvivalgames.rollback.ChangedBlock;
import com.communitysurvivalgames.thesurvivalgames.util.FireworkUtil;

public class SGArena {

	private ArenaState currentState;
	private int id = 0;

	public Location lobby = null;
	public SGWorld currentMap;

	public Map<MapHash, Integer> votes = new HashMap<>();
	public Map<String, Integer> kills = new HashMap<String, Integer>();

	public int maxPlayers;
	public int minPlayers;
	public int dead;

	public final List<String> players = new CopyOnWriteArrayList<>();
	public final List<String> spectators = new CopyOnWriteArrayList<>();

	public List<String> voted = new ArrayList<>();

	public List<ChangedBlock> changedBlocks = new ArrayList<ChangedBlock>();
	public List<Chest> looted = new ArrayList<Chest>();
	public List<DoubleChest> dLooted = new ArrayList<DoubleChest>();

	public boolean countdown = false;

	/**
	 * Name: ArenaState.java Edited: 8 December 2013
	 * 
	 * 
	 * @version 1.0.0
	 */
	public enum ArenaState {

		WAITING_FOR_PLAYERS("Waiting for players", "WAITING_FOR_PLAYERS"), PRE_COUNTDOWN("Starting soon", "PRE_COUNTDOWN"), STARTING_COUNTDOWN("Starting Countdown", "STARTING_COUNTDOWN"), IN_GAME("In Game", "IN_GAME"), DEATHMATCH("Deathmatch", "DEATHMATCH"), POST_GAME("Restarting", "POST_GAME");

		String name;
		String trueName;

		ArenaState(String s, String t) {
			name = s;
			trueName = t;
		}

		public boolean isConvertable(SGArena arena, ArenaState a) {
			if (a.equals(WAITING_FOR_PLAYERS)) {
				if (arena.getState().equals(WAITING_FOR_PLAYERS)) {
					return false;
				}
			}

			if (a.equals(STARTING_COUNTDOWN)) {
				if (arena.getState().equals(WAITING_FOR_PLAYERS)) {
					return false;
				} else if (arena.getState().equals(STARTING_COUNTDOWN)) {
					return false;
				}
			}

			if (a.equals(DEATHMATCH)) {
				if (arena.getState().equals(WAITING_FOR_PLAYERS)) {
					return false;
				} else if (arena.getState().equals(STARTING_COUNTDOWN)) {
					return false;
				} else if (arena.getState().equals(IN_GAME)) {
					return false;
				} else if (arena.getState().equals(DEATHMATCH)) {
					return false;
				}
			}

			if (a.equals(IN_GAME)) {
				if (!arena.getState().equals(POST_GAME)) {
					return false;
				}
			}

			return true;
		}

		public String toString() {
			return name;
		}

		public String getTrueName() {
			return trueName;
		}
	}

	/**
	 * Constructs a new arena based off of a Location and an ID
	 * 
	 * @param id The ID the arena will have
	 */
	public void createArena(int id) {
		this.id = id;
	}

	public SGArena() {
	}

	/**
	 * Makes sure that the fields aren't null on startup
	 *
	 * @param lob The lobby spawn
	 * @param maxPlayers The max players for the arena
	 * @param minPlayers The min players needed for the game to start
	 */
	public void initialize(Location lob, int maxPlayers, int minPlayers) {
		this.lobby = lob;
		this.maxPlayers = maxPlayers;
		this.minPlayers = minPlayers;

		restart();
	}

	/**
	 * Sends all the players a message
	 * 
	 * @param message The message to send, do not include prefix
	 */
	public void broadcast(String message) {
		for (String s : players) {
			Player p = Bukkit.getServer().getPlayerExact(s);
			if (p != null) {
				p.sendMessage(SGApi.getArenaManager().prefix + message);
			}
		}

		for (String s : spectators) {
			Player p = Bukkit.getServer().getPlayerExact(s);
			if (p != null) {
				p.sendMessage(SGApi.getArenaManager().prefix + message);
			}
		}
	}

	/**
	 * Puts the arena into deathmatch
	 */
	public void dm() {
		int i = 0;
		for (String s : players) {
			Player p;
			if ((p = Bukkit.getServer().getPlayer(s)) != null) {
				p.teleport(currentMap.locs.get(i));
				i++;
			}
		}
	}

	/**
	 * Ends the arena
	 */
	public void end() {
		if (players.size() == 1) {
			broadcast(SGApi.getArenaManager().prefix + I18N.getLocaleString("END") + " " + players.get(0));
			Player winner = Bukkit.getPlayer(players.get(0));
			PlayerData data = SGApi.getPlugin().getPlayerData(winner);
			data.addWin();
			data.addPoints(100);
			SGApi.getPlugin().setPlayerData(data);
			winner.sendMessage(ChatColor.GOLD + "Plus 100 coins!");

			FireworkUtil.getCircleUtil().playFireworkCircle(winner, FireworkEffect.builder().with(Type.BALL).withColor(Color.RED).withColor(Color.GREEN).withColor(Color.BLUE).withColor(Color.YELLOW).withTrail().build(), 10, 10);
		} else {
			broadcast(SGApi.getArenaManager().prefix + I18N.getLocaleString("ARENA_END"));
		}
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(SGApi.getPlugin(), new Runnable() {

			@Override
			public void run() {
				for (String s : players) {
					SGApi.getArenaManager().removePlayer(Bukkit.getPlayer(s));
				}
				for (String s : spectators) {
					SGApi.getArenaManager().removePlayer(Bukkit.getPlayer(s));
				}
				voted.clear();
				votes.clear();

				setState(ArenaState.POST_GAME);
				SGApi.getRollbackManager().rollbackArena(getThis());
			}
		}, 200L);

		//Auto restarts after rollback
	}

	/**
	 * Sets the state of the SG arena
	 * 
	 * @param state - The new state
	 */
	public void setState(ArenaState state) {
		currentState = state;
	}

	/**
	 * Gets the ID of the arena
	 * 
	 * @return The ID of the arena
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Gets the list of players in the arena
	 * 
	 * @return List of players in the arena
	 */
	public List<String> getPlayers() {
		return this.players;
	}

	/**
	 * Makes a player vote
	 * 
	 * @param p the voter
	 * @param i the map number
	 */
	public void vote(Player p, int i) {
		if (currentState != ArenaState.WAITING_FOR_PLAYERS && currentState != ArenaState.PRE_COUNTDOWN) {
			p.sendMessage(SGApi.getArenaManager().error + I18N.getLocaleString("NOT_VOTING"));
			return;
		}

		if (voted.contains(p.getName())) {
			p.sendMessage(ChatColor.RED + "You have alredy voted!");
			return;
		}

		MapHash voteWorld = null;
		for (Map.Entry<MapHash, Integer> e : votes.entrySet()) {
			if (e.getKey().getId() == i) {
				Bukkit.getLogger().info("Attempting to vote for world: " + e.getKey().getWorld() + " with a value of: " + e.getValue() + " and an input number of: " + i);
				voteWorld = e.getKey();
			}
		}
		if (voteWorld == null)
			return;
		votes.put(voteWorld, votes.get(voteWorld) + 1);
		this.broadcast(ChatColor.GOLD + p.getDisplayName() + " has voted! Use /vote to cast your vote!");
		for (Map.Entry<MapHash, Integer> entry : votes.entrySet()) {
			broadcast(ChatColor.GOLD.toString() + entry.getKey().getId() + ". " + ChatColor.DARK_AQUA.toString() + entry.getKey().getWorld().getDisplayName() + ": " + ChatColor.GREEN.toString() + entry.getValue());
		}
		voted.add(p.getName());
	}

	/**
	 * Gets the current state of the arena
	 * 
	 * @return The current state
	 */
	public ArenaState getState() {
		return currentState;
	}

	/**
	 * Gets the max number of players the arena will hold
	 * 
	 * @return Number of players
	 */
	public int getMaxPlayers() {
		return maxPlayers;
	}

	/**
	 * Gets the min number of players for the arena to start
	 * 
	 * @return Number of players
	 */
	public int getMinPlayers() {
		return minPlayers;
	}

	public List<String> getSpectators() {
		return spectators;
	}

	public World getArenaWorld() {
		return currentMap.getWorld();
	}

	public SGWorld getCurrentMap() {
		return currentMap;
	}

	public String toString() {
		return "SGArena.java - Id: " + this.getId() + " State: " + this.getState() + " " + "Players: " + this.players;
	}

	public void death(Player p) {
		dead++;
		getPlayers().remove(p.getName());
		getSpectators().add(p.getName());
		if (players.size() == 1)
			end();
	}
	
	public void deathAndLeave(Player p) {
		dead++;
		getPlayers().remove(p.getName());
		SGApi.getArenaManager().removePlayer(p);
		if (players.size() == 1)
			end();
	}

	public void deathWithQuit(Player p) {
		dead++;
		getPlayers().remove(p.getName());
		if (players.size() == 1)
			end();
	}

	public void addKill(Player p) {
		if (kills.get(p.getName()) == null) {
			kills.put(p.getName(), 0);
		}
		kills.put(p.getName(), kills.get(p.getName()) + 1);

		PlayerData data = SGApi.getPlugin().getPlayerData(p);
		data.addKill();
		data.addPoints(10);
		p.sendMessage(ChatColor.GOLD + "Plus 10 points!");
		SGApi.getPlugin().setPlayerData(data);
	}

	public void restart() {
		this.players.clear();
		this.spectators.clear();
		this.changedBlocks.clear();
		this.looted.clear();
		this.dLooted.clear();
		this.voted.clear();
		this.votes.clear();
		this.dead = 0;
		this.kills.clear();
		countdown = false;
		
		SGApi.getTimeManager(this).forceReset();

		this.setState(ArenaState.WAITING_FOR_PLAYERS);
	}

	SGArena getThis() {
		return this;
	}
}
