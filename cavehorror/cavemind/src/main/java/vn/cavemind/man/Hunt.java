package vn.cavemind.man;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.CaveMind;
import vn.cavemind.Cfg;
import vn.cavemind.compat.Compat;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.net.Net;
import vn.cavemind.sense.Group;
import vn.cavemind.sense.Sight;
import vn.cavemind.sense.Swing;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Spots;

final class Hunt {
    UUID crawler;
    Vec3 lastKnown;
    long lastSeen, start, end, phaseAt, nextPath, nextLoop, nextSearch, lastProgressAt;
    int phase, attackCd, searchTicks, breakTicks, hits, hitsTaken, weightTaken;
    long baitSince = -1, noBaitUntil;
    Vec3 searchPoint, lastPos;
    BlockPos breaking, hint, spawnAt;
    boolean caught, gaveUp, retreat, hasLos, wasChasing = true, screamed, fled;
    long retreatAt;
    final String why;
    int side = 1;
    long flipAt, nextPause, pauseUntil, nextBlink, siegeAt;
    boolean siege;
    Vec3 siegeGoal;
    int smashed;
    UUID target, lastHit, marked, stand;
    int pack = 1, kills;
    long runUntil, surroundedAt;
    Vec3 runTo;

    Hunt(ServerPlayer p, Watch w, BlockPos hint, long now, String why) {
        this.hint = hint;
        this.why = why;
        start = now;
        phaseAt = now;
        pack = Pack.members(p).size();
        end = now + (long) ((1100 + p.getRandom().nextInt(600)) * (1 + 0.25 * (pack - 1)));
        side = p.getRandom().nextBoolean() ? 1 : -1;
        flipAt = now + 160;
        nextPause = now + 120;
        for (ServerPlayer o : Pack.members(p)) {
            if (Cfg.SILENCE.get()) Net.fx(o, Net.SILENCE, 0, 160);
            if (Cfg.FOG.get()) Net.fx(o, Net.FOG, 0.8f, 70);
        }
    }

    // in blow weight (Swing.Blow): alone that is 8 rushed swings, 4 full ones or 2 full swings of a heavy weapon
    int need() {
        return Math.min(24, 8 + 5 * (pack - 1));
    }

    int maxKills() {
        return Math.max(1, pack - 1);
    }

    boolean regrouped(ServerLevel lvl, Entity e) {
        List<ServerPlayer> alive = new ArrayList<>();
        for (ServerPlayer o : lvl.players()) if (o.isAlive() && !Compat.exempt(o) && o.distanceTo(e) < 60) alive.add(o);
        for (int i = 0; i < alive.size(); i++) for (int j = i + 1; j < alive.size(); j++) if (alive.get(i).distanceTo(alive.get(j)) < 8) return true;
        return alive.isEmpty();
    }

    ServerPlayer nextVictim(ServerPlayer dead, ServerLevel lvl) {
        Entity e = crawler == null ? null : lvl.getEntity(crawler);
        if (e == null || retreat || kills + 1 >= maxKills()) return null;
        ServerPlayer best = null;
        double bs = -1e9;
        for (ServerPlayer o : lvl.players()) {
            if (o == dead || !o.isAlive() || Compat.exempt(o) || o.distanceTo(e) > 60) continue;
            double iso = 1e9;
            for (ServerPlayer q : lvl.players()) if (q != o && q != dead && q.isAlive() && !q.isSpectator()) iso = Math.min(iso, q.distanceTo(o));
            if (iso < 12) continue;
            double s = -o.distanceTo(e) * 0.05 + (1 - o.getHealth() / o.getMaxHealth()) * 2;
            if (s > bs) {
                bs = s;
                best = o;
            }
        }
        return best;
    }

    ServerPlayer pick(ServerPlayer owner, ServerLevel lvl, Entity e) {
        ServerPlayer best = null;
        double bs = -1e9;
        for (ServerPlayer o : lvl.players()) {
            if (!o.isAlive() || Compat.exempt(o)) continue;
            double d = o.distanceTo(e);
            if (d > (o == owner ? 90 : 48)) continue;
            double iso = Group.isolation(o);
            double s = Math.min(iso, 40) * 0.08 + (1 - o.getHealth() / o.getMaxHealth()) * 2.2 - d * 0.04 + (Watch.of(o).light <= 6 ? 0.5 : 0)
                + (o.getUUID().equals(marked) ? 1.0 : 0) - (o.getUUID().equals(lastHit) ? 1.4 : 0) + (o == owner ? 0.3 : 0);
            if (s > bs) {
                bs = s;
                best = o;
            }
        }
        return best;
    }

    boolean stunned;

    Entity body(ServerLevel lvl) {
        return crawler == null ? null : lvl.getEntity(crawler);
    }

    boolean stun(ServerLevel lvl, long now) {
        if (stunned || phase == 0 || retreat) return false;
        Entity e = body(lvl);
        if (e == null) return false;
        stunned = true;
        runUntil = 0;
        pauseUntil = now + 70;
        nextPause = now + 240;
        attackCd = Math.max(attackCd, 90);
        Compat.anim(e, "cm_twitch");
        Fx.later(lvl, 70, () -> {
            if (e.isAlive()) Compat.anim(e, "empty");
        });
        return true;
    }

    boolean tick(ServerPlayer p, Watch ownerWatch, long now) {
        ServerLevel lvl = p.serverLevel();
        if (phase == 0) return telegraph(p, ownerWatch, lvl, now);
        Entity e = crawler == null ? null : lvl.getEntity(crawler);
        if (!(e instanceof Mob m) || !e.isAlive()) return false;
        if (!p.isAlive()) {
            caught = true;
            return false;
        }
        if (retreat) {
            Vec3 away = e.position().add(e.position().subtract(p.position()).normalize().scale(24));
            if (now % 20 == 0) m.getNavigation().moveTo(away.x, away.y, away.z, 1.15);
            boolean seen = false;
            for (ServerPlayer o : Pack.members(p)) {
                Sight si = Sight.of(o, Watch.of(o), e.getBoundingBox().getCenter());
                if (si.on && Sight.visible(o, e) > 0) seen = true;
            }
            return !(now - retreatAt > 40 && !seen || now - retreatAt > 160 || e.distanceTo(p) > 48);
        }
        if (now >= end) {
            startRetreat(p, now, false);
            return true;
        }
        if (ownerWatch.outside && lvl.isDay() && !lvl.isThundering() && now - start > 200) {
            startRetreat(p, now, false);
            return true;
        }
        ServerPlayer tgt = target == null ? null : lvl.getServer().getPlayerList().getPlayer(target);
        if (tgt != null && !tgt.isAlive() && tgt != p) {
            kills++;
            caught = true;
            Pack.sayAll(Pack.members(p), "Hắn đã hạ " + tgt.getGameProfile().getName());
            if (kills >= maxKills() || regrouped(lvl, e)) {
                startRetreat(p, now, true);
                return true;
            }
            tgt = null;
        }
        if (tgt == null || tgt.level() != lvl || Compat.exempt(tgt) || tgt.distanceTo(e) > 90 || now >= surroundedAt && now % 80 == 0 && runUntil < now) {
            ServerPlayer nt = pick(p, lvl, e);
            if (nt != null && (tgt == null || nt != tgt)) {
                lastSeen = now;
                lastKnown = nt.position();
            }
            tgt = nt;
            target = tgt == null ? null : tgt.getUUID();
        }
        if (tgt == null) {
            startRetreat(p, now, true);
            return true;
        }
        Watch w = Watch.of(tgt);
        double dist = Math.sqrt(e.distanceToSqr(tgt));
        if (now % 3 == 0) hasLos = Sight.clear(lvl, e.getEyePosition(), tgt.getEyePosition(), e);
        if (hasLos && dist < 64) {
            lastKnown = tgt.position();
            lastSeen = now;
            searchTicks = 0;
        } else if (w.noise > 0.38f && dist < 30) {
            lastKnown = tgt.position();
            lastSeen = now - 30;
        }
        if (now >= nextLoop) {
            Fx.soundAll(lvl, "man:loop", SoundSource.HOSTILE, e.position(), 1.0f, 1.0f);
            nextLoop = now + 105;
        }
        if (attackCd > 0) attackCd--;
        int close = 0;
        for (ServerPlayer o : lvl.players()) if (o.isAlive() && !Compat.exempt(o) && o.distanceTo(e) < 4.5) close++;
        if (close >= 2 && runUntil < now && pack >= 2) {
            runUntil = now + 45 + p.getRandom().nextInt(30);
            Vec3 away = e.position().subtract(tgt.position()).multiply(1, 0, 1);
            runTo = e.position().add((away.lengthSqr() > 1e-3 ? away.normalize() : new Vec3(1, 0, 0)).scale(12));
            surroundedAt = now;
            side = -side;
        }
        if (now < runUntil && runTo != null) {
            if (now % 6 == 0) m.getNavigation().moveTo(runTo.x, runTo.y, runTo.z, 1.35);
            m.setAggressive(false);
            if (now + 1 >= runUntil) target = null;
            return true;
        }
        boolean chase = now - lastSeen < 50;
        if (chase != wasChasing) {
            wasChasing = chase;
        }
        if (now < pauseUntil) {
            m.getNavigation().stop();
            vn.cavemind.world.Motion.turn(m, tgt.getEyePosition(), 25f);
            return true;
        }
        if (chase && hasLos && dist > 7 && now >= nextPause) {
            pauseUntil = now + 8 + p.getRandom().nextInt(14);
            nextPause = pauseUntil + 35 + p.getRandom().nextInt(80);
            Compat.anim(e, "cm_twitch");
            Fx.later(lvl, (int) (pauseUntil - now), () -> {
                if (e.isAlive()) Compat.anim(e, "empty");
            });
            return true;
        }
        if (!hasLos && dist > 30 && now >= nextBlink) {
            nextBlink = now + 120;
            boolean anySees = false;
            for (ServerPlayer o : Pack.members(tgt)) {
                Sight si = Sight.of(o, Watch.of(o), e.getBoundingBox().getCenter());
                if (si.on && Sight.visible(o, e) > 0) anySees = true;
            }
            if (!anySees) {
                ServerPlayer prey = tgt;
                Spots.Cand c = Spots.find(prey, w, new Spots.Spec().ring(14, 20).dir(180, 80).body(0.5, 0.9).cave(w.underground).tries(30)
                    .when(k -> !k.sight.on && !Group.boxSeenByOthers(prey, k.box, 64)).rank(k -> k.vis));
                if (c != null) {
                    e.teleportTo(c.feet.getX() + 0.5, c.feet.getY(), c.feet.getZ() + 0.5);
                    lastPos = e.position();
                    lastProgressAt = now;
                }
            }
        }
        if (now >= flipAt) {
            side = -side;
            flipAt = now + 100 + p.getRandom().nextInt(140);
        }
        boolean shut = (w.indoors || w.confined && !w.underground) && dist < 24;
        if (shut && !siege && now - lastProgressAt > 40 && dist > 2.5) {
            siege = true;
            siegeGoal = null;
        }
        if (siege && (!shut || hasLos && dist < 5)) siege = false;
        double speed;
        Vec3 goal;
        if (siege) {
            if (siegeGoal == null || e.position().distanceToSqr(siegeGoal) < 2.5 || now - siegeAt > 70) {
                double ang = Math.atan2(e.getZ() - tgt.getZ(), e.getX() - tgt.getX()) + side * Math.toRadians(65 + p.getRandom().nextInt(55));
                double r = 4.5 + p.getRandom().nextDouble() * 3;
                BlockPos feet = Spots.nearFloor(lvl, BlockPos.containing(tgt.getX() + Math.cos(ang) * r, tgt.getY(), tgt.getZ() + Math.sin(ang) * r), 2, 3);
                siegeAt = now;
                if (feet != null) siegeGoal = Vec3.atBottomCenterOf(feet);
                bang(tgt, lvl, m);
            }
            goal = siegeGoal != null ? siegeGoal : tgt.position();
            speed = 1.3;
            m.setAggressive(true);
        } else if (chase) {
            // He reads a slow weapon: while it is ready he circles just out of its reach, and the moment it has been
            // swung (hit or miss) he goes straight in before it can come round again. After a few seconds of
            // circling he commits anyway, so standing still with a charged axe does not keep him off for ever.
            boolean slowHand = Swing.slow(tgt);
            boolean open = slowHand && hasLos && dist < 8 && attackCd <= 6 && Swing.recovering(tgt, now);
            boolean ready = slowHand && hasLos && !open && dist > 2.4 && dist < 6 && now >= noBaitUntil;
            if (open) {
                goal = tgt.position();
                speed = 1.6;
                baitSince = -1;
                attackCd = Math.min(attackCd, 2);
                nextPath = Math.min(nextPath, now);
            } else if (ready) {
                if (baitSince < 0) {
                    baitSince = now;
                    Compat.anim(e, "cm_twitch");
                    Fx.later(lvl, 12, () -> {
                        if (e.isAlive()) Compat.anim(e, "empty");
                    });
                }
                if (now - baitSince > 50) {
                    noBaitUntil = now + 100;
                    baitSince = -1;
                }
                double ang = Math.atan2(e.getZ() - tgt.getZ(), e.getX() - tgt.getX()) + side * 0.5;
                goal = tgt.position().add(Math.cos(ang) * 4.2, 0, Math.sin(ang) * 4.2);
                speed = 1.0;
                attackCd = Math.max(attackCd, 1);
            } else {
                baitSince = -1;
                Vec3 lead = hasLos ? w.vel.scale(Math.min(10, dist * 0.5)) : Vec3.ZERO;
                goal = lastKnown.add(lead);
                if (hasLos && dist > 9) {
                    Vec3 to = tgt.position().subtract(e.position());
                    Vec3 perp = new Vec3(-to.z, 0, to.x);
                    if (perp.lengthSqr() > 1e-4) goal = goal.add(perp.normalize().scale(side * Math.min(7, dist * 0.35)));
                }
                speed = dist > 22 ? 1.45 : dist > 10 ? 1.3 : 1.15;
            }
            m.setAggressive(true);
        } else {
            searchTicks++;
            if (searchTicks > 340 + p.getRandom().nextInt(160)) {
                gaveUp = true;
                startRetreat(p, now, true);
                return true;
            }
            if (searchPoint == null || now >= nextSearch || e.position().distanceToSqr(searchPoint) < 3) {
                searchPoint = lastKnown.add((p.getRandom().nextDouble() - 0.5) * 14, 0, (p.getRandom().nextDouble() - 0.5) * 14);
                nextSearch = now + 50 + p.getRandom().nextInt(50);
                if (p.getRandom().nextFloat() < 0.25f) bang(tgt, lvl, m);
            }
            goal = searchPoint;
            speed = 0.85;
            m.setAggressive(false);
        }
        if (now >= nextPath) {
            m.getNavigation().moveTo(goal.x, goal.y, goal.z, speed);
            nextPath = now + (chase || siege ? 6 : 20);
        }
        boolean climbing = e.horizontalCollision && (goal.y > e.getY() + 0.6 || stuck(now)) && chase && !siege;
        if (climbing) {
            Vec3 v = e.getDeltaMovement();
            e.setDeltaMovement(v.x * 0.6, 0.24, v.z * 0.6);
            e.fallDistance = 0;
        }
        e.setSprinting(climbing);
        if (e.position().distanceToSqr(lastPos) > 0.25) {
            lastPos = e.position();
            lastProgressAt = now;
            breaking = null;
            breakTicks = 0;
        } else if (stuck(now) && dist > 2.2) {
            bash(tgt, lvl, m, siege ? tgt.position() : goal, now);
        }
        for (ServerPlayer o : lvl.players()) {
            if (o != tgt && o.isAlive() && o.distanceTo(e) < 1.9 && attackCd == 0 && !Compat.exempt(o)) {
                strike(p, o, lvl, e, Cfg.MAN_DAMAGE.get().floatValue() * 0.6f, now);
            }
        }
        if (dist < 1.9 && hasLos && attackCd == 0) {
            strike(p, tgt, lvl, e, Cfg.MAN_DAMAGE.get().floatValue(), now);
        }
        return true;
    }

    private void strike(ServerPlayer owner, ServerPlayer o, ServerLevel lvl, Entity e, float dmg, long now) {
        o.hurt(lvl.damageSources().mobAttack((LivingEntity) e), dmg);
        o.knockback(0.6, e.getX() - o.getX(), e.getZ() - o.getZ());
        attackCd = 26;
        hits++;
        Net.fx(o, Net.SHAKE, 3f, 10);
        Director.scare(o, 15);
        lastHit = o.getUUID();
        if (pack >= 2 && owner.getRandom().nextFloat() < 0.7f) {
            runUntil = now + 40 + owner.getRandom().nextInt(40);
            Vec3 away = e.position().subtract(o.position()).multiply(1, 0, 1);
            runTo = e.position().add((away.lengthSqr() > 1e-3 ? away.normalize() : new Vec3(0, 0, 1)).scale(10));
            side = -side;
        } else if (owner.getRandom().nextFloat() < 0.5f) {
            pauseUntil = now + 14;
            nextPause = now + 60;
        }
    }

    private boolean telegraph(ServerPlayer p, Watch w, ServerLevel lvl, long now) {
        long t = now - phaseAt;
        if (spawnAt == null) {
            BlockPos at = hint != null ? Spots.nearFloor(lvl, hint, 1, 3) : null;
            if (at == null || at.distSqr(p.blockPosition()) < 64) {
                Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(18, 30).dir(180, 70).body(0.5, 0.9).cave(w.underground).tries(40)
                    .when(k -> !k.sight.on).rank(k -> -Math.abs(k.dist - 24) * 0.05 + (k.vis > 0 ? 0.3 : 0)));
                if (c != null) at = c.feet;
            }
            if (at == null) return t < 140;
            spawnAt = at;
            Vec3 from = Vec3.atCenterOf(at);
            for (Animal a : lvl.getEntitiesOfClass(Animal.class, p.getBoundingBox().inflate(28, 8, 28), a -> !Compat.managed(a))) {
                Vec3 away = a.position().add(a.position().subtract(from).normalize().scale(16));
                a.getNavigation().moveTo(away.x, away.y, away.z, 1.8);
            }
        }
        if (!screamed && t >= 34) {
            screamed = true;
            Vec3 dir = Vec3.atCenterOf(spawnAt).subtract(p.position());
            Vec3 far = p.getEyePosition().add(dir.normalize().scale(38));
            Fx.sound(p, "man:scream_loop", SoundSource.HOSTILE, far, 1.6f, 0.85f);
        }
        if (t < 78) return true;
        if (stand != null) {
            Entity st = lvl.getEntity(stand);
            stand = null;
            if (st != null) {
                if (st.distanceToSqr(p) > 49) spawnAt = st.blockPosition();
                st.discard();
            }
        }
        if (!Spots.standable(lvl, spawnAt, 1)) {
            BlockPos alt = Spots.nearFloor(lvl, spawnAt, 1, 3);
            if (alt == null) return false;
            spawnAt = alt;
        }
        Entity e = ManMind.make(p, Compat.MAN_CRAWLER, spawnAt, false);
        if (e == null) return false;
        e.getPersistentData().putString(Compat.TAG, "man_hunt");
        crawler = e.getUUID();
        if (e instanceof Mob m) {
            m.setAggressive(true);
            m.setSilent(false);
        }
        Fx.soundAll(lvl, "man:scream_loop", SoundSource.HOSTILE, e.position(), 3.0f, 1.0f);
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 90, 0, false, false));
        Net.fx(p, Net.SHAKE, 1.4f, 20);
        Director.scare(p, 40);
        lastKnown = p.position();
        lastSeen = now;
        lastPos = e.position();
        lastProgressAt = now;
        nextLoop = now + 100;
        nextPause = now + 50 + p.getRandom().nextInt(40);
        phase = 1;
        CaveMind.debug("frenzy begins at {} ({})", spawnAt, why);
        return true;
    }

    // A rushed swing barely registers and a full one makes him flinch, but a full swing of a heavy weapon lands with a
    // dull boom, throws him about four blocks and drops him to one knee for two seconds.
    void hit(ServerPlayer p, ServerPlayer by, Entity e, Swing.Blow b, long now) {
        if (retreat || phase == 0) return;
        if (b == null) b = new Swing.Blow(1, false, false);
        hitsTaken++;
        weightTaken += b.weight();
        ServerLevel lvl = p.serverLevel();
        Vec3 push = e.position().subtract(by.position()).multiply(1, 0, 1);
        push = push.lengthSqr() > 1e-4 ? push.normalize() : Vec3.ZERO;
        if (b.heavy()) {
            e.setDeltaMovement(push.x * 0.8, 0.35, push.z * 0.8);
            e.hurtMarked = true;
            pauseUntil = now + 40;
            attackCd = Math.max(attackCd, 50);
            runUntil = 0;
            baitSince = -1;
            Compat.anim(e, "cm_twitch");
            Fx.later(lvl, 40, () -> {
                if (e.isAlive()) Compat.anim(e, "empty");
            });
            Fx.soundAll(lvl, "minecraft:entity.generic.explode", SoundSource.HOSTILE, e.position(), 0.55f, 0.45f);
            Fx.soundAll(lvl, "minecraft:block.anvil.land", SoundSource.HOSTILE, e.position(), 0.5f, 0.5f);
            lvl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, e.getX(), e.getY() + 0.6, e.getZ(), 14, 0.35, 0.3, 0.35, 0.02);
            Net.fx(by, Net.SHAKE, 1.6f, 10);
        } else if (b.charged()) {
            e.setDeltaMovement(push.x * 0.35, 0.18, push.z * 0.35);
            e.hurtMarked = true;
            pauseUntil = Math.max(pauseUntil, now + 6);
        }
        Fx.soundAll(lvl, "man:scream_loop", SoundSource.HOSTILE, e.position(), b.weight() >= 2 ? 1.2f : 0.6f, b.heavy() ? 0.9f : 1.35f);
        Director.scare(p, 6);
        if (weightTaken >= need()) {
            fled = true;
            ServerLevel sl = p.serverLevel();
            boolean blood = vn.cavemind.world.Moon.is(sl, vn.cavemind.world.Moon.BLOOD);
            int bones = blood ? 2 : p.getRandom().nextFloat() < 0.4f ? 1 : 0;
            if (bones > 0) {
                net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(sl, e.getX(), e.getY() + 1, e.getZ(), new net.minecraft.world.item.ItemStack(vn.cavemind.gear.Gear.BONE.get(), bones));
                ie.setDefaultPickUpDelay();
                ie.setUnlimitedLifetime();
                sl.addFreshEntity(ie);
                p.displayClientMessage(net.minecraft.network.chat.Component.literal(blood ? "Dưới Trăng Máu, hắn bỏ chạy và rơi lại mấy mảnh xương lạnh như sương." : "Hắn bỏ chạy, để rơi lại một mảnh xương lạnh như sương.")
                    .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC), false);
            }
            startRetreat(p, now, false);
        } else {
        }
    }

    private void bang(ServerPlayer p, ServerLevel lvl, Mob m) {
        Vec3 from = m.getEyePosition(), to = p.getEyePosition();
        BlockPos hitPos = BlockGetter.traverseBlocks(from, to, lvl, (level, pos) -> {
            BlockState st = level.getBlockState(pos);
            return st.isAir() || st.getCollisionShape(level, pos).isEmpty() ? null : pos.immutable();
        }, level -> null);
        if (hitPos == null || Vec3.atCenterOf(hitPos).distanceTo(from) > 4.5) return;
        BlockState st = lvl.getBlockState(hitPos);
        Fx.soundAll(lvl, net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getKey(st.getSoundType().getHitSound()).toString(),
            SoundSource.HOSTILE, Vec3.atCenterOf(hitPos), 1.4f, 0.55f);
        Fx.soundAll(lvl, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR.getLocation().toString(), SoundSource.HOSTILE, Vec3.atCenterOf(hitPos), 0.7f, 0.6f);
        lvl.destroyBlockProgress(m.getId(), hitPos, 3 + p.getRandom().nextInt(5));
        Net.fx(p, Net.SHAKE, 0.6f, 6);
        if (breakable(lvl, hitPos) && p.getRandom().nextFloat() < 0.4f && (smashed < 2 || !Cfg.MERCY.get())) {
            smashed++;
            lvl.destroyBlockProgress(m.getId(), hitPos, -1);
            lvl.destroyBlock(hitPos, true, m);
        }
    }

    private boolean stuck(long now) {
        return now - lastProgressAt > 30;
    }

    private void bash(ServerPlayer p, ServerLevel lvl, Mob m, Vec3 goal, long now) {
        if (breaking == null) {
            Vec3 dir = goal.subtract(m.position());
            Direction d = Direction.getNearest(dir.x, 0, dir.z);
            BlockPos base = m.blockPosition().relative(d);
            for (BlockPos q : new BlockPos[]{base, base.above()}) {
                if (breakable(lvl, q)) {
                    breaking = q;
                    break;
                }
            }
            if (breaking == null) return;
            breakTicks = 0;
        }
        breakTicks++;
        if (breakTicks % 10 == 1) {
            Fx.soundAll(lvl, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR.getLocation().toString(), SoundSource.HOSTILE, Vec3.atCenterOf(breaking), 1.0f, 0.7f);
        }
        lvl.destroyBlockProgress(m.getId(), breaking, Mth.clamp(breakTicks / 5, 0, 9));
        if (breakTicks >= 48) {
            lvl.destroyBlockProgress(m.getId(), breaking, -1);
            if (smashed >= 2 && Cfg.MERCY.get()) {
                breaking = null;
                breakTicks = 0;
                lastProgressAt = now;
                return;
            }
            smashed++;
            lvl.destroyBlock(breaking, true, m);
            breaking = null;
            breakTicks = 0;
            lastProgressAt = now;
        }
    }

    static boolean breakable(ServerLevel lvl, BlockPos q) {
        BlockState st = lvl.getBlockState(q);
        if (st.isAir() || st.hasBlockEntity()) return false;
        float hard = st.getDestroySpeed(lvl, q);
        if (hard < 0 || hard > 3.1f) return false;
        return Sight.glassy(st) || st.is(BlockTags.DOORS) || st.is(BlockTags.TRAPDOORS) || st.is(BlockTags.FENCE_GATES)
            || st.is(BlockTags.FENCES) || st.is(BlockTags.LEAVES) || st.is(BlockTags.WOOL);
    }

    private void startRetreat(ServerPlayer p, long now, boolean lost) {
        retreat = true;
        retreatAt = now;
        ServerLevel lvl = p.serverLevel();
        Entity e = crawler == null ? null : lvl.getEntity(crawler);
        if (e != null && (lost || fled)) {
            Fx.soundAll(lvl, "man:scream_loop", SoundSource.HOSTILE, e.position(), 1.2f, 0.7f);
        }
        if (Cfg.FOG.get()) Net.fx(p, Net.FOG, 0.3f, 200);
    }

    void discard(ServerLevel lvl) {
        if (stand != null) {
            Entity st = lvl.getEntity(stand);
            if (st != null) st.discard();
            stand = null;
        }
        if (crawler == null) return;
        Entity e = lvl.getEntity(crawler);
        if (e != null && e.isAlive()) e.discard();
    }

    void record(ServerPlayer p, Profile pr) {
        ServerLevel lvl = p.serverLevel();
        if (caught) pr.manCaught++;
        else if (gaveUp || retreat) {
            pr.manEscapes++;
            pr.moment(new Moment(Moment.ESCAPED, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), hits, 0));
        }
        pr.moment(new Moment(Moment.FRENZY, Dread.day(lvl), Dread.time(lvl), p.blockPosition(), hits, hitsTaken));
        pr.anger(fled ? 20 : -50);
    }
}
