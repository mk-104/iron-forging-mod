package net.ironforging.mod.registry;

import net.ironforging.mod.IronForgingMod;
import net.ironforging.mod.block.entity.IronExtractorCoreBlockEntity;
import net.ironforging.mod.block.entity.IronExtractorDummyBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/**
 * Mod内のBlockEntityタイプ登録を統括するクラス（Mojmap対応版）。
 */
public class ModBlockEntities {

    public static final BlockEntityType<IronExtractorCoreBlockEntity> IRON_EXTRACTOR_CORE_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(IronForgingMod.MOD_ID, "iron_extractor_core_be"),
            new BlockEntityType<>(
                    IronExtractorCoreBlockEntity::new,
                    Set.of(ModBlocks.IRON_EXTRACTOR_CORE_BLOCK)
            )
    );

    public static final BlockEntityType<IronExtractorDummyBlockEntity> IRON_EXTRACTOR_DUMMY_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(IronForgingMod.MOD_ID, "iron_extractor_dummy_be"),
            new BlockEntityType<>(
                    IronExtractorDummyBlockEntity::new,
                    Set.of(ModBlocks.IRON_EXTRACTOR_DUMMY_BLOCK)
            )
    );

    public static void registerBlockEntities() {
        IronForgingMod.LOGGER.info("Registering ModBlockEntities for " + IronForgingMod.MOD_ID);
    }
}
