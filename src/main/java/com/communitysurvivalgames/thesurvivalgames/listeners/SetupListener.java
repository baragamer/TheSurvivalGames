/**
 * Name: SetupListener.java Created: 7 December 2013
 *
 * @version 1.0.0
 */
package com.communitysurvivalgames.thesurvivalgames.listeners;

import com.communitysurvivalgames.thesurvivalgames.managers.ArenaManager;
import com.communitysurvivalgames.thesurvivalgames.objects.SGArena;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class SetupListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (ArenaManager.getManager().getCreators().containsKey(e.getPlayer().getName()) && e.getAction() == Action.LEFT_CLICK_BLOCK) {
            SGArena a = ArenaManager.getManager().getCreators().get(e.getPlayer().getName());

            a.nextSpawn(e.getClickedBlock().getLocation());
            e.getPlayer().sendMessage(ArenaManager.getManager().prefix + "Spawn " + a.locs.size() + " set!");
        }
    }

}