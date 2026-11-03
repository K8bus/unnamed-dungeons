package com.k8bus;

import com.k8bus.block.ModBlocks;
import com.k8bus.block.entity.ModBlockEntities;
import com.k8bus.command.DungeonCommand;
import com.k8bus.dungeon.DungeonTickHandler;
import com.k8bus.network.ServerNetworkHandler;
import com.k8bus.effect.ModEffects;
import com.k8bus.menu.ModMenuTypes;
import com.k8bus.enchantment.ModEnchantments;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnnamedDungeons implements ModInitializer {
	public static final String MOD_ID = "unnamed-dungeons";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEnchantments.initialize();
		ModEffects.initialize();
		ModBlocks.initialize();       // najpierw bloki
		ModBlockEntities.initialize(); // potem block entities
		ModMenuTypes.initialize();    // potem menu
		ServerNetworkHandler.initialize();
		com.k8bus.dungeon.event.DungeonPlayerLifecycleEvents.initialize();
		DungeonCommand.register();
		DungeonTickHandler.register();
		LOGGER.info("Unnamed Dungeons initialized!");
	}
}