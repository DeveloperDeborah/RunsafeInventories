package no.runsafe.runsafeinventories.commands;

import no.runsafe.framework.api.command.ICommandExecutor;
import no.runsafe.framework.internal.command.ExecutableCommand;
import no.runsafe.framework.minecraft.RunsafeServer;
import no.runsafe.framework.minecraft.inventory.RunsafeInventory;
import no.runsafe.framework.minecraft.item.meta.RunsafeMeta;
import no.runsafe.framework.minecraft.player.RunsafeAmbiguousPlayer;
import no.runsafe.framework.minecraft.player.RunsafePlayer;

import java.util.HashMap;

public class DropItems extends ExecutableCommand
{
	public DropItems()
	{
		super("drop", "Causes a player to drop all of their items", "runsafe.inventories.drop");
	}

	@Override
	public String OnExecute(ICommandExecutor executor, HashMap<String, String> parameters)
	{
		return null;
	}

	@Override
	public String OnExecute(ICommandExecutor executor, HashMap<String, String> parameters, String[] arguments)
	{
		if (arguments.length > 0)
		{
			RunsafePlayer player = RunsafeServer.Instance.getPlayer(arguments[0]);
			if (player != null)
			{
				if (player instanceof RunsafeAmbiguousPlayer)
					return player.toString();

				if (player.isOnline())
				{
					this.dropItems(player);
					return "&2Caused " + player.getPrettyName() + "&2 to drop their items.";
				}
				return "&cThat player is not online.";
			}
			return "&cThat player does not exist.";
		}
		else
		{
			if (executor instanceof RunsafePlayer)
			{
				this.dropItems((RunsafePlayer) executor);
				return "&cYour items have been dropped.";
			}
			return "&cPlease specify a player.";
		}
	}

	private void dropItems(RunsafePlayer player)
	{
		RunsafeInventory inventory = player.getInventory();

		for (RunsafeMeta itemStack : inventory.getContents())
		{
			inventory.remove(itemStack);
			player.getWorld().dropItem(player.getLocation(), itemStack);
		}

		player.updateInventory();
	}
}
