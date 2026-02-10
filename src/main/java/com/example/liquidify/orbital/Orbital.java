package com.example.liquidify.orbital;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
public class Orbital implements ModInitializer {
    private static final Component ORBITAL_NAME =
            Component.literal("Orbital").withStyle(Style.EMPTY.withBold(true).withColor(0xAA00FF));
    private static final Component STABSHOT_NAME =
            Component.literal("StabShot").withStyle(Style.EMPTY.withBold(true).withColor(0xFFFF00));
    private static final Component STAIRS_NAME =
            Component.literal("Stairs").withStyle(Style.EMPTY.withBold(true).withColor(0x00FFFF));
    private static final Component RAILGUN_NAME =
            Component.literal("Railgun").withStyle(Style.EMPTY.withBold(true).withColor(0xFF5500));
    private static final Component BLACKHOLE_NAME =
            Component.literal("BlackHole").withStyle(Style.EMPTY.withBold(true).withColor(0x0000AA));
    private static final int ORBITAL_RINGS = 10;
    private static final double ORBITAL_MAX_RADIUS = 67.0;
    private static final double ORBITAL_SPACING = 2.5;
    private static final int ORBITAL_RINGS_PER_BATCH = 2;
    private static final int ORBITAL_TICKS_PER_BATCH = 2;
    private static final int FINAL_FUSE = 90;
    private static final List<OrbitalTask> TASKS = new ArrayList<>();
    private static final Set<UUID> ACTIVE = new HashSet<>();
    private static final List<BlackHoleTask> BLACKHOLES = new ArrayList<>();
    private static class OrbitalTask {
        final ServerLevel level;
        final UUID playerId;
        final double px, py, pz;
        final List<PrimedTnt> tntList = new ArrayList<>();
        int nextRing = 1;
        int tick = 0;
        OrbitalTask(ServerLevel level, Player p) {
            this.level = level;
            this.playerId = p.getUUID();
            this.px = p.getX();
            this.py = p.getY() + 67;
            this.pz = p.getZ();
        }
    }
    private static class BlackHoleTask {
        final ServerLevel level;
        final Player owner;
        final double cx, cy, cz;
        int ticks = 0;
        int stage2Tick = -1;
        int stage3Tick = -1;
        int stage4Tick = -1;
        final List<ArmorStand> sphere = new ArrayList<>();
        BlackHoleTask(ServerLevel level, Player owner, double cx, double cy, double cz) {
            this.level = level;
            this.owner = owner;
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
        }
    }
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("Wemmburods")
                            .then(Commands.literal("orbital")
                                    .executes(ctx -> {
                                        Player p = ctx.getSource().getPlayerOrException();
                                        ItemStack rod = new ItemStack(Items.FISHING_ROD);
                                        rod.setDamageValue(rod.getMaxDamage() - 1);
                                        rod.set(DataComponents.CUSTOM_NAME, ORBITAL_NAME);
                                        p.getInventory().add(rod);
                                        return 1;
                                    })
                            )
                            .then(Commands.literal("blackhole")
                                    .executes(ctx -> {
                                        Player p = ctx.getSource().getPlayerOrException();
                                        ItemStack rod = new ItemStack(Items.FISHING_ROD);
                                        rod.setDamageValue(rod.getMaxDamage() - 1);
                                        rod.set(DataComponents.CUSTOM_NAME, BLACKHOLE_NAME);
                                        p.getInventory().add(rod);
                                        return 1;
                                    })
                            )
                            .then(Commands.literal("stabshot")
                                    .executes(ctx -> {
                                        Player p = ctx.getSource().getPlayerOrException();
                                        ItemStack rod = new ItemStack(Items.FISHING_ROD);
                                        rod.setDamageValue(rod.getMaxDamage() - 1);
                                        rod.set(DataComponents.CUSTOM_NAME, STABSHOT_NAME);
                                        p.getInventory().add(rod);
                                        return 1;
                                    })
                            )
                            .then(Commands.literal("stairs")
                                    .executes(ctx -> {
                                        Player p = ctx.getSource().getPlayerOrException();
                                        ItemStack rod = new ItemStack(Items.FISHING_ROD);
                                        rod.setDamageValue(rod.getMaxDamage() - 1);
                                        rod.set(DataComponents.CUSTOM_NAME, STAIRS_NAME);
                                        p.getInventory().add(rod);
                                        return 1;
                                    })
                            )
                            .then(Commands.literal("railgun")
                                    .executes(ctx -> {
                                        Player p = ctx.getSource().getPlayerOrException();
                                        ItemStack rod = new ItemStack(Items.FISHING_ROD);
                                        rod.setDamageValue(rod.getMaxDamage() - 1);
                                        rod.set(DataComponents.CUSTOM_NAME, RAILGUN_NAME);
                                        p.getInventory().add(rod);
                                        return 1;
                                    })
                            )
            );
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            Component name = stack.get(DataComponents.CUSTOM_NAME);
            if (name == null) return InteractionResult.PASS;
            String n = name.getString();
            if (!level.isClientSide()) {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 255, false, false, false));
            }
            if (n.contains("Orbital")) {
                if (!level.isClientSide()) {
                    UUID id = player.getUUID();
                    if (!ACTIVE.contains(id)) {
                        ACTIVE.add(id);
                        ServerLevel sl = (ServerLevel) level;
                        OrbitalTask task = new OrbitalTask(sl, player);
                        spawnCenter(task);
                        TASKS.add(task);
                        stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                    }
                }
                return InteractionResult.SUCCESS;
            }
            if (n.contains("BlackHole")) {
                if (!level.isClientSide()) {
                    spawnBlackHole(level, player);
                    stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                }
                return InteractionResult.SUCCESS;
            }
            if (n.contains("StabShot")) {
                if (!level.isClientSide()) {
                    spawnStabShot(level, player);
                    stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                }
                return InteractionResult.SUCCESS;
            }
            if (n.contains("Stairs")) {
                if (!level.isClientSide()) {
                    spawnStairs(level, player);
                    stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                }
                return InteractionResult.SUCCESS;
            }
            if (n.contains("Railgun")) {
                if (!level.isClientSide()) {
                    spawnRailgun(level, player);
                    stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<OrbitalTask> it = TASKS.iterator();
            while (it.hasNext()) {
                OrbitalTask task = it.next();
                task.tick++;
                if (task.tick % ORBITAL_TICKS_PER_BATCH != 0) continue;
                for (int i = 0; i < ORBITAL_RINGS_PER_BATCH && task.nextRing <= ORBITAL_RINGS; i++) {
                    spawnRing(task, task.nextRing);
                    task.nextRing++;
                }
                if (task.nextRing > ORBITAL_RINGS) {
                    for (PrimedTnt t : task.tntList) t.setFuse(FINAL_FUSE);
                    ACTIVE.remove(task.playerId);
                    it.remove();
                }
            }
            Iterator<BlackHoleTask> bhIt = BLACKHOLES.iterator();
            while (bhIt.hasNext()) {
                BlackHoleTask bh = bhIt.next();
                bh.ticks++;
                List<Entity> entities = bh.level.getEntities(
                        null,
                        new AABB(
                                bh.cx - 25, bh.cy - 25, bh.cz - 25,
                                bh.cx + 25, bh.cy + 25, bh.cz + 25
                        )
                );
                for (Entity e : entities) {
                    if (e instanceof FallingBlockEntity) continue;
                    double dx = bh.cx - e.getX();
                    double dy = bh.cy - e.getY();
                    double dz = bh.cz - e.getZ();
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist > 1) {
                        double pull = 0.35 / dist;
                        double jitterX = (Math.random() - 0.5) * 0.05;
                        double jitterZ = (Math.random() - 0.5) * 0.05;
                        e.setDeltaMovement(
                                e.getDeltaMovement().add(
                                        dx * pull + jitterX,
                                        dy * pull * 0.5,
                                        dz * pull + jitterZ
                                )
                        );
                    }
                }
                if (bh.ticks % 3 == 0) {
                    double angle = Math.random() * Math.PI * 2;
                    double radius = 6 + Math.random() * 10;
                    int bx = (int) (bh.cx + Math.cos(angle) * radius);
                    int bz = (int) (bh.cz + Math.sin(angle) * radius);
                    int by = (int) (bh.cy - 1);
                    BlockPos pos = new BlockPos(bx, by, bz);
                    BlockState state = bh.level.getBlockState(pos);
                    if (!state.isAir() && !state.getFluidState().isSource()) {
                        bh.level.removeBlock(pos, false);
                        FallingBlockEntity fb = FallingBlockEntity.fall(bh.level, pos, state);
                        fb.setUUID(UUID.randomUUID());
                        fb.setPos(bx + 0.5, by + 0.2, bz + 0.5);
                        fb.setDeltaMovement(0, 0, 0);
                        double dx = bh.cx - fb.getX();
                        double dz = bh.cz - fb.getZ();
                        double vx = dx * 0.15;
                        double vy = 0.35;
                        double vz = dz * 0.15;
                        fb.setDeltaMovement(vx, vy, vz);
                    }
                }
                if (bh.ticks == bh.stage2Tick) {
                    LightningBolt b = new LightningBolt(EntityType.LIGHTNING_BOLT, bh.level);
                    b.setPos(bh.cx, bh.cy, bh.cz);
                    bh.level.addFreshEntity(b);
                    bh.sphere.addAll(spawnSphere(bh.level, bh.cx, bh.cy, bh.cz, 1.2, 10));
                }
                if (bh.ticks == bh.stage3Tick) {
                    LightningBolt b = new LightningBolt(EntityType.LIGHTNING_BOLT, bh.level);
                    b.setPos(bh.cx, bh.cy, bh.cz);
                    bh.level.addFreshEntity(b);
                    bh.sphere.addAll(spawnSphere(bh.level, bh.cx, bh.cy, bh.cz, 2.2, 14));
                }
                if (bh.ticks == bh.stage4Tick) {
                    LightningBolt b = new LightningBolt(EntityType.LIGHTNING_BOLT, bh.level);
                    b.setPos(bh.cx, bh.cy, bh.cz);
                    bh.level.addFreshEntity(b);
                    bh.sphere.addAll(spawnSphere(bh.level, bh.cx, bh.cy, bh.cz, 3.5, 18));
                }
                if (bh.ticks >= 40) {
                    for (int i = 0; i < 120; i++) {
                        PrimedTnt t = new PrimedTnt(bh.level, bh.cx, bh.cy, bh.cz, bh.owner);
                        t.setFuse(1);
                        bh.level.addFreshEntity(t);
                    }
                    for (ArmorStand a : bh.sphere) {
                        a.discard();
                    }
                    bh.sphere.clear();
                    bhIt.remove();
                }
            }
        });
    }
    private void spawnCenter(OrbitalTask task) {
        PrimedTnt t = new PrimedTnt(task.level, task.px, task.py, task.pz, null);
        t.setFuse(9999);
        task.level.addFreshEntity(t);
        task.tntList.add(t);
    }
    private void spawnRing(OrbitalTask task, int ring) {
        double radiusStep = ORBITAL_MAX_RADIUS / ORBITAL_RINGS;
        double radius = ring * radiusStep;
        double circumference = 2 * Math.PI * radius;
        int count = Math.max(8, (int) Math.round(circumference / ORBITAL_SPACING));
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            double x = task.px + Math.cos(angle) * radius;
            double z = task.pz + Math.sin(angle) * radius;
            PrimedTnt t = new PrimedTnt(task.level, x, task.py, z, null);
            t.setFuse(9999);
            task.level.addFreshEntity(t);
            task.tntList.add(t);
        }
    }
    private void spawnStabShot(Level level, Player player) {
        double yaw = Math.toRadians(player.getYRot());
        double dx = -Math.sin(yaw);
        double dz = Math.cos(yaw);
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double baseX = px + dx * 10;
        double baseZ = pz + dz * 10;
        for (double y = -30; y <= py; y += 2) {
            for (int i = 0; i < 3; i++) {
                PrimedTnt t = new PrimedTnt(level, baseX, y, baseZ, player);
                t.setFuse(1);
                level.addFreshEntity(t);
            }
        }
    }
    private void spawnStairs(Level level, Player player) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        for (int i = 0; i < 50; i++) {
            double angle = i * 0.4;
            double x = px + Math.cos(angle) * 5;
            double z = pz + Math.sin(angle) * 5;
            double y = py + (i * 0.5);
            PrimedTnt t = new PrimedTnt(level, x, y, z, player);
            t.setFuse(100);
            level.addFreshEntity(t);
        }
    }
    private void spawnRailgun(Level level, Player player) {
        double yaw = Math.toRadians(player.getYRot());
        double dx = -Math.sin(yaw);
        double dz = Math.cos(yaw);
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        for (int i = 1; i <= 30; i++) {
            double x = px + dx * (i * 2.5);
            double z = pz + dz * (i * 2.5);
            for (int t = 0; t < 4; t++) {
                PrimedTnt e = new PrimedTnt(level, x, py, z, player);
                e.setFuse(1);
                level.addFreshEntity(e);
            }
        }
    }
    private void spawnBlackHole(Level level, Player player) {
        HitResult hit = player.pick(100, 0, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockHitResult bhr = (BlockHitResult) hit;
        BlockPos pos = bhr.getBlockPos();
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 1.0;
        double cz = pos.getZ() + 0.5;
        ServerLevel sl = (ServerLevel) level;
        BlackHoleTask bh = new BlackHoleTask(sl, player, cx, cy, cz);
        BLACKHOLES.add(bh);
        LightningBolt bolt1 = new LightningBolt(EntityType.LIGHTNING_BOLT, sl);
        bolt1.setPos(cx, cy, cz);
        sl.addFreshEntity(bolt1);
        ArmorStand center = new ArmorStand(sl, cx, cy, cz);
        center.setInvisible(true);
        center.setNoGravity(true);
        center.setInvulnerable(true);
        center.setNoBasePlate(true);
        center.setShowArms(false);
        center.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.OBSIDIAN));
        sl.addFreshEntity(center);
        bh.sphere.add(center);
        bh.stage2Tick = 10;
        bh.stage3Tick = 20;
        bh.stage4Tick = 30;
    }
    private List<ArmorStand> spawnSphere(ServerLevel level, double cx, double cy, double cz, double radius, int steps) {
        List<ArmorStand> list = new ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            double lat = Math.PI * (-0.5 + (double) i / steps);
            double y = Math.sin(lat);
            double r = Math.cos(lat);
            for (int j = 0; j < steps * 2; j++) {
                double lon = 2 * Math.PI * j / (steps * 2);
                double x = r * Math.cos(lon);
                double z = r * Math.sin(lon);
                ArmorStand a = new ArmorStand(level, cx + x * radius, cy + y * radius, cz + z * radius);
                a.setInvisible(true);
                a.setNoGravity(true);
                a.setInvulnerable(true);
                a.setNoBasePlate(true);
                a.setShowArms(false);
                a.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.OBSIDIAN));
                level.addFreshEntity(a);
                list.add(a);
            }
        }
        return list;
    }
}
