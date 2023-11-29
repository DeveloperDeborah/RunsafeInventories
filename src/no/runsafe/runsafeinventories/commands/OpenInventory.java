package no.runsafe.runsafeinventories.commands;

import no.runsafe.framework.api.command.argument.IArgumentList;
import no.runsafe.framework.api.command.argument.OptionalArgument;
import no.runsafe.framework.api.command.argument.Player;
import no.runsafe.framework.api.command.player.PlayerCommand;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.runsafeinventories.InventoryViewer;
import no.runsafe.runsafeinventories.UniverseHandler;

public class OpenInventory extends PlayerCommand
{
	public OpenInventory(InventoryViewer inventoryViewer, UniverseHandler universeHandler)
	{
		super(
			"open",
			"Opens a players inventory",
			"runsafe.inventories.open",
			new Player().require(),
			new UniverseArgument(universeHandler),
			new OptionalArgument("region")
		);
		this.inventoryViewer = inventoryViewer;
		this.universeHandler = universeHandler;
	}

	@Override
	public String OnExecute(IPlayer executor, IArgumentList parameters)
	{
		IPlayer target = parameters.getRequired("player");
		String universeName = parameters.getValue("universe");
		String regionName = parameters.getValue("region");
		if (universeName != null)
		{
			if (!this.universeHandler.universeExists(universeName) && !this.universeHandler.worldExists(universeName))
				return "&cThe universe/world you are looking for does not exist.";

			if (!this.inventoryViewer.viewUniverseInventory(executor, target, universeName, regionName))
				return "&cThat player does not have an inventory in that universe.";
		}
		else
		{
			if (!this.inventoryViewer.hasDefaultUniverse())
				return "&cNo default universe has been defined, please specify one.";

			if (!this.inventoryViewer.viewUniverseInventory(executor, target))
				return "&cThat player does not have an inventory in that universe.";
		}
		return null;
	}

	private final InventoryViewer inventoryViewer;
	private final UniverseHandler universeHandler;
}
