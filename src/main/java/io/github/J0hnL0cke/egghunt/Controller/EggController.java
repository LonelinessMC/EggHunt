package io.github.J0hnL0cke.egghunt.Controller;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import io.github.J0hnL0cke.egghunt.Controller.ConfigManager.CONFIG_ITEMS;
import io.github.J0hnL0cke.egghunt.Model.Data;
import io.github.J0hnL0cke.egghunt.Model.LogHandler;

/**
 * Provides functionality relating to the dragon egg, but does not register any event handlers.
 * This is a pure fabrication class to simplify functionality used by multiple controllers.
 */
public class EggController {

    /**
     * Makes the given entity invulnerable if enabled in the config
     */
    public static void makeEggInvulnerable(Entity entity, ConfigManager config) {
        if (config.getBoolean(CONFIG_ITEMS.EGG_INV)) {
            entity.setInvulnerable(true);
        }
    }

    /**
     * Alerts when the egg is destroyed and respawns it if needed
     */
    public static void eggDestroyed(ConfigManager config, Data data, LogHandler logger) {
        logger.log("Egg was destroyed");
        OfflinePlayer oldOwner = data.getEggOwner();
        data.resetEggOwner(false, config);
        data.resetEggLocation();

        if (config.getBoolean(CONFIG_ITEMS.RESP_EGG)) {
            if (config.getBoolean(CONFIG_ITEMS.RESP_IMM)) {
                logger.log("Immediate respawn enabled- respawning egg");
                respawnEgg(config, data, logger);
                //msg = "The dragon egg was destroyed and has respawned in The End!";
                if (oldOwner != null) { //prevent spamming of egg destruction
                }
            } else {
                logger.log("Immediate respawn disabled- egg will respawn after next dragon fight");
                //msg = "The dragon egg has been destroyed! It will respawn the next time the Ender Dragon is defeated.";
            }
        } else {
            logger.log("Egg respawn is disabled");
            //msg = "The dragon egg has been destroyed!";
        }   
    }
    
    /**
     * Returns the location above the end fountain where the dragon will respawn
     */
    public static Location getEggRespawnLocation(ConfigManager config) {
        //the block above the bedrock fountain where the egg spawns
        return Bukkit.getWorld(config.getString(CONFIG_ITEMS.END)).getEnderDragonBattle().getEndPortalLocation().add(0, 4, 0);
    }

    /**
     * Drop the egg out of the given player's inventory
     */
    public static void dropEgg(Player player, Data data, ConfigManager config) {

        Boolean hadToRemoveFromInventory = false;

        // If player has dragon egg in the inventory it gets removed
        if (player.getInventory().contains(Material.DRAGON_EGG)) {
            // Set owner and remove
            data.setEggOwner(player, config); //TODO is this necessary? player will likely already be owner
            player.getInventory().remove(Material.DRAGON_EGG);
            hadToRemoveFromInventory = true;
        }

        // If player has dragon egg in second hand it gets removed
        if(player.getInventory().getItemInOffHand().getType().equals(Material.DRAGON_EGG)){
            // Set owner and remove
            data.setEggOwner(player, config); //TODO is this necessary? player will likely already be owner
            player.getInventory().setItemInOffHand(null);
            hadToRemoveFromInventory = true;
        }

        // If egg has been removed from the player it gets to spawn on the floor
        if(hadToRemoveFromInventory){
            // Drop it on the floor and set its location
            //TODO use drop egg method in EggRespawn
            Item eggDrop = player.getWorld().dropItem(player.getLocation(),
                    new ItemStack(Material.DRAGON_EGG));
            eggDrop.setVelocity(new Vector(0, 0, 0));
            data.updateEggLocation(eggDrop);
        }
    }

    /**
     * Updates the egg ownership scoreboard tag for the given player if tagging is enabled
     * Adds the tag if the player is the owner, otherwise removes it
     */
    public static void updateOwnerTag(Player player, Data data, ConfigManager config) {
        if (player != null) {
            if (config.getBoolean(CONFIG_ITEMS.TAG_OWNER)) {
                OfflinePlayer owner = data.getEggOwner();
                //if the given player owns the egg
                if (owner != null && owner.getUniqueId().equals(player.getUniqueId())) {
                    player.addScoreboardTag(config.getString(CONFIG_ITEMS.OWNER_TAG_NAME));
                } else {
                    player.removeScoreboardTag(config.getString(CONFIG_ITEMS.OWNER_TAG_NAME));
                }
            }
        }
    }

    /**
     * Spawns a new egg item at the given location, sets it to invincible if enabled in the given config.
     * @return the egg item that was spawned
     */
    public static void spawnEggItem(Location loc, ConfigManager config, Data data) {
        ItemStack egg = new ItemStack(Material.DRAGON_EGG);
        egg.setAmount(1);
        Item drop = loc.getWorld().dropItem(loc, egg);
        drop.setGravity(false);
        drop.setGlowing(true);
        drop.setVelocity(new Vector().setX(0).setY(0).setZ(0));
        if (config.getBoolean(CONFIG_ITEMS.EGG_INV)) {
            drop.setInvulnerable(true);
        }
        data.updateEggLocation(drop);
    }

    /**
     * Respawns the dragon egg in the end
     */
    public static void respawnEgg(ConfigManager config, Data data, LogHandler logger) {
        logger.log("Respawning egg");
        Block eggBlock = getEggRespawnLocation(config).getBlock();
        eggBlock.setType(Material.DRAGON_EGG);
        data.updateEggLocation(eggBlock);
    }    
}
