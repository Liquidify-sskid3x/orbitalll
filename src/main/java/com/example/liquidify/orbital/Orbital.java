package com.example.liquidify.orbital;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.*;

public class Orbital implements ModInitializer {
    private static final Component ORBITAL_NAME =
            Component.literal("Orbital").withStyle(Style.EMPTY.withBold(true).withColor(0xAA00FF));
    private static final Component STABSHOT_NAME =
            Component.literal("StabShot").withStyle(Style.EMPTY.withBold(true).withColor(0xFFFF00));
    private static final Component STAIRS_NAME =
            Component.literal("Stairs").withStyle(Style.EMPTY.withBold(true).withColor(0x00FFFF));
    private static final Component RAILGUN_NAME =
            Component.literal("Railgun").withStyle(Style.EMPTY.withBold(true).withColor(0xFF5500));

    private static final int ORBITAL_RINGS = 10;
    private static final double ORBITAL_MAX_RADIUS = 67.0;
    private static final double ORBITAL_SPACING = 2.5;
    private static final int ORBITAL_RINGS_PER_BATCH = 2;
    private static final int ORBITAL_TICKS_PER_BATCH = 2;
    private static final int FINAL_FUSE = 90;

    private static final List<OrbitalTask> TASKS = new ArrayList<>();
    private static final Set<UUID> ACTIVE = new HashSet<>();

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
}
