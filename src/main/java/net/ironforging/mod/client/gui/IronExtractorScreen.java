package net.ironforging.mod.client.gui;

import net.ironforging.mod.screen.IronExtractorScreenHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * 鉄錬成装置のGUI画面（Mojmap対応版）。
 * バニラの炎アイコンに加え、視認性に優れた専用の燃料レベルゲージおよびツールチップを実装。
 */
public class IronExtractorScreen extends AbstractContainerScreen<IronExtractorScreenHandler> {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier BURN_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");

    public IronExtractorScreen(IronExtractorScreenHandler menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        int x = this.leftPos;
        int y = this.topPos;

        // 1. かまどベース背景描画
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 2. スロット2（ポピー出力枠）のスロット背景
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 133, y + 34, 55.0f, 52.0f, 18, 18, 256, 256);

        // 3. バニラ炎アイコン（blitSprite対応）
        if (this.menu.isBurning()) {
            int litHeight = Mth.ceil(this.menu.getFuelProgress() * 13.0F) + 1;
            extractor.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    LIT_PROGRESS_SPRITE,
                    14, 14,
                    0, 14 - litHeight,
                    x + 56, y + 36 + 14 - litHeight,
                    14, litHeight
            );
        }

        // 4. 精錬矢印プログレスバー（blitSprite対応）
        int cookWidth = Mth.ceil(this.menu.getCookProgress() * 24.0F);
        if (cookWidth > 0) {
            extractor.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    BURN_PROGRESS_SPRITE,
                    24, 16,
                    0, 0,
                    x + 79, y + 34,
                    cookWidth, 16
            );
        }

        // 5. 【専用燃料ゲージバー】燃料スロット左横の縦型インジケーター (x: 40, y: 36, 幅: 8, 高さ: 33)
        int gaugeX = x + 40;
        int gaugeY = y + 36;
        int gaugeWidth = 8;
        int gaugeHeight = 33;

        // ゲージ外枠（暗い金属ベベル）
        extractor.fill(gaugeX - 1, gaugeY - 1, gaugeX + gaugeWidth + 1, gaugeY + gaugeHeight + 1, 0xFF2A2B30);
        extractor.fill(gaugeX, gaugeY, gaugeX + gaugeWidth, gaugeY + gaugeHeight, 0xFF141518);

        // 燃料残量バー（下から上へ充填）
        float fuelProgress = this.menu.getFuelProgress();
        if (this.menu.isBurning() && fuelProgress > 0.0F) {
            int fillHeight = Math.max(1, Mth.ceil(fuelProgress * (gaugeHeight - 2)));
            int fillTop = gaugeY + gaugeHeight - 1 - fillHeight;

            // 残量に応じたゲージ色（緑 -> 橙 -> 赤）
            int barColor;
            if (fuelProgress > 0.50F) {
                barColor = 0xFF35D040; // 50%以上: エメラルドグリーン
            } else if (fuelProgress > 0.20F) {
                barColor = 0xFFF5A623; // 20%〜50%: オレンジイエロー
            } else {
                barColor = 0xFFE6392E; // 20%以下: 警告クリムゾンレッド
            }

            extractor.fill(gaugeX + 1, fillTop, gaugeX + gaugeWidth - 1, gaugeY + gaugeHeight - 1, barColor);
            // 表面の反射ハイライト
            extractor.fill(gaugeX + 1, fillTop, gaugeX + 2, gaugeY + gaugeHeight - 1, 0x55FFFFFF);
        }

        // 目盛り線 (25%, 50%, 75%)
        for (int step = 1; step <= 3; step++) {
            int markY = gaugeY + gaugeHeight - (step * (gaugeHeight / 4));
            extractor.fill(gaugeX, markY, gaugeX + gaugeWidth, markY + 1, 0x44FFFFFF);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        super.extractTooltip(extractor, mouseX, mouseY);

        int x = this.leftPos;
        int y = this.topPos;

        // 燃料ゲージまたは炎アイコンにマウスホバーした時のツールチップ
        boolean isHoveringFuelGauge = (mouseX >= x + 38 && mouseX <= x + 72 && mouseY >= y + 34 && mouseY <= y + 70);
        if (isHoveringFuelGauge) {
            float fuelProgress = this.menu.getFuelProgress();
            int percent = Math.round(fuelProgress * 100.0F);

            if (this.menu.isBurning()) {
                extractor.setTooltipForNextFrame(
                        Component.literal("§6燃料残量: §e" + percent + "% §7(燃焼中)"),
                        mouseX,
                        mouseY
                );
            } else {
                extractor.setTooltipForNextFrame(
                        Component.literal("§7燃料残量: §c0% §8(停止中)"),
                        mouseX,
                        mouseY
                );
            }
        }
    }
}
