package dev.kunas.eldenblade.client;

import dev.kunas.eldenblade.BloodCombat;
import dev.kunas.eldenblade.EldenBlade;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in development capture harness; excluded from the distributable JAR. */
@Mod.EventBusSubscriber(modid = EldenBlade.ID, value = Dist.CLIENT)
public final class SaVisualCheck {
    private static int ticks;
    private static boolean prepared;
    private static int frame;
    private static ArmorStand camera;
    private static long lastCapture;
    private static final java.util.List<Long> captureTimes = new java.util.ArrayList<>();

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("eldenblade.visualDemo") || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        if (++ticks < 80) return;
        if (!prepared) {
            prepared = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.options.fov().set(65);
            mc.options.hideGui = true;
            mc.options.pauseOnLostFocus = false;
            mc.setScreen(null);
            var uuid = mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                if (player == null) return;
                var level = player.serverLevel();
                level.setDayTime(6000);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                for (int x = -12; x <= 12; x++)
                    for (int z = -12; z <= 16; z++)
                        level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE_BRICKS.defaultBlockState());
                player.setGameMode(GameType.CREATIVE);
                player.connection.teleport(.5, 100, .5, 0, 0);
                var blade = level.registryAccess().registryOrThrow(
                    mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition.REGISTRY_KEY)
                    .get(EldenBlade.id("rivers_of_blood")).getBlade();
                player.setItemSlot(EquipmentSlot.MAINHAND, blade);
            });
        }
        if (ticks == 150) view(mc, .5, 103, -7.5);
        if (ticks == 190) view(mc, -7, 104, 3);
        if (ticks == 230) view(mc, .5, 103, 10);
        if (ticks == 270) view(mc, -4, 110, -.5);
        if (ticks == 310) {
            mc.setCameraEntity(mc.player);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (ticks == 160 || ticks == 200 || ticks == 240 || ticks == 280 || ticks == 320) {
            var uuid = mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                if (player != null) {
                    var state = player.getMainHandItem().getCapability(
                        mods.flammpfeil.slashblade.item.ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new);
                    state.setComboSeq(EldenBlade.CORPSE_PILER_COMBO.getId());
                    state.setLastActionTime(player.level().getGameTime());
                    BloodCombat.start(player);
                }
            });
        }
        if (ticks == 360) {
            StringBuilder concat = new StringBuilder("ffconcat version 1.0\n");
            for (int i = 0; i < captureTimes.size(); i++) {
                double duration = i + 1 < captureTimes.size()
                    ? (captureTimes.get(i + 1) - captureTimes.get(i)) / 1e9 : 1.0 / 60;
                concat.append(String.format(java.util.Locale.ROOT,
                    "file 'screenshots/sweep-hq-%04d.png'\nduration %.6f\n", i, duration));
            }
            try {
                java.nio.file.Files.writeString(mc.gameDirectory.toPath().resolve("sweep.ffconcat"), concat);
            } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
        }
        if (ticks == 370) mc.stop();
    }

    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event) {
        if (!Boolean.getBoolean("eldenblade.visualDemo") || event.phase != TickEvent.Phase.END
            || ticks < 150 || ticks >= 350) return;
        long now = System.nanoTime();
        if (now - lastCapture < 15_000_000) return;
        lastCapture = now;
        captureTimes.add(now);
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, String.format("sweep-hq-%04d.png", frame++),
            mc.getMainRenderTarget(), ignored -> {});
    }

    private static void view(Minecraft mc, double x, double y, double z) {
        if (camera == null) camera = new ArmorStand(mc.level, x, y, z);
        double dx = .5 - x, dy = 101.7 - y, dz = 2.2 - z;
        float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float)-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        camera.moveTo(x, y - camera.getEyeHeight(), z, yaw, pitch);
        camera.xo = camera.getX(); camera.yo = camera.getY(); camera.zo = camera.getZ();
        camera.yRotO = yaw; camera.xRotO = pitch;
        camera.setYHeadRot(yaw);
        camera.yHeadRotO = yaw;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.setCameraEntity(camera);
    }
}
