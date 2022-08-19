package me.nullium21.questionable.mixin;

import me.nullium21.questionable.PlayerEntityCustom;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LeadItem;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class LeadPlayerMixin implements PlayerEntityCustom {

    private Entity leashHolder;

    @Shadow public abstract boolean isSpectator();

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void interact(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (isSpectator()) return; // fall-through to method

        if (!(entity instanceof PlayerEntity other)) return;
        LeadPlayerMixin otherMixin = (LeadPlayerMixin) (Object) other;

        PlayerEntity self = (PlayerEntity) (Object) this;
        ItemStack item = self.getStackInHand(hand);

        if (!(item.getItem() instanceof LeadItem)) return;

        otherMixin.leashHolder = self;

        if (!self.world.isClient && self.world instanceof ServerWorld sw) {
            sw.getChunkManager().sendToOtherNearbyPlayers(other, new EntityAttachS2CPacket(other, self));
        }

        item.decrement(1);

        cir.setReturnValue(ActionResult.SUCCESS);
        cir.cancel();
    }

    @Override
    public Entity getLeashHolder() {
        return leashHolder;
    }
}