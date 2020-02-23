package org.serversmc.parkour.objects

import org.bukkit.*
import org.bukkit.configuration.file.*
import org.bukkit.entity.*
import java.io.*

class Course(private val file: File) {
	
	enum class Mode {
		OPEN, CLOSED, DISABLED
	}
	
	private val players = HashMap<Player, PlayerData>()
	private val regions = ArrayList<CRegion>()
	private val sensors = ArrayList<CSensor>()
	
	private var plays = 0
	private var wins = 0
	
	private var name = file.nameWithoutExtension
	private var mode = Mode.CLOSED
	private var author = "ServersMC"
	
	init {
		// Check if file exists
		if (file.exists()) {
			// Load yaml file
			val yaml = YamlConfiguration.loadConfiguration(file)
			// Initialize attributes
			name = yaml.getString("name")!!
			mode = Mode.valueOf(yaml.getString("mode")!!)
			author = yaml.getString("author")!!
			// Try to load sensors
			val yamlSensors = yaml.getConfigurationSection("sections")
			yamlSensors?.getKeys(false)?.forEach {
				// Check if section is valid
				if (it == null) return@forEach
				val section = yamlSensors.getConfigurationSection(it) ?: return@forEach
				// Load sensor
				sensors.add(CSensor().apply { load(section) })
			}
			// Try to load regions
			val yamlRegions = yaml.getConfigurationSection("regions")
			yamlRegions?.getKeys(false)?.forEach {
				// Check if section is valid
				if (it == null) return@forEach
				val section = yamlRegions.getConfigurationSection(it) ?: return@forEach
				// Load region
				regions.add(CRegion().apply { load(section) })
			}
		}
		else {
			// Create new data file
			file.createNewFile()
		}
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
	}
	
	fun removePlayer(player: Player) {
		players.remove(player)
	}
	
	fun hasPlayer(player: Player) = players.containsKey(player)
	
	fun getPlayerPos(player: Player) = players[player]!!.getPosition()
	
	fun getPlayerCheckpoint(player: Player) = players[player]?.getCheckpoint()
	
	fun getPlayerData(player: Player) = players[player]!!
	
	/*************/
	/** REGIONS **/
	/*************/
	
	fun getRegions() = regions
	
	fun createRegion(): CRegion {
		return CRegion().apply {
			regions.add(this)
			setIndex(regions.size)
		}
	}
	
	fun removeRegion(region: CRegion) {
		regions.remove(region)
	}
	
	/*************/
	/** SENSORS **/
	/*************/
	
	fun getStartSensor(): CSensor? = sensors.singleOrNull { it.getType() == CSensor.Type.START }
	
	fun setStartSensor(location: Location) {
		sensors.singleOrNull { it.getType() == CSensor.Type.START }?.apply {
			sensors.remove(this)
		}
		sensors.add(CSensor.create(CSensor.Type.START, location))
	}
	
	fun getCheckpoints(): List<CSensor> = sensors.filter { it.getType() == CSensor.Type.CHECKPOINT }
	
	fun addCheckpoint(location: Location) {
		sensors.add(CSensor.create(CSensor.Type.CHECKPOINT, location))
	}
	
	fun getFinishSensor(): CSensor? = sensors.singleOrNull { it.getType() == CSensor.Type.FINISH }
	
	fun setFinishSensor(location: Location) {
		sensors.singleOrNull { it.getType() == CSensor.Type.FINISH }?.apply {
			sensors.remove(this)
		}
		sensors.add(CSensor.create(CSensor.Type.FINISH, location))
	}
	
	/***********************/
	/** GETTERS / SETTERS **/
	/***********************/
	
	fun getName() = name
	
	fun setName(name: String) {
		this.name = name
	}
	
	fun getMode() = mode
	
	fun setMode(mode: Mode) {
		this.mode = mode
	}
	
	fun getAuthor() = author
	
	fun getPlays() = plays
	
	fun getWins() = wins
	
	/***************/
	/** MODIFIERS **/
	/***************/
	
	fun save() {
		// Initialize variables
		val yaml = YamlConfiguration()
		// Add course attributes
		yaml.set("name", name)
		yaml.set("mode", mode.name)
		yaml.set("author", author)
		// Save Sensors
		sensors.forEachIndexed { id, sensor ->
			yaml.set("sensors.$id.type", sensor.getType())
			yaml.set("sensors.$id.loc", sensor.getLocation())
		}
		// Save Regions
		regions.forEach { region ->
			region.getBlocks().forEachIndexed { blockId, b ->
				yaml.set("regions.${region.getIndex()}.$blockId.loc", b.getLocation())
				yaml.set("regions.${region.getIndex()}.$blockId.data", b.getBlockData().getAsString(true))
			}
		}
		// Save file
		yaml.save(file)
	}
	
	fun delete() {
		file.delete()
	}
	
	fun show() {
		regions.forEach { it.show() }
	}
	
	fun hide() {
		regions.forEach { it.hide() }
	}
	
}