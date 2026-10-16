package net.ironforging.mod.screen;

import net.ironforging.mod.block.entity.IronExtractorCoreBlockEntity;
import net.ironforging.mod.registry.ModScreenHandlers;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 鉄錬成装置のGUIコンテナ（Mojmap対応版）。
 */
public class IronExtractorScreenHandler extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData containerData;

    public IronExtractorScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(3), new SimpleContainerData(4));
    }

    public IronExtractorScreenHandler(int syncId, Inventory playerInventory, Container container, ContainerData containerData) {
        super(ModScreenHandlers.IRON_EXTRACTOR_SCREEN_HANDLER, syncId);
        checkContainerSize(container, 3);
        this.container = container;
        this.containerData = containerData;

        container.startOpen(playerInventory.player);

        // スロット0: 燃料
        this.addSlot(new FuelSlot(container, 0, 56, 53));

        // スロット1: 鉄インゴット出力
        this.addSlot(new OutputSlot(container, 1, 112, 35));

        // スロット2: ポピー出力
        this.addSlot(new OutputSlot(container, 2, 134, 35));

        // プレイヤーインベントリ（3行 x 9列）
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // プレイヤーホットバー（1行 x 9列）
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(containerData);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public boolean isBurning() {
        return this.containerData.get(0) > 0;
    }

    public float getFuelProgress() {
        int burnTime = this.containerData.get(0);
        int maxBurnTime = this.containerData.get(1);
        if (maxBurnTime <= 0) {
            maxBurnTime = 200;
        }
        return Mth.clamp((float) burnTime / (float) maxBurnTime, 0.0f, 1.0f);
    }

    public float getCookProgress() {
        int cookTime = this.containerData.get(2);
        int cookTimeTotal = this.containerData.get(3);
        if (cookTimeTotal <= 0) {
            cookTimeTotal = 200;
        }
        return Mth.clamp((float) cookTime / (float) cookTimeTotal, 0.0f, 1.0f);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < 3) {
                if (!this.moveItemStackTo(originalStack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(originalStack, newStack);
            } else {
                if (IronExtractorCoreBlockEntity.getFuelTime(originalStack) > 0) {
                    if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 3 && invSlot < 30) {
                    if (!this.moveItemStackTo(originalStack, 30, 39, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 30 && invSlot < 39) {
                    if (!this.moveItemStackTo(originalStack, 3, 30, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (originalStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, originalStack);
        }

        return newStack;
    }

    private static class FuelSlot extends Slot {
        public FuelSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return IronExtractorCoreBlockEntity.getFuelTime(stack) > 0;
        }
    }

    private static class OutputSlot extends Slot {
        public OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
