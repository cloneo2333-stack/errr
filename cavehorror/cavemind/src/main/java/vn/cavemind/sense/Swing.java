package vn.cavemind.sense;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

// How hard a player hits, judged from the weapon's stats rather than its name, so every mod's weapons and every tier
// sort themselves out (a wooden battle axe is slow but light, a netherite greatsword is both).
// Vanilla resets the attack charge before the target's hurt events run, so the charge is read when the swing starts
// (AttackEntityEvent) and looked up again from the attack event in the same tick. Swings at the air only exist on the
// client; it reports them so the Man can punish a whiffed heavy swing.
public final class Swing {
    public record Blow(int weight, boolean heavy, boolean charged) {
        // a rushed swing 1, a full one 2, a full swing of a heavy weapon 4, landed as a critical 6
        static final Blow GLANCE = new Blow(1, false, false), SHOT = new Blow(2, false, true);
    }

    static final class S {
        long at = -1000, hitAt = -1000, missAt = -1000;
        float charge;
        boolean crit;
        UUID target;
    }

    private static final Map<UUID, S> ALL = new HashMap<>();

    public static void forget(UUID id) {
        ALL.remove(id);
    }

    public static void attack(ServerPlayer p, Entity target) {
        S s = ALL.computeIfAbsent(p.getUUID(), k -> new S());
        long now = p.server.getTickCount();
        s.charge = p.getAttackStrengthScale(0.5f);
        s.crit = s.charge > 0.9f && p.fallDistance > 0 && !p.onGround() && !p.onClimbable() && !p.isInWater() && !p.isPassenger()
            && !p.isSprinting() && !p.hasEffect(MobEffects.BLINDNESS);
        s.target = target.getUUID();
        s.at = s.hitAt = now;
    }

    public static void miss(ServerPlayer p) {
        S s = ALL.computeIfAbsent(p.getUUID(), k -> new S());
        long now = p.server.getTickCount();
        if (now - s.missAt < 4 || !slow(p)) return;
        s.missAt = s.at = now;
    }

    public static boolean slow(Player p) {
        return p.getAttributeValue(Attributes.ATTACK_SPEED) <= 1.0;
    }

    public static boolean heavy(Player p) {
        return slow(p) && p.getAttributeValue(Attributes.ATTACK_DAMAGE) >= 10;
    }

    // still recovering from a swing of a slow weapon: the opening the Man waits for
    public static boolean recovering(ServerPlayer p, long now) {
        S s = ALL.get(p.getUUID());
        return s != null && slow(p) && now - s.at < p.getCurrentItemAttackStrengthDelay() * 0.8f;
    }

    public static Blow blow(ServerPlayer p, Entity target, DamageSource src) {
        if (src.getDirectEntity() != p) return Blow.SHOT;
        S s = ALL.get(p.getUUID());
        if (s == null || s.hitAt != p.server.getTickCount() || !target.getUUID().equals(s.target)) return Blow.GLANCE;
        if (s.charge < 0.9f) return Blow.GLANCE;
        if (!heavy(p)) return new Blow(2, false, true);
        return new Blow(s.crit ? 6 : 4, true, true);
    }

    private Swing() {}
}
