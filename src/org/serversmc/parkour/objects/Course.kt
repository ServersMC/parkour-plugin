package org.serversmc.parkour.objects

import org.bukkit.*
import org.bukkit.configuration.file.*
import org.bukkit.entity.*
import org.serversmc.parkour.interfaces.*
import org.serversmc.parkour.utils.*
import org.serversmc.parkour.utils.Console
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.*
class Course(private val file: File) {
	
	enum class Mode {
		OPEN, CLOSED
	}
	
	private val viewDistance = 3
	
	private val players = HashMap<Player, PlayerData>()
	private val regions = ArrayList<CRegion>()
	private val sensors = ArrayList<CSensor>()
	
	private var plays = 0
	private var wins = 0
	
	private lateinit var author: UUID
	private val spawn: CSpawn = CSpawn()
	private var name = file.nameWithoutExtension
	private var mode = Mode.CLOSED
	
	fun load() {
		// Check if file exists
		if (file.exists()) {
			// Load yaml file
			val yaml = YamlConfiguration.loadConfiguration(file)
			// Initialize attributes
			name = yaml.getString("name")!!
			spawn.setLocation(yaml.get("spawn") as? Location)
			mode = Mode.valueOf(yaml.getString("mode")!!)
			author = UUID.fromString(yaml.getString("author")!!)
			// Try to load regions
			val yamlRegions = yaml.getConfigurationSection("regions")
			yamlRegions?.getKeys(false)?.forEach {
				// Check if section is valid
				if (it == null) return@forEach
				val section = yamlRegions.getConfigurationSection(it) ?: return@forEach
				// Load region
				regions.add(CRegion().apply { load(section) })
			}
			// Try to load sensors
			val yamlSensors = yaml.getConfigurationSection("sensors")
			yamlSensors?.getKeys(false)?.forEach {
				// Check if section is valid
				if (it == null) return@forEach
				val section = yamlSensors.getConfigurationSection(it) ?: return@forEach
				// Load sensor
				sensors.add(CSensor().apply { load(section) })
			}
		}
		else {
			// Create new data file
			file.createNewFile()
		}
		// Validate if course isReady
		if (mode == Mode.OPEN) {
			if (!isReady()) {
				Console.warn("Course: \"$name\" loaded as open, but does not qualify as ready.")
				mode = Mode.CLOSED
			}
		}
		// Show / Hide depending on mode
		if (mode == Mode.OPEN) hide()
		else show()
	}
	
	/*************/
	/** PLAYERS **/
	/*************/
	
	fun getPlayers(): ArrayList<Player> {
		return ArrayList<Player>().apply {
			players.forEach { (t, _) ->
				add(t)
			}
		}
	}
	
	fun addPlayer(player: Player) {
		players[player] = PlayerData()
		updateVisibility()
	}
	
	fun removePlayer(player: Player) {
		players.remove(player)
		updateVisibility()
	}
	
	fun hasPlayer(player: Player) = players.containsKey(player)
	
	fun getPlayerPos(player: Player) = players[player]!!.getPosition()
	
	fun setPlayerPos(player: Player, id: Int) {
		players[player]!!.setPosition(id)
		updateVisibility()
	}
	
	fun getPlayerCheckpoint(player: Player) = players[player]!!.getCheckpoint()
	
	fun setPlayerCheckpoint(player: Player, sensor: CSensor) {
		players[player]!!.setCheckpoint(sensor)
	}
	
	/*************/
	/** REGIONS **/
	/*************/
	
	fun getRegions() = regions
	
	fun getRegion(index: Int) = regions.getOrNull(index)
	
	fun createRegion(world: World): CRegion {
		return CRegion().apply {
			setId(regions.size)
			setWorld(world)
			regions.add(this)
		}
	}
	
	fun removeRegion(region: CRegion) {
		regions.remove(region)
		region.hideHologram()
		regions.forEachIndexed { index, cRegion ->
			cRegion.setId(index)
			cRegion.updateHoloName()
		}
	}
	
	/*************/
	/** SENSORS **/
	/*************/
	
	fun getSensors() = sensors
	
	fun hasSensor(location: Location): Boolean {
		sensors.forEach { if (it.getLocation() == location) return true }
		return false
	}
	
	fun getSensor(location: Location) = sensors.singleOrNull { it.getLocation() == location }
	
	fun getStartSensor() = sensors.singleOrNull { it.getType() == CSensor.Type.START }
	
	fun setStartSensor(location: Location?) {
		sensors.singleOrNull { it.getType() == CSensor.Type.START }?.apply {
			this.hideHologram()
			sensors.remove(this)
		}
		if (location == null) return
		sensors.add(CSensor.create(CSensor.Type.START, location).apply { showHologram() })
	}
	
	fun getCheckpoints() = sensors.filter { it.getType() == CSensor.Type.CHECKPOINT }
	
	fun addCheckpoint(location: Location) {
		sensors.add(CSensor.create(CSensor.Type.CHECKPOINT, location).apply { showHologram() })
	}
	
	fun getFinishSensor() = sensors.singleOrNull { it.getType() == CSensor.Type.FINISH }
	
	fun setFinishSensor(location: Location?) {
		sensors.singleOrNull { it.getType() == CSensor.Type.FINISH }?.apply {
			hideHologram()
			sensors.remove(this)
		}
		if (location == null) return
		sensors.add(CSensor.create(CSensor.Type.FINISH, location).apply { showHologram() })
	}
	
	/***********************/
	/** GETTERS / SETTERS **/
	/***********************/
	
	fun getId() = CourseManager.getCourses().indexOf(this)
	
	fun getViewDistance() = viewDistance
	
	fun getSpawn() = spawn.getLocation()!!
	
	fun setSpawn(location: Location) {
		val tempLocation = location.clone().apply {
			x = location.blockX + 0.5
			y = location.blockY + 0.5
			z = location.blockZ + 0.5
			pitch = ((location.pitch / 15).roundToInt() * 15f)
			yaw = (location.yaw / 15).roundToInt() * 15f
		}
		spawn.setLocation(tempLocation.clone())
		updateHolograms()
	}
	
	fun getName() = name
	
	fun setName(name: String) {
		this.name = name
	}
	
	fun getMode() = mode
	
	fun setOpen() {
		mode = Mode.OPEN
		SelectManager.get(this).forEach {
			SelectManager.remove(it)
		}
		hide()
	}
	
	fun isOpen() = (mode == Mode.OPEN)
	
	fun setClosed() {
		mode = Mode.CLOSED
		players.forEach {
			it.key.teleport(getSpawn())
		}
		players.clear()
		show()
	}
	
	fun getAuthor() = author
	
	fun setAuthor(player: Player) {
		author = player.uniqueId
	}
	
	fun getPlays() = plays
	
	fun getWins() = wins
	
	fun getAveragePlayTime(): Long {
		// TODO - Return average time it takes to complete
		return 0
	}
	
	fun isReady(): Boolean {
		return when {
			(spawn.getLocation() == null) -> false
			(getStartSensor() == null) -> false
			(getFinishSensor() == null) -> false
			(regions.isEmpty()) -> false
			else -> true
		}
	}
	
	/***************/
	/** MODIFIERS **/
	/***************/
	
	private fun showHolograms() {
		sensors.forEach { (it as IHologram).showHologram() }
		regions.forEach { (it as IHologram).showHologram() }
		spawn.showHologram()
	}
	
	private fun hideHolograms() {
		sensors.forEach { (it as IHologram).hideHologram() }
		regions.forEach { (it as IHologram).hideHologram() }
		spawn.hideHologram()
	}
	
	fun updateHolograms() {
		if (isOpen()) {
			hideHolograms()
			return
		}
		if (SelectManager.contains(this)) {
			showHolograms()
		}
		else {
			hideHolograms()
		}
	}
	
	private fun updateVisibility() {
		val show = ArrayList<Int>()
		players.values.forEach {
			for (i in 0..viewDistance) show.add(it.getPosition() + i)
		}
		regions.forEach {
			if (show.contains(it.getId())) it.show()
			else it.hide()
		}
	}
	
	fun save() {
		// Initialize variables
		val yaml = YamlConfiguration()
		// Add course attributes
		yaml.set("name", name)
		yaml.set("spawn", spawn.getLocation())
		yaml.set("mode", mode.name)
		yaml.set("author", author.toString())
		// Save Sensors
		sensors.forEachIndexed { id, sensor ->
			yaml.set("sensors.$id.type", sensor.getType().name)
			yaml.set("sensors.$id.loc", sensor.getLocation())
		}
		// Save Regions
		regions.forEachIndexed { regionId, region ->
			yaml.set("regions.$regionId.world", region.getWorld().uid.toString())
			region.getBlocks().forEachIndexed { blockId, b ->
				yaml.set("regions.$regionId.$blockId.loc", b.getLocation())
				yaml.set("regions.$regionId.$blockId.data", b.getBlockData().getAsString(true))
			}
		}
		// Save file
		yaml.save(file)
		hideHolograms()
	}
	
	fun delete() {
		file.delete()
		hideHolograms()
	}
	
	fun show() {
		regions.forEach { it.show() }
	}
	
	fun hide() {
		regions.forEach { it.hide() }
	}
	
}

