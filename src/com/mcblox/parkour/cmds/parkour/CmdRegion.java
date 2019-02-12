package com.mcblox.parkour.cmds.parkour;

import static org.bukkit.ChatColor.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
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
import com.mcblox.parkour.objects.CourseRegion;

public class CmdRegion extends BloxCommand implements Listener {

	private static HashMap<Player, CourseRegion> regionQueue = new HashMap<Player, CourseRegion>();
	private CourseRegion region;

	@Override
	public void execute(CommandSender sender, String[] args) {

		// Initialize variables
		Player player = (Player) sender;
		Course course = null;

		// Check if player is in session
		if (regionQueue.containsKey(player)) {

			// Check if args length is correct
			if (args.length > 1) {
				
				// Check if cancel function is called
				if (args[1].equalsIgnoreCase("cancel")) {
					
					// Prompt Message
					player.sendMessage(GREEN + "Canceled region selection");
					
					// Remove player from queue
					regionQueue.remove(player);
					
					return;
				}
				
			}

			// Prompt currently in session
			player.sendMessage(RED + "You are already in session, to cancel region type: /parkour region cancel");
			return;

		}

		// Check args length
		if (args.length == 1) {
			sender.sendMessage(RED + "Incorrect usage. " + getUsage());
			return;
		}

		// Check if id of course was given
		if (args.length == 2) {
			if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("reset")) {
				sender.sendMessage(RED + "Please give course ID.");
			} else {
				sender.sendMessage(RED + "Incorrect usage. " + getUsage());
			}
			return;
		}

		// Check if course id is valid
		try {
			course = Course.courses.get(Integer.parseInt(args[2]));
		} catch (NumberFormatException e) {
			sender.sendMessage(RED + "Please enter a number for course ID. ");
			return;
		} catch (IndexOutOfBoundsException e) {
			player.sendMessage(RED + "Course ID not found!");
			return;
		}

		// Declare Course
		region = new CourseRegion(course);

		// Check if reset function is called
		if (args[1].equalsIgnoreCase("reset")) {
			player.sendMessage(GREEN + "Reset all regions for " + GOLD + course.getName() + GREEN + " course!");
			course.show();
			course.resetRegions();
			return;
		}

		// Prompt
		player.sendMessage(YELLOW + "[" + GOLD + "LEFT CLICK" + YELLOW + "] the blocks to assign to the region");
		player.sendMessage(YELLOW + "[" + GOLD + "RIGHT CLICK" + YELLOW + "] to finish");
		player.sendMessage(YELLOW + "To cancel region type /parkour region cancel, or create an empty region");

		// Add player to queue
		regionQueue.put(player, region);

		// -- End: execute(CommandSender, String[])
	}

	@Override
	public List<String> tabComplete(Player player, String[] args) {
		List<String> list = new ArrayList<String>();
		String[] labels;
		if (regionQueue.containsKey(player)) {
			labels = new String[]{"cancel"};
		}
		else {
			labels = new String[]{"add", "reset"};
		}
		if (args.length == 2) {
			for (String label : labels) {
				if (label.startsWith(args[1])) {
					list.add(label);
				}
			}
			return list;
		}
		if (args.length == 3 && !args[1].equalsIgnoreCase("cancel")) {
			list.add(AQUA + "<id>");
			return list;
		}
		return list;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {

		// Check if action is a click
		if (event.getAction().equals(Action.PHYSICAL)) {
			return;
		}

		// Initialize variables
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		CourseRegion region;

		// Fix double right-click
		if (event.getHand().equals(EquipmentSlot.OFF_HAND)) {
			return;
		}

		// Check if player is in queue
		if (!regionQueue.containsKey(player)) {
			return;
		}

		// Cancel block break
		event.setCancelled(true);

		// Declare CourseRegion
		region = regionQueue.get(player);

		// Check if left click
		if (event.getAction().name().contains("LEFT")) {

			// Check if block is already in region
			if (region.getCourse().hasBlock(block) || region.hasRawBlock(block)) {

				// Play failed note
				player.playNote(player.getLocation(), Instrument.BASS_DRUM, Note.flat(1, Tone.C));

			}
			else {

				// Play accomplished note
				player.playNote(player.getLocation(), Instrument.CHIME, Note.natural(0, Tone.C));

				// Add raw block to region
				region.addRawBlock(block);

			}
			return;

			// -- End: if #Left Click
		}

		// Check if right click
		if (event.getAction().name().contains("RIGHT")) {

			// Brew raw blocks in region
			region.brewRawBlocks();

			// Check if region has any blocks selected
			if (region.getBlocks().size() == 0) {

				// Play failed note
				player.playNote(player.getLocation(), Instrument.CHIME, Note.flat(1, Tone.A));

				// Prompt failed message
				player.sendMessage(YELLOW + "No region has been added. No blocks selected.");

				// Remove player from session
				regionQueue.remove(player);

				return;
			}

			// Play accomplished note
			player.playNote(player.getLocation(), Instrument.CHIME, Note.natural(1, Tone.C));

			// Prompt message
			player.sendMessage(GREEN + "Region added to " + GOLD + region.getCourse().getName() + GREEN + " course with " + GOLD + region.getBlocks().size() + GREEN + " blocks!");

			// Add brewed region to course
			region.getCourse().addRegion(region);

			// Reset session
			regionQueue.replace(player, new CourseRegion(region.getCourse()));

			// -- End: if #Right Click
		}

		// -- End: onInteractEvent(PlayerInteractEvent)
	}

	@Override
	public String getLabel() {
		return "REGION";
	}

	@Override
	public String getPermission() {
		return "mcblox.parkour.region";
	}

	@Override
	public String getUsage() {
		return "/parkour region <add | reset> <courseId>";
	}

	@Override
	public String getDescription() {
		return "Add or reset the regions of a course";
	}

}
