package me.nullium21.questionable.mixin;

import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.LeadItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(LeadItem.class)
public abstract class LeadItemMixin {

    @Inject(method = "attachHeldMobsToBlock", at = @At("TAIL"), cancellable = true)
    private static void attachHeldMobsToBlock(PlayerEntity player, World world, BlockPos pos, CallbackInfoReturnable<ActionResult> cir) {
        LeashKnotEntity knot = null;
        boolean bl = false;
        double d = 7.0;
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        List<PlayerEntity> list = world.getNonSpectatingEntities(PlayerEntity.class, new Box((double)i - d, (double)j - d, (double)k - d, (double)i + d, (double)j + d, (double)k + d));
        for (PlayerEntity mobEntity : list) {
            if (mobEntity.getLeashHolder() != player) continue;
            if (knot == null) {
                knot = LeashKnotEntity.getOrCreate(world, pos);
                knot.onPlace();
            }
            mobEntity.attachLeash(knot);
            bl = true;
        }
        cir.setReturnValue(cir.getReturnValue() == ActionResult.SUCCESS || bl ? ActionResult.SUCCESS : ActionResult.PASS);
    }
}
