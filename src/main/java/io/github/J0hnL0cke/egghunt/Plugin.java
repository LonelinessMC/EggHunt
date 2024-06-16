package io.github.J0hnL0cke.egghunt;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import io.github.J0hnL0cke.egghunt.Controller.Announcement;
import io.github.J0hnL0cke.egghunt.Controller.CommandHandler;
import io.github.J0hnL0cke.egghunt.Controller.ConfigManager;
import io.github.J0hnL0cke.egghunt.Controller.EggController;
import io.github.J0hnL0cke.egghunt.Controller.EggDestroyListener;
import io.github.J0hnL0cke.egghunt.Controller.MiscListener;
import io.github.J0hnL0cke.egghunt.Controller.ScoreboardController;
import io.github.J0hnL0cke.egghunt.Controller.ConfigManager.CONFIG_ITEMS;
import io.github.J0hnL0cke.egghunt.Controller.EventScheduler;
import io.github.J0hnL0cke.egghunt.Controller.InventoryListener;
import io.github.J0hnL0cke.egghunt.Model.Data;
import io.github.J0hnL0cke.egghunt.Model.LogHandler;
import io.github.J0hnL0cke.egghunt.Model.Version;
import io.github.J0hnL0cke.egghunt.Persistence.DataFileDAO;


public final class Plugin extends JavaPlugin {
    Data data;
    Version version;
    CommandHandler commandHandler;
    ConfigManager configManager;

    BukkitTask belowWorldTask;
    LogHandler logger;
	
	@Override
    public void onEnable() {
        logger = LogHandler.getInstance(getLogger());
        logger.info("Enabling the plugin");

        configManager = new ConfigManager(this);
        if(configManager.getBoolean(ConfigManager.CONFIG_ITEMS.DEBUG)){
            logger.setDebug(true);
        }

        data = new Data(this, DataFileDAO.getDataDAO(this, logger), logger);
        version = new Version();

        logger.setDebug(configManager.getBoolean(CONFIG_ITEMS.DEBUG));

        //create controller instances
        ScoreboardController.getScoreboardHandler(this, data, logger, version);
        MiscListener miscListener = new MiscListener(this, logger, data);
        InventoryListener inventoryListener = new InventoryListener(this, logger, data);
        EggDestroyListener destroyListener = new EggDestroyListener(this, logger, data);
        EventScheduler scheduler = new EventScheduler(this, data, logger);
        commandHandler = new CommandHandler(this, data);
		
		//register event handlers
        logger.log("Registering event listeners...");
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(miscListener, this);
        manager.registerEvents(inventoryListener, this);
        manager.registerEvents(destroyListener, this);

		//schedule tasks
		//TODO: disable task when not in use
		logger.log("Scheduling task...");
        belowWorldTask = scheduler.runTaskTimer(this, 20, 20);
        
		logger.info("EggHunt enabled.");
	}

	@Override
	public void onDisable() {
        logger.log("Preparing to disable EggHunt..."); //server already provides a disable message
        if (data != null) {
            //if a player has the egg in their inventory,
            //drop it on the ground in case the server is closing
            Entity eggHolder = data.getEggEntity();
            if (eggHolder != null) {
                if (eggHolder instanceof Player) {
                    logger.log("Egg is held by a player, dropping egg...");
                    Player p = (Player) eggHolder;
                    EggController.dropEgg(p, data, this.getConfigManager());
                    //in case the server isn't restarting, let the player know what happend
                    Announcement.getInstance(this).sendPrivateMessage(p, "The dragon egg was dropped from your inventory");
                }
            }
            logger.log("Saving data...");
            data.saveData();
        }
        logger.log("Disabling task...");
		if (belowWorldTask!=null) {
			belowWorldTask.cancel();
		}
		logger.info("EggHunt disabled.");
	}

	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return commandHandler.onCommand(sender, cmd, label, args);
    }

    public ConfigManager getConfigManager(){
        return this.configManager;
    }
    
}
