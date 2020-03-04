package org.serversmc.parkour.cmds.pk

import org.bukkit.command.*
import org.bukkit.entity.*
import org.bukkit.permissions.*
import org.serversmc.parkour.cmds.*
import org.serversmc.parkour.enums.*
import org.serversmc.parkour.interfaces.*
import org.serversmc.parkour.objects.*
import org.serversmc.parkour.utils.*

object CAddRegion : ICommand {
	
	// TODO - Add cave regions
	
	private val session = HashMap<Player, CRegion>()
	
	override fun execute(sender: CommandSender, args: MutableList<out String>) {
		// Initialize variables
		val player = sender as? Player ?: throw(ICommand.PlayerOnlyCommand())
		val course = SelectManager.get(player) ?: run {
			ErrorMessenger.noCourseSelect(sender)
			return
		}
		// Check if player is already in session
		if (session.contains(player)) {
			// Cancel session
			player.sendMessage("${AQUA}Finished region editor")
			session.remove(player)
			return
		}
		// Prompt messages
		player.sendMessage("${AQUA}Breaking a block will remove the block from region")
		player.sendMessage("${AQUA}Placing a block will add the block to the region")
		player.sendMessage("${GRAY}${getUsage()} test ${AQUA}will flash the region")
		player.sendMessage("${GRAY}${getUsage()}${AQUA}will save and close the editor")
		// Add to session and create region
		session[player] = course.createRegion()
	}
	
	override fun tabComplete(player: Player, args: MutableList<out String>): MutableList<String>? = ArrayList()
	override fun getLabel(): String = "ADDREGION"
	override fun getPermString(): String = "parkour.addregion"
	override fun getPermDefault(): PermissionDefault = OP
	override fun getUsage(): String = "/pk addregion"
	override fun getDescription(): String = "Adds a region to the course"
	override fun hasListener(): Boolean = true
	override fun getSubCmd(): ICommand? = CParkour
	
}