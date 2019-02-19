package com.mcblox.parkour.cmds.parkour;

import static org.bukkit.ChatColor.*;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import com.mcblox.parkour.objects.BloxCommand;
import com.mcblox.parkour.objects.Course;
import com.mcblox.parkour.utils.CourseSelect;

public class CmdSetFinish extends BloxCommand implements Listener {

	private static List<Player> session = new ArrayList<Player>();
	
	@Override
	public void execute(CommandSender sender, String[] args) {
		
		// Initialize variables
		Player player = (Player) sender;
		
		// Check if course is selected
		if (!CourseSelect.contains(player)) {
			CourseSelect.noSelectionMessage(player);
			return;
		}
		
		// Check if player cancelled session
		if (session.contains(player)) {
			player.sendMessage(GREEN + "Session canceled.");
			session.remove(player);
			return;
		}
		
		// Ask to select block
		player.sendMessage(YELLOW + "[" + GOLD + "LEFT CLICK" + YELLOW + "] the finishing pressure plate...");
		player.sendMessage(YELLOW + "Type \"/parkour setfinish\" to cancel session.");

		
		// Add player to session
		session.add(player);
		
		//-- End: execute(CommandSender, String[])
	}

	@Override
	public List<String> tabComplete(Player player, String[] args) {
		return null;
	}
	
	@EventHandler
	public void onInteractEvent(PlayerInteractEvent event) {
		
		// Check if action is a click
		if (event.getAction().equals(Action.PHYSICAL)) {
			return;
		}
		
		// Initialize variables
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		Course course;

		// Fix double right-click
		if (event.getHand().equals(EquipmentSlot.OFF_HAND)) {
			return;
		}

		// Check if player is in session
		if (!session.contains(player)) {
			return;
		}
		
		// Check if course is still selected
		if (!CourseSelect.contains(player)) {
			session.remove(player);
			return;
		}
		
		// Declare course
		course = CourseSelect.get(player);

		// cancel event to prevent block breaking
		event.setCancelled(true);
		
		// Check if block is a pressure plate
		if (!block.getType().name().endsWith("_PLATE")) {
			player.sendMessage(RED + "Please select a pressure plate!");
			return;
		}
		
		// Check if block is start block
		if (course.getStartBlock() != null) {
			if (course.getStartBlock().equals(block)) {
				player.sendMessage(RED + "Finish plate can not be the same as the start block!");
				return;
			}
		}
		
		// Prompt selected block
		player.sendMessage(GREEN + "Created finishing point for course " + GOLD + course.getName() + GREEN + "!");
		course.setFinishBlock(block);
		
		// Cancel task and session
		session.remove(player);
		
		//-- End: onInteractEvent(PlayerInteractEvent)
	}

	@Override
	public String getLabel() {
		return "SETFINISH";
	}

	@Override
	public String getPermission() {
		return "parkour.setfinish";
	}

	@Override
	public String getUsage() {
		return "/parkour setfinish";
	}

	@Override
	public String getDescription() {
		return "Adds the finishing plate to the course!";
	}

}
