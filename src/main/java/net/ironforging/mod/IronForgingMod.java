package net.ironforging.mod;

import net.fabricmc.api.ModInitializer;
import net.ironforging.mod.registry.ModBlockEntities;
import net.ironforging.mod.registry.ModBlocks;
import net.ironforging.mod.registry.ModCreativeModeTabs;
import net.ironforging.mod.registry.ModItems;
import net.ironforging.mod.registry.ModScreenHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 鉄錬成装置Mod（IronForgingMod）のメイン初期化クラス。
 */
public class IronForgingMod implements ModInitializer {

    public static final String MOD_ID = "iron_forging";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing IronForgingMod...");

        // ブロック・BlockEntity・アイテム・ScreenHandler・クリエイティブタブの登録
        ModBlocks.registerBlocks();
        ModBlockEntities.registerBlockEntities();
        ModItems.registerItems();
        ModScreenHandlers.registerScreenHandlers();
        ModCreativeModeTabs.registerCreativeModeTabs();

        LOGGER.info("IronForgingMod initialized successfully.");
    }
}
