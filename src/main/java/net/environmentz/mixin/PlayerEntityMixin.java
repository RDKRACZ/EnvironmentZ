package net.environmentz.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import net.environmentz.util.TemperatureAspects;
import net.environmentz.access.PlayerEnvAccess;
import net.environmentz.init.ConfigInit;
import net.environmentz.init.EffectInit;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements PlayerEnvAccess {
    private int ticker;
    private boolean isHotEnvAffected = true;
    private boolean isColdEnvAffected = true;

    public PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At(value = "TAIL"))
    private void readCustomDataFromTagMixin(NbtCompound tag, CallbackInfo info) {
        this.isHotEnvAffected = tag.getBoolean("IsHotEnvAffected");
        this.isColdEnvAffected = tag.getBoolean("IsColdEnvAffected");
    }

    @Inject(method = "writeCustomDataToNbt", at = @At(value = "TAIL"))
    private void writeCustomDataToTagMixin(NbtCompound tag, CallbackInfo info) {
        tag.putBoolean("IsHotEnvAffected", this.isHotEnvAffected);
        tag.putBoolean("IsColdEnvAffected", this.isColdEnvAffected);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void tickMixin(CallbackInfo info) {
        PlayerEntity playerEntity = (PlayerEntity) (Object) this;
        if (!this.world.isClient && !playerEntity.isCreative() && !playerEntity.isSpectator() && playerEntity.isAlive()) {
            ticker++;
            if (ticker >= 20) {
                if (this.world.getBiome(this.getBlockPos()).getTemperature() <= ConfigInit.CONFIG.biome_freeze_temp) {
                    TemperatureAspects.coldEnvironment(playerEntity);
                } else if (TemperatureAspects.coldnessTimer > 0) {
                    TemperatureAspects.coldnessTimer = 0;
                } else if (this.world.getBiome(this.getBlockPos()).getTemperature() >= ConfigInit.CONFIG.biome_overheat_temp) {
                    TemperatureAspects.hotEnvironment(playerEntity);
                } else if (TemperatureAspects.dehydrationTimer > 0) {
                    TemperatureAspects.dehydrationTimer = 0;
                }
                if (this.hasStatusEffect(EffectInit.COLDNESS) || this.hasStatusEffect(EffectInit.OVERHEATING)) {
                    TemperatureAspects.acclimatize(playerEntity);
                } else if (TemperatureAspects.acclimatizeTimer > 0) {
                    TemperatureAspects.acclimatizeTimer = 0;
                }
                TemperatureAspects.dryOrWett(playerEntity);
                ticker = 0;
            }
        }
    }

    @Override
    public void setHotEnvAffected(boolean affected) {
        this.isHotEnvAffected = affected;
    }

    @Override
    public void setColdEnvAffected(boolean affected) {
        this.isColdEnvAffected = affected;
    }

    @Override
    public boolean isHotEnvAffected() {
        return this.isHotEnvAffected;
    }

    @Override
    public boolean isColdEnvAffected() {
        return this.isColdEnvAffected;
    }
}