package no.runsafe.runsafeinventories;

import no.runsafe.framework.api.IConfiguration;
import no.runsafe.framework.api.IServer;
import no.runsafe.framework.api.event.plugin.IConfigurationChanged;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.framework.minecraft.inventory.RunsafeInventory;
import no.runsafe.framework.minecraft.inventory.RunsafeInventoryType;
import no.runsafe.runsafeinventories.repositories.InventoryRepository;

public class InventoryViewer implements IConfigurationChanged
{
	public InventoryViewer(IServer server, InventoryRepository repository)
	{
		this.server = server;
		this.repository = repository;
	}

	public boolean viewUniverseInventory(IPlayer viewer, IPlayer owner)
	{
		return this.viewUniverseInventory(viewer, owner, this.defaultUniverse);
	}

	public boolean viewUniverseInventory(IPlayer viewer, IPlayer owner, String universeName)
	{
		// Is the player online and in the same world? If so, get their current inventory from memory.
		if (owner.isOnline() && owner.isInUniverse(universeName))
		{
			viewer.openInventory(owner.getInventory());
			return true;
		}

		// Player is not online, pull inventory data from database.
		PlayerInventory inventoryData = this.repository.getInventory(owner, universeName);
		if (inventoryData == null)
			return false;

		RunsafeInventory inventory = server.createInventory(null, RunsafeInventoryType.PLAYER.getDefaultSize(), String.format("%s's Inventory", owner.getName()));
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
	private String defaultUniverse;
}
