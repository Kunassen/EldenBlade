package dev.kunas.eldenblade;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;

public final class BladeCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        var config = Commands.literal("config").executes(c -> show(c.getSource()))
            .then(Commands.literal("show").executes(c -> show(c.getSource())))
            .then(setting("attack", BladeConfig.ATTACK, 1000000))
            .then(setting("sa_percent", BladeConfig.SA_PERCENT, 100))
            .then(setting("se_chance", BladeConfig.SE_CHANCE, 100))
            .then(setting("se_percent", BladeConfig.SE_PERCENT, 100))
            .then(Commands.literal("se_cooldown").then(Commands.argument("ticks", IntegerArgumentType.integer(0, 12000))
                .executes(c -> {
                    BladeConfig.setCooldown(IntegerArgumentType.getInteger(c, "ticks"));
                    BladeConfig.SPEC.save();
                    return show(c.getSource());
                })));
        event.getDispatcher().register(Commands.literal("eldenblade").requires(s -> s.hasPermission(2))
            .executes(c -> {
                c.getSource().sendSuccess(() -> Component.literal("/eldenblade config 查看配置；config attack|sa_percent|se_chance|se_percent <数值>；config se_cooldown <tick>；attack <数值>|reset 修改手持刀；give 获取刀。百分比填写 10 表示 10%。"), false);
                return 1;
            })
            .then(config)
            .then(Commands.literal("health").then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.entity())
                .executes(c -> {
                    var entity = net.minecraft.commands.arguments.EntityArgument.getEntity(c, "target");
                    if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) {
                        c.getSource().sendFailure(Component.literal("目标不是生物。")); return 0;
                    }
                    var snapshot = HealthStorage.inspect(living);
                    String storage = snapshot.supported() ? "已识别同步血量，字段编号 " + snapshot.key().getId()
                        : "暂不支持或读取来源不明确，不会猜测字段";
                    c.getSource().sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT,
                        "%s：服务端生命 %.3f / %.3f；%s。此指令只读取，不扣血。",
                        living.getName().getString(), snapshot.health(), living.getMaxHealth(), storage)), false);
                    return snapshot.supported() ? 1 : 0;
                })))
            .then(Commands.literal("attack")
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 1000000)).executes(c ->
                    heldAttack(c.getSource(), DoubleArgumentType.getDouble(c, "value"))))
                .then(Commands.literal("reset").executes(c -> heldAttack(c.getSource(), null))))
            .then(Commands.literal("give").executes(context -> {
                var player = context.getSource().getPlayerOrException();
                var blade = player.level().registryAccess().registryOrThrow(SlashBladeDefinition.REGISTRY_KEY)
                    .get(EldenBlade.id("rivers_of_blood"));
                if (blade == null) { context.getSource().sendFailure(Component.literal("Rivers of Blood definition missing")); return 0; }
                var stack = blade.getBlade();
                BladeBalance.apply(stack);
                if (!player.getInventory().add(stack)) player.drop(stack, false);
                return 1;
            })));
    }
    private static LiteralArgumentBuilder<CommandSourceStack> setting(String name, ForgeConfigSpec.DoubleValue value, double max) {
        return Commands.literal(name).then(Commands.argument("value", DoubleArgumentType.doubleArg(0, max)).executes(c -> {
            BladeConfig.setDouble(value, DoubleArgumentType.getDouble(c, "value"));
            BladeConfig.SPEC.save();
            return show(c.getSource());
        }));
    }
    private static int show(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT,
            "尸山血海配置：默认基础攻击 %.2f；SA 每段最大生命 %.2f%%（两段 %.2f%%）；SE 概率 %.2f%%、最大生命 %.2f%%、冷却 %d tick。文件：当前存档/serverconfig/eldenblade-server.toml。",
            BladeConfig.attack(), BladeConfig.saPercent(), BladeConfig.saPercent()*2,
            BladeConfig.seChance(), BladeConfig.sePercent(), BladeConfig.cooldown())), false);
        return 1;
    }
    private static int heldAttack(CommandSourceStack source, Double value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        var stack = player.getMainHandItem();
        if (!BladeBalance.isBloodBlade(stack)) {
            source.sendFailure(Component.literal("请将尸山血海拿在主手。")); return 0;
        }
        if (value == null) stack.getOrCreateTag().remove(BladeBalance.OVERRIDE);
        else stack.getOrCreateTag().putDouble(BladeBalance.OVERRIDE, value);
        BladeBalance.apply(stack);
        player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
        source.sendSuccess(() -> Component.literal(value == null ? "此刀已恢复跟随存档默认攻击力。" : "此刀基础攻击力已设为 " + value + "，已保存至物品数据。"), true);
        return 1;
    }
}
