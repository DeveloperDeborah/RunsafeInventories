package no.runsafe.runsafeinventories.repositories;

import no.runsafe.framework.api.IWorld;
import no.runsafe.framework.api.database.IRow;
import no.runsafe.framework.api.database.ISchemaUpdate;
import no.runsafe.framework.api.database.Repository;
import no.runsafe.framework.api.database.SchemaUpdate;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.framework.api.server.IWorldManager;
import no.runsafe.runsafeinventories.PlayerInventory;

import javax.annotation.Nonnull;

public class InventoryRepository extends Repository
{
	public InventoryRepository(IWorldManager worldManager)
	{
		this.worldManager = worldManager;
	}

	@Nonnull
	@Override
	public String getTableName()
	{
		return "runsafeInventories";
	}

	public void saveInventory(PlayerInventory inventory)
	{
		database.execute(
			"INSERT INTO runsafeInventories (owner, inventoryName, inventory, level, experience, foodLevel) VALUES(?, ?, ?, ?, ?, ?)" +
				" ON DUPLICATE KEY UPDATE inventory = ?, level = ?, experience = ?, foodLevel = ?",
			inventory.getPlayer().getUniqueId().toString(), inventory.getInventoryName(),
			inventory.getInventoryString(), inventory.getLevel(), inventory.getExperience(), inventory.getFoodLevel(),
			inventory.getInventoryString(), inventory.getLevel(), inventory.getExperience(), inventory.getFoodLevel()
		);
	}

	/**
	 * Retrieve a player inventory by its name.
	 * @param player The player to grab the inventory for.
	 * @param universeName The name identifying the inventory to retrieve.
	 * @return A player inventory object or null if none could be found.
	 */
	public PlayerInventory getInventory(IPlayer player, String universeName)
	{
		return getInventoryData(player, universeName);
	}

	/**
	 * Retrieve a player inventory for a specific region.
	 * @param player The player to grab the inventory for.
	 * @param universeName The universe to grab the inventory for.
	 * @param regionName The region to grab the inventory for.
	 * @return A player inventory object or null if none could be found.
	 */
	public PlayerInventory getInventoryForRegion(IPlayer player, String universeName, String regionName)
	{
		// Check if the player and their world is null; if not the universe will not be null either.
		if (player == null || player.getWorld() == null)
			return null;

		// Append the region name to the end to create a unique key.
		return getInventoryData(player, universeName + "-" + regionName);
	}

	private PlayerInventory getInventoryData(IPlayer player, String universeName)
	{
		IRow data = database.queryRow(
			"SELECT inventory, level, experience, foodLevel FROM runsafeInventories WHERE owner = ? AND inventoryName = ?",
			player.getUniqueId().toString(), universeName
		);

		// Check if the player is offline and hasn't had their username converted to a unique id yet.
		if (!player.isOnline() && data.isEmpty())
			data = database.queryRow(
				"SELECT inventory, level, experience, foodLevel FROM runsafeInventories WHERE owner = ? AND inventoryName = ?",
				player.getName(), universeName
			);

		if (data.isEmpty())
			return null; // We have no inventory, so no need to return a blank one.

		return new PlayerInventory(
			player,
			universeName,
			data.String("inventory"),
			data.Integer("level"),
			data.Float("experience"),
			data.Integer("foodLevel")
		);
	}

	/**
	 * Wipes inventories from all players in an inventory region.
	 * @param worldName Name of the world the region is in. Assumed to be valid.
	 * @param regionName Name of the region to wipe inventories for. Assumed to be valid.
	 */
	public void wipeRegionInventories(String worldName, String regionName)
	{
		IWorld world = worldManager.getWorld(worldName);
		if (world == null)
			return;

		wipeInventories(world.getUniverse().getName() + "-" + regionName);
	}

	/**
	 * Wipes inventories from all players with a given inventory name.
	 * @param inventoryName Name of the inventory to wipe.
	 *                      Usually the universe name the inventory is in, unless it's a region inventory.
	 */
	public void wipeInventories(String inventoryName)
	{
		database.execute("DELETE FROM runsafeInventories WHERE inventoryName = ?", inventoryName);
	}

	@Nonnull
	@Override
	public ISchemaUpdate getSchemaUpdateQueries()
	{
		ISchemaUpdate update = new SchemaUpdate();

		update.addQueries(
			"CREATE TABLE `runsafeInventories` (" +
				"`owner` varchar(50) NOT NULL," +
				"`inventoryName` varchar(255) NOT NULL," +
				"`inventory` longtext," +
				"`level` int(10) unsigned NOT NULL DEFAULT '0'," +
				"`experience` float unsigned NOT NULL DEFAULT '0'," +
				"PRIMARY KEY (`owner`,`inventoryName`)" +
			")"
		);

		update.addQueries("ALTER TABLE `runsafeInventories`" +
			"ADD COLUMN `foodLevel` TINYINT(2) UNSIGNED NOT NULL DEFAULT '20' AFTER `experience`");

		update.addQueries( // Clean up empty inventories before updating UUIDs to reduce issues.
			String.format(
				"DELETE FROM `%s` where `inventory` = 'contents: {}\narmour: {}\n'",
				getTableName()
			)
		);

		update.addQueries( // Update UUIDs
			String.format(
				"UPDATE IGNORE `%s` SET `owner` = " +
					"COALESCE((SELECT `uuid` FROM player_db WHERE `name`=`%s`.`owner`), `owner`) " +
					"WHERE length(`owner`) != 36",
				getTableName(), getTableName()
			)
		);

		return update;
	}

	private final IWorldManager worldManager;
}
