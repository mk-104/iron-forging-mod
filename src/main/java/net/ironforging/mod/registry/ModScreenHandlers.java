package net.ironforging.mod.registry;

import net.ironforging.mod.IronForgingMod;
import net.ironforging.mod.screen.IronExtractorScreenHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/**
 * Mod内のMenuType（ScreenHandlerType）登録を統括するクラス（Mojmap対応版）。
 */
public class ModScreenHandlers {

    public static final MenuType<IronExtractorScreenHandler> IRON_EXTRACTOR_SCREEN_HANDLER = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(IronForgingMod.MOD_ID, "iron_extractor"),
            new MenuType<>(IronExtractorScreenHandler::new, FeatureFlags.VANILLA_SET)
    );

    public static void registerScreenHandlers() {
        IronForgingMod.LOGGER.info("Registering ModScreenHandlers for " + IronForgingMod.MOD_ID);
    }
}
