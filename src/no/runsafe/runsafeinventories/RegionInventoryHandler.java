package no.runsafe.runsafeinventories;

import no.runsafe.framework.api.IConfiguration;
import no.runsafe.framework.api.IWorld;
import no.runsafe.framework.api.event.IServerReady;
import no.runsafe.framework.api.event.player.IPlayerCustomEvent;
import no.runsafe.framework.api.event.plugin.IConfigurationChanged;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.framework.api.server.IWorldManager;
import no.runsafe.framework.minecraft.event.player.RunsafeCustomEvent;
import no.runsafe.runsafeinventories.events.InventoryRegionEnter;
import no.runsafe.runsafeinventories.events.InventoryRegionExit;
import no.runsafe.runsafeinventories.repositories.InventoryRegionRepository;
import no.runsafe.runsafeinventories.repositories.InventoryRepository;
import no.runsafe.worldguardbridge.WorldGuardInterface;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionInventoryHandler implements IConfigurationChanged, IPlayerCustomEvent, IServerReady
{
	/**
	 * Constructor for RegionInventoryHandler
	 * @param inventoryRegionRepository Inventory region repository instance.
	 * @param inventoryRepository Inventory repository instance. Used to clear region inventory data.
	 * @param worldGuard WorldGuard bridge instance.
	 * @param worldManager Interface for getting worlds.
	 */
	public RegionInventoryHandler(
		InventoryRegionRepository inventoryRegionRepository,
		InventoryRepository inventoryRepository,
		WorldGuardInterface worldGuard,
		IWorldManager worldManager
	)
	{
		this.inventoryRegionRepository = inventoryRegionRepository;
		this.inventoryRepository = inventoryRepository;
		this.worldGuard = worldGuard;
		this.worldManager = worldManager;
	}

	/**
	 * Check if the player is currently in an inventory region.
	 * @param player The player to check for.
	 * @return The name of the region if one exists, otherwise null.
	 */
	public String getPlayerInventoryRegion(IPlayer player)
	{
		String worldName = player.getWorldName(); // Name of the world the player is in.
		List<String> regions = worldGuard.getApplicableRegions(player); // Get region list.

		// Check each region and see if we have an inventory associated with any.
		for (String region : regions)
			if (doesRegionHaveInventory(worldName, region))
				return region;

		return null;
	}

	/**
	 * Check if a region has a separate inventory associated with it.
	 * @param worldName Name of the world the region is in.
	 * @param regionName Name of the region to check.
	 * @return True if the region has it's own inventory associated with it.
	 */
	public boolean doesRegionHaveInventory(String worldName, String regionName)
	{
		return inventoryRegions.containsKey(worldName) && inventoryRegions.get(worldName).contains(regionName);
	}

	/**
	 * Prevent the player's current regions from triggering entry events.
	 * @param player The player to perform this on.
	 */
	public void blacklistCurrentRegionsEntry(IPlayer player)
	{
		// Get all current regions and blacklist their entry events.
		for (String region : worldGuard.getApplicableRegions(player))
			ignoreEntryEventRegions.add(getRegionKey(player, region));
	}

	/**
	 * Prevent the player's current regions from triggering exit events.
	 * @param player The player to perform this on.
	 */
	public void blacklistCurrentRegionsExit(IPlayer player)
	{
		// Get all current regions and blacklist their exit events.
		for (String region : worldGuard.getApplicableRegions(player))
			ignoreExitEventRegions.add(getRegionKey(player, region));
	}

	/**
	 * Gives a region an inventory.
	 * Does nothing if worldName or regionName are null or empty.
	 * @param worldName Name of the world the region is in.
	 * @param regionName Name of the region to give an inventory.
	 * @return True if region inventory region could be added, false otherwise.
	 */
	public Boolean addInventoryRegion(String worldName, String regionName)
	{
		// Check for null or empty arguments.
		if ((worldName == null) || (worldName.isEmpty()) || (regionName == null) || (regionName.isEmpty()))
			return false;

		// Disallow giving the __global__ region an inventory. It already gets the default inventory.
		if (regionName.equals("__global__"))
			return false;

		// Avoid giving the region an inventory if it already has an inventory.
		if (doesRegionHaveInventory(worldName, regionName))
			return false;

		// Make absolutely sure that the region exists.
		IWorld world = worldManager.getWorld(worldName);
		Rectangle2D regionRectangle = worldGuard.getRectangle(world, regionName);
		if (regionRectangle == null)
			return false;

		// Check if the region overlaps with any other inventory regions. Nested regions are not currently supported.
		List<String> allInventoryRegions = getInventoryRegionsInWorld(world);
		if ((allInventoryRegions != null) && allInventoryRegions.size() > 1)
			for (String region : allInventoryRegions)
				if (regionRectangle.intersects(worldGuard.getRectangle(world, region)))
					return false;

		// World and region passed all checks, give that region an inventory.
		if (!inventoryRegions.containsKey(worldName))
			inventoryRegions.put(worldName, new ArrayList<>(1));

		inventoryRegions.get(worldName).add(regionName);
		inventoryRegionRepository.addInventoryRegion(worldName, regionName);
		return true;
	}

	/**
	 * Removes an inventory from a region.
	 * Inventory data from that region will be removed.
	 * Region itself will not be removed.
	 * Make sure all players are out of this region before removing it.
	 * Does nothing if worldName or regionName are null or empty.
	 * @param worldName Name of the world the region is in.
	 * @param regionName Name of the region to remove the inventory of.
	 * @return True if region inventory region could be removed, false otherwise.
	 */
	public Boolean removeInventoryRegion(String worldName, String regionName)
	{
		// Check for null or empty arguments.
		if ((worldName == null) || (worldName.isEmpty()) || (regionName == null) || (regionName.isEmpty()))
			return false;

		// Avoid trying to remove the region inventory if it isn't on the list.
		if (!doesRegionHaveInventory(worldName, regionName))
			return false;

		// World and region passed all checks, remove that region's inventory.
		inventoryRegions.get(worldName).remove(regionName);
		inventoryRegionRepository.removeInventoryRegion(worldName, regionName);
		inventoryRepository.wipeRegionInventories(worldName, regionName);
		return true;
	}

	/**
	 * Gets a list of all the inventory regions in a world.
	 * @param world World to get inventory regions from.
	 * @return Inventory regions in a world, if any.
	 */
	public List<String> getInventoryRegionsInWorld(IWorld world)
	{
		if (world == null)
			return Collections.emptyList();

		List<String> worldInventoryRegions = inventoryRegions.get(world.getName());
		if (worldInventoryRegions == null)
			return Collections.emptyList();
		else
			return worldInventoryRegions;
	}

	/**
	 * Remove a region from the ignore list for a player, allowing it to trigger events again.
	 * @param player The player who the event should be pardoned for.
	 * @param region The region which should no longer be ignored.
	 */
	private void removeIgnoredRegionEntry(IPlayer player, String region)
	{
		// Remove the region from the ignore list.
		ignoreEntryEventRegions.remove(getRegionKey(player, region));
	}

	/**
	 * Remove a region from the ignore list for a player, allowing it to trigger exit events again.
	 * @param player The player who the event should be ignored for.
	 * @param region The region which should no longer be ignored.
	 */
	private void removeIgnoredRegionExit(IPlayer player, String region)
	{
		// Remove the region from the ignore list.
		ignoreExitEventRegions.remove(getRegionKey(player, region));
	}

	/**
	 * Check if entry events for a specific region are currently ignored.
	 * @param player The player to check the event for.
	 * @param region The region to check the event for.
	 * @return True if entry events for the specified region are being ignored.
	 */
	private boolean isRegionEntryIgnored(IPlayer player, String region)
	{
		return ignoreEntryEventRegions.contains(getRegionKey(player, region));
	}

	/**
	 * Check if exit events for a specific region are currently ignored.
	 * @param player The player to check the event for.
	 * @param region The region to check the event for,
	 * @return True if the event events for the specified region are being ignored.
	 */
	private boolean isRegionExitIgnored(IPlayer player, String region)
	{
		return ignoreExitEventRegions.contains(getRegionKey(player, region));
	}

	/**
	 * Present a single string representing a player and world specific region.
	 * @param player The player to associate this key with.
	 * @param region The region to associate this key with.
	 * @return A combined key for the player, world and region.
	 */
	private String getRegionKey(IPlayer player, String region)
	{
		return String.format("%s-%s-%s", player.getName(), player.getWorldName(), region);
	}

	/**
	 * Called when the server is ready.
	 * Updates locally stored inventory regions.
	 */
	@Override
	public void OnServerReady()
	{
		inventoryRegions = inventoryRegionRepository.getInventoryRegions();
	}

	/**
	 * Called when the configuration for this plug-in is changed.
	 * @param configuration Object for accessing this plug-ins configuration.
	 */
	@Override
	public void OnConfigurationChanged(IConfiguration configuration)
	{
		inventoryRegions = inventoryRegionRepository.getInventoryRegions();
	}

	/**
	 * Called when a custom event is fired from within the framework.
	 * @param event Object presenting event related data.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void OnPlayerCustomEvent(RunsafeCustomEvent event)
	{
		String eventName = event.getEvent();

		if (!eventName.equals("region.enter") && !eventName.equals("region.leave"))
			return;

		Map<String, String> data = (Map<String, String>) event.getData();
		IPlayer player = event.getPlayer();
		String world = data.get("world");
		String region = data.get("region");

		if (eventName.equals("region.enter"))
		{
			if (doesRegionHaveInventory(world, region) && !isRegionEntryIgnored(player, region))
			{
				// Fire an inventory region enter event.
				new InventoryRegionEnter(player, region).Fire();
			}
			else
			{
				// The region is ignored, so let's remove it so next time it isn't ignored.
				removeIgnoredRegionEntry(player, region);
			}
		}
		else
		{
			// Handle region leaving.
			if (doesRegionHaveInventory(world, region) && !isRegionExitIgnored(player, region))
			{
				// Fire an inventory region exit event.
				new InventoryRegionExit(player, region).Fire();
			}
			else
			{
				// The region is ignored, so let's remove it so next time it isn't ignored.
				removeIgnoredRegionExit(player, region);
			}
		}
	}

	private final IWorldManager worldManager;
	private final InventoryRegionRepository inventoryRegionRepository;
	private final InventoryRepository inventoryRepository;
	private final List<String> ignoreEntryEventRegions = new ArrayList<>();
	private final List<String> ignoreExitEventRegions = new ArrayList<>();
	private HashMap<String, List<String>> inventoryRegions = new HashMap<>();
	private final WorldGuardInterface worldGuard;
}
