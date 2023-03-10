package org.serversmc.parkour.interfaces

import org.bukkit.*
import org.bukkit.configuration.file.*
import org.serversmc.parkour.core.*
import org.serversmc.parkour.utils.Console
import java.io.*
import java.util.*
import kotlin.collections.ArrayList

interface ConfigAPI {
	
	companion object {
		var globalConfig = YamlConfiguration()
		val configList = ArrayList<ConfigFile>()
	}
	
	class ConfigFile(fileName: String) {
		val file = File(PLUGIN.dataFolder, "$fileName.yml")
		var yamlConfiguration = YamlConfiguration()
		var version: Int = 0
	}
	
	fun getString(path: String): String = ChatColor.translateAlternateColorCodes('&', globalConfig.getString(path).toString())
	fun getInt(path: String): Int = globalConfig.getInt(path)
	fun getDouble(path: String): Double = globalConfig.getDouble(path)
	fun getBoolean(path: String): Boolean = globalConfig.getBoolean(path)
	fun getIntegerList(path: String): MutableList<Int> = globalConfig.getIntegerList(path)
	fun getStringList(path: String): MutableList<String> = globalConfig.getStringList(path)
	
	fun addFile(fileName: String) {
		configList.add(ConfigFile(fileName))
	}
	
	fun setupConfigs()
	
	private fun combineSubConfigs() {
		// Combine config to one Main config
		configList.forEach { config ->
			val yaml = config.yamlConfiguration
			for (key in yaml.getKeys(true)) globalConfig.set(key, yaml.get(key))
		}
		// Remove version node
		globalConfig.addDefault("version", null)
	}
	
	fun reloadConfig() {
		setupConfigs()
		combineSubConfigs()
	}
	
	fun init() {
		setupConfigs()
		configList.forEach { subConfig ->
			// Initialize attributes
			val file = subConfig.file
			val yaml = subConfig.yamlConfiguration
			// Save configs if needed
			if (!file.exists()) {
				PLUGIN.saveResource(file.name, false)
				Console.info("Created ${file.name} config file!")
			}
			// Get config defaults
			yaml.load(file)
			val inputStream = PLUGIN.getResource(file.name) as InputStream
			val defaultConfig = YamlConfiguration.loadConfiguration(InputStreamReader(inputStream))
			globalConfig.addDefaults(defaultConfig)
			subConfig.version = yaml.getInt("version")
			/**
			 * yaml.getInt()            local version
			 * globalConfig.getInt()    default version
			 */
			// Update configs if needed
			if (yaml.getInt("version") != globalConfig.getInt("version")) {
				// Prompt console about update
				Console.info("The config file ${file.name} has changed since the last update!")
				// Create rename file
				val cal = Calendar.getInstance()
				val time = cal.time.toString().replace(":", "_")
				val rename = File(PLUGIN.dataFolder, "($time) ${file.name}")
				// Check if already exists (should never happen)
				if (rename.exists()) rename.delete()
				// Update config file
				file.renameTo(rename)
				PLUGIN.saveResource(file.name, false)
				// Reload updated config
				yaml.load(file)
				subConfig.version = yaml.getInt("version")
				// Prompt update message
				Console.warn("Config file has been backed up to ${rename.name}!")
			}
		}
		combineSubConfigs()
	}
	
}