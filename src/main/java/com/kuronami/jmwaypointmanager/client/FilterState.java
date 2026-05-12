package com.kuronami.jmwaypointmanager.client;

import journeymap.api.v2.common.waypoint.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Mutable filter / sort state that the {@link WaypointManagerScreen} drives.
 *
 * <p>Holding this as a separate object keeps the Screen's onUpdate logic
 * tidy — the Screen just calls {@link #apply(List)} after any UI control
 * changes and gets a freshly-filtered list back.
 */
public final class FilterState {

    public enum SortMode {
        NAME_ASC("Name A→Z"),
        NAME_DESC("Name Z→A"),
        DISTANCE("Distance"),
        DIMENSION("Dimension");

        public final String label;
        SortMode(String label) { this.label = label; }
    }

    public enum EnabledFilter {
        ALL("All"),
        ENABLED_ONLY("Enabled"),
        DISABLED_ONLY("Disabled");

        public final String label;
        EnabledFilter(String label) { this.label = label; }
    }

    /** Empty = no filter. Lowercased before matching. */
    public String search = "";
    /** Null = all dims. Otherwise compare against {@code wp.getPrimaryDimension()}. */
    public String dimensionFilter = null;
    public EnabledFilter enabledFilter = EnabledFilter.ALL;
    public SortMode sort = SortMode.DISTANCE;

    /** Cycle through sort modes for the toolbar button. */
    public SortMode nextSort() {
        SortMode[] all = SortMode.values();
        this.sort = all[(this.sort.ordinal() + 1) % all.length];
        return this.sort;
    }

    public EnabledFilter nextEnabledFilter() {
        EnabledFilter[] all = EnabledFilter.values();
        this.enabledFilter = all[(this.enabledFilter.ordinal() + 1) % all.length];
        return this.enabledFilter;
    }

    /** Filter then sort {@code source} according to current state. Non-destructive. */
    public List<Waypoint> apply(List<? extends Waypoint> source) {
        String q = search.toLowerCase(Locale.ROOT).trim();
        List<Waypoint> out = new ArrayList<>(source.size());
        for (Waypoint wp : source) {
            if (!q.isEmpty()) {
                String name = wp.getName() == null ? "" : wp.getName().toLowerCase(Locale.ROOT);
                if (!name.contains(q)) continue;
            }
            if (dimensionFilter != null && !dimensionFilter.equals(wp.getPrimaryDimension())) {
                continue;
            }
            switch (enabledFilter) {
                case ENABLED_ONLY -> { if (!wp.isEnabled()) continue; }
                case DISABLED_ONLY -> { if (wp.isEnabled()) continue; }
                case ALL -> {}
            }
            out.add(wp);
        }
        out.sort(comparator());
        return out;
    }

    private Comparator<Waypoint> comparator() {
        return switch (sort) {
            case NAME_ASC -> Comparator.comparing(this::nameKey);
            case NAME_DESC -> Comparator.comparing(this::nameKey).reversed();
            case DIMENSION -> Comparator
                    .comparing((Waypoint w) -> nullSafe(w.getPrimaryDimension()))
                    .thenComparing(this::nameKey);
            case DISTANCE -> Comparator.comparingDouble(this::distanceFromPlayer);
        };
    }

    private String nameKey(Waypoint w) {
        return nullSafe(w.getName()).toLowerCase(Locale.ROOT);
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }

    /**
     * Squared distance from the player to {@code wp}'s block position in the
     * player's current dimension. Returns Double.MAX_VALUE for waypoints in
     * other dimensions so they sort to the end of a distance-sorted list.
     */
    private double distanceFromPlayer(Waypoint wp) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return Double.MAX_VALUE;
        ResourceKey<Level> playerDim = mc.player.level().dimension();
        String wpDim = wp.getPrimaryDimension();
        if (wpDim == null || !wpDim.equals(playerDim.location().toString())) {
            // Different dimension — sort last but keep a relative order within
            // the "elsewhere" bucket by name.
            return Double.MAX_VALUE - Math.abs(nameKey(wp).hashCode());
        }
        BlockPos pos = wp.getBlockPos();
        if (pos == null) return Double.MAX_VALUE;
        double dx = pos.getX() - mc.player.getX();
        double dy = pos.getY() - mc.player.getY();
        double dz = pos.getZ() - mc.player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
