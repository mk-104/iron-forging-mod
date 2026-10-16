package net.ironforging.mod.block;

import net.ironforging.mod.block.entity.IronExtractorDummyBlockEntity;
import net.ironforging.mod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * 5x5x5マルチブロックの空間を埋めるためのダミーブロック（Mojmap対応版）。
 */
public class IronExtractorDummyBlock extends BaseEntityBlock {

    public static final EnumProperty<DummyPart> PART = EnumProperty.create("part", DummyPart.class);

    // 3x3に拡大されたバニラガラス用のVoxelShape (幅3x高さ3x奥行き1)
    private static final VoxelShape SHAPE_BIG_GLASS_Z = Block.box(-16.0, -16.0, 0.0, 32.0, 32.0, 16.0);
    private static final VoxelShape SHAPE_BIG_GLASS_X = Block.box(0.0, -16.0, -16.0, 16.0, 32.0, 32.0);

    public IronExtractorDummyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, DummyPart.GLASS_WALL));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        DummyPart part = state.getValue(PART);
        if (part == DummyPart.BIG_GLASS_Z) {
            return SHAPE_BIG_GLASS_Z;
        }
        if (part == DummyPart.BIG_GLASS_X) {
            return SHAPE_BIG_GLASS_X;
        }
        return super.getShape(state, level, pos, context);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IronExtractorDummyBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.emptyList();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IronExtractorDummyBlockEntity dummyBe) {
                BlockPos corePos = dummyBe.getCorePos();

                if (corePos != null && level.isLoaded(corePos)) {
                    BlockState coreState = level.getBlockState(corePos);
                    if (coreState.is(ModBlocks.IRON_EXTRACTOR_CORE_BLOCK)) {
                        BlockHitResult redirectedHit = new BlockHitResult(hit.getLocation(), hit.getDirection(), corePos, hit.isInside());
                        return coreState.useWithoutItem(level, player, redirectedHit);
                    }
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        super.destroy(level, pos, state);
        if (level instanceof Level l && !l.isClientSide()) {
            BlockEntity be = l.getBlockEntity(pos);
            if (be instanceof IronExtractorDummyBlockEntity dummyBe) {
                BlockPos corePos = dummyBe.getCorePos();

                if (corePos != null && l.isLoaded(corePos)) {
                    BlockState coreState = l.getBlockState(corePos);
                    if (coreState.is(ModBlocks.IRON_EXTRACTOR_CORE_BLOCK)) {
                        l.destroyBlock(corePos, true);
                    }
                }
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block sourceBlock, @Nullable Orientation orientation, boolean isMoving) {
        super.neighborChanged(state, level, pos, sourceBlock, orientation, isMoving);

        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IronExtractorDummyBlockEntity dummyBe) {
                BlockPos corePos = dummyBe.getCorePos();

                if (corePos != null && level.isLoaded(corePos)) {
                    BlockState coreState = level.getBlockState(corePos);
                    if (!coreState.is(ModBlocks.IRON_EXTRACTOR_CORE_BLOCK)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                } else if (corePos == null) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }
}
