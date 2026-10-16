package net.ironforging.mod.block;

import net.ironforging.mod.block.entity.IronExtractorCoreBlockEntity;
import net.ironforging.mod.block.entity.IronExtractorDummyBlockEntity;
import net.ironforging.mod.registry.ModBlockEntities;
import net.ironforging.mod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 鉄錬成装置の本体となるコアブロック（Mojmap対応版）。
 * 正面下部中央に配置され、プレイヤーの向いている方向に応じて奥・左右・上方に
 * 5x5x5（幅5×奥行5×高さ5）の重厚な工業用ガラス・フレーム・溶岩・スポナー・ゴーレム構造を展開します。
 */
public class IronExtractorCoreBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public IronExtractorCoreBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IronExtractorCoreBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * ローカル座標 (lx, ly, lz) に応じたパーツ種類を判定します。
     * lx: -2..2 (左から右)
     * ly: 0..4 (底面から天井)
     * lz: 0..4 (正面から背面奥へ)
     * (0, 0, 0) はコア自身のため null を返します。
     */
    public static @Nullable DummyPart getPartAt(int lx, int ly, int lz) {
        return getPartAt(lx, ly, lz, Direction.NORTH);
    }

    public static @Nullable DummyPart getPartAt(int lx, int ly, int lz, Direction facing) {
        if (lx == 0 && ly == 0 && lz == 0) {
            return null; // コア自身
        }

        // 1. 手前正面最下段（ly=0, lz=0）: ホッパー接続専用ポート（横2ブロックずつ）
        if (ly == 0 && lz == 0) {
            if (lx == -2 || lx == -1) {
                return DummyPart.FUEL_PORT;   // コア左側2マス: 燃料投入口（ホッパー＋石炭イラスト）
            }
            if (lx == 1 || lx == 2) {
                return DummyPart.OUTPUT_PORT; // コア右側2マス: 鉄取出口（ホッパー＋鉄インゴットイラスト）
            }
        }

        // 2. 4隅の垂直金属フレーム柱
        if (Math.abs(lx) == 2 && (lz == 0 || lz == 4)) {
            return DummyPart.FRAME_CORNER;
        }

        // 2. 上下の水平金属フレーム梁
        if (ly == 0 || ly == 4) {
            if (Math.abs(lx) == 2 || lz == 0 || lz == 4) {
                return DummyPart.FRAME_BEAM;
            }
        }

        // 3. 外壁のクリアガラス窓 (y: 1..3 の外周面)
        if (ly >= 1 && ly <= 3) {
            if (Math.abs(lx) == 2 || lz == 0 || lz == 4) {
                // 各側面（4面）の中央ブロック（ly=2）に 3x3 の特大1枚ガラス窓を配置
                if (ly == 2) {
                    boolean isZAxis = facing.getAxis() == Direction.Axis.Z;
                    if (lx == 0 && (lz == 0 || lz == 4)) {
                        // 手前面・奥面: コア向きがZ軸ならワールドXY面(BIG_GLASS_Z)、X軸ならワールドZY面(BIG_GLASS_X)
                        return isZAxis ? DummyPart.BIG_GLASS_Z : DummyPart.BIG_GLASS_X;
                    }
                    if (lz == 2 && Math.abs(lx) == 2) {
                        // 左右面: コア向きがZ軸ならワールドZY面(BIG_GLASS_X)、X軸ならワールドXY面(BIG_GLASS_Z)
                        return isZAxis ? DummyPart.BIG_GLASS_X : DummyPart.BIG_GLASS_Z;
                    }
                }
                // 不可視ブロックは配置せず、3x3に拡大した中央ブロックのみ設置
                return null;
            }
        }

        // 4. 底面（ly == 0）の内部: 溶岩を完全に廃止し、重厚な金属フレーム床に変更
        if (ly == 0) {
            return DummyPart.FRAME_BEAM;
        }

        // 5. 天井（ly == 4）の内部: 排気グリル（スモークベント）
        if (ly == 4) {
            return DummyPart.TOP_GRATE;
        }

        // 6. 内部空間 (ly: 1..3, lx: -1..1, lz: 1..3)
        // 最下段 (ly == 1) の 3x3 範囲:
        // - 中心 (lx == 0, lz == 2): ゴーレム足元のインゴット金型 (MOLD)
        // - 周囲 8 マス: 燃え盛る炎 (FIRE)
        if (ly == 1) {
            if (lx == 0 && lz == 2) {
                return DummyPart.MOLD;
            } else {
                return DummyPart.FIRE;
            }
        }

        // 上部空間 (ly == 2..3) はすべて空間 (null)
        // スポナー、アイアンゴーレム、下降プレス機、鉄インゴットは BER (動的レンダラー) により描画
        return null;
    }

    /**
     * コア座標と向き（Facing）からローカル座標 (lx, ly, lz) のワールド座標を計算します。
     */
    public static BlockPos getBlockPosForLocal(BlockPos corePos, Direction facing, int lx, int ly, int lz) {
        Direction forward = facing.getOpposite(); // 正面（手前）を向いているため、奥方向は facing の逆
        Direction right = forward.getClockWise();
        return corePos.relative(forward, lz)
                .relative(right, lx)
                .relative(Direction.UP, ly);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, placer, itemStack);

        if (level.isClientSide()) {
            return;
        }

        Direction facing = state.getValue(FACING);

        // 設置可能か全5x5x5スペースを事前チェック
        boolean canPlace = true;
        for (int ly = 0; ly < 5; ly++) {
            for (int lx = -2; lx <= 2; lx++) {
                for (int lz = 0; lz < 5; lz++) {
                    if (lx == 0 && ly == 0 && lz == 0) {
                        continue;
                    }

                    BlockPos targetPos = getBlockPosForLocal(pos, facing, lx, ly, lz);

                    if (!level.isLoaded(targetPos)) {
                        canPlace = false;
                        break;
                    }

                    BlockState targetState = level.getBlockState(targetPos);
                    if (!targetState.isAir() && !targetState.canBeReplaced()) {
                        canPlace = false;
                        break;
                    }
                }
                if (!canPlace) {
                    break;
                }
            }
            if (!canPlace) {
                break;
            }
        }

        if (!canPlace) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            Block.popResource(level, pos, new ItemStack(this));

            if (placer instanceof Player player) {
                player.sendSystemMessage(Component.literal("§c設置スペースが足りません (幅5x奥行5x高さ5の空間が必要です)"));
            }
            return;
        }

        // ダミーブロックおよび内部空間の自動配置
        for (int ly = 0; ly < 5; ly++) {
            for (int lx = -2; lx <= 2; lx++) {
                for (int lz = 0; lz < 5; lz++) {
                    if (lx == 0 && ly == 0 && lz == 0) {
                        continue;
                    }

                    BlockPos targetPos = getBlockPosForLocal(pos, facing, lx, ly, lz);
                    DummyPart part = getPartAt(lx, ly, lz, facing);

                    if (part != null) {
                        BlockState dummyState = ModBlocks.IRON_EXTRACTOR_DUMMY_BLOCK.defaultBlockState()
                                .setValue(IronExtractorDummyBlock.PART, part);
                        level.setBlock(targetPos, dummyState, Block.UPDATE_ALL);

                        BlockEntity be = level.getBlockEntity(targetPos);
                        if (be instanceof IronExtractorDummyBlockEntity dummyBe) {
                            dummyBe.setCorePos(pos);
                        }
                    } else {
                        // 内部の空気スペース
                        level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level instanceof Level l && !l.isClientSide()) {
            BlockEntity be = l.getBlockEntity(pos);
            if (be instanceof IronExtractorCoreBlockEntity coreBe) {
                Containers.dropContents(l, pos, coreBe);
                l.updateNeighbourForOutputSignal(pos, this);
            }

            Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;

            // ローカル座標基準でダミーを一括消去
            for (int ly = 0; ly < 5; ly++) {
                for (int lx = -2; lx <= 2; lx++) {
                    for (int lz = 0; lz < 5; lz++) {
                        if (lx == 0 && ly == 0 && lz == 0) {
                            continue;
                        }

                        BlockPos dummyPos = getBlockPosForLocal(pos, facing, lx, ly, lz);
                        if (l.isLoaded(dummyPos)) {
                            BlockState targetState = l.getBlockState(dummyPos);
                            if (targetState.is(ModBlocks.IRON_EXTRACTOR_DUMMY_BLOCK)) {
                                l.setBlock(dummyPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                            }
                        }
                    }
                }
            }

            // 安全のための周囲（7x7x7）自己診断スキャン（残留防止）
            for (int dy = -1; dy <= 5; dy++) {
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        BlockPos checkPos = pos.offset(dx, dy, dz);
                        if (l.isLoaded(checkPos) && l.getBlockState(checkPos).is(ModBlocks.IRON_EXTRACTOR_DUMMY_BLOCK)) {
                            BlockEntity checkBe = l.getBlockEntity(checkPos);
                            if (checkBe instanceof IronExtractorDummyBlockEntity dummyBe) {
                                if (pos.equals(dummyBe.getCorePos())) {
                                    l.setBlock(checkPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                                }
                            }
                        }
                    }
                }
            }
        }
        super.destroy(level, pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IronExtractorCoreBlockEntity coreBe) {
                player.openMenu(coreBe);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }

        Direction facing = state.getValue(FACING);
        Direction forward = facing.getOpposite();
        Direction right = forward.getClockWise();

        // 1. 天井排気口（y=4, 中央3x3）から立ち上る濃いキャンプファイヤー煙
        if (random.nextFloat() < 0.85F) {
            int rx = random.nextInt(3) - 1; // -1, 0, 1
            int rz = random.nextInt(3) + 1; // 1, 2, 3
            BlockPos topPos = getBlockPosForLocal(pos, facing, rx, 4, rz);
            double smokeX = topPos.getX() + 0.3 + random.nextDouble() * 0.4;
            double smokeY = topPos.getY() + 1.05;
            double smokeZ = topPos.getZ() + 0.3 + random.nextDouble() * 0.4;
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, smokeX, smokeY, smokeZ, 0.0, 0.08, 0.0);
        }

        // 2. 内部溶岩で溶かされているアイアンゴーレム付近（lx=0, ly=1, lz=2）からの炎と溶岩飛沫
        BlockPos golemPos = getBlockPosForLocal(pos, facing, 0, 1, 2);
        if (random.nextFloat() < 0.60F) {
            double fx = golemPos.getX() + 0.2 + random.nextDouble() * 0.6;
            double fy = golemPos.getY() + 0.5 + random.nextDouble() * 0.7;
            double fz = golemPos.getZ() + 0.2 + random.nextDouble() * 0.6;
            level.addParticle(ParticleTypes.FLAME, fx, fy, fz, 0.0, 0.02, 0.0);
        }
        if (random.nextFloat() < 0.30F) {
            double lx = golemPos.getX() + 0.1 + random.nextDouble() * 0.8;
            double ly = golemPos.getY() + 0.2 + random.nextDouble() * 0.4;
            double lz = golemPos.getZ() + 0.1 + random.nextDouble() * 0.8;
            level.addParticle(ParticleTypes.SMALL_FLAME, lx, ly, lz, 0.0, 0.02, 0.0);
        }

        // 3. かまどの燃焼パチパチ音
        if (random.nextDouble() < 0.15) {
            level.playLocalSound(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    SoundEvents.FURNACE_FIRE_CRACKLE,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F,
                    false
            );
        }

        // 4. アイアンゴーレムが溶かされる重厚なダメージ音
        if (random.nextDouble() < 0.04) {
            float pitch = 0.5F + random.nextFloat() * 0.15F;
            level.playLocalSound(
                    golemPos.getX() + 0.5,
                    golemPos.getY() + 0.5,
                    golemPos.getZ() + 0.5,
                    SoundEvents.IRON_GOLEM_HURT,
                    SoundSource.BLOCKS,
                    0.5F,
                    pitch,
                    false
            );
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.IRON_EXTRACTOR_CORE_BE, IronExtractorCoreBlockEntity::tick);
    }
}
