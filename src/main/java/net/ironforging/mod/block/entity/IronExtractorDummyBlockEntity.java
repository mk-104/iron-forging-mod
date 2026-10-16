package net.ironforging.mod.block.entity;

import net.ironforging.mod.block.DummyPart;
import net.ironforging.mod.block.IronExtractorDummyBlock;
import net.ironforging.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * ダミーブロック専用のBlockEntity（Mojmap対応版）。
 * 親となる中央コアブロックの絶対座標（BlockPos）を保持し、
 * 燃料投入口（FUEL_PORT）や鉄取出口（OUTPUT_PORT）としてホッパーからの自動搬入出をコアへ中継します。
 */
public class IronExtractorDummyBlockEntity extends BlockEntity implements WorldlyContainer {

    private static final int[] SLOTS_FUEL = new int[]{IronExtractorCoreBlockEntity.SLOT_FUEL};
    private static final int[] SLOTS_OUTPUT = new int[]{IronExtractorCoreBlockEntity.SLOT_OUTPUT_IRON, IronExtractorCoreBlockEntity.SLOT_OUTPUT_POPPY};
    private static final int[] SLOTS_EMPTY = new int[0];

    @Nullable
    private BlockPos corePos;

    public IronExtractorDummyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IRON_EXTRACTOR_DUMMY_BE, pos, state);
    }

    @Nullable
    public BlockPos getCorePos() {
        return this.corePos;
    }

    public void setCorePos(@Nullable BlockPos corePos) {
        this.corePos = corePos;
        this.setChanged();
    }

    @Nullable
    public IronExtractorCoreBlockEntity getCoreBlockEntity() {
        if (this.corePos != null && this.level != null && this.level.isLoaded(this.corePos)) {
            BlockEntity be = this.level.getBlockEntity(this.corePos);
            if (be instanceof IronExtractorCoreBlockEntity coreBe) {
                return coreBe;
            }
        }
        return null;
    }

    @Nullable
    public DummyPart getPart() {
        BlockState state = getBlockState();
        if (state.hasProperty(IronExtractorDummyBlock.PART)) {
            return state.getValue(IronExtractorDummyBlock.PART);
        }
        return null;
    }

    // --- WorldlyContainer（ホッパー搬入出）中継実装 ---

    @Override
    public int[] getSlotsForFace(Direction side) {
        DummyPart part = getPart();
        if (part == DummyPart.FUEL_PORT) {
            return SLOTS_FUEL; // 燃料投入口: 燃料スロットのみ
        }
        if (part == DummyPart.OUTPUT_PORT) {
            return SLOTS_OUTPUT; // 鉄取出口: 鉄インゴット＆ポピースロットのみ
        }
        return SLOTS_EMPTY;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        DummyPart part = getPart();
        if (part == DummyPart.FUEL_PORT && slot == IronExtractorCoreBlockEntity.SLOT_FUEL) {
            return IronExtractorCoreBlockEntity.getFuelTime(stack) > 0;
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        DummyPart part = getPart();
        if (part == DummyPart.OUTPUT_PORT) {
            return slot == IronExtractorCoreBlockEntity.SLOT_OUTPUT_IRON || slot == IronExtractorCoreBlockEntity.SLOT_OUTPUT_POPPY;
        }
        return false;
    }

    @Override
    public int getContainerSize() {
        IronExtractorCoreBlockEntity core = getCoreBlockEntity();
        return core != null ? core.getContainerSize() : 0;
    }

    @Override
    public boolean isEmpty() {
        IronExtractorCoreBlockEntity core = getCoreBlockEntity();
        return core == null || core.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        IronExtractorCoreBlockEntity core = getCoreBlockEntity();
        return core != null ? core.getItem(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        IronExtractorCoreBlockEntity core = getCoreBlockEntity();
        return core != null ? core.removeItem(slot, amount) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        IronExtractorCoreBlockEntity core = getCoreBlockEntity();
        return core != null ? core.removeItemNoUpdate(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        IronExtractorCoreBlockEntity core = getCoreBlockEntity();
        if (core != null) {
            core.setItem(slot, stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        IronExtractorCoreBlockEntity core = getCoreBlockEntity();
        return core != null && core.stillValid(player);
    }

    @Override
    public void clearContent() {
        IronExtractorCoreBlockEntity core = getCoreBlockEntity();
        if (core != null) {
            core.clearContent();
        }
    }

    // --- NBT保存・同期 ---

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.corePos != null) {
            output.putInt("CoreX", this.corePos.getX());
            output.putInt("CoreY", this.corePos.getY());
            output.putInt("CoreZ", this.corePos.getZ());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        var x = input.getInt("CoreX");
        var y = input.getInt("CoreY");
        var z = input.getInt("CoreZ");
        if (x.isPresent() && y.isPresent() && z.isPresent()) {
            this.corePos = new BlockPos(x.get(), y.get(), z.get());
        } else {
            this.corePos = null;
        }
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
