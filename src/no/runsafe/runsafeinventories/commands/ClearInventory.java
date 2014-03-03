package no.runsafe.runsafeinventories.commands;

import no.runsafe.framework.api.command.ExecutableCommand;
import no.runsafe.framework.api.command.ICommandExecutor;
import no.runsafe.framework.api.command.argument.IArgumentList;
import no.runsafe.framework.api.command.argument.Player;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.runsafeinventories.InventoryHistory;

public class ClearInventory extends ExecutableCommand
{
	public ClearInventory(InventoryHistory history)
	{
		super("clear", "Clears a players inventory", "runsafe.inventories.clear", new Player.Online("player", true));
		this.history = history;
	}

	@Override
	public String OnExecute(ICommandExecutor executor, IArgumentList parameters)
	{
		IPlayer player = parameters.getValue("player");
		if (player == null)
			return null;
		this.history.save(player);
		player.getInventory().clear();
		player.updateInventory();
		if (executor instanceof IPlayer && executor.getName().equals(player.getName()))
			return "&2Your inventory has been cleared.";
		return "&2Inventory for " + player.getPrettyName() + " &2cleared.";
	}

	private final InventoryHistory history;
}
