package no.runsafe.runsafeinventories;

import no.runsafe.framework.api.player.IPlayer;

import java.util.HashMap;

public class InventoryHistory
{
	public void save(IPlayer player)
	{
		this.history.put(player, player.getInventory().serialize());
	}

	public void save(IPlayer player, PlayerInventory inventory)
	{
		this.history.put(player, inventory.getInventoryString());
	}

	public boolean restore(IPlayer player)
	{
		if (this.history.containsKey(player))
		{
			player.getInventory().unserialize(this.history.get(player));
			return true;
		}
		return false;
	}

	private final HashMap<IPlayer, String> history = new HashMap<>();
}
