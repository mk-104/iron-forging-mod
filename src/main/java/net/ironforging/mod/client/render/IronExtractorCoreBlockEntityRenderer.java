package net.ironforging.mod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.ironforging.mod.block.IronExtractorCoreBlock;
import net.ironforging.mod.block.entity.IronExtractorCoreBlockEntity;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 鉄錬成装置コアの動的BlockEntityRenderer（Mojmap 26.3対応版）。
 * 工業的プレス機演出：
 * 1. コアブロック（溶鉱炉側）を向くようにゴーレムの向きを反転（プレイヤーに背中を向ける）
 * 2. 進行度0%〜50%: プレス機（重り鉄ブロック）が上空からゴーレム頭上へ下降
 * 3. 進行度50%〜90%: プレス開始で苦悶テクスチャ（iron_golem_pain）に切り替え、金型（Mold）の上で押し潰す圧縮演出
 * 4. 進行度90%〜100%: ゴーレム消去、プレス機が上昇し、金型の上から完成した鉄インゴットが出現
 */
@Environment(EnvType.CLIENT)
public class IronExtractorCoreBlockEntityRenderer implements BlockEntityRenderer<IronExtractorCoreBlockEntity, IronExtractorRenderState> {

    private static final Identifier GOLEM_TEXTURE = Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png");
    private static final Identifier GOLEM_PAIN_TEXTURE = Identifier.fromNamespaceAndPath("iron_forging", "textures/entity/iron_golem_pain.png");
    private static final AtomicInteger DUMMY_ID_COUNTER = new AtomicInteger(-100000);

    // コアブロック（手前正面・溶鉱炉）をまっすぐ見つめるオフセット (0度)
    private static final float GOLEM_YAW_OFFSET = 0.0F;

    // 内部空間の幾何定数（金型トレイの上に立つ）
    private static final double FLOOR_Y = 1.15;          // 金型（Mold）上面の高さ
    private static final double GOLEM_HEIGHT = 2.7;      // ゴーレム全高 (約2.7ブロック)
    private static final double PRESS_CEILING_Y = 4.2;   // プレス機待機時の天井格納高さ

    // アニメーション進行度（progress: 0.0F 〜 1.0F）のフェーズ分割
    // 0.00F 〜 0.35F (35%): 天井からゴーレム頭上へ下降
    // 0.35F 〜 0.65F (30%): ゴーレムを金型上面までペチャンコに圧縮
    // 0.65F 〜 1.00F (35%): 床から天井へ完全にリフトアップ（100%でピッタリ天井格納完了）
    private static final float PHASE_DROP_END = 0.35F;
    private static final float PHASE_PRESS_END = 0.65F;

    private final EntityRenderDispatcher entityRenderer;
    private final BlockModelResolver blockModelResolver;
    private final ItemModelResolver itemModelResolver;
    private final IronGolemModel golemModel;

    // パフォーマンスのためダミーエンティティをキャッシュ
    private IronGolem dummyGolem;

    public IronExtractorCoreBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
        this.blockModelResolver = context.blockModelResolver();
        this.itemModelResolver = context.itemModelResolver();
        this.golemModel = new IronGolemModel(context.bakeLayer(ModelLayers.IRON_GOLEM));
    }

    @Override
    public IronExtractorRenderState createRenderState() {
        return new IronExtractorRenderState();
    }

    @Override
    public void extractRenderState(IronExtractorCoreBlockEntity blockEntity, IronExtractorRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(blockEntity, state, crumbling);

        BlockState blockState = blockEntity.getBlockState();
        state.facing = blockState.hasProperty(IronExtractorCoreBlock.FACING) ? blockState.getValue(IronExtractorCoreBlock.FACING) : Direction.NORTH;
        state.isLit = blockState.hasProperty(IronExtractorCoreBlock.LIT) && blockState.getValue(IronExtractorCoreBlock.LIT);
        state.progress = blockEntity.getProgress(partialTick);
        state.clientTicks = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() + partialTick : 0.0F;

        // 1. ダミーのアイアンゴーレムエンティティの準備と向き・状態の固定
        if (this.dummyGolem == null && blockEntity.getLevel() != null) {
            this.dummyGolem = new IronGolem(EntityTypes.IRON_GOLEM, blockEntity.getLevel());
            this.dummyGolem.setId(DUMMY_ID_COUNTER.decrementAndGet());
            this.dummyGolem.setPos(blockEntity.getBlockPos().getX() + 0.5, blockEntity.getBlockPos().getY(), blockEntity.getBlockPos().getZ() + 0.5);
        }

        if (this.dummyGolem != null) {
            // コアブロック（手前正面）をまっすぐ見つめる向き
            float yaw = state.facing.toYRot() + GOLEM_YAW_OFFSET;
            this.dummyGolem.setYRot(yaw);
            this.dummyGolem.yRotO = yaw;
            this.dummyGolem.setYBodyRot(yaw);
            this.dummyGolem.yBodyRotO = yaw;
            this.dummyGolem.setYHeadRot(yaw);
            this.dummyGolem.yHeadRotO = yaw;

            // コアブロックを見下ろす視線ピッチ
            float lookPitch = (state.progress < PHASE_DROP_END) ? 14.0F : -5.0F;
            this.dummyGolem.setXRot(lookPitch);
            this.dummyGolem.xRotO = lookPitch;

            // 白枠・発光フラグ防止
            this.dummyGolem.setGlowingTag(false);
            this.dummyGolem.hurtTime = 0;

            state.golemRenderState = this.entityRenderer.extractEntity(this.dummyGolem, partialTick);

            if (state.golemRenderState instanceof IronGolemRenderState golemState) {
                golemState.hasRedOverlay = false;
                golemState.attackTicksRemaining = 0.0F;
                golemState.bodyRot = yaw;
                golemState.yRot = 0.0F;
                golemState.outlineColor = 0;
                golemState.xRot = lookPitch; // コアブロックを見つめる視線
            }
        }

        // 2. スポナーブロックモデルのRenderState更新
        this.blockModelResolver.update(state.spawnerModel, Blocks.SPAWNER.defaultBlockState(), BlockDisplayContext.create());

        // 3. プレス機ブロックモデル（Blocks.IRON_BLOCK）のRenderState更新
        this.blockModelResolver.update(state.pressModel, Blocks.IRON_BLOCK.defaultBlockState(), BlockDisplayContext.create());

        // 4. 終盤の鉄インゴット（Items.IRON_INGOT）RenderState更新
        if (state.isLit && state.progress >= PHASE_PRESS_END && blockEntity.getLevel() != null) {
            this.itemModelResolver.updateForTopItem(
                    state.ingotItemState,
                    new ItemStack(Items.IRON_INGOT),
                    ItemDisplayContext.GROUND,
                    blockEntity.getLevel(),
                    null,
                    blockEntity.getBlockPos().hashCode()
            );
        } else {
            state.ingotItemState.clear();
        }

        // 5. 3x3特大バニラガラス窓のRenderState更新
        this.blockModelResolver.update(state.glassModel, Blocks.GLASS.defaultBlockState(), BlockDisplayContext.create());

        // 6. 外部燃料ゲージ用モデルのRenderState更新
        state.burnProgress = blockEntity.getBurnProgress();
        this.blockModelResolver.update(state.gaugeFrameModel, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), BlockDisplayContext.create());

        BlockState fuelState;
        if (state.burnProgress > 0.5F) {
            fuelState = Blocks.EMERALD_BLOCK.defaultBlockState();       // 高残量: エメラルドグリーン
        } else if (state.burnProgress > 0.2F) {
            fuelState = Blocks.GOLD_BLOCK.defaultBlockState();          // 中残量: ゴールデンイエロー
        } else if (state.burnProgress > 0.0F) {
            fuelState = Blocks.REDSTONE_BLOCK.defaultBlockState();      // ピンチ: 発光レッドストーン
        } else {
            fuelState = Blocks.COAL_BLOCK.defaultBlockState();          // 燃料切れ: 消灯コールブラック
        }
        this.blockModelResolver.update(state.fuelGaugeModel, fuelState, BlockDisplayContext.create());
    }

    @Override
    public void submit(IronExtractorRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Direction facing = state.facing;
        Direction forward = facing.getOpposite(); // 正面（手前）を向いているため、奥方向は facing の逆
        Direction right = forward.getClockWise();

        // --------------------------------------------------------------------
        // 1. スポナーの描画 (奥の壁際空中: lx=0, ly=2.8, lz=3.65 / サイズ0.6倍)
        // --------------------------------------------------------------------
        poseStack.pushPose();
        double spawnerX = forward.getStepX() * 3.65 + right.getStepX() * 0.0 + 0.5;
        double spawnerY = 2.8 + Math.sin(state.clientTicks * 0.06) * 0.05; // わずかな浮遊上下運動
        double spawnerZ = forward.getStepZ() * 3.65 + right.getStepZ() * 0.0 + 0.5;

        poseStack.translate(spawnerX, spawnerY, spawnerZ);
        poseStack.scale(0.6F, 0.6F, 0.6F); // 要件3: 本来の6割 (0.6倍) に縮小
        poseStack.translate(-0.5, -0.5, -0.5);

        state.spawnerModel.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();

        // 内部中央（ゴーレム、金型、およびプレス機の水平位置: lx=0, lz=2）
        double centerX = forward.getStepX() * 2.0 + right.getStepX() * 0.0 + 0.5;
        double centerZ = forward.getStepZ() * 2.0 + right.getStepZ() * 0.0 + 0.5;

        float progress = state.progress;

        // --------------------------------------------------------------------
        // プレス機（重り）の高さ pressY の計算
        // --------------------------------------------------------------------
        double pressY;
        double targetHeadY = FLOOR_Y + GOLEM_HEIGHT;
        if (!state.isLit || progress <= 0.0F) {
            // 未稼働時は天井裏で待機
            pressY = PRESS_CEILING_Y;
        } else if (progress < PHASE_DROP_END) {
            // 前半（0%〜35%）: 天井からゴーレム頭上（FLOOR_Y + GOLEM_HEIGHT）へゆっくり下降
            float t1 = progress / PHASE_DROP_END;
            pressY = PRESS_CEILING_Y - (t1 * (PRESS_CEILING_Y - targetHeadY));
        } else if (progress < PHASE_PRESS_END) {
            // 中盤（35%〜65%）: ゴーレムを金型上面（FLOOR_Y）までペチャンコに押し潰す
            float t2 = (progress - PHASE_DROP_END) / (PHASE_PRESS_END - PHASE_DROP_END); // 0.0 〜 1.0
            float scaleY = 1.0F - t2;
            pressY = FLOOR_Y + (GOLEM_HEIGHT * scaleY);
        } else {
            // 終盤（65%〜100%）: 潰し切った後、金型上面（FLOOR_Y）から天井（PRESS_CEILING_Y）まで完全に上昇
            // 進行度1.0（100%）に到達した瞬間にピッタリ天井に戻りきる
            float t3 = (progress - PHASE_PRESS_END) / (1.0F - PHASE_PRESS_END); // 0.0 〜 1.0
            t3 = Mth.clamp(t3, 0.0F, 1.0F);
            pressY = FLOOR_Y + (t3 * (PRESS_CEILING_Y - FLOOR_Y));
        }

        // --------------------------------------------------------------------
        // 2. プレス機（重り鉄ブロック）の描画
        // --------------------------------------------------------------------
        poseStack.pushPose();
        poseStack.translate(centerX, pressY, centerZ);

        // 圧縮中（35%〜65%）は激しい圧縮抵抗によるガタガタ振動
        if (state.isLit && progress >= PHASE_DROP_END && progress < PHASE_PRESS_END) {
            float jitter = (float) (Math.sin(state.clientTicks * 6.0) * 0.015);
            poseStack.translate(jitter, 0.0, -jitter);
        }

        // ゴーレムの肩幅をしっかり覆う 1.4 x 1.0 x 1.4 の巨大プレスヘッド
        poseStack.scale(1.4F, 1.0F, 1.4F);
        poseStack.translate(-0.5, 0.0, -0.5); // 底面を pressY に揃え、中心合わせ
        state.pressModel.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();

        // --------------------------------------------------------------------
        // 3. アイアンゴーレムの描画（進行度 0%〜65% の間のみ描画、65%以降は消去）
        // --------------------------------------------------------------------
        if (progress < PHASE_PRESS_END && state.golemRenderState instanceof IronGolemRenderState golemState) {
            poseStack.pushPose();
            poseStack.translate(centerX, FLOOR_Y, centerZ);

            // 要件1: コアブロック側を向くように回転固定
            float yaw = state.facing.toYRot() + GOLEM_YAW_OFFSET;
            poseStack.rotateDegrees(Axis.YP, 180.0F - yaw);

            // 稼働中の振動・身もだえ演出
            if (state.isLit) {
                if (progress >= PHASE_DROP_END) {
                    // 押し潰されている間の激しい抵抗振動
                    float jitterX = (float) (Math.sin(state.clientTicks * 6.0) * 0.035F);
                    float jitterZ = (float) (Math.cos(state.clientTicks * 5.5) * 0.035F);
                    poseStack.translate(jitterX, 0.0, jitterZ);
                } else {
                    // 迫りくるプレス機を前にした微小な震え
                    float jitterX = (float) (Math.sin(state.clientTicks * 2.0) * 0.008F);
                    poseStack.translate(jitterX, 0.0, 0.0);
                }
            }

            // 要件3: 足元（金型上面）基準での圧縮（Squish）アニメーション
            float scaleY;
            float scaleXZ;
            if (progress < PHASE_DROP_END) {
                scaleY = 1.0F;
                scaleXZ = 1.0F;
            } else {
                float t2 = (progress - PHASE_DROP_END) / (PHASE_PRESS_END - PHASE_DROP_END); // 0.0 〜 1.0
                scaleY = Math.max(0.01F, 1.0F - t2);
                // ペチャンコになるにつれて横にベチャッと広がるスクワッシュ演出
                scaleXZ = 1.0F + (t2 * 0.45F);
            }

            // 足元を基準にペチャンコに潰すスケール
            poseStack.scale(scaleXZ, scaleY, scaleXZ);

            // バニラモデル座標系への適合（Y軸反転と原点合わせ）
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0F, -1.501F, 0.0F);

            // モデルのアニメーション計算
            this.golemModel.setupAnim(golemState);

            // 要件4: プレス開始（progress >= PHASE_DROP_END）で苦悶テクスチャ（iron_golem_pain）に切り替え
            Identifier currentTexture = (state.isLit && progress >= PHASE_DROP_END) ? GOLEM_PAIN_TEXTURE : GOLEM_TEXTURE;

            collector.submitModel(
                    this.golemModel,
                    golemState,
                    poseStack,
                    this.golemModel.renderType(currentTexture),
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0
            );

            poseStack.popPose();
        }

        // --------------------------------------------------------------------
        // 4. 鉄インゴットの出現演出（進行度 65%〜100%）
        // --------------------------------------------------------------------
        if (progress >= PHASE_PRESS_END && !state.ingotItemState.isEmpty()) {
            poseStack.pushPose();

            float t3 = (progress - PHASE_PRESS_END) / (1.0F - PHASE_PRESS_END); // 0.0 〜 1.0
            t3 = Mth.clamp(t3, 0.0F, 1.0F);

            // 金型の上から、プレス機が持ち上がるのに合わせてフワッと浮かび上がる
            double ingotY = FLOOR_Y + 0.10 + (t3 * 0.35);

            poseStack.translate(centerX, ingotY, centerZ);

            // ゆっくり回転しながら完成品をアピール
            float spin = (state.clientTicks * 5.0F) % 360.0F;
            poseStack.rotateDegrees(Axis.YP, spin);
            poseStack.scale(1.3F, 1.3F, 1.3F);

            state.ingotItemState.submit(
                    poseStack,
                    collector,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0
            );

            poseStack.popPose();
        }

        // --------------------------------------------------------------------
        // 5. 外壁4面の特大3x3バニラガラス窓の描画
        // バニラのガラスブロック（Blocks.GLASS）をそのまま 3x3 にスケール拡大し、
        // 9ブロックの格子分割のない「1枚の巨大なバニラガラス」としてレンダリング
        // --------------------------------------------------------------------
        float yaw = state.facing.toYRot();

        // 4面の中央座標 (Y=2.5)
        double frontX = centerX - forward.getStepX() * 2.0;
        double frontZ = centerZ - forward.getStepZ() * 2.0;

        double backX = centerX + forward.getStepX() * 2.0;
        double backZ = centerZ + forward.getStepZ() * 2.0;

        double leftX = centerX - right.getStepX() * 2.0;
        double leftZ = centerZ - right.getStepZ() * 2.0;

        double rightX = centerX + right.getStepX() * 2.0;
        double rightZ = centerZ + right.getStepZ() * 2.0;

        // 手前面
        renderGlassPane(poseStack, collector, state, frontX, 2.5, frontZ, yaw);
        // 背面
        renderGlassPane(poseStack, collector, state, backX, 2.5, backZ, yaw);
        // 左面
        renderGlassPane(poseStack, collector, state, leftX, 2.5, leftZ, yaw + 90.0F);
        // 右面
        renderGlassPane(poseStack, collector, state, rightX, 2.5, rightZ, yaw + 90.0F);

        // --------------------------------------------------------------------
        // 6. 外部燃料ゲージの描画（暗所でも光るリアルタイム燃料インジケーター）
        // --------------------------------------------------------------------
        // 内側燃料投入口（lx = -1, ly = 0, lz = 0）の中心座標
        double fuelPort1X = 0.5 - right.getStepX() * 1.0;
        double fuelPort1Z = 0.5 - right.getStepZ() * 1.0;
        // 外側にホッパーを接続してもクリアに見える内側ポートの燃料ゲージ
        renderFuelGauge(poseStack, collector, state, fuelPort1X, 0.5, fuelPort1Z, yaw, 0.32F, 0.12F, 0.65F);

        // 外側燃料投入口（lx = -2, ly = 0, lz = 0）の中心座標
        double fuelPort2X = 0.5 - right.getStepX() * 2.0;
        double fuelPort2Z = 0.5 - right.getStepZ() * 2.0;
        // 内側にホッパーを接続した場合でも見える外側ポートの燃料ゲージ
        renderFuelGauge(poseStack, collector, state, fuelPort2X, 0.5, fuelPort2Z, yaw, 0.32F, 0.12F, 0.65F);

        // コアブロック（lx = 0, ly = 0, lz = 0）の前面左寄り（燃料口側: offsetX = -0.42F）にサブ燃料ゲージ
        renderFuelGauge(poseStack, collector, state, 0.5, 0.5, 0.5, yaw, -0.42F, 0.09F, 0.60F);
    }

    private void renderFuelGauge(PoseStack poseStack, SubmitNodeCollector collector, IronExtractorRenderState state,
                                 double blockCenterX, double blockCenterY, double blockCenterZ,
                                 float rotationYaw, float offsetX, float gaugeWidth, float gaugeHeight) {
        poseStack.pushPose();
        poseStack.translate(blockCenterX, blockCenterY, blockCenterZ);
        poseStack.rotateDegrees(Axis.YP, -rotationYaw);

        // 手前面表面 Z = 0.500F に対して、わずかに浮かせた Z = 0.503F（Z-fighting防止）
        float surfaceZ = 0.503F;
        float barMaxHeight = gaugeHeight * 0.88F;
        float bottomY = -gaugeHeight * 0.44F;

        // 稼働（燃焼）中は微小なパルス振動（息づかいのような演出）
        float pulse = state.isLit ? (float) (Math.sin(state.clientTicks * 0.25F) * 0.012F) : 0.0F;
        float currentBarHeight = (barMaxHeight + pulse) * Math.clamp(state.burnProgress, 0.0F, 1.0F);

        // 1. ゲージ外枠フレーム（研磨ブラックストーン）
        poseStack.pushPose();
        poseStack.translate(offsetX, 0.0F, surfaceZ);
        poseStack.scale(gaugeWidth, gaugeHeight, 0.018F);
        poseStack.translate(-0.5F, -0.5F, 0.0F);
        state.gaugeFrameModel.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();

        // 2. 燃料レベルインジケーターバー（FULL_BRIGHT 0xF000F0 自発光）
        if (state.burnProgress > 0.001F) {
            poseStack.pushPose();
            poseStack.translate(offsetX, bottomY, surfaceZ + 0.006F);
            poseStack.scale(gaugeWidth * 0.68F, currentBarHeight, 0.022F);
            poseStack.translate(-0.5F, 0.0F, 0.0F); // X中央、Y底面、Z手前

            state.fuelGaugeModel.submit(poseStack, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        // 3. 計器目盛り線（25%, 50%, 75%）
        for (int i = 1; i <= 3; i++) {
            float tickY = bottomY + (barMaxHeight * (i * 0.25F));
            poseStack.pushPose();
            poseStack.translate(offsetX, tickY, surfaceZ + 0.008F);
            poseStack.scale(gaugeWidth * 1.05F, 0.012F, 0.024F);
            poseStack.translate(-0.5F, -0.5F, 0.0F);
            state.pressModel.submit(poseStack, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void renderGlassPane(PoseStack poseStack, SubmitNodeCollector collector, IronExtractorRenderState state, double x, double y, double z, float rotationYaw) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.rotateDegrees(Axis.YP, -rotationYaw);

        // バニラのガラスブロックを幅3.0 x 高さ3.0 x 厚み0.99 にスケール拡大
        // フレームとの干渉・Z-fightingを防止しつつ、外周だけに枠線のある1枚の大きなガラスを表現
        poseStack.scale(3.0F, 3.0F, 0.99F);
        poseStack.translate(-0.5F, -0.5F, -0.5F); // 中心合わせ

        state.glassModel.submit(
                poseStack,
                collector,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );

        poseStack.popPose();
    }
}
