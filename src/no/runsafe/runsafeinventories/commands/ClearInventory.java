package no.runsafe.runsafeinventories.commands;

import no.runsafe.framework.api.command.ExecutableCommand;
import no.runsafe.framework.api.command.ICommandExecutor;
import no.runsafe.framework.api.command.argument.IArgumentList;
import no.runsafe.framework.api.command.argument.OptionalArgument;
import no.runsafe.framework.api.command.argument.Player;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.runsafeinventories.InventoryHandler;
import no.runsafe.runsafeinventories.InventoryHistory;
import no.runsafe.runsafeinventories.RegionInventoryHandler;
import no.runsafe.runsafeinventories.UniverseHandler;

public class ClearInventory extends ExecutableCommand
{
	public ClearInventory(InventoryHistory history, InventoryHandler inventoryHandler, UniverseHandler universeHandler, RegionInventoryHandler regionHandler)
	{
		super(
			"clear",
			"Clears a players inventory",
			"runsafe.inventories.clear",
			new Player().defaultToExecutor(),
			new UniverseArgument(universeHandler),
			new OptionalArgument("regionName")
		);
		this.history = history;
		this.inventoryHandler = inventoryHandler;
		this.universeHandler = universeHandler;
		this.regionHandler = regionHandler;
	}

	@Override
	public String OnExecute(ICommandExecutor executor, IArgumentList parameters)
	{
		IPlayer player = parameters.getRequired("player");
		String universeName = parameters.getValue("universe");
		String regionName = parameters.getValue("regionName");

		// Get players current universe if none specified
		if (universeName == null && player.getUniverse() != null)
			universeName = player.getUniverse().getName();

		if (universeName == null)
			return "&cA universe must be specified for offline players.";

		if (!this.universeHandler.universeExists(universeName) && !this.universeHandler.worldExists(universeName))
			return "&cThe universe/world you are looking for does not exist.";

		String currentInventoryRegion = regionHandler.getPlayerInventoryRegion(player);
		if (currentInventoryRegion == null)
			currentInventoryRegion = "";

		// Handle deleting a player's currently open inventory
		if (player.isOnline() && universeName.equals(player.getUniverse().getName())
			&& (regionName == null || currentInventoryRegion.equals(regionName)))
		{
			this.history.save(player);
			player.getInventory().clear();
			player.updateInventory();
			if (executor instanceof IPlayer && executor.equals(player))
				return "&2Your inventory has been cleared.";
			return String.format("&2Inventory for %s &2cleared.", player.getPrettyName());
		}

		// Handle deleting a player's saved inventory
		this.history.save(player, inventoryHandler.getInventory(player, universeName, regionName));
		inventoryHandler.clearInventory(player, universeName, regionName);

		String outputUniverseName = universeName;
		if (regionName != null)
			outputUniverseName += "-" + regionName;

		if (executor instanceof IPlayer && executor.equals(player))
			return String.format("&2Your inventory has been cleared in: %s.", outputUniverseName);
		return String.format("&2Inventory for %s &2cleared in: %s&2.", player.getPrettyName(), outputUniverseName);
	}

	private final InventoryHistory history;
	private final InventoryHandler inventoryHandler;
	private final UniverseHandler universeHandler;
	private final RegionInventoryHandler regionHandler;
}
