package com.kuronami.jmwaypointmanager.compat.jm;

import com.kuronami.jmwaypointmanager.JmWaypointManager;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.JourneyMapPlugin;

/**
 * Plugin entry that JourneyMap discovers on startup. The {@link IClientAPI}
 * handle gets stashed in a {@code static volatile} so any class can poke it
 * — the rest of this mod treats {@link #api} as "the JM handle, or null if
 * JM hasn't initialized yet".
 */
@JourneyMapPlugin(apiVersion = IClientAPI.API_VERSION)
public final class JmPlugin implements IClientPlugin {

    public static volatile IClientAPI api;

    @Override
    public void initialize(IClientAPI jmClientApi) {
        api = jmClientApi;
        JmWaypointManager.LOGGER.info("JourneyMap API initialized for JM Waypoint Manager.");
    }

    @Override
    public String getModId() {
        return JmWaypointManager.MODID;
    }
}
