package net.ironforging.mod.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

/**
 * 鉄錬成装置コアのレンダリング用状態クラス（Minecraft 26.3 RenderState仕様）。
 */
public class IronExtractorRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public boolean isLit = false;
    public float progress = 0.0F;      // 錬成進行度 (0.0F 〜 1.0F)
    public float burnProgress = 0.0F;  // 外部燃料ゲージ割合 (0.0F 〜 1.0F)
    public float clientTicks = 0.0F;    // アニメーション用経過Tick
    public EntityRenderState golemRenderState;
    public final BlockModelRenderState spawnerModel = new BlockModelRenderState();
    public final BlockModelRenderState pressModel = new BlockModelRenderState();
    public final BlockModelRenderState glassModel = new BlockModelRenderState();
    public final BlockModelRenderState gaugeFrameModel = new BlockModelRenderState();
    public final BlockModelRenderState fuelGaugeModel = new BlockModelRenderState();
    public final ItemStackRenderState ingotItemState = new ItemStackRenderState();
}
