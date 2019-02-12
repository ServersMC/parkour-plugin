package com.mcblox.parkour.objects;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.mcblox.parkour.enums.FolderStructure;
import com.mcblox.parkour.utils.Console;

public class Course {

	// STATIC

	public static final int VIEW_DISTANCE = 3;
	
	public static List<Course> courses = new ArrayList<Course>();

	private static void resetIds() {
		for (int i = 0; i < courses.size(); i++) {
			courses.get(i).id = i;
		}
	}

	public static Course createCourse(String name) {
		Course output = new Course(name);
		courses.add(output);
		return output;
	}

	public static void deleteCourse(Course course) {
		courses.remove(course);
		resetIds();
		File file = new File(FolderStructure.PARKOUR_COURSES.file(), course.getName() + ".dat");
		file.delete();
		course.show();
		course = null;
	}

	public static void loadCourses() {
		for (File file : FolderStructure.PARKOUR_COURSES.file().listFiles()) {
			YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

			// Initialize Course Load
			Course course = new Course();

			// Load initial variables
			course.rename(yaml.getString("name"));
			course.setSpawn((Location) yaml.get("spawn"));
			course.setStartBlock(((Location) yaml.get("start_block")).getBlock());
			course.setFinishBlock(((Location) yaml.get("finish_block")).getBlock());

			// Go into regions
			ConfigurationSection regions = yaml.getConfigurationSection("regions");
			for (String region_id : regions.getKeys(false)) {

				// Initialize region
				CourseRegion region = new CourseRegion(course);

				// Go into region blocks
				ConfigurationSection blocks = regions.getConfigurationSection(region_id);
				for (String block_id : blocks.getKeys(false)) {

					// Load block data
					Location loc = ((Location) blocks.get(block_id + ".loc"));
					BlockData blockdata = Bukkit.createBlockData(blocks.getString(block_id + ".data"));
					List<String> lines = new ArrayList<String>();
					lines = blocks.getStringList(block_id + ".lines");

					// Initialize CourseBlock
					CourseBlock block = new CourseBlock(loc, blockdata, lines);

					// Register course block into region
					region.addBlock(block);

					// -- End: for #Block Data
				}

				// Finish region load, and register region to course
				region.finishLoad();
				course.addRegion(region);

				// -- End: for #Region Blocks
			}

			// Register course into memory
			courses.add(course);

			// Hide Course
			course.hide();

			// -- End: for #Course Regions
		}

		// -- End: loadCourses()
	}

	// PUBLIC

	private int id;
	private String name;
	private Location spawn;
	private Block start;
	private Block finish;

	private List<CourseRegion> regions = new ArrayList<CourseRegion>();
	private HashMap<Player, Integer> playing = new HashMap<Player, Integer>();
	private List<Player> inQueue = new ArrayList<Player>();

	public Course(String name) {
		id = courses.size();
		this.name = name;
	}

	public Course() {
		id = courses.size();
	}

	public int getId() {
		return id;
	}

	public void rename(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setSpawn(Location loc) {
		spawn = loc;
	}
	
	public Location getSpawn() {
		return spawn;
	}
	
	public void setStartBlock(Block start) {
		this.start = start;
	}

	public Block getStartBlock() {
		return start;
	}

	public void setFinishBlock(Block finish) {
		this.finish = finish;
	}

	public Block getFinishBlock() {
		return finish;
	}

	public void resetRegions() {
		regions.clear();
	}

	public void addRegion(CourseRegion region) {
		regions.add(region);
		for (int i = 0; i < regions.size(); i++) {
			region.id = i;
		}
	}

	public List<CourseRegion> getRegions() {
		return regions;
	}

	public boolean hasBlock(Block block) {
		for (CourseRegion region : regions) {
			if (region.hasBlock(block)) {
				return true;
			}
		}
		return false;
	}

	public void addQueue(Player player) {
		inQueue.add(player);
		updateVisibility();
	}

	public boolean isQueue(Player player) {
		return inQueue.contains(player);
	}

	public boolean isPlaying(Player player) {
		if (inQueue.contains(player)) {
			return true;
		}
		if (playing.containsKey(player)) {
			return true;
		}
		return false;
	}

	public void startPlayer(Player player) {
		inQueue.remove(player);
		playing.put(player, 0);
		updateVisibility();
	}

	public void removePlayer(Player player) {
		if (inQueue.contains(player)) {
			inQueue.remove(player);
		}
		if (playing.containsKey(player)) {
			playing.remove(player);
		}
		updateVisibility();
	}

	public Integer getPlayerPos(Player player) {
		return playing.get(player);
	}

	public void updatePlayerPos(Player player, Integer position) {
		playing.replace(player, position);
		updateVisibility();
	}

	public void show() {
		for (CourseRegion region : regions) {
			region.show();
		}
	}

	public void hide() {
		for (CourseRegion region : regions) {
			region.hide();
		}
	}

	public void updateVisibility() {

		// Initialize variables
		List<Integer> positionsPlaying = new ArrayList<Integer>();
		HashMap<Integer, Boolean> visiblePlan = new HashMap<Integer, Boolean>();

		// Setup visibility plan
		for (CourseRegion region : regions) {
			visiblePlan.put(region.id, false);
		}

		// Check if someone is in queue
		if (inQueue.size() != 0) {
			visiblePlan.replace(0, true);
		}

		// Add the region positions that are currently occupying players
		playing.forEach((k, v) -> {
			if (!positionsPlaying.contains(v)) {
				positionsPlaying.add(v);
			}
		});

		// Show regions that are in front of the regions occupied by the players
		for (Integer pos : positionsPlaying) {
			for (int i = 0; i < VIEW_DISTANCE; i++) {
				visiblePlan.replace(pos + i, true);
			}
		}

		// Execute visibility plan
		visiblePlan.forEach((k, v) -> {
			if (regions.get(k).isVisible()) {
				if (!v) {
					regions.get(k).hide();
				}
			}
			else {
				if (v) {
					regions.get(k).show();
				}
			}
		});

		// -- End: updateVisibility()
	}

	public void saveCourse() {
		File file = new File(FolderStructure.PARKOUR_COURSES.file(), name + ".dat");

		try {

			// Initialize writer buffer
			YamlConfiguration yaml = new YamlConfiguration();

			// Save initial variables
			yaml.set("name", name);
			yaml.set("spawn", spawn);
			yaml.set("start_block", start.getLocation());
			yaml.set("finish_block", finish.getLocation());

			// Save regions
			for (CourseRegion region : regions) {

				// Save region blocks
				for (int i = 0; i < region.getBlocks().size(); i++) {

					// Serialize region blocks
					CourseBlock block = region.getBlocks().get(i);
					yaml.set("regions." + region.id + "." + i + ".loc", block.getLocation());
					yaml.set("regions." + region.id + "." + i + ".data", block.getBlockData().getAsString());
					yaml.set("regions." + region.id + "." + i + ".lines", block.getLines());

				}

			}

			// End save
			yaml.save(file);

			// -- End: try_catch
		} catch (IOException e) {
			Console.catchError(e, "Course.saveCourse()");
		}

		// -- End: saveCourse()
	}

}
