package dev.kunas.eldenblade;

import net.minecraftforge.common.ForgeConfigSpec;

/** World/server-authoritative balance values. Percent values use 10 = 10%, not 0.10. */
public final class BladeConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue ATTACK, SA_PERCENT, SE_CHANCE, SE_PERCENT;
    public static final ForgeConfigSpec.IntValue SE_COOLDOWN;
    static {
        var b = new ForgeConfigSpec.Builder();
        b.push("balance");
        ATTACK = b.comment("Default base attack for Rivers of Blood. Existing non-overridden blades follow this value.")
            .defineInRange("baseAttack", 24.0, 0.0, 1000000.0);
        SA_PERCENT = b.comment("Maximum-health percent PER SA SEGMENT. 10 means 10%; two segments total 20%.")
            .defineInRange("saMaxHealthPercent", 10.0, 0.0, 100.0);
        SE_CHANCE = b.comment("SE chance per eligible hit, in percent. 0 disables SE; 100 always procs when off cooldown.")
            .defineInRange("seChancePercent", 10.0, 0.0, 100.0);
        SE_PERCENT = b.comment("Maximum-health percent removed by one successful SE proc.")
            .defineInRange("seMaxHealthPercent", 10.0, 0.0, 100.0);
        SE_COOLDOWN = b.comment("SE cooldown in ticks, shared per player. 2 ticks = 0.1 seconds at 20 TPS; 0 disables cooldown.")
            .defineInRange("seCooldownTicks", 2, 0, 12000);
        b.pop(); SPEC = b.build();
    }
    private record Values(double attack, double sa, double chance, double percent, int cooldown) {}
    private static volatile Values values = new Values(24, 10, 10, 10, 2);
    public static double attack() { return values.attack; }
    public static double saPercent() { return values.sa; }
    public static double seChance() { return values.chance; }
    public static double sePercent() { return values.percent; }
    public static int cooldown() { return values.cooldown; }
    public static void setDouble(ForgeConfigSpec.DoubleValue key, double value) {
        key.set(value);
        Values v=values;
        values=new Values(key==ATTACK ? value : v.attack, key==SA_PERCENT ? value : v.sa,
            key==SE_CHANCE ? value : v.chance, key==SE_PERCENT ? value : v.percent, v.cooldown);
    }
    public static void setCooldown(int value) {
        SE_COOLDOWN.set(value);
        Values v=values; values=new Values(v.attack,v.sa,v.chance,v.percent,value);
    }
    private static void loadValues() {
        if (SPEC.isLoaded()) values=new Values(ATTACK.get(),SA_PERCENT.get(),SE_CHANCE.get(),SE_PERCENT.get(),SE_COOLDOWN.get());
    }
    public static void onConfigEvent(net.minecraftforge.fml.event.config.ModConfigEvent event) {
        if (event.getConfig().getSpec()!=SPEC) return;
        if (event instanceof net.minecraftforge.fml.event.config.ModConfigEvent.Unloading) {
            values=new Values(24,10,10,10,2);
        } else if (event instanceof net.minecraftforge.fml.event.config.ModConfigEvent.Reloading) {
            // File watcher runs off-thread. Serialize reloads with commands/combat on the server thread.
            var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server!=null) server.execute(BladeConfig::loadValues);
            else loadValues();
        } else loadValues();
    }
    private BladeConfig() {}
}
