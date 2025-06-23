package dev.hetils.worldmole;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.forge.ForgeAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Worldmole.MODID)
public class Worldmole
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "worldmole";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public Worldmole(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        MinecraftForge.EVENT_BUS.addListener(this::onCommandRegister);
    }

    public static Vec3 ctrl = null;

    public static WorldEdit we = null;

//    public static final RenderType GREEN_WIRE = RenderType.create("green_wire",
//            DefaultVertexFormat.POSITION_COLOR,
//            VertexFormat.Mode.LINES,
//            256, false, true,
//            RenderType.CompositeState.builder()
//                    .setShaderState(new RenderStateShard.ShaderStateShard())
////                    .setLineState(new RenderStateShard.LineStateShard())
//                    .setLayeringState(new RenderStateShard.LayeringStateShard("green_wire", () -> {}, () -> {}))
//                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("green_wire", () -> {}, () -> {}))
//                    .createCompositeState(true)
//    );

    public static int mole(CommandContext<CommandSourceStack> ctx, double radius, double precision) {
        LocalSession ls;
        Player p;

        double dx, dy, dz, bx, by, bz;

        try {

            ServerPlayer sp = ctx.getSource().getPlayer();
            if (sp == null)
                throw new NullPointerException("ServerPlayer is null");
            p = ForgeAdapter.adaptPlayer(sp);
            SessionManager sm = we.getSessionManager();
            ls = sm.get(p);

            Vector3 v1 = ls.getRegionSelector(p.getWorld()).getPrimaryPosition().toVector3();

            bx = v1.getX();
            by = v1.getY();
            bz = v1.getZ();

            Region r = ls.getSelection(p.getWorld());

            BlockVector3 min = r.getMinimumPoint();

            dx = ((int) v1.getX()) - min.getX() == 0 ? r.getWidth() : -r.getWidth();
            dy = ((int) v1.getY()) - min.getY() == 0 ? r.getHeight() : -r.getHeight();
            dz = ((int) v1.getZ()) - min.getZ() == 0 ? r.getLength() : -r.getLength();

        } catch (NullPointerException np) {
            throw new RuntimeException(np);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Make a line selection first").withStyle(ChatFormatting.RED));
            return 1;
        }

        ServerLevel level = ctx.getSource().getLevel();

        double cx, cy, cz;

        if (ctrl == null) {
            cx = dx/2;
            cy = dy/2;
            cz = dz/2;
        } else {
            cx = ctrl.x - bx;
            cy = ctrl.y - by;
            cz = ctrl.z - bz;
        }

        double lx = 0, ly = 0, lz = 0;

        try (EditSession es = ls.createEditSession(p)) {
            int count = 0;
            double dist = precision, step = precision/10d;

            for (double t = precision; t < 1.0; t += step) {
                double txt = 2 * (1d - t) * t,
                        x = txt * cx + (t * t) * dx,
                        y = txt * cy + (t * t) * dy,
                        z = txt * cz + (t * t) * dz;

                lx -= x;
                ly -= y;
                lz -= z;

                if ((dist += Math.sqrt(lx*lx + ly*ly + lz*lz)) >= precision) {
                    for (double j = -radius; j <= radius; j++)
                        for (double k = -radius; k <= radius; k++)
                            for (double l = -radius; l <= radius; l++)
                                if (Math.sqrt(j * j + k * k + l * l) < radius) {
                                    BlockPos pos = new BlockPos((int) (bx + x + j), (int) (by + y + k), (int) (bz + z + l));
                                    BlockState current = level.getBlockState(pos);
                                    if (!current.isAir()) {
                                        es.setBlock(BlockVector3.at(pos.getX(), pos.getY(), pos.getZ()), BlockTypes.AIR.getDefaultState());
                                        count++;
                                    }
                                }
                    dist = 0d;
                }

                lx = x;
                ly = y;
                lz = z;
            }

            ls.remember(es);
            final int fcount = count;
            ctx.getSource().sendSuccess(() -> Component.literal("Operation completed (" + fcount + " blocks affected)").withStyle(ChatFormatting.LIGHT_PURPLE), false);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 1;
    }

    @Contract("_ -> new")
    public static @NotNull Vec3 Bp2V3(@NotNull BlockPos b) { return new Vec3(b.getX(), b.getY(), b.getZ()); }

    private void onCommandRegister(@NotNull RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("/mole")
                .requires(cs -> cs.hasPermission(2)) // 2 = OP level
                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1))
                .executes(ctx ->
                        mole(ctx, DoubleArgumentType.getDouble(ctx, "radius"), .1)
                )
                .then(Commands.argument("precision", DoubleArgumentType.doubleArg(0.0001))
                .executes(ctx ->
                    mole(ctx, DoubleArgumentType.getDouble(ctx, "radius"), DoubleArgumentType.getDouble(ctx, "precision"))
                )))
        );

        dispatcher.register(Commands.literal("/ctrl")
                .requires(cs -> cs.hasPermission(2))
                .executes(ctx -> {
                    ctrl = ctx.getSource().getPosition();
                    ctrl = new Vec3((int) ctrl.x,(int) ctrl.y, (int) ctrl.z);
                    ctx.getSource().sendSuccess(() -> Component.literal("Control point set to " + ctrl.toString()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
                    return 1;
                })
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(ctx -> {
                    ctrl = Bp2V3(BlockPosArgument.getBlockPos(ctx, "pos"));
                    ctx.getSource().sendSuccess(() -> Component.literal("Control point set to " + ctrl.toString()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
                    return 1;
                }))
                .then(Commands.argument("unset", StringArgumentType.string())
                .executes(ctx -> {
                    ctrl = null;
                    ctx.getSource().sendSuccess(() -> Component.literal("Unset control point").withStyle(ChatFormatting.LIGHT_PURPLE), false);
                    return 1;
                })));
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        we = WorldEdit.getInstance();
        if (we == null)
            throw new RuntimeException("World edit is not loaded or present");
        // Some common setup code
        LOGGER.info("WorldMole loaded");
    }
}
