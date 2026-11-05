package com.valor.items.relic;

import com.valor.items.item.ValorItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Map;
import java.util.Optional;

/**
 * Tells client and server where a relic stack lives: a hand, the player inventory or a Curios slot.
 * Sent in packets so that the server can find the very same stack.
 */
public final class RelicLocation {
    public enum Type { MAIN_HAND, OFF_HAND, INVENTORY, CURIOS }

    private final Type type;
    private final String identifier;
    private final int index;

    private RelicLocation(Type type, String identifier, int index) {
        this.type = type;
        this.identifier = identifier;
        this.index = index;
    }

    public static RelicLocation hand(InteractionHand hand) {
        return new RelicLocation(hand == InteractionHand.MAIN_HAND ? Type.MAIN_HAND : Type.OFF_HAND, "", 0);
    }

    public static RelicLocation inventory(int slot) {
        return new RelicLocation(Type.INVENTORY, "", slot);
    }

    public static RelicLocation curios(String identifier, int index) {
        return new RelicLocation(Type.CURIOS, identifier, index);
    }

    /** Finds the location of the stack shown in a container slot, or null if it is not a supported place. */
    public static RelicLocation fromSlot(Player player, Slot slot, ItemStack stack) {
        if (slot.container == player.getInventory()) {
            return inventory(slot.getContainerSlot());
        }
        if (isCuriosSlot(slot)) {
            return findInCurios(player, stack);
        }
        return null;
    }

    public static boolean isCuriosSlot(Slot slot) {
        return slot.getClass().getName().startsWith("top.theillusivec4.curios.");
    }

    /** Searches the (non cosmetic) Curios slots for exactly this stack instance. */
    public static RelicLocation findInCurios(Player player, ItemStack stack) {
        Optional<RelicLocation> found = CuriosApi.getCuriosInventory(player).resolve().flatMap(handler -> {
            for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
                IDynamicStackHandler stacks = entry.getValue().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (stacks.getStackInSlot(i) == stack) {
                        return Optional.of(curios(entry.getKey(), i));
                    }
                }
            }
            return Optional.<RelicLocation>empty();
        });
        return found.orElse(null);
    }

    /** First relic equipped in any Curios slot, or null. */
    public static RelicLocation findFirstEquippedRelic(Player player) {
        Optional<RelicLocation> found = CuriosApi.getCuriosInventory(player).resolve().flatMap(handler -> {
            for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
                IDynamicStackHandler stacks = entry.getValue().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (stacks.getStackInSlot(i).getItem() instanceof ValorItem) {
                        return Optional.of(curios(entry.getKey(), i));
                    }
                }
            }
            return Optional.<RelicLocation>empty();
        });
        return found.orElse(null);
    }

    public ItemStack get(Player player) {
        switch (type) {
            case MAIN_HAND:
                return player.getMainHandItem();
            case OFF_HAND:
                return player.getOffhandItem();
            case INVENTORY: {
                Inventory inv = player.getInventory();
                return index >= 0 && index < inv.getContainerSize() ? inv.getItem(index) : ItemStack.EMPTY;
            }
            case CURIOS:
                return CuriosApi.getCuriosInventory(player).resolve()
                        .flatMap(handler -> handler.getStacksHandler(identifier))
                        .map(handler -> {
                            IDynamicStackHandler stacks = handler.getStacks();
                            return index >= 0 && index < stacks.getSlots()
                                    ? stacks.getStackInSlot(index) : ItemStack.EMPTY;
                        })
                        .orElse(ItemStack.EMPTY);
            default:
                return ItemStack.EMPTY;
        }
    }

    /** Replaces the stack at this location (server side). */
    public void set(Player player, ItemStack stack) {
        switch (type) {
            case MAIN_HAND:
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                break;
            case OFF_HAND:
                player.setItemInHand(InteractionHand.OFF_HAND, stack);
                break;
            case INVENTORY:
                if (index >= 0 && index < player.getInventory().getContainerSize()) {
                    player.getInventory().setItem(index, stack);
                }
                break;
            case CURIOS:
                CuriosApi.getCuriosInventory(player).resolve()
                        .flatMap(handler -> handler.getStacksHandler(identifier))
                        .ifPresent(handler -> {
                            IDynamicStackHandler stacks = handler.getStacks();
                            if (index >= 0 && index < stacks.getSlots()) {
                                stacks.setStackInSlot(index, stack);
                            }
                        });
                break;
            default:
                break;
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(type);
        buf.writeUtf(identifier, 64);
        buf.writeVarInt(index);
    }

    public static RelicLocation read(FriendlyByteBuf buf) {
        Type type = buf.readEnum(Type.class);
        String identifier = buf.readUtf(64);
        int index = buf.readVarInt();
        return new RelicLocation(type, identifier, index);
    }
}
