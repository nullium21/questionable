package me.nullium21.questionable.mixin;

import me.nullium21.questionable.PlayerEntityCustom;
import me.nullium21.questionable.Questionable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements PlayerEntityCustom {

    private Entity leashHolder;
    private long leashAttachedAt = -1;

    @Shadow public abstract boolean isSpectator();

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void interact(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (isSpectator()) return; // fall-through to method

        if (!(entity instanceof PlayerEntity other)) return;
        PlayerEntityMixin otherMixin = (PlayerEntityMixin) (Object) other;

        PlayerEntity self = (PlayerEntity) (Object) this;
        ItemStack item = self.getStackInHand(hand);

        if (item.isOf(Items.LEAD) && other.getLeashHolder() == null) {
            other.attachLeash(self);

            item.decrement(1);

            cir.setReturnValue(ActionResult.SUCCESS);
            cir.cancel();
        } else if (self.equals(other.getLeashHolder()) && (entity.world.getTime() - otherMixin.leashAttachedAt) > 5) { // reversing .equals will cause NPEs
            Questionable.LOGGER.debug("removing leash from {}", other.getUuidAsString());

            otherMixin.leashHolder = null;
            otherMixin.leashAttachedAt = -1;
            other.dropItem(Items.LEAD);

            cir.setReturnValue(ActionResult.SUCCESS);
            cir.cancel();
        } else if (item.isOf(Items.SHEARS) && other.hasCustomName()) {
            item.damage(1, self, unused -> {});

            other.setCustomName(null);

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

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void tickMovement(CallbackInfo ci) {
        if (leashHolder == null) return;

        PlayerEntity self = (PlayerEntity) (Object) this;

        Vec3d dist = leashHolder.getPos().subtract(self.getPos());

        if (dist.length() >= 20) {
            self.teleport(leashHolder.getX(), leashHolder.getY(), leashHolder.getZ());
        } else if (dist.length() >= 7) {
            self.setVelocity(dist.multiply(.05));
            self.velocityModified = true;
            self.velocityDirty = true;
        }
    }

    @Override
    public Entity getLeashHolder() {
        return leashHolder;
    }

    @Override
    public void attachLeash(Entity holder) {
        PlayerEntity self = (PlayerEntity) (Object) this;

        Questionable.LOGGER.debug("{} attaching leash to {}", holder.getUuidAsString(), self.getUuidAsString());

        leashHolder = holder;
        leashAttachedAt = holder.world.getTime();

        if (!self.world.isClient && self.world instanceof ServerWorld sw) {
            sw.getChunkManager().sendToOtherNearbyPlayers(self, new EntityAttachS2CPacket(self, holder));
            Questionable.LOGGER.debug("sent packet EntityAttachS2c: {}, {}", self.getUuidAsString(), holder.getUuidAsString());
        }
    }
}
