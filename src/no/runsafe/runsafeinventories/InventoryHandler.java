package no.runsafe.runsafeinventories;

import no.runsafe.framework.api.IConfiguration;
import no.runsafe.framework.api.IWorld;
import no.runsafe.framework.api.event.player.IPlayerCustomEvent;
import no.runsafe.framework.api.event.plugin.IConfigurationChanged;
import no.runsafe.framework.api.log.IDebug;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.framework.api.server.IWorldManager;
import no.runsafe.framework.minecraft.event.player.RunsafeCustomEvent;
import no.runsafe.runsafeinventories.repositories.InventoryRepository;
import no.runsafe.runsafeinventories.repositories.TemplateRepository;

import java.util.Map;

public class InventoryHandler implements IPlayerCustomEvent, IConfigurationChanged
{
	public InventoryHandler(InventoryRepository inventoryRepository, TemplateRepository templateRepository, IDebug output, RegionInventoryHandler regionInventoryHandler, IWorldManager worldManager)
	{
		this.inventoryRepository = inventoryRepository;
		this.templateRepository = templateRepository;
		this.debugger = output;
		this.regionInventoryHandler = regionInventoryHandler;
		this.worldManager = worldManager;
	}

	@Override
	public void OnConfigurationChanged(IConfiguration configuration)
	{
		maxInventoryDataSize = configuration.getConfigValueAsInt("maxInventoryDataSize");
		overloadedWarningMessage = configuration.getConfigValueAsString("overloadedWarningMessage");
	}

	public void saveInventory(IPlayer player)
	{
		String inventoryRegion = regionInventoryHandler.getPlayerInventoryRegion(player);
		IWorld world = player.getWorld();
		if (world == null)
			return;
		String universeName = world.getUniverse().getName();
		String inventoryName = inventoryRegion == null ?  universeName : universeName + "-" + inventoryRegion;

		saveInventory(player, inventoryName);
	}

	private void saveInventory(IPlayer player, String inventoryName)
	{
		PlayerInventory inventory = new PlayerInventory(player, inventoryName);
		int inventoryDataSize = inventory.getInventoryString().length();
		if (inventoryDataSize > maxInventoryDataSize)
		{
			Plugin.console.logInformation(
				"Player inventory: " + player.getName() + " too large to be saved. Size: " + inventoryDataSize
			);
			debugger.debugFine(
				"Could not save inventory %s for %s. Size: %d", inventoryName, player.getName(), inventoryDataSize
			);
			player.getInventory().clear();
			player.sendColouredMessage(overloadedWarningMessage);
			return;
		}

		debugger.debugFine("Saving inventory %s for %s", inventoryName, player.getName());
		inventoryRepository.saveInventory(inventory);
	}

	public void handlePreWorldChange(IPlayer player)
	{
		this.saveInventory(player); // Save inventory
		this.wipeInventory(player);
	}

	public void wipeInventory(IPlayer player)
	{
		this.debugger.debugFine("Wiping inventory for %s", player.getName());
		player.getInventory().clear(); // Clear inventory
		player.setXP(0); // Remove all XP
		player.setLevel(0); // Remove all levels
		player.setFoodLevel(20);
	}

	public void clearInventory(IPlayer player, String universeName, String regionName)
	{
		if (regionName == null)
			inventoryRepository.clearInventory(player, universeName);

		inventoryRepository.clearInventory(player, universeName + "-" + regionName);
	}

	public PlayerInventory getInventory(IPlayer player, String universeName, String regionName)
	{
		if (regionName == null)
			return inventoryRepository.getInventory(player, universeName);
		return inventoryRepository.getInventory(player, universeName + "-" + regionName);
	}

	public void handlePostWorldChange(IPlayer player)
	{
		IWorld world = player.getWorld();
		if (world == null)
			return;
		String universeName = world.getUniverse().getName();

		PlayerInventory inventory;
		String inventoryRegion = regionInventoryHandler.getPlayerInventoryRegion(player);

		if (inventoryRegion != null)
			inventory = inventoryRepository.getInventoryForRegion(player, inventoryRegion);
		else
			inventory = inventoryRepository.getInventory(player, universeName); // Get inventory

		// If we are null, the player had no stored inventory.
		if (inventory != null)
			setInventory(player, inventory);
		else
		{
			// Let's check if we can give them a template.
			this.templateRepository.setToTemplate(universeName, player.getInventory());
		}
	}

	public void wipeUniverse(String universeName)
	{
		inventoryRepository.wipeInventories(universeName);
	}

	/**
	 * Called when a custom event is fired from within the framework.
	 * @param event Object containing event related data.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void OnPlayerCustomEvent(RunsafeCustomEvent event)
	{
		String eventName = event.getEvent();
		if (eventName.startsWith("inventory.region."))
		{
			IPlayer player = event.getPlayer();
			Map<String, String> data = (Map<String, String>) event.getData();
			IWorld world = worldManager.getWorld(data.get("world"));
			if (world == null)
				return;
			String universeName = world.getUniverse().getName();

			if (eventName.equals("inventory.region.enter"))
			{
				// Assume the player was not already in an inventory region; save and wipe their previous inventory.
				saveInventory(player, universeName);
				wipeInventory(player);

				// Load the player's inventory for this region.
				setInventory(player, inventoryRepository.getInventoryForRegion(player, data.get("region")));
			}
			else if (eventName.equals("inventory.region.exit"))
			{
				// Save the inventory for the region we left and clear the current inventory.
				saveInventory(player, universeName + "-" + data.get("region"));
				wipeInventory(player);

				// Assume the player has left all inventory regions and get their default universe inventory.
				setInventory(player, inventoryRepository.getInventory(player, universeName));
			}
		}
	}

	/**
	 * Sets a player's inventory.
	 * Assumes it is already empty.
	 * Also sets their level, xp and saturation (food level).
	 * @param player User whose inventory to set.
	 * @param inventory Player inventory to give to the player.
	 *                  Null for an empty inventory.
	 */
	private void setInventory(IPlayer player, PlayerInventory inventory)
	{
		if (inventory != null)
		{
			this.debugger.debugFine("Settings inventory for %s to %s", player.getName(), inventory.getInventoryName());
			player.getInventory().unserialize(inventory.getInventoryString()); // Restore inventory
			player.setLevel(inventory.getLevel()); // Restore level
			player.setXP(inventory.getExperience()); // Restore experience
			player.setFoodLevel(inventory.getFoodLevel()); // Restore food level
			player.updateInventory();
		}
	}

	private String overloadedWarningMessage;
	private int maxInventoryDataSize;
	private final IWorldManager worldManager;
	private final InventoryRepository inventoryRepository;
	private final TemplateRepository templateRepository;
	private final IDebug debugger;
	private final RegionInventoryHandler regionInventoryHandler;
}
