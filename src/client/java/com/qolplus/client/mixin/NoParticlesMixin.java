package com.qolplus.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.qolplus.QolConfig;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;

/** No Particles: particles are never created, so they cost nothing at all. */
@Mixin(ParticleEngine.class)
public class NoParticlesMixin {
	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
	private void qolplus$skipCreate(ParticleOptions options, double x, double y, double z, double xa, double ya, double za,
			CallbackInfoReturnable<Particle> cir) {
		if (QolConfig.get().noParticles) cir.setReturnValue(null);
	}

	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void qolplus$skipAdd(Particle particle, CallbackInfo ci) {
		if (QolConfig.get().noParticles) ci.cancel();
	}
}
