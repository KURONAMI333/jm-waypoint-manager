package com.kuronami.jmwaypointmanager.compat.jm;

import com.kuronami.jmwaypointmanager.JmWaypointManager;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.common.waypoint.Waypoint;

import java.util.Collections;
import java.util.List;

/**
 * Thin wrapper around {@link IClientAPI} for waypoint reads / mutations.
 *
 * <p>Why this exists instead of inlining the API calls: the {@code IClientAPI}
 * reference is nullable (JM may not be loaded, or might initialize after us),
 * and every call site doing its own null check gets repetitive. This class
 * centralizes that — return empty list / no-op if JM isn't there.
 *
 * <p>Mutations go through {@code addWaypoint(modId, wp)} to "save" changes
 * (JM's API treats {@code add} as upsert for the same waypoint guid).
 */
public final class WaypointService {

    private WaypointService() {}

    /** True if JourneyMap has handed us the API. False until {@link JmPlugin#initialize} runs. */
    public static boolean available() {
        return JmPlugin.api != null;
    }

    /** All waypoints across all dimensions, or empty list if JM isn't available. */
    public static List<? extends Waypoint> getAll() {
        IClientAPI api = JmPlugin.api;
        if (api == null) return Collections.emptyList();
        try {
            return api.getAllWaypoints();
        } catch (Exception e) {
            JmWaypointManager.LOGGER.warn("getAllWaypoints failed: {}", e.toString());
            return Collections.emptyList();
        }
    }

    /**
     * Persist a mutation to a waypoint. JourneyMap's API treats {@code addWaypoint}
     * as upsert — same guid replaces the existing record. Use this after calling
     * any {@code wp.setX(...)} mutator.
     *
     * <p>The {@code modId} we pass is the waypoint's <em>owning</em> mod id, not
     * our own — i.e., for a waypoint that originally came from {@code compass-to-map},
     * we want JM to remember it as a C2M waypoint, not a Manager waypoint.
     */
    public static void save(Waypoint wp) {
        IClientAPI api = JmPlugin.api;
        if (api == null || wp == null) return;
        try {
            api.addWaypoint(wp.getModId(), wp);
        } catch (Exception e) {
            JmWaypointManager.LOGGER.warn("addWaypoint (save) failed for {}: {}",
                    wp.getName(), e.toString());
        }
    }

    /** Delete a waypoint from JM's store. No-op if JM isn't loaded. */
    public static void remove(Waypoint wp) {
        IClientAPI api = JmPlugin.api;
        if (api == null || wp == null) return;
        try {
            api.removeWaypoint(wp.getModId(), wp);
        } catch (Exception e) {
            JmWaypointManager.LOGGER.warn("removeWaypoint failed for {}: {}",
                    wp.getName(), e.toString());
        }
    }
}
