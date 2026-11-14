package com.valor.items.relic;

import com.valor.items.item.ArcticOrbItem;
import com.valor.items.item.HerculesMadnessItem;
import com.valor.items.item.MailOfPainItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * The relics with a unique active ability that count: at most {@link #MAX} of them, the ones in the first Curios
 * slots of the "items" slot type (the one worn first is the first). The same ones are used for the keys and for the
 * icons on the screen: the first is on the first key (H) and the bottom icon, the second on the second key (Y).
 */
public final class ActiveRelics {
    public static final int MAX = 2;

    public enum Kind { MAIL_OF_PAIN, HERCULES_MADNESS, ARCTIC_ORB }

    private static Kind kindOf(Item item) {
        if (item instanceof MailOfPainItem) {
            return Kind.MAIL_OF_PAIN;
        }
        if (item instanceof HerculesMadnessItem) {
            return Kind.HERCULES_MADNESS;
        }
        if (item instanceof ArcticOrbItem) {
            return Kind.ARCTIC_ORB;
        }
        return null;
    }

    /** The active relics worn, in the order of the Curios slots, at most {@link #MAX}; a relic worn twice counts once. */
    public static List<Kind> worn(LivingEntity entity) {
        List<Kind> list = new ArrayList<>();
        CuriosApi.getCuriosInventory(entity).resolve().ifPresent(handler -> {
            var slotType = handler.getCurios().get("items");
            if (slotType == null) {
                return;
            }
            var stacks = slotType.getStacks();
            for (int i = 0; i < stacks.getSlots() && list.size() < MAX; i++) {
                Kind kind = kindOf(stacks.getStackInSlot(i).getItem());
                if (kind != null && !list.contains(kind)) {
                    list.add(kind);
                }
            }
        });
        return list;
    }

    private ActiveRelics() {}
}
