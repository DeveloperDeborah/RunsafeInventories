package no.runsafe.runsafeinventories.events;

import no.runsafe.framework.api.IDebug;
import no.runsafe.framework.api.IWorld;
import no.runsafe.framework.api.event.player.IPlayerPortalEvent;
import no.runsafe.framework.api.event.player.IPlayerTeleportEvent;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.framework.minecraft.RunsafeLocation;
import no.runsafe.framework.minecraft.event.player.RunsafePlayerPortalEvent;
import no.runsafe.framework.minecraft.event.player.RunsafePlayerTeleportEvent;
import no.runsafe.runsafeinventories.InventoryHandler;

public class PlayerTeleport implements IPlayerTeleportEvent, IPlayerPortalEvent
{
	public PlayerTeleport(InventoryHandler inventoryHandler, IDebug output)
	{
		this.inventoryHandler = inventoryHandler;
		this.debugger = output;
	}

	@Override
	public void OnPlayerTeleport(RunsafePlayerTeleportEvent event)
	{
		this.debugger.debugFine("Detected teleport event: " + event.getPlayer().getName());
		RunsafeLocation from = event.getFrom();
		RunsafeLocation to = event.getTo();
		this.checkTeleportEvent(to == null ? null : to.getWorld(), from == null ? null : from.getWorld(), event.getPlayer());
	}

	@Override
	public void OnPlayerPortalEvent(RunsafePlayerPortalEvent event)
	{
		if (event.isCancelled())
			return;

		this.debugger.debugFine("Detected portal event: " + event.getPlayer().getName());
		RunsafeLocation from = event.getFrom();
		RunsafeLocation to = event.getTo();
		this.checkTeleportEvent(to == null ? null : to.getWorld(), from == null ? null : from.getWorld(), event.getPlayer());
	}

	private void checkTeleportEvent(IWorld to, IWorld from, IPlayer player)
	{
		if (to != null && from != null && !to.equals(from))
			this.inventoryHandler.handlePreWorldChange(player);
	}

	private InventoryHandler inventoryHandler;
	private IDebug debugger;

}
