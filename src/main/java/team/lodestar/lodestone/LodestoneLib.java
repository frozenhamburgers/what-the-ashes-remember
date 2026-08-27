package team.lodestar.lodestone;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Small shim retained from upstream Lodestone so ported code (which logs via
 * {@code LodestoneLib.LOGGER}) needs no further changes. This is not a separate mod -
 * everything under {@code team.lodestar.lodestone} is vendored directly into the
 * {@code echoesofwar} mod; see {@code net.jelly.echoesofwar.EchoesofWar} for the actual
 * mod entry point and mod id.
 */
public class LodestoneLib {
    public static final Logger LOGGER = LogUtils.getLogger();
}
