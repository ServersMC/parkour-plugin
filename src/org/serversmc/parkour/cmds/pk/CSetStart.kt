package org.serversmc.parkour.cmds.pk

import org.bukkit.command.*
import org.bukkit.entity.*
import org.bukkit.event.*
import org.bukkit.event.block.*
import org.bukkit.event.player.*
import org.bukkit.inventory.*
import org.bukkit.permissions.*
import org.serversmc.parkour.cmds.*
import org.serversmc.parkour.enums.*
import org.serversmc.parkour.interfaces.*
import org.serversmc.parkour.objects.*
import org.serversmc.parkour.utils.*

object CSetStart : ICommand, ITrackedEvent {
	
	override val start: String get() = "${GREEN}Click on a pressure plate to select new start position"
	override val canceled: String get() = "${RED}Canceled start position set"
	override val inUse: String get() = "${GRAY}${EventTracker.getPlayer(this)!!.name} ${RED}is already setting a start position!"
	
	override fun execute(sender: CommandSender, args: MutableList<out String>) {
		// Initialize variables
		val player: Player = sender as? Player ?: throw(ICommand.PlayerOnlyCommand("Please select a course, using course_id"))
		// Register player to EventTracker
		registerPlayer(player, args)
	}
	
	@EventHandler
	@Suppress("warnings")
	fun onPlayerInteract(event: PlayerInteractEvent) {
		// Check if player is in tracker
		if (!EventTracker.containsPlayer(event.player)) return
		// Cancel event
		event.isCancelled = true
		// Initialize Variables
		val player = event.player
		val block = event.clickedBlock ?: return
		// Check if action is a left/right click block
		if (event.action != Action.LEFT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_BLOCK) return
		// Fix duplicate click trigger
		if (event.hand != EquipmentSlot.HAND) return
		// Check if block is a pressure plate
		if (block.type.toString().endsWith("_PLATE", true)) {
			// Initialize course variable
			val course = EventTracker.getTracker(player)!!.course
			// Check if plate is already a sensor
			if (CourseManager.isSensor(block.location)) {
				// Initialize sensor variable
				val (parent, sensor) = CourseManager.getSensor(block.location)!!
				// Check if sensor is part of selected course
				if (course != parent) {
					// Prompt message
					player.sendMessage("${RED}This block is part of course '${GRAY}${parent.getName()}${RED}'!")
					EventTracker.remove(player, true)
					return
				}
				// Switch case sensor type
				when (sensor.getType()) {
					CSensor.Type.START -> player.sendMessage("${YELLOW}This is already the start position!")
					CSensor.Type.FINISH -> player.sendMessage("${RED}This block is the finish position!")
					CSensor.Type.CHECKPOINT -> player.sendMessage("${RED}This block is a checkpoint!")
				}
				EventTracker.remove(player, true)
				return
			}
			// Update course start position and prompt message
			course.setStartSensor(block.location)
			player.sendMessage("${GREEN}Updated start position for course '${GRAY}${course.getName()}${RED}'!")
			EventTracker.remove(player, false)
		}
		else {
			// Prompt incorrect block clicked
			player.sendMessage("${RED}Please select a ${GRAY}Pressure Plate${RED}!")
		}
	}
	
	override fun tabComplete(player: Player, args: MutableList<out String>): MutableList<String>? = ArrayList()
	override fun getLabel(): String = "SETSTART"
	override fun getPermString(): String = "parkour.setstart"
	override fun getPermDefault(): PermissionDefault = OP
	override fun getUsage(): String = "/parkour setstart <course_id>"
	override fun getDescription(): String = "Updates the course start position"
	override fun hasListener(): Boolean = true
	override fun getSubCmd(): ICommand? = CParkour
	
}