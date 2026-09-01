package net.jelly.echoesofwar.entity.nuclear;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.modules.toolkit.worldevent.WorldEventHandler;

// debug harness for the standalone nuclear detonation, so it can be tuned in isolation.
// the command literal is kept as /trinitydetonation for muscle memory; Trinity the boss does NOT
// use this world event - it renders its own meltdowns through its unified chain, reusing the same
// shared volume model in include/nuclear/detonation.glsl
public class NuclearDetonationCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trinitydetonation")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("spawn")
                        .executes(ctx -> spawn(ctx.getSource(), ctx.getSource().getPosition(),
                                NuclearDetonationWorldEvent.DEFAULT_HEIGHT,
                                NuclearDetonationWorldEvent.DEFAULT_LIFETIME_TICKS))
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(ctx -> spawn(ctx.getSource(), Vec3Argument.getVec3(ctx, "pos"),
                                        NuclearDetonationWorldEvent.DEFAULT_HEIGHT,
                                        NuclearDetonationWorldEvent.DEFAULT_LIFETIME_TICKS))
                                .then(Commands.argument("height", FloatArgumentType.floatArg(8f))
                                        .executes(ctx -> spawn(ctx.getSource(), Vec3Argument.getVec3(ctx, "pos"),
                                                FloatArgumentType.getFloat(ctx, "height"),
                                                NuclearDetonationWorldEvent.DEFAULT_LIFETIME_TICKS))
                                        .then(Commands.argument("lifetimeTicks", IntegerArgumentType.integer(20))
                                                .executes(ctx -> spawn(ctx.getSource(), Vec3Argument.getVec3(ctx, "pos"),
                                                        FloatArgumentType.getFloat(ctx, "height"),
                                                        IntegerArgumentType.getInteger(ctx, "lifetimeTicks"))))))));
    }

    private static int spawn(CommandSourceStack source, Vec3 pos, float height, int lifetimeTicks) {
        // the cap keeps its proportion to the column, so height alone retunes the whole silhouette
        float capRadius = height * (NuclearDetonationWorldEvent.DEFAULT_CAP_RADIUS
                / NuclearDetonationWorldEvent.DEFAULT_HEIGHT);
        float seed = (float) ((pos.x * 12.9898 + pos.z * 78.233) % 1000.0);

        NuclearDetonationWorldEvent event = new NuclearDetonationWorldEvent()
                .setup(pos, height, capRadius, seed, lifetimeTicks);
        WorldEventHandler.addWorldEvent(source.getLevel(), event);

        source.sendSuccess(() -> Component.literal("Detonated nuke (height " + height
                + ", " + lifetimeTicks + " ticks) at " + pos), true);
        return 1;
    }
}
