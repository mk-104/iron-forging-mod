package net.ironforging.mod.registry;

import net.ironforging.mod.IronForgingMod;
import net.ironforging.mod.block.DummyPart;
import net.ironforging.mod.block.IronExtractorCoreBlock;
import net.ironforging.mod.block.IronExtractorDummyBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

/**
 * Mod内のブロック登録を統括するクラス（Mojmap対応版）。
 */
public class ModBlocks {

    // 鉄錬成装置コアブロック（燃焼稼働時は光度13で発光）
    public static final Block IRON_EXTRACTOR_CORE_BLOCK = register(
            "iron_extractor_core",
            key -> new IronExtractorCoreBlock(
                    BlockBehaviour.Properties.of()
                            .setId(key)
                            .strength(4.0f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .lightLevel(state -> state.getValue(IronExtractorCoreBlock.LIT) ? 13 : 0)
            )
    );

    // 鉄錬成装置ダミーブロック（マルチブロックの占有空間用、溶岩柱部位は光度15で発光）
    public static final Block IRON_EXTRACTOR_DUMMY_BLOCK = register(
            "iron_extractor_dummy",
            key -> new IronExtractorDummyBlock(
                    BlockBehaviour.Properties.of()
                            .setId(key)
                            .strength(4.0f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.GLASS)
                            .noOcclusion()
                            .lightLevel(state -> {
                                DummyPart part = state.getValue(IronExtractorDummyBlock.PART);
                                return (part == DummyPart.LAVA_FLOOR || part == DummyPart.LAVA_PILLAR || part == DummyPart.LAVA_NOZZLE || part == DummyPart.GOLEM_MELTING || part == DummyPart.FIRE) ? 15 : 0;
                            })
            )
    );

    private static Block register(String name, Function<ResourceKey<Block>, Block> blockFactory) {
        Identifier id = Identifier.fromNamespaceAndPath(IronForgingMod.MOD_ID, name);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        return Registry.register(
                BuiltInRegistries.BLOCK,
                key,
                blockFactory.apply(key)
        );
    }

    public static void registerBlocks() {
        IronForgingMod.LOGGER.info("Registering ModBlocks for " + IronForgingMod.MOD_ID);
    }
}
