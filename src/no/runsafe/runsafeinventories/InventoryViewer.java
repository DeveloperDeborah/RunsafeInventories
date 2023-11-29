package no.runsafe.runsafeinventories;

import no.runsafe.framework.api.IConfiguration;
import no.runsafe.framework.api.IServer;
import no.runsafe.framework.api.event.plugin.IConfigurationChanged;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.framework.minecraft.inventory.RunsafeInventory;
import no.runsafe.runsafeinventories.repositories.InventoryRepository;

import java.util.Objects;

public class InventoryViewer implements IConfigurationChanged
{
	public InventoryViewer(IServer server, InventoryRepository repository, RegionInventoryHandler regionHandler)
	{
		this.server = server;
		this.repository = repository;
		this.regionHandler = regionHandler;
	}

	public boolean viewUniverseInventory(IPlayer viewer, IPlayer owner)
	{
		return this.viewUniverseInventory(viewer, owner, this.defaultUniverse, null);
	}

	public boolean viewUniverseInventory(IPlayer viewer, IPlayer owner, String universeName, String regionName)
	{
		// Is the player online and in the same world? If so, get their current inventory from memory.
		if (owner.isOnline() && owner.isInUniverse(universeName))
		{
			if (regionName == null)
			{
				viewer.openInventory(owner.getInventory());
				return true;
			}
			if (regionName.equals(regionHandler.getPlayerInventoryRegion(owner)))
			{
				viewer.openInventory(owner.getInventory());
				return true;
			}
		}

		// Player is not online, pull inventory data from database.
		PlayerInventory inventoryData;
		if (regionName == null)
			inventoryData = this.repository.getInventory(owner, universeName);
		else
			inventoryData = this.repository.getInventoryForRegion(owner, universeName, regionName);
		if (inventoryData == null)
			return false;

		RunsafeInventory inventory = server.createInventory(null, 45, String.format("%s's Inventory", owner.getName()));
		inventory.unserialize(inventoryData.getInventoryString());
		viewer.openInventory(inventory);

		return true;
	}

	public boolean hasDefaultUniverse()
	{
		return (this.defaultUniverse != null);
	}

	@Override
	public void OnConfigurationChanged(IConfiguration configuration)
	{
		this.defaultUniverse = configuration.getConfigValueAsString("defaultOpenInventoryUniverse");
	}

	private final IServer server;
	private final InventoryRepository repository;
	private final RegionInventoryHandler regionHandler;
	private String defaultUniverse;
}
