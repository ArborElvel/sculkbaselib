package com.unddefined.sculkbaselib.client.api;

import com.unddefined.sculkbaselib.SculkBaseLib;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Client-side particle rings that can follow any entity, including vanilla entities.
 *
 * <p>Attach from client code; each client tick emits {@link Spec#count()} particles
 * around the entity. The registry uses weak entity keys and automatically drops
 * entities that leave the current level or are removed.
 */
@EventBusSubscriber(modid = SculkBaseLib.MODID, value = Dist.CLIENT)
public final class ParticleOrbit {
    public record Spec(ParticleOptions particle, int count, double radius, double height,
                       double angularSpeed, double verticalAmplitude, double radialJitter) {
        public Spec {
            Objects.requireNonNull(particle, "particle");
            if (count < 0) throw new IllegalArgumentException("count must be >= 0");
            if (radius < 0) throw new IllegalArgumentException("radius must be >= 0");
            if (verticalAmplitude < 0) throw new IllegalArgumentException("verticalAmplitude must be >= 0");
            if (radialJitter < 0) throw new IllegalArgumentException("radialJitter must be >= 0");
        }
    }

    private static final Map<Entity, Spec> ACTIVE = new WeakHashMap<>();

    /** Attach an orbit to a client-side entity. No-op on the logical server. */
    public static void attach(Entity entity, Spec spec) {
        if (!entity.level().isClientSide) return;
        ACTIVE.put(entity, Objects.requireNonNull(spec, "spec"));
    }

    public static void detach(Entity entity) {
        ACTIVE.remove(entity);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ACTIVE.clear();
            return;
        }

        var iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Entity, Spec> entry = iterator.next();
            Entity entity = entry.getKey();
            if (entity.isRemoved() || entity.level() != mc.level) {
                iterator.remove();
                continue;
            }

            Spec spec = entry.getValue();
            if (spec.count() == 0) continue;

            double angle = ((entity.getId() & 0xFFFF) * 0.618033988749895)
                    + entity.tickCount * spec.angularSpeed();
            double step = Math.PI * 2 / spec.count();
            for (int i = 0; i < spec.count(); i++) {
                double particleAngle = angle + i * step;
                double radius = spec.radius()
                        + (entity.getRandom().nextDouble() - 0.5) * spec.radialJitter();
                double y = entity.getY() + spec.height()
                        + Math.sin(particleAngle * 2 + entity.tickCount * 0.1) * spec.verticalAmplitude();
                mc.level.addParticle(spec.particle(),
                        entity.getX() + Math.cos(particleAngle) * radius,
                        y,
                        entity.getZ() + Math.sin(particleAngle) * radius,
                        0.0, 0.0, 0.0);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
    }

    private ParticleOrbit() {
    }
}
