package net.ironforging.mod.registry;

import net.ironforging.mod.IronForgingMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.function.Function;

/**
 * Mod内のアイテム登録を統括するクラス（Mojmap対応版）。
 */
public class ModItems {

    // 鉄錬成装置コアのブロックアイテム
    public static final Item IRON_EXTRACTOR_CORE_ITEM = register(
            "iron_extractor_core",
            key -> new BlockItem(ModBlocks.IRON_EXTRACTOR_CORE_BLOCK, new Item.Properties().setId(key))
    );

    private static Item register(String name, Function<ResourceKey<Item>, Item> itemFactory) {
        Identifier id = Identifier.fromNamespaceAndPath(IronForgingMod.MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                itemFactory.apply(key)
        );
    }

    public static void registerItems() {
        IronForgingMod.LOGGER.info("Registering ModItems for " + IronForgingMod.MOD_ID);
    }
}
