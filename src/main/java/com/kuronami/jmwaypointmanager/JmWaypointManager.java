package com.kuronami.jmwaypointmanager;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * JM Waypoint Manager — a modern UI for JourneyMap's waypoint list.
 *
 * <p>This is a CLIENT-only addon: it reads / writes waypoints through
 * {@code journeymap.api.v2.client.IClientAPI} and presents them in a
 * searchable, filterable, bulk-operable {@link net.minecraft.client.gui.screens.Screen}.
 *
 * <p>The {@link com.kuronami.jmwaypointmanager.compat.jm.JmPlugin}
 * (annotated with {@code @JourneyMapPlugin}) receives the API handle at
 * JM init time and stashes it for the rest of the mod to use.
 */
@Mod(JmWaypointManager.MODID)
public final class JmWaypointManager {

    public static final String MODID = "jmwaypointmanager";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JmWaypointManager(IEventBus modEventBus) {
        LOGGER.info("JM Waypoint Manager loading - waiting for JourneyMap API handshake.");
    }
}
