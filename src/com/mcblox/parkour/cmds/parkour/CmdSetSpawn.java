package com.mcblox.parkour.cmds.parkour;

import static org.bukkit.ChatColor.*;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mcblox.parkour.objects.BloxCommand;
import com.mcblox.parkour.objects.Course;
import com.mcblox.parkour.utils.CourseSelect;

public class CmdSetSpawn extends BloxCommand {

	@Override
	public void execute(CommandSender sender, String[] args) {
		
		// Initialize variables
		Player player = (Player) sender;
		Course course = null;
		
		// Check if course is selected
		if (!CourseSelect.contains(player)) {
			CourseSelect.noSelectionMessage(player);
			return;
		}
		
		// Declare course
		course = CourseSelect.get(player);
		
		// Prompt message
		player.sendMessage(GREEN + "Updated spawn point for " + GOLD + course.getName() + GREEN + "!");
		
		// Set new spawn for course
		course.setSpawn(player.getLocation());
		
		// -- End: execute(CommandSender sender, args)
	}

	@Override
	public List<String> tabComplete(Player player, String[] args) {
		return null;
	}

	@Override
	public String getLabel() {
		return "SETSPAWN";
	}

	@Override
	public String getPermission() {
		return "parkour.setspawn";
	}

	@Override
	public String getUsage() {
		return "/parkour setspawn";
	}

	@Override
	public String getDescription() {
		return "Sets the spawn of a course.";
	}

}
