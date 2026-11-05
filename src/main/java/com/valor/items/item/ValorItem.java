package com.valor.items.item;

import com.valor.items.client.ClientHooks;
import com.valor.items.relic.RelicAbility;
import com.valor.items.relic.RelicLocation;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Base class for every relic ("thanh di vat"): an item with a level stored in its NBT,
 * that is worn in a Curios slot and can be upgraded from its own UI.
 */
public abstract class ValorItem extends Item implements ICurioItem {
    public static final String LEVEL_TAG = "RelicLevel";

    protected ValorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** The keys of the two active abilities, for the descriptions (the upper icon first: "Y / H"). */
    public static Component activeKeys() {
        return Component.empty().append(Component.keybind("key.valor_items.use_active_2"))
                .append(" / ").append(Component.keybind("key.valor_items.use_active"));
    }

    /** Highest level the relic can reach. */
    public abstract int getMaxLevel();

    /** Experience levels needed to go from {@code level} to {@code level + 1}. */
    public abstract int getUpgradeCost(int level);

    /** Curios slot identifier the relic is meant for (used for the UI). */
    public abstract String getSlotId();

    /** Abilities shown in the relic UI. */
    public abstract List<RelicAbility> getAbilities();

    /** Fixed bonuses (attributes) shown in the UI, one per line. */
    public List<Component> getStatLines() {
        return List.of();
    }

    public int getLevel(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(LEVEL_TAG)) {
            return 1;
        }
        return Mth.clamp(tag.getInt(LEVEL_TAG), 1, getMaxLevel());
    }

    public void setLevel(ItemStack stack, int level) {
        stack.getOrCreateTag().putInt(LEVEL_TAG, Mth.clamp(level, 1, getMaxLevel()));
    }

    /** Relics never show the enchantment glint, whatever their level. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    /** Right click in hand: open the relic UI. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openRelicScreen(RelicLocation.hand(hand)));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Right click the relic with an empty cursor in the inventory or in a Curios slot: open the relic UI. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action,
                                            Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !other.isEmpty()) {
            return false;
        }
        if (slot.container != player.getInventory() && !RelicLocation.isCuriosSlot(slot)) {
            return false;
        }
        if (player.level().isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openRelicScreenForSlot(slot, stack));
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int lvl = getLevel(stack);
        tooltip.add(Component.translatable("tooltip.valor_items.level", lvl, getMaxLevel())
                .withStyle(ChatFormatting.GOLD));
        for (RelicAbility ability : getAbilities()) {
            tooltip.add(Component.empty());
            tooltip.add(ability.name().copy().withStyle(ability.upgradable() ? ChatFormatting.AQUA : ChatFormatting.YELLOW));
            tooltip.add(ability.description().apply(lvl).copy().withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.valor_items.open_ui").withStyle(ChatFormatting.DARK_GRAY));
    }
}
