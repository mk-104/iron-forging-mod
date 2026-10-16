package net.ironforging.mod.registry;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.ironforging.mod.IronForgingMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * クリエイティブインベントリのタブ登録を統括するクラス。
 * Mod専用タブ「鉄錬成Mod」の作成、およびバニラの機能ブロック・レッドストーンタブへの追加を行います。
 */
public class ModCreativeModeTabs {

    // Mod専用クリエイティブタブのキー
    public static final ResourceKey<CreativeModeTab> IRON_FORGING_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(IronForgingMod.MOD_ID, "iron_forging_tab")
    );

    // Mod専用クリエイティブタブ本体
    public static final CreativeModeTab IRON_FORGING_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            IRON_FORGING_TAB_KEY,
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.iron_forging"))
                    .icon(() -> new ItemStack(ModItems.IRON_EXTRACTOR_CORE_ITEM))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.IRON_EXTRACTOR_CORE_ITEM);
                    })
                    .build()
    );

    public static void registerCreativeModeTabs() {
        IronForgingMod.LOGGER.info("Registering ModCreativeModeTabs for " + IronForgingMod.MOD_ID);

        // バニラの「機能ブロック」タブにも追加
        ResourceKey<CreativeModeTab> functionalKey = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                Identifier.withDefaultNamespace("functional_blocks")
        );
        CreativeModeTabEvents.modifyOutputEvent(functionalKey).register(output -> {
            output.accept(ModItems.IRON_EXTRACTOR_CORE_ITEM);
        });

        // バニラの「レッドストーン」タブにも追加
        ResourceKey<CreativeModeTab> redstoneKey = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                Identifier.withDefaultNamespace("redstone_blocks")
        );
        CreativeModeTabEvents.modifyOutputEvent(redstoneKey).register(output -> {
            output.accept(ModItems.IRON_EXTRACTOR_CORE_ITEM);
        });
    }
}
