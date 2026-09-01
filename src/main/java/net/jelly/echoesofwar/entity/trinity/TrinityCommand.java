package net.jelly.echoesofwar.entity.trinity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventHandler;


public class TrinityCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trinity")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("spawn")
                        .executes(ctx -> spawn(ctx.getSource(), ctx.getSource().getPosition()))
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(ctx -> spawn(ctx.getSource(), Vec3Argument.getVec3(ctx, "pos")))))
                .then(Commands.literal("criticality")
                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0f, 100f))
                                .executes(ctx -> criticality(ctx.getSource(),
                                        FloatArgumentType.getFloat(ctx, "amount")))))
                .then(Commands.literal("status")
                        .executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("remove")
                        .executes(ctx -> remove(ctx.getSource()))));
    }

    private static int spawn(CommandSourceStack source, Vec3 pos) {
        if (find(source.getLevel()) != null) {
            source.sendFailure(Component.literal("Trinity is already active in this dimension."));
            return 0;
        }
        // Same offset the Crucible uses, so what this spawns is what the fight spawns.
        Vec3 centre = pos.add(0.0, TrinityTuning.SPAWN_HEIGHT, 0.0);
        float seed = (float) ((pos.x * 12.9898 + pos.z * 78.233) % 1000.0);
        WorldEventHandler.addWorldEvent(source.getLevel(),
                new TrinityWorldEvent().setup(centre, seed));
        source.sendSuccess(() -> Component.literal("Trinity manifesting at " + centre), true);
        return 1;
    }

    private static int criticality(CommandSourceStack source, float amount) {
        TrinityWorldEvent trinity = find(source.getLevel());
        if (trinity == null) {
            source.sendFailure(Component.literal("No active Trinity."));
            return 0;
        }
        trinity.addCriticality(amount);
        source.sendSuccess(() -> Component.literal("Criticality now "
                + String.format("%.1f", trinity.criticality())), true);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        TrinityWorldEvent trinity = find(source.getLevel());
        if (trinity == null) {
            source.sendFailure(Component.literal("No active Trinity."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(String.format(
                "phase=%s criticality=%.1f degree=%d difficulty=%.2f",
                trinity.phase(), trinity.criticality(), trinity.criticalityDegree(),
                trinity.difficulty())), false);
        return 1;
    }

    private static int remove(CommandSourceStack source) {
        TrinityWorldEvent trinity = find(source.getLevel());
        if (trinity == null) {
            source.sendFailure(Component.literal("No active Trinity."));
            return 0;
        }
        trinity.forceRetire();
        source.sendSuccess(() -> Component.literal("Trinity removed."), true);
        return 1;
    }

    private static @Nullable TrinityWorldEvent find(Level level) {
        return TrinityWorldEvent.find(level);
    }
}
