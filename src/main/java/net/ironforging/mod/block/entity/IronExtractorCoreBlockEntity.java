package net.ironforging.mod.block.entity;

import net.ironforging.mod.block.IronExtractorCoreBlock;
import net.ironforging.mod.inventory.ImplementedInventory;
import net.ironforging.mod.registry.ModBlockEntities;
import net.ironforging.mod.screen.IronExtractorScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * 鉄錬成装置コアブロックのBlockEntity（Mojmap対応版）。
 */
public class IronExtractorCoreBlockEntity extends BlockEntity implements ImplementedInventory, MenuProvider, WorldlyContainer {

    public static final int SLOT_FUEL = 0;          // 燃料スロット
    public static final int SLOT_OUTPUT_IRON = 1;   // 鉄インゴット出力スロット (100%)
    public static final int SLOT_OUTPUT_POPPY = 2;  // ポピー出力スロット (25%)
    public static final int TOTAL_SLOTS = 3;

    private static final int[] SLOTS_UP = new int[]{SLOT_FUEL};
    private static final int[] SLOTS_DOWN = new int[]{SLOT_OUTPUT_IRON, SLOT_OUTPUT_POPPY};
    private static final int[] SLOTS_SIDES = new int[]{SLOT_FUEL, SLOT_OUTPUT_IRON, SLOT_OUTPUT_POPPY};

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    private int burnTime = 0;
    private int maxBurnTime = 0;
    private int cookTime = 0;
    private int cookTimeTotal = 200;

    // クライアント側のアニメーション補間用フィールド
    private float clientCookTime = 0;
    private float prevClientCookTime = 0;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> IronExtractorCoreBlockEntity.this.burnTime;
                case 1 -> IronExtractorCoreBlockEntity.this.maxBurnTime;
                case 2 -> IronExtractorCoreBlockEntity.this.cookTime;
                case 3 -> IronExtractorCoreBlockEntity.this.cookTimeTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> IronExtractorCoreBlockEntity.this.burnTime = value;
                case 1 -> IronExtractorCoreBlockEntity.this.maxBurnTime = value;
                case 2 -> IronExtractorCoreBlockEntity.this.cookTime = value;
                case 3 -> IronExtractorCoreBlockEntity.this.cookTimeTotal = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public IronExtractorCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IRON_EXTRACTOR_CORE_BE, pos, state);
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.iron_forging.iron_extractor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new IronExtractorScreenHandler(syncId, playerInventory, this, this.containerData);
    }

    // --- WorldlyContainer（ホッパー搬入出）実装 ---

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return SLOTS_DOWN; // 下からは出力のみ
        }
        if (side == Direction.UP) {
            return SLOTS_UP; // 上からは燃料のみ
        }
        return SLOTS_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        // 燃料スロットかつ有効な燃料アイテムのみ搬入可能
        return slot == SLOT_FUEL && getFuelTime(stack) > 0;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // 鉄インゴットおよびポピーの出力スロットのみ搬出可能（燃料は吸い出さない）
        return slot == SLOT_OUTPUT_IRON || slot == SLOT_OUTPUT_POPPY;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IronExtractorCoreBlockEntity blockEntity) {
        if (level.isClientSide()) {
            blockEntity.prevClientCookTime = blockEntity.clientCookTime;
            if (state.getValue(IronExtractorCoreBlock.LIT)) {
                blockEntity.clientCookTime++;
                if (blockEntity.clientCookTime >= blockEntity.cookTimeTotal) {
                    blockEntity.clientCookTime = 0;
                }
                if (blockEntity.burnTime > 0) {
                    blockEntity.burnTime--;
                }
            } else if (blockEntity.clientCookTime > 0) {
                blockEntity.clientCookTime = Math.max(0, blockEntity.clientCookTime - 2);
                blockEntity.burnTime = 0;
            }
            return;
        }

        boolean isDirty = false;
        boolean wasBurning = blockEntity.isBurning();

        if (blockEntity.burnTime > 0) {
            blockEntity.burnTime--;
            isDirty = true;
        }

        boolean canAccept = canAcceptOutput(blockEntity.inventory);
        ItemStack fuelStack = blockEntity.inventory.get(SLOT_FUEL);

        if (!blockEntity.isBurning() && canAccept && !fuelStack.isEmpty()) {
            int fuelTicks = getFuelTime(fuelStack);
            if (fuelTicks > 0) {
                blockEntity.burnTime = fuelTicks;
                blockEntity.maxBurnTime = fuelTicks;

                boolean isBucket = fuelStack.is(Items.LAVA_BUCKET);
                fuelStack.shrink(1);
                if (fuelStack.isEmpty() && isBucket) {
                    blockEntity.inventory.set(SLOT_FUEL, new ItemStack(Items.BUCKET));
                }

                isDirty = true;
            }
        }

        if (blockEntity.isBurning() && canAccept) {
            blockEntity.cookTime++;
            if (blockEntity.cookTime >= blockEntity.cookTimeTotal) {
                craftItems(level, blockEntity.inventory);
                blockEntity.cookTime = 0;
            }
            isDirty = true;
        } else if (!canAccept) {
            // 出力枠満杯時は停止
        } else if (!blockEntity.isBurning() && blockEntity.cookTime > 0) {
            blockEntity.cookTime = Math.max(0, blockEntity.cookTime - 2);
            isDirty = true;
        }

        if (wasBurning != blockEntity.isBurning()) {
            state = state.setValue(IronExtractorCoreBlock.LIT, blockEntity.isBurning());
            level.setBlock(pos, state, Block.UPDATE_ALL);
            isDirty = true;
        }

        // クライアント側外部ゲージ同期のため、燃焼開始/終了時および20tickごとにパケット送信
        if (wasBurning != blockEntity.isBurning() || (blockEntity.isBurning() && blockEntity.burnTime % 20 == 0)) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        if (isDirty) {
            blockEntity.setChanged();
        }
    }

    /**
     * 外部ゲージ用の燃料残量割合 (0.0F 〜 1.0F) を取得します。
     */
    public float getBurnProgress() {
        if (this.maxBurnTime <= 0) {
            return 0.0F;
        }
        return Math.clamp((float) this.burnTime / (float) this.maxBurnTime, 0.0F, 1.0F);
    }

    private static boolean canAcceptOutput(NonNullList<ItemStack> inventory) {
        ItemStack ironStack = inventory.get(SLOT_OUTPUT_IRON);
        ItemStack poppyStack = inventory.get(SLOT_OUTPUT_POPPY);

        boolean canFitIron = ironStack.isEmpty() ||
                (ironStack.is(Items.IRON_INGOT) && ironStack.getCount() < ironStack.getMaxStackSize());
        if (!canFitIron) {
            return false;
        }

        boolean canFitPoppy = poppyStack.isEmpty() ||
                (poppyStack.is(Items.POPPY) && poppyStack.getCount() < poppyStack.getMaxStackSize());
        return canFitPoppy;
    }

    private static void craftItems(Level level, NonNullList<ItemStack> inventory) {
        ItemStack ironStack = inventory.get(SLOT_OUTPUT_IRON);
        if (ironStack.isEmpty()) {
            inventory.set(SLOT_OUTPUT_IRON, new ItemStack(Items.IRON_INGOT, 1));
        } else if (ironStack.is(Items.IRON_INGOT)) {
            ironStack.grow(1);
        }

        if (level.getRandom().nextFloat() < 0.25f) {
            ItemStack poppyStack = inventory.get(SLOT_OUTPUT_POPPY);
            if (poppyStack.isEmpty()) {
                inventory.set(SLOT_OUTPUT_POPPY, new ItemStack(Items.POPPY, 1));
            } else if (poppyStack.is(Items.POPPY)) {
                poppyStack.grow(1);
            }
        }
    }

    public static int getFuelTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) return 1600;
        if (stack.is(Items.COAL_BLOCK)) return 16000;
        if (stack.is(Items.LAVA_BUCKET)) return 20000;
        if (stack.is(Items.BLAZE_ROD)) return 2400;
        return 0;
    }

    public boolean isBurning() {
        return this.burnTime > 0;
    }

    public int getBurnTime() {
        return this.burnTime;
    }

    public int getMaxBurnTime() {
        return this.maxBurnTime;
    }

    public int getCookTime() {
        return this.cookTime;
    }

    public int getCookTimeTotal() {
        return this.cookTimeTotal;
    }

    /**
     * クライアント側のアニメーション補間用進行度 (0.0F 〜 1.0F) を取得します。
     */
    public float getProgress(float partialTick) {
        float time = net.minecraft.util.Mth.lerp(partialTick, this.prevClientCookTime, this.clientCookTime);
        return Math.clamp(time / (float) Math.max(1, this.cookTimeTotal), 0.0F, 1.0F);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.inventory);
        output.putInt("BurnTime", this.burnTime);
        output.putInt("MaxBurnTime", this.maxBurnTime);
        output.putInt("CookTime", this.cookTime);
        output.putInt("CookTimeTotal", this.cookTimeTotal);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.inventory);
        this.burnTime = input.getIntOr("BurnTime", 0);
        this.maxBurnTime = input.getIntOr("MaxBurnTime", 0);
        this.cookTime = input.getIntOr("CookTime", 0);
        this.cookTimeTotal = input.getIntOr("CookTimeTotal", 200);
        this.clientCookTime = this.cookTime;
        this.prevClientCookTime = this.cookTime;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        return saveWithoutMetadata(lookup);
    }
}
