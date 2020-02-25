package org.serversmc.parkour.events

import org.bukkit.event.*
import org.bukkit.event.player.*
import org.serversmc.parkour.utils.*

object PlayerQuit : Listener {
	
	@EventHandler
	fun onPlayerQuit(event: PlayerQuitEvent) {
		// Initialize variables
		val player = event.player
		// Try to get course player is in
		val course = CourseManager.getCourses().singleOrNull { it.hasPlayer(player) } ?: return
		// Remove player from game
		course.removePlayer(player)
		// Try to get spawn
		val spawn = course.getSpawn() ?: return
		// Teleport player to spawn
		player.teleport(spawn)
	}
	
}