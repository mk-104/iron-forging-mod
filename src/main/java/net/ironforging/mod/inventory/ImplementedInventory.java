package net.ironforging.mod.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric推奨のインベントリ共通インターフェース（Mojmap対応版）。
 * NonNullList<ItemStack> を委任先として、Containerの必須メソッドをデフォルト実装します。
 */
@FunctionalInterface
public interface ImplementedInventory extends Container {

    /**
     * インベントリ内のアイテムスロットリストを取得します。
     *
     * @return アイテムスタックのリスト
     */
    NonNullList<ItemStack> getItems();

    /**
     * 指定されたアイテムリストからインベントリインスタンスを作成します。
     */
    static ImplementedInventory of(NonNullList<ItemStack> items) {
        return () -> items;
    }

    /**
     * 指定されたサイズで空のインベントリを作成します。
     */
    static ImplementedInventory ofSize(int size) {
        return of(NonNullList.withSize(size, ItemStack.EMPTY));
    }

    @Override
    default int getContainerSize() {
        return getItems().size();
    }

    @Override
    default boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    default ItemStack getItem(int slot) {
        return getItems().get(slot);
    }

    @Override
    default ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(getItems(), slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(getItems(), slot);
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        if (stack.getCount() > stack.getMaxStackSize()) {
            stack.setCount(stack.getMaxStackSize());
        }
        setChanged();
    }

    @Override
    default void clearContent() {
        getItems().clear();
    }

    @Override
    default void setChanged() {
        // 必要に応じて呼び出し元でオーバーライド
    }

    @Override
    default boolean stillValid(Player player) {
        return true;
    }
}
