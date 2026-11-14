package com.valor.items.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Config of the mod: config/valor_items-common.toml and config/valor_items-client.toml. */
public final class ValorConfig {
    private static final double[] DEFAULT_BURN_RANGE = {1.5D, 1.65D, 1.75D, 1.85D, 2.0D};

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        ForgeConfigSpec.Builder common = new ForgeConfigSpec.Builder();
        COMMON = new Common(common);
        COMMON_SPEC = common.build();
        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        CLIENT = new Client(client);
        CLIENT_SPEC = client.build();
    }

    public static final class Common {
        public final ForgeConfigSpec.IntValue sickleCooldownSeconds;
        public final ForgeConfigSpec.DoubleValue[] sickleDurationSeconds = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue sickleMovementSpeedPercent;
        public final ForgeConfigSpec.DoubleValue sickleAttackSpeedBonus;
        public final ForgeConfigSpec.DoubleValue[] mailRipostePercent = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue[] mailCountersiegePercent = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue mailCountersiegeSeconds;
        public final ForgeConfigSpec.IntValue mailCooldownSeconds;
        public final ForgeConfigSpec.DoubleValue[] burnPercent = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue[] burnRange = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue burnArthropodBonusPercent;
        public final ForgeConfigSpec.DoubleValue soulPrisonReducePercent;
        public final ForgeConfigSpec.DoubleValue soulPrisonSeconds;
        public final ForgeConfigSpec.DoubleValue[] adrenalineSpeedPercent = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue[] adrenalineDamagePercent = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.IntValue adrenalineMaxStacks;
        public final ForgeConfigSpec.DoubleValue adrenalineSeconds;
        public final ForgeConfigSpec.DoubleValue[] longevityPercent = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue[] longevityRestPercent = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue longevityIntervalSeconds;
        public final ForgeConfigSpec.DoubleValue longevitySafeSeconds;
        public final ForgeConfigSpec.DoubleValue[] gaiaRestorePercent = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue gaiaRestoreSeconds;
        public final ForgeConfigSpec.DoubleValue gaiaCooldownSeconds;
        public final ForgeConfigSpec.DoubleValue gaiaBloodlineStepPercent;
        public final ForgeConfigSpec.DoubleValue gaiaBloodlineBonusPercent;
        public final ForgeConfigSpec.DoubleValue[] flameAttackSpeed = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue[] flamePercent = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.IntValue flameMaxStacks;
        public final ForgeConfigSpec.DoubleValue flameSeconds;
        public final ForgeConfigSpec.DoubleValue[] conquerPercent = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue conquerThresholdPercent;
        public final ForgeConfigSpec.DoubleValue[] swiftPercent = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue swiftSeconds;
        public final ForgeConfigSpec.DoubleValue infiniteCritRate;
        public final ForgeConfigSpec.DoubleValue infiniteCritDamage;
        public final ForgeConfigSpec.DoubleValue infiniteRateStep;
        public final ForgeConfigSpec.DoubleValue infiniteDamagePerStep;
        public final ForgeConfigSpec.DoubleValue infiniteMaxBonus;
        public final ForgeConfigSpec.DoubleValue[] windAttackSpeed = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue[] windMoveSpeed = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue windSeconds;
        public final ForgeConfigSpec.DoubleValue[] berserkPercent = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue[] madnessShieldPercent = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue berserkThreshold;
        public final ForgeConfigSpec.DoubleValue berserkLowMin;
        public final ForgeConfigSpec.DoubleValue berserkLowMax;
        public final ForgeConfigSpec.DoubleValue madnessDrainPercent;
        public final ForgeConfigSpec.DoubleValue madnessDrainSeconds;
        public final ForgeConfigSpec.DoubleValue madnessShieldSeconds;
        public final ForgeConfigSpec.IntValue madnessCooldownSeconds;
        public final ForgeConfigSpec.DoubleValue[] dragonPercent = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue dragonCooldownSeconds;
        public final ForgeConfigSpec.DoubleValue baneConvertPercent;
        public final ForgeConfigSpec.DoubleValue baneMaxMagicArmor;
        public final ForgeConfigSpec.DoubleValue[] arcticFreezeSeconds = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue arcticShieldBase;
        public final ForgeConfigSpec.DoubleValue arcticShieldMagicPercent;
        public final ForgeConfigSpec.DoubleValue arcticShieldSeconds;
        public final ForgeConfigSpec.IntValue arcticCooldownSeconds;
        public final ForgeConfigSpec.DoubleValue[] breakArmorPenetration = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue wildwalkSpeedPercent;
        public final ForgeConfigSpec.IntValue wildwalkSeconds;
        public final ForgeConfigSpec.DoubleValue guardianThreshold;
        public final ForgeConfigSpec.DoubleValue[] guardianShieldPercent = new ForgeConfigSpec.DoubleValue[5];
        public final ForgeConfigSpec.DoubleValue guardianSeconds;
        public final ForgeConfigSpec.DoubleValue guardianSpeedPercent;
        public final ForgeConfigSpec.IntValue guardianCooldownSeconds;

        private static final double[] DEFAULT_DURATIONS = {1.5D, 1.75D, 2.0D, 2.5D, 3.0D};

        Common(ForgeConfigSpec.Builder builder) {
            builder.comment("Death Sickle - Unique Passive: Insatiable").push("death_sickle");
            sickleCooldownSeconds = builder.comment("Cooldown of the passive, in seconds (starts when it triggers)")
                    .defineInRange("cooldownSeconds", 120, 0, 100000);
            for (int i = 0; i < 5; i++) {
                sickleDurationSeconds[i] = builder.comment("Seconds of immortality at level " + (i + 1))
                        .defineInRange("durationSecondsLevel" + (i + 1), DEFAULT_DURATIONS[i], 0.05D, 600.0D);
            }
            sickleMovementSpeedPercent = builder.comment("Movement speed bonus while immortal, in percent")
                    .defineInRange("movementSpeedPercent", 25.0D, 0.0D, 1000.0D);
            sickleAttackSpeedBonus = builder.comment("Attack speed added while immortal")
                    .defineInRange("attackSpeedBonus", 0.25D, 0.0D, 100.0D);
            builder.pop();

            builder.comment("Mail of Pain - Unique Passive: Riposte, Active: Countersiege").push("mail_of_pain");
            for (int i = 0; i < 5; i++) {
                mailRipostePercent[i] = builder.comment("Percent of the damage taken that is reflected as true damage, level " + (i + 1))
                        .defineInRange("ripostePercentLevel" + (i + 1), 5.0D + 0.5D * i, 0.0D, 1000.0D);
            }
            for (int i = 0; i < 5; i++) {
                mailCountersiegePercent[i] = builder.comment("Percent reflected while Countersiege is active, level " + (i + 1))
                        .defineInRange("countersiegePercentLevel" + (i + 1), 25.0D + 2.5D * i, 0.0D, 1000.0D);
            }
            mailCountersiegeSeconds = builder.comment("Seconds Countersiege lasts")
                    .defineInRange("countersiegeSeconds", 6.0D, 0.05D, 600.0D);
            mailCooldownSeconds = builder.comment("Cooldown of Countersiege, in seconds (starts when it is used)")
                    .defineInRange("cooldownSeconds", 75, 0, 100000);
            builder.pop();

            builder.comment("Mantle of Ra - Unique Passive: Burn").push("mantle_of_ra");
            for (int i = 0; i < 5; i++) {
                burnPercent[i] = builder.comment("Magic damage per second, in percent of the max health, level " + (i + 1))
                        .defineInRange("burnPercentLevel" + (i + 1), 2.0D + 0.5D * i, 0.0D, 1000.0D);
            }
            for (int i = 0; i < 5; i++) {
                burnRange[i] = builder.comment("Range of the aura, in blocks, level " + (i + 1))
                        .defineInRange("burnRangeLevel" + (i + 1), DEFAULT_BURN_RANGE[i], 0.1D, 64.0D);
            }
            burnArthropodBonusPercent = builder.comment("Extra damage against arthropods (spiders, silverfish...), in percent")
                    .defineInRange("burnArthropodBonusPercent", 50.0D, 0.0D, 1000.0D);
            soulPrisonReducePercent = builder.comment("Soul Prison: healing reduction of the burnt enemies, in percent")
                    .defineInRange("soulPrisonReducePercent", 35.0D, 0.0D, 100.0D);
            soulPrisonSeconds = builder.comment("Soul Prison: seconds the effect lasts after the last Burn damage")
                    .defineInRange("soulPrisonSeconds", 4.0D, 0.05D, 600.0D);
            builder.pop();

            builder.comment("Odin's Will - Unique Passive: Adrenaline").push("odins_will");
            for (int i = 0; i < 5; i++) {
                adrenalineSpeedPercent[i] = builder.comment("Movement speed per stack, in percent, level " + (i + 1))
                        .defineInRange("speedPercentLevel" + (i + 1), 1.0D + 0.5D * i, 0.0D, 1000.0D);
            }
            for (int i = 0; i < 5; i++) {
                adrenalineDamagePercent[i] = builder.comment("Extra damage dealt per stack, in percent, level " + (i + 1))
                        .defineInRange("damagePercentLevel" + (i + 1), 2.0D + 0.5D * i, 0.0D, 1000.0D);
            }
            adrenalineMaxStacks = builder.comment("Maximum number of stacks")
                    .defineInRange("maxStacks", 5, 1, 100);
            adrenalineSeconds = builder.comment("Seconds until one stack is lost (every hit taken restarts it)")
                    .defineInRange("seconds", 5.0D, 0.05D, 600.0D);
            builder.pop();

            builder.comment("Amulet of Longevity - Unique Passive: Longevity").push("amulet_of_longevity");
            for (int i = 0; i < 5; i++) {
                longevityPercent[i] = builder.comment("Percent of the max health recovered every interval, level " + (i + 1))
                        .defineInRange("percentLevel" + (i + 1), 1.0D + 0.25D * i, 0.0D, 1000.0D);
            }
            for (int i = 0; i < 5; i++) {
                longevityRestPercent[i] = builder.comment("Percent recovered every interval when not attacked, level " + (i + 1))
                        .defineInRange("restPercentLevel" + (i + 1), 4.0D + 0.5D * i, 0.0D, 1000.0D);
            }
            longevityIntervalSeconds = builder.comment("Seconds between two recoveries")
                    .defineInRange("intervalSeconds", 2.0D, 0.05D, 600.0D);
            longevitySafeSeconds = builder.comment("Seconds without being attacked to get the higher recovery")
                    .defineInRange("safeSeconds", 3.0D, 0.0D, 600.0D);
            builder.pop();

            builder.comment("Gaia's Standard - Unique Passives: Restore and Bloodline").push("gaias_standard");
            for (int i = 0; i < 5; i++) {
                gaiaRestorePercent[i] = builder.comment("Percent of the max health restored by Restore, level " + (i + 1))
                        .defineInRange("restorePercentLevel" + (i + 1), 4.0D + i, 0.0D, 1000.0D);
            }
            gaiaRestoreSeconds = builder.comment("Seconds over which Restore heals")
                    .defineInRange("restoreSeconds", 2.0D, 0.05D, 600.0D);
            gaiaCooldownSeconds = builder.comment("Cooldown of Restore, in seconds (starts when it triggers)")
                    .defineInRange("restoreCooldownSeconds", 10.0D, 0.0D, 100000.0D);
            gaiaBloodlineStepPercent = builder.comment("Bloodline: every this percent of the max health lost...")
                    .defineInRange("bloodlineStepPercent", 10.0D, 1.0D, 100.0D);
            gaiaBloodlineBonusPercent = builder.comment("Bloodline: ...the healing effect is increased by this percent")
                    .defineInRange("bloodlineBonusPercent", 5.0D, 0.0D, 1000.0D);
            builder.pop();

            builder.comment("Devil's Handshake - Unique Passive: Flame Wraith").push("devils_handshake");
            for (int i = 0; i < 5; i++) {
                flameAttackSpeed[i] = builder.comment("Attack speed per stack, level " + (i + 1))
                        .defineInRange("attackSpeedLevel" + (i + 1), 0.02D + 0.005D * i, 0.0D, 100.0D);
            }
            for (int i = 0; i < 5; i++) {
                flamePercent[i] = builder.comment("Movement speed and damage dealt per stack, in percent, level " + (i + 1))
                        .defineInRange("percentLevel" + (i + 1), 2.0D + 0.5D * i, 0.0D, 1000.0D);
            }
            flameMaxStacks = builder.comment("Maximum number of stacks")
                    .defineInRange("maxStacks", 5, 1, 100);
            flameSeconds = builder.comment("Seconds until one stack is lost (every hit restarts it)")
                    .defineInRange("seconds", 5.0D, 0.05D, 600.0D);
            builder.pop();

            builder.comment("Fenrir's Fang - Unique Passive: Conquer").push("fenrirs_fang");
            double[] conquerDefaults = {30.0D, 32.0D, 35.0D, 37.0D, 40.0D};
            for (int i = 0; i < 5; i++) {
                conquerPercent[i] = builder.comment("Additional damage against targets below the threshold, in percent, level " + (i + 1))
                        .defineInRange("percentLevel" + (i + 1), conquerDefaults[i], 0.0D, 1000.0D);
            }
            conquerThresholdPercent = builder.comment("The target must have less than this percent of its health")
                    .defineInRange("thresholdPercent", 50.0D, 1.0D, 100.0D);
            builder.pop();

            builder.comment("Claves Sancti - Unique Passives: Gift of the Swift and Infinite").push("claves_sancti");
            double[] swiftDefaults = {5.0D, 5.5D, 6.0D, 6.5D, 7.0D};
            for (int i = 0; i < 5; i++) {
                swiftPercent[i] = builder.comment("Movement speed after a ranged critical hit, in percent, level " + (i + 1))
                        .defineInRange("swiftPercentLevel" + (i + 1), swiftDefaults[i], 0.0D, 1000.0D);
            }
            swiftSeconds = builder.comment("Duration of the movement speed bonus, in seconds")
                    .defineInRange("swiftSeconds", 1.5D, 0.05D, 600.0D);
            infiniteCritRate = builder.comment("Crit rate bonus of Infinite, in percent")
                    .defineInRange("infiniteCritRatePercent", 25.0D, 0.0D, 1000.0D);
            infiniteCritDamage = builder.comment("Crit damage bonus of Infinite, in percent")
                    .defineInRange("infiniteCritDamagePercent", 20.0D, 0.0D, 10000.0D);
            infiniteRateStep = builder.comment("Every this much crit rate (percent) ...")
                    .defineInRange("infiniteRateStepPercent", 2.0D, 0.01D, 1000.0D);
            infiniteDamagePerStep = builder.comment("... gives this much crit damage (percent)")
                    .defineInRange("infiniteDamagePerStepPercent", 1.0D, 0.0D, 10000.0D);
            infiniteMaxBonus = builder.comment("Maximum crit damage (percent) given by the crit rate")
                    .defineInRange("infiniteMaxBonusPercent", 50.0D, 0.0D, 10000.0D);
            builder.pop();

            builder.comment("Stikk's Sting - Unique Passive: Wind's Blessing").push("stikks_sting");
            double[] windAttackDefaults = {10.0D, 11.0D, 12.0D, 13.0D, 14.0D};
            double[] windMoveDefaults = {5.0D, 5.5D, 6.0D, 6.5D, 7.0D};
            for (int i = 0; i < 5; i++) {
                windAttackSpeed[i] = builder.comment("Attack speed bonus (percent of the base) after a hit, level " + (i + 1))
                        .defineInRange("attackSpeedPercentLevel" + (i + 1), windAttackDefaults[i], 0.0D, 1000.0D);
                windMoveSpeed[i] = builder.comment("Movement speed bonus (percent) after a hit, level " + (i + 1))
                        .defineInRange("moveSpeedPercentLevel" + (i + 1), windMoveDefaults[i], 0.0D, 1000.0D);
            }
            windSeconds = builder.comment("Duration of the bonuses, in seconds (they are doubled by a critical hit)")
                    .defineInRange("seconds", 4.0D, 0.05D, 600.0D);
            builder.pop();

            builder.comment("Hercules' Madness - Unique Passive: Berserk, Unique Active: Blood Madness").push("hercules_madness");
            double[] berserkDefaults = {1.5D, 1.65D, 1.75D, 1.85D, 2.0D};
            double[] shieldDefaults = {50.0D, 55.0D, 60.0D, 65.0D, 70.0D};
            for (int i = 0; i < 5; i++) {
                berserkPercent[i] = builder.comment("Extra damage of a normal attack, in percent of the max health, level " + (i + 1))
                        .defineInRange("berserkPercentLevel" + (i + 1), berserkDefaults[i], 0.0D, 1000.0D);
                madnessShieldPercent[i] = builder.comment("Shield of Blood Madness, in percent of the max health, level " + (i + 1))
                        .defineInRange("shieldPercentLevel" + (i + 1), shieldDefaults[i], 0.0D, 10000.0D);
            }
            berserkThreshold = builder.comment("Below this percent of the max health the extra damage grows")
                    .defineInRange("berserkThresholdPercent", 50.0D, 1.0D, 100.0D);
            berserkLowMin = builder.comment("Growth of the extra damage (percent) when the health is just below the threshold")
                    .defineInRange("berserkLowMinPercent", 37.5D, 0.0D, 10000.0D);
            berserkLowMax = builder.comment("Growth of the extra damage (percent) when the health is almost empty")
                    .defineInRange("berserkLowMaxPercent", 75.0D, 0.0D, 10000.0D);
            madnessDrainPercent = builder.comment("Percent of the current health that Blood Madness costs")
                    .defineInRange("drainPercent", 30.0D, 0.0D, 100.0D);
            madnessDrainSeconds = builder.comment("Seconds over which that health is drained (the shield comes after)")
                    .defineInRange("drainSeconds", 1.5D, 0.05D, 60.0D);
            madnessShieldSeconds = builder.comment("Seconds the shield lasts")
                    .defineInRange("shieldSeconds", 6.0D, 0.05D, 600.0D);
            madnessCooldownSeconds = builder.comment("Cooldown of Blood Madness, in seconds (starts when it is used)")
                    .defineInRange("cooldownSeconds", 60, 0, 100000);
            builder.pop();

            builder.comment("Fafnir's Talon - Unique Passive: Dragon's Breath").push("fafnirs_talon");
            double[] dragonDefaults = {7.0D, 7.5D, 8.0D, 8.5D, 9.0D};
            for (int i = 0; i < 5; i++) {
                dragonPercent[i] = builder.comment("Additional physical damage, in percent of the current health of the target, level " + (i + 1))
                        .defineInRange("percentLevel" + (i + 1), dragonDefaults[i], 0.0D, 1000.0D);
            }
            dragonCooldownSeconds = builder.comment("Cooldown of the passive, in seconds")
                    .defineInRange("cooldownSeconds", 1.6D, 0.0D, 600.0D);
            builder.pop();

            builder.comment("Uriel's Brand - Unique Passives: Devil's Bane and Magic Guardian").push("uriels_brand");
            baneConvertPercent = builder.comment("Devil's Bane: percent of the attack damage that becomes magic armor")
                    .defineInRange("baneConvertPercent", 50.0D, 0.0D, 1000.0D);
            baneMaxMagicArmor = builder.comment("Devil's Bane: the most magic armor it gives")
                    .defineInRange("baneMaxMagicArmor", 25.0D, 0.0D, 1000.0D);
            builder.comment("Arctic Orb - Unique Active: Impervious").push("arctic_orb");
            double[] arcticDefaults = {1.5D, 1.75D, 2.0D, 2.5D, 3.0D};
            for (int i = 0; i < 5; i++) {
                arcticFreezeSeconds[i] = builder.comment("Seconds of immunity and immobility, level " + (i + 1)
                                + " (the ice animation of that level has the same length)")
                        .defineInRange("freezeSecondsLevel" + (i + 1), arcticDefaults[i], 0.05D, 60.0D);
            }
            arcticShieldBase = builder.comment("Shield after the immunity: flat points")
                    .defineInRange("shieldBase", 5.0D, 0.0D, 100000.0D);
            arcticShieldMagicPercent = builder.comment("Shield after the immunity: percent of the magic damage added")
                    .defineInRange("shieldMagicPercent", 50.0D, 0.0D, 100000.0D);
            arcticShieldSeconds = builder.comment("Seconds the shield lasts")
                    .defineInRange("shieldSeconds", 2.0D, 0.05D, 600.0D);
            arcticCooldownSeconds = builder.comment("Cooldown in seconds (starts when it is used)")
                    .defineInRange("cooldownSeconds", 75, 0, 100000);
            builder.pop();

            double[] breakDefaults = {40.0D, 50.0D, 60.0D, 70.0D, 80.0D};
            for (int i = 0; i < 5; i++) {
                breakArmorPenetration[i] = builder.comment("Break: armor penetration, level " + (i + 1))
                        .defineInRange("breakArmorPenetrationLevel" + (i + 1), breakDefaults[i], 0.0D, 1000.0D);
            }
            wildwalkSpeedPercent = builder.comment("Wildwalk: movement speed bonus in percent")
                    .defineInRange("wildwalkSpeedPercent", 10.0D, 0.0D, 1000.0D);
            wildwalkSeconds = builder.comment("Wildwalk: seconds without damage from an enemy before the bonus applies")
                    .defineInRange("wildwalkSeconds", 8, 0, 100000);
            guardianThreshold = builder.comment("Magic Guardian: it triggers when the health falls below this percent of the max health")
                    .defineInRange("guardianThresholdPercent", 40.0D, 1.0D, 100.0D);
            double[] guardianShieldDefaults = {25.0D, 26.25D, 27.5D, 28.75D, 30.0D};
            for (int i = 0; i < 5; i++) {
                guardianShieldPercent[i] = builder.comment("Magic Guardian: magic shield, in percent of the max health, level " + (i + 1))
                        .defineInRange("guardianShieldPercentLevel" + (i + 1), guardianShieldDefaults[i], 0.0D, 10000.0D);
            }
            guardianSeconds = builder.comment("Magic Guardian: seconds the magic shield and the movement speed last")
                    .defineInRange("guardianSeconds", 5.0D, 0.05D, 600.0D);
            guardianSpeedPercent = builder.comment("Magic Guardian: movement speed bonus, in percent")
                    .defineInRange("guardianSpeedPercent", 30.0D, 0.0D, 1000.0D);
            guardianCooldownSeconds = builder.comment("Magic Guardian: cooldown in seconds (starts when it triggers)")
                    .defineInRange("guardianCooldownSeconds", 75, 0, 100000);
            builder.pop();
        }
    }

    public static final class Client {
        public final ForgeConfigSpec.BooleanValue showSickleHud;
        public final ForgeConfigSpec.IntValue hudOffsetX;
        public final ForgeConfigSpec.IntValue hudOffsetY;
        public final ForgeConfigSpec.IntValue activeHudOffsetX;
        public final ForgeConfigSpec.IntValue activeHudOffsetY;

        Client(ForgeConfigSpec.Builder builder) {
            builder.comment("Cooldown display of the Death Sickle").push("hud");
            showSickleHud = builder.comment("Show the cooldown of the Death Sickle on the screen")
                    .define("showSickleHud", true);
            hudOffsetX = builder.comment("Extra distance to the right of the hotbar, in pixels")
                    .defineInRange("offsetX", 0, -4000, 4000);
            hudOffsetY = builder.comment("Extra distance upwards from the hotbar, in pixels")
                    .defineInRange("offsetY", 0, -4000, 4000);
            activeHudOffsetX = builder.comment("Active abilities (Mail of Pain): distance from the right edge of the screen")
                    .defineInRange("activeOffsetX", 10, 0, 4000);
            activeHudOffsetY = builder.comment("Active abilities (Mail of Pain): distance from the bottom edge of the screen")
                    .defineInRange("activeOffsetY", 40, 0, 4000);
            builder.pop();
        }
    }

    // Safe getters: the descriptions can be asked for before the config is loaded.

    public static int sickleCooldownSeconds() {
        try { return COMMON.sickleCooldownSeconds.get(); } catch (IllegalStateException e) { return 120; }
    }

    public static double sickleDurationSeconds(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.sickleDurationSeconds[i].get(); } catch (IllegalStateException e) { return Common.DEFAULT_DURATIONS[i]; }
    }

    public static double sickleMovementSpeedPercent() {
        try { return COMMON.sickleMovementSpeedPercent.get(); } catch (IllegalStateException e) { return 25.0D; }
    }

    public static double sickleAttackSpeedBonus() {
        try { return COMMON.sickleAttackSpeedBonus.get(); } catch (IllegalStateException e) { return 0.25D; }
    }

    public static double mailRipostePercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.mailRipostePercent[i].get(); } catch (IllegalStateException e) { return 5.0D + 0.5D * i; }
    }

    public static double mailCountersiegePercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.mailCountersiegePercent[i].get(); } catch (IllegalStateException e) { return 25.0D + 2.5D * i; }
    }

    public static double mailCountersiegeSeconds() {
        try { return COMMON.mailCountersiegeSeconds.get(); } catch (IllegalStateException e) { return 6.0D; }
    }

    public static int mailCooldownSeconds() {
        try { return COMMON.mailCooldownSeconds.get(); } catch (IllegalStateException e) { return 75; }
    }

    public static double burnPercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.burnPercent[i].get(); } catch (IllegalStateException e) { return 2.0D + 0.5D * i; }
    }


    public static double burnRange(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.burnRange[i].get(); } catch (IllegalStateException e) { return DEFAULT_BURN_RANGE[i]; }
    }

    public static double burnArthropodBonusPercent() {
        try { return COMMON.burnArthropodBonusPercent.get(); } catch (IllegalStateException e) { return 50.0D; }
    }

    public static double adrenalineSpeedPercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.adrenalineSpeedPercent[i].get(); } catch (IllegalStateException e) { return 1.0D + 0.5D * i; }
    }

    public static double adrenalineDamagePercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.adrenalineDamagePercent[i].get(); } catch (IllegalStateException e) { return 2.0D + 0.5D * i; }
    }

    public static int adrenalineMaxStacks() {
        try { return COMMON.adrenalineMaxStacks.get(); } catch (IllegalStateException e) { return 5; }
    }

    public static double adrenalineSeconds() {
        try { return COMMON.adrenalineSeconds.get(); } catch (IllegalStateException e) { return 5.0D; }
    }

    public static double longevityPercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.longevityPercent[i].get(); } catch (IllegalStateException e) { return 1.0D + 0.25D * i; }
    }

    public static double longevityRestPercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.longevityRestPercent[i].get(); } catch (IllegalStateException e) { return 4.0D + 0.5D * i; }
    }

    public static double longevityIntervalSeconds() {
        try { return COMMON.longevityIntervalSeconds.get(); } catch (IllegalStateException e) { return 2.0D; }
    }

    public static double longevitySafeSeconds() {
        try { return COMMON.longevitySafeSeconds.get(); } catch (IllegalStateException e) { return 3.0D; }
    }

    public static double gaiaRestorePercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.gaiaRestorePercent[i].get(); } catch (IllegalStateException e) { return 4.0D + i; }
    }

    public static double gaiaRestoreSeconds() {
        try { return COMMON.gaiaRestoreSeconds.get(); } catch (IllegalStateException e) { return 2.0D; }
    }

    public static double gaiaCooldownSeconds() {
        try { return COMMON.gaiaCooldownSeconds.get(); } catch (IllegalStateException e) { return 10.0D; }
    }

    public static double gaiaBloodlineStepPercent() {
        try { return COMMON.gaiaBloodlineStepPercent.get(); } catch (IllegalStateException e) { return 10.0D; }
    }

    public static double gaiaBloodlineBonusPercent() {
        try { return COMMON.gaiaBloodlineBonusPercent.get(); } catch (IllegalStateException e) { return 5.0D; }
    }

    public static double flameAttackSpeed(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.flameAttackSpeed[i].get(); } catch (IllegalStateException e) { return 0.02D + 0.005D * i; }
    }

    public static double flamePercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.flamePercent[i].get(); } catch (IllegalStateException e) { return 2.0D + 0.5D * i; }
    }

    public static int flameMaxStacks() {
        try { return COMMON.flameMaxStacks.get(); } catch (IllegalStateException e) { return 5; }
    }

    public static double flameSeconds() {
        try { return COMMON.flameSeconds.get(); } catch (IllegalStateException e) { return 5.0D; }
    }

    private static final double[] CONQUER_DEFAULTS = {30.0D, 32.0D, 35.0D, 37.0D, 40.0D};

    public static double conquerPercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.conquerPercent[i].get(); } catch (IllegalStateException e) { return CONQUER_DEFAULTS[i]; }
    }

    public static double conquerThresholdPercent() {
        try { return COMMON.conquerThresholdPercent.get(); } catch (IllegalStateException e) { return 50.0D; }
    }

    private static final double[] SWIFT_DEFAULTS = {5.0D, 5.5D, 6.0D, 6.5D, 7.0D};

    public static double swiftPercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.swiftPercent[i].get(); } catch (IllegalStateException e) { return SWIFT_DEFAULTS[i]; }
    }

    public static double swiftSeconds() {
        try { return COMMON.swiftSeconds.get(); } catch (IllegalStateException e) { return 1.5D; }
    }

    public static double infiniteCritRatePercent() {
        try { return COMMON.infiniteCritRate.get(); } catch (IllegalStateException e) { return 25.0D; }
    }

    public static double infiniteCritDamagePercent() {
        try { return COMMON.infiniteCritDamage.get(); } catch (IllegalStateException e) { return 20.0D; }
    }

    public static double infiniteRateStepPercent() {
        try { return COMMON.infiniteRateStep.get(); } catch (IllegalStateException e) { return 2.0D; }
    }

    public static double infiniteDamagePerStepPercent() {
        try { return COMMON.infiniteDamagePerStep.get(); } catch (IllegalStateException e) { return 1.0D; }
    }

    public static double infiniteMaxBonusPercent() {
        try { return COMMON.infiniteMaxBonus.get(); } catch (IllegalStateException e) { return 50.0D; }
    }

    private static final double[] WIND_ATTACK_DEFAULTS = {10.0D, 11.0D, 12.0D, 13.0D, 14.0D};
    private static final double[] WIND_MOVE_DEFAULTS = {5.0D, 5.5D, 6.0D, 6.5D, 7.0D};

    public static double windAttackSpeedPercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.windAttackSpeed[i].get(); } catch (IllegalStateException e) { return WIND_ATTACK_DEFAULTS[i]; }
    }

    public static double windMoveSpeedPercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.windMoveSpeed[i].get(); } catch (IllegalStateException e) { return WIND_MOVE_DEFAULTS[i]; }
    }

    public static double windSeconds() {
        try { return COMMON.windSeconds.get(); } catch (IllegalStateException e) { return 4.0D; }
    }

    private static final double[] BERSERK_DEFAULTS = {1.5D, 1.65D, 1.75D, 1.85D, 2.0D};
    private static final double[] MADNESS_SHIELD_DEFAULTS = {50.0D, 55.0D, 60.0D, 65.0D, 70.0D};

    public static double berserkPercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.berserkPercent[i].get(); } catch (IllegalStateException e) { return BERSERK_DEFAULTS[i]; }
    }

    public static double madnessShieldPercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.madnessShieldPercent[i].get(); } catch (IllegalStateException e) { return MADNESS_SHIELD_DEFAULTS[i]; }
    }

    public static double berserkThresholdPercent() {
        try { return COMMON.berserkThreshold.get(); } catch (IllegalStateException e) { return 50.0D; }
    }

    public static double berserkLowMinPercent() {
        try { return COMMON.berserkLowMin.get(); } catch (IllegalStateException e) { return 37.5D; }
    }

    public static double berserkLowMaxPercent() {
        try { return COMMON.berserkLowMax.get(); } catch (IllegalStateException e) { return 75.0D; }
    }

    public static double madnessDrainPercent() {
        try { return COMMON.madnessDrainPercent.get(); } catch (IllegalStateException e) { return 30.0D; }
    }

    public static double madnessDrainSeconds() {
        try { return COMMON.madnessDrainSeconds.get(); } catch (IllegalStateException e) { return 1.5D; }
    }

    public static double madnessShieldSeconds() {
        try { return COMMON.madnessShieldSeconds.get(); } catch (IllegalStateException e) { return 6.0D; }
    }

    public static int madnessCooldownSeconds() {
        try { return COMMON.madnessCooldownSeconds.get(); } catch (IllegalStateException e) { return 60; }
    }

    private static final double[] DRAGON_DEFAULTS = {7.0D, 7.5D, 8.0D, 8.5D, 9.0D};

    public static double dragonPercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.dragonPercent[i].get(); } catch (IllegalStateException e) { return DRAGON_DEFAULTS[i]; }
    }

    public static double dragonCooldownSeconds() {
        try { return COMMON.dragonCooldownSeconds.get(); } catch (IllegalStateException e) { return 1.6D; }
    }

    public static double baneConvertPercent() {
        try { return COMMON.baneConvertPercent.get(); } catch (IllegalStateException e) { return 50.0D; }
    }

    public static double breakArmorPenetration(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.breakArmorPenetration[i].get(); } catch (IllegalStateException e) { return 40.0D + 10.0D * i; }
    }

    public static double arcticFreezeSeconds(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.arcticFreezeSeconds[i].get(); } catch (IllegalStateException e) { return i < 2 ? 1.5D + 0.25D * i : (i == 2 ? 2.0D : 2.5D + 0.5D * (i - 3)); }
    }

    public static double arcticShieldBase() {
        try { return COMMON.arcticShieldBase.get(); } catch (IllegalStateException e) { return 5.0D; }
    }

    public static double arcticShieldMagicPercent() {
        try { return COMMON.arcticShieldMagicPercent.get(); } catch (IllegalStateException e) { return 50.0D; }
    }

    public static double arcticShieldSeconds() {
        try { return COMMON.arcticShieldSeconds.get(); } catch (IllegalStateException e) { return 2.0D; }
    }

    public static int arcticCooldownSeconds() {
        try { return COMMON.arcticCooldownSeconds.get(); } catch (IllegalStateException e) { return 75; }
    }

    public static double wildwalkSpeedPercent() {
        try { return COMMON.wildwalkSpeedPercent.get(); } catch (IllegalStateException e) { return 10.0D; }
    }

    public static int wildwalkSeconds() {
        try { return COMMON.wildwalkSeconds.get(); } catch (IllegalStateException e) { return 8; }
    }

    public static double baneMaxMagicArmor() {
        try { return COMMON.baneMaxMagicArmor.get(); } catch (IllegalStateException e) { return 25.0D; }
    }

    public static double guardianThresholdPercent() {
        try { return COMMON.guardianThreshold.get(); } catch (IllegalStateException e) { return 40.0D; }
    }

    private static final double[] GUARDIAN_SHIELD_DEFAULTS = {25.0D, 26.25D, 27.5D, 28.75D, 30.0D};

    public static double guardianShieldPercent(int level) {
        int i = Math.max(1, Math.min(5, level)) - 1;
        try { return COMMON.guardianShieldPercent[i].get(); } catch (IllegalStateException e) { return GUARDIAN_SHIELD_DEFAULTS[i]; }
    }

    public static double guardianSeconds() {
        try { return COMMON.guardianSeconds.get(); } catch (IllegalStateException e) { return 5.0D; }
    }

    public static double guardianSpeedPercent() {
        try { return COMMON.guardianSpeedPercent.get(); } catch (IllegalStateException e) { return 30.0D; }
    }

    public static int guardianCooldownSeconds() {
        try { return COMMON.guardianCooldownSeconds.get(); } catch (IllegalStateException e) { return 75; }
    }

    public static double soulPrisonReducePercent() {
        try { return COMMON.soulPrisonReducePercent.get(); } catch (IllegalStateException e) { return 35.0D; }
    }

    public static double soulPrisonSeconds() {
        try { return COMMON.soulPrisonSeconds.get(); } catch (IllegalStateException e) { return 4.0D; }
    }

    public static boolean showSickleHud() {
        try { return CLIENT.showSickleHud.get(); } catch (IllegalStateException e) { return true; }
    }

    public static int hudOffsetX() {
        try { return CLIENT.hudOffsetX.get(); } catch (IllegalStateException e) { return 0; }
    }

    public static int hudOffsetY() {
        try { return CLIENT.hudOffsetY.get(); } catch (IllegalStateException e) { return 0; }
    }

    public static int activeHudOffsetX() {
        try { return CLIENT.activeHudOffsetX.get(); } catch (IllegalStateException e) { return 10; }
    }

    public static int activeHudOffsetY() {
        try { return CLIENT.activeHudOffsetY.get(); } catch (IllegalStateException e) { return 40; }
    }

    private ValorConfig() {}
}
