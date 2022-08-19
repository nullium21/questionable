package me.nullium21.questionable.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.NameTagItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NameTagItem.class)
public class NameTagMixin {

    @Inject(method = "useOnEntity", at = @At("HEAD"), cancellable = true)
    public void useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!user.world.isClient && stack.hasCustomName() && entity instanceof PlayerEntity && entity.isAlive()) {
            entity.setCustomName(stack.getName());

            cir.setReturnValue(ActionResult.success(user.world.isClient));
        }
    }
}
