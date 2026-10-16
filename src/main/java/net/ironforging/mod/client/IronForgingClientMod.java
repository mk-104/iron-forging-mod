package net.ironforging.mod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.ironforging.mod.client.gui.IronExtractorScreen;
import net.ironforging.mod.client.render.IronExtractorCoreBlockEntityRenderer;
import net.ironforging.mod.registry.ModBlockEntities;
import net.ironforging.mod.registry.ModScreenHandlers;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/**
 * 鉄錬成装置Modのクライアント側初期化クラス（Mojmap対応版）。
 * MenuTypeとGUI画面（Screen）の紐付け登録、およびBlockEntityRendererの登録を行います。
 */
@Environment(EnvType.CLIENT)
public class IronForgingClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // MenuType と Screen を紐付け
        MenuScreens.register(
                ModScreenHandlers.IRON_EXTRACTOR_SCREEN_HANDLER,
                IronExtractorScreen::new
        );

        // BlockEntityRenderer (BER) の登録
        BlockEntityRenderers.register(
                ModBlockEntities.IRON_EXTRACTOR_CORE_BE,
                IronExtractorCoreBlockEntityRenderer::new
        );
    }
}
