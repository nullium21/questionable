package me.nullium21.questionable.mixin;

import me.nullium21.questionable.PlayerEntityCustom;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LeadItem;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements PlayerEntityCustom {

    private Entity leashHolder;

    @Shadow public abstract boolean isSpectator();

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void interact(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (isSpectator()) return; // fall-through to method

        if (!(entity instanceof PlayerEntity other)) return;
        PlayerEntityMixin otherMixin = (PlayerEntityMixin) (Object) other;

        PlayerEntity self = (PlayerEntity) (Object) this;
        ItemStack item = self.getStackInHand(hand);

        if (!(item.getItem() instanceof LeadItem)) return;

        if (item.isOf(Items.LEAD)) {
            other.attachLeash(self);

            item.decrement(1);

            cir.setReturnValue(ActionResult.SUCCESS);
            cir.cancel();
        } else if (self.equals(other.getLeashHolder())) { // reversing .equals will cause NPEs
            otherMixin.leashHolder = null;
            other.dropItem(Items.LEAD);

            cir.setReturnValue(ActionResult.SUCCESS);
            cir.cancel();
        }
    }

    @Inject(method = "getName", at = @At("TAIL"), cancellable = true)
    private void getName(CallbackInfoReturnable<Text> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;

        if (self.hasCustomName() && self.getCustomName() != null) {
            MutableText prefix = self.getCustomName().copy().append(" ");
            cir.setReturnValue(prefix.append(cir.getReturnValue()));
        }
    }

    @Override
    public Entity getLeashHolder() {
        return leashHolder;
    }

    @Override
    public void attachLeash(Entity holder) {
        PlayerEntity self = (PlayerEntity) (Object) this;

        leashHolder = holder;

        if (!self.world.isClient && self.world instanceof ServerWorld sw) {
            sw.getChunkManager().sendToOtherNearbyPlayers(self, new EntityAttachS2CPacket(self, holder));
        }
    }
}
