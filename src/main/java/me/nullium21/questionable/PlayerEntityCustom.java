package me.nullium21.questionable;

import net.minecraft.entity.Entity;

public interface PlayerEntityCustom {

    Entity getLeashHolder();

    void attachLeash(Entity holder);
}
