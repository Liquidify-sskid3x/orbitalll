package com.example.liquidify.orbital;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
// bold and custom color names
public class Orbital implements ModInitializer {
    private static final Component ORBITAL_NAME =
            Component.literal("Orbital").withStyle(Style.EMPTY.withBold(true).withColor(0xAA00FF));
    private static final Component STABSHOT_NAME =
            Component.literal("StabShot").withStyle(Style.EMPTY.withBold(true).withColor(0xFFFF00));
    private static final Component STAIRS_NAME =
            Component.literal("Stairs").withStyle(Style.EMPTY.withBold(true).withColor(0x00FFFF));
    private static final Component RAILGUN_NAME =
            Component.literal("Railgun").withStyle(Style.EMPTY.withBold(true).withColor(0xFF5500));
    @Override
    //command setup
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
    //gives you resistance so you dont die
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
                    spawnOrbital(level, player);
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
    }
    //orbital strike cannon setup
private void spawnOrbital(Level level, Player player) {
    double px = player.getX();
    double py = player.getY() + 10;
    double pz = player.getZ();
    spawn(level, px, py, pz, player);
    int rings = 10;
    double maxRadius = 67.0;
    double radiusStep = maxRadius / rings;
    double spacing = 2.5;
    for (int r = 1; r <= rings; r++) {
        double radius = r * radiusStep;
        double circumference = 2 * Math.PI * radius;
        int count = Math.max(8, (int) Math.round(circumference / spacing));
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            double x = px + Math.cos(angle) * radius;
            double z = pz + Math.sin(angle) * radius;
            spawn(level, x, py, z, player);
        }
    }
}
    //stabshot tnt setup
    private void spawnStabShot(Level level, Player player) {
        double yaw = Math.toRadians(player.getYRot());
        double dx = -Math.sin(yaw);
        double dz = Math.cos(yaw);
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double baseX = px + dx * 10;
        double baseZ = pz + dz * 10;
        double startY = -30;
        double endY = py;
        for (double y = startY; y <= endY; y += 2) {
            for (int i = 0; i < 3; i++) {
                PrimedTnt tnt = new PrimedTnt(level, baseX, y, baseZ, player);
                tnt.setFuse(1);
                level.addFreshEntity(tnt);
            }
        }
    }
    //stairs tnt setup
    private void spawnStairs(Level level, Player player) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double radius = 5;
        int steps = 50;
        for (int i = 0; i < steps; i++) {
            double angle = i * 0.4;
            double x = px + Math.cos(angle) * radius;
            double z = pz + Math.sin(angle) * radius;
            double y = py + (i * 0.5);
            PrimedTnt tnt = new PrimedTnt(level, x, y, z, player);
            tnt.setFuse(100);
            level.addFreshEntity(tnt);
        }
    }
    //railgun tnt setup
    private void spawnRailgun(Level level, Player player) {
        double yaw = Math.toRadians(player.getYRot());
        double dx = -Math.sin(yaw);
        double dz = Math.cos(yaw);

        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double spacing = 2.5;
        for (int i = 1; i <= 30; i++) {
            double x = px + dx * (i * spacing);
            double z = pz + dz * (i * spacing);
            for (int t = 0; t < 4; t++) {
                PrimedTnt tnt = new PrimedTnt(level, x, py, z, player);
                tnt.setFuse(1);
                level.addFreshEntity(tnt);
            }
        }
    }
    private void spawn(Level level, double x, double y, double z, Player player) {
        PrimedTnt tnt = new PrimedTnt(level, x, y, z, player);
        level.addFreshEntity(tnt);
    }

}

