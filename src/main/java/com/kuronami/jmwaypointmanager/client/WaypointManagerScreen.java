package com.kuronami.jmwaypointmanager.client;

import com.kuronami.jmwaypointmanager.compat.jm.WaypointService;
import journeymap.api.v2.common.waypoint.Waypoint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Top-level waypoint manager Screen.
 *
 * <p>Layout (top → bottom):
 * <pre>
 *   [ search box ][ sort: btn ][ filter: btn ][ dim: btn ][ refresh ]
 *   ─────────────────────────────────────────────────────────────
 *   [ ☐ ◆ Waypoint Name ............... (x,y,z) dim · 250m ]
 *   [ ☐ ◆ Another Waypoint ............ (x,y,z) dim · 80m  ]
 *   [ ☐ ◆ ...                                              ]
 *   ─────────────────────────────────────────────────────────────
 *   [ N selected | Delete | Toggle Enabled | Done ]
 * </pre>
 *
 * <p>The list rendering and per-row hit-testing are owned by this Screen
 * directly (rather than extending {@code ObjectSelectionList} or similar)
 * to keep the multi-select interaction simple. Vanilla's list widget
 * mixes selection with click-action which we don't want here — checkboxes
 * for selection are separate from "click name to edit".
 */
public final class WaypointManagerScreen extends Screen {

    private static final int TOP_BAR_H = 26;
    private static final int BOTTOM_BAR_H = 28;
    private static final int ROW_H = 18;
    private static final int CHECKBOX_W = 14;
    private static final int COLOR_DOT_W = 12;

    private static final int INK_PRIMARY = 0xFFE8E8E8;
    private static final int INK_SECONDARY = 0xFF8B8B8B;
    private static final int INK_DIM = 0xFF5C5C5C;
    private static final int BG_ROW_ALT = 0x202E2E2E;
    private static final int BG_ROW_SELECTED = 0x4060A060;
    private static final int BG_PANEL = 0xC0101010;

    private final FilterState filter = new FilterState();
    private final Set<String> selected = new HashSet<>();
    private List<Waypoint> visible = Collections.emptyList();

    private EditBox searchBox;
    private Button sortBtn;
    private Button enabledFilterBtn;
    private Button dimFilterBtn;

    private int scrollOffset = 0;
    private int listTop;
    private int listBottom;

    public WaypointManagerScreen() {
        super(Component.translatable("jmwaypointmanager.screen.title"));
    }

    @Override
    protected void init() {
        int x = 8;
        int y = 6;
        int searchW = Math.min(220, this.width - 16 - 270);

        this.searchBox = new EditBox(this.font, x, y, searchW, 18,
                Component.translatable("jmwaypointmanager.screen.search"));
        this.searchBox.setHint(Component.translatable("jmwaypointmanager.screen.search.hint"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setResponder(s -> {
            filter.search = s;
            refresh();
        });
        this.searchBox.setValue(filter.search);
        addRenderableWidget(this.searchBox);

        int btnX = x + searchW + 6;
        this.sortBtn = Button.builder(
                        Component.literal("Sort: " + filter.sort.label),
                        b -> {
                            filter.nextSort();
                            b.setMessage(Component.literal("Sort: " + filter.sort.label));
                            refresh();
                        })
                .bounds(btnX, y, 90, 18)
                .build();
        addRenderableWidget(this.sortBtn);

        btnX += 92;
        this.enabledFilterBtn = Button.builder(
                        Component.literal("Show: " + filter.enabledFilter.label),
                        b -> {
                            filter.nextEnabledFilter();
                            b.setMessage(Component.literal("Show: " + filter.enabledFilter.label));
                            refresh();
                        })
                .bounds(btnX, y, 86, 18)
                .build();
        addRenderableWidget(this.enabledFilterBtn);

        btnX += 88;
        this.dimFilterBtn = Button.builder(
                        Component.literal(dimFilterLabel()),
                        b -> {
                            cycleDimFilter();
                            b.setMessage(Component.literal(dimFilterLabel()));
                            refresh();
                        })
                .bounds(btnX, y, 80, 18)
                .build();
        addRenderableWidget(this.dimFilterBtn);

        // Bottom bar
        int by = this.height - BOTTOM_BAR_H + 4;
        addRenderableWidget(Button.builder(
                Component.translatable("jmwaypointmanager.screen.delete_selected"),
                b -> deleteSelected())
                .bounds(8, by, 110, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("jmwaypointmanager.screen.toggle_enabled"),
                b -> toggleSelectedEnabled())
                .bounds(122, by, 130, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("jmwaypointmanager.screen.select_all"),
                b -> selectAllVisible())
                .bounds(256, by, 80, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("jmwaypointmanager.screen.clear_selection"),
                b -> selected.clear())
                .bounds(340, by, 80, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                b -> this.onClose())
                .bounds(this.width - 88, by, 80, 20).build());

        this.listTop = TOP_BAR_H + 2;
        this.listBottom = this.height - BOTTOM_BAR_H - 2;

        refresh();
    }

    private String dimFilterLabel() {
        if (filter.dimensionFilter == null) return "Dim: All";
        // Compact: "minecraft:overworld" → "overworld"
        String dim = filter.dimensionFilter;
        int colon = dim.indexOf(':');
        return "Dim: " + (colon >= 0 ? dim.substring(colon + 1) : dim);
    }

    /**
     * Cycle dim filter through {@code null → each unique dim across all
     * loaded waypoints}. Pulling dims from the live list (rather than a
     * static enum) means modded dimensions show up automatically.
     */
    private void cycleDimFilter() {
        List<String> dims = new ArrayList<>();
        dims.add(null);
        Set<String> seen = new HashSet<>();
        for (Waypoint wp : WaypointService.getAll()) {
            String d = wp.getPrimaryDimension();
            if (d != null && seen.add(d)) dims.add(d);
        }
        int idx = dims.indexOf(filter.dimensionFilter);
        filter.dimensionFilter = dims.get((idx + 1) % dims.size());
    }

    private void refresh() {
        this.visible = filter.apply(WaypointService.getAll());
        clampScroll();
    }

    private void clampScroll() {
        int maxScroll = Math.max(0, visible.size() * ROW_H - (listBottom - listTop));
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackgroundDim(g);

        // Top bar background
        g.fill(0, 0, this.width, TOP_BAR_H, BG_PANEL);
        g.fill(0, TOP_BAR_H, this.width, TOP_BAR_H + 1, INK_DIM);

        // List area
        int y = listTop - scrollOffset;
        int rowIndex = 0;
        int rowW = this.width - 12;
        for (Waypoint wp : visible) {
            int rowTop = y + rowIndex * ROW_H;
            int rowBot = rowTop + ROW_H;
            if (rowBot >= listTop && rowTop <= listBottom) {
                renderRow(g, wp, 6, rowTop, rowW, rowIndex);
            }
            rowIndex++;
        }

        // Bottom bar background
        int bbY = this.height - BOTTOM_BAR_H;
        g.fill(0, bbY, this.width, this.height, BG_PANEL);
        g.fill(0, bbY, this.width, bbY + 1, INK_DIM);

        // Status: "N selected of M shown / K total"
        String status = visible.size() + " shown · " + WaypointService.getAll().size() + " total";
        if (!selected.isEmpty()) {
            status = selected.size() + " selected · " + status;
        }
        g.drawString(this.font, status, 8, bbY - 12, INK_SECONDARY, false);

        if (!WaypointService.available()) {
            String msg = "JourneyMap not detected — waiting for plugin handshake…";
            int w = this.font.width(msg);
            g.drawString(this.font, msg,
                    (this.width - w) / 2, (listTop + listBottom) / 2 - 4,
                    0xFFEE6666, false);
        } else if (visible.isEmpty()) {
            String msg = WaypointService.getAll().isEmpty()
                    ? "No waypoints yet — go explore!"
                    : "No matches.";
            int w = this.font.width(msg);
            g.drawString(this.font, msg,
                    (this.width - w) / 2, (listTop + listBottom) / 2 - 4,
                    INK_SECONDARY, false);
        }

        super.render(g, mouseX, mouseY, partial);
    }

    private void renderBackgroundDim(GuiGraphics g) {
        // 80%-opaque black over the game so the panel reads as a real overlay.
        g.fill(0, 0, this.width, this.height, 0xC0000000);
    }

    private void renderRow(GuiGraphics g, Waypoint wp, int x, int y, int w, int rowIndex) {
        boolean isSelected = selected.contains(wp.getGuid());
        // Alternating row tint + selected overlay
        if (rowIndex % 2 == 1) {
            g.fill(x, y, x + w, y + ROW_H, BG_ROW_ALT);
        }
        if (isSelected) {
            g.fill(x, y, x + w, y + ROW_H, BG_ROW_SELECTED);
        }

        // Checkbox
        int cbX = x + 2;
        int cbY = y + 3;
        g.fill(cbX, cbY, cbX + CHECKBOX_W, cbY + CHECKBOX_W, 0xFF202020);
        g.renderOutline(cbX, cbY, CHECKBOX_W, CHECKBOX_W, 0xFF808080);
        if (isSelected) {
            // Inner fill = check
            g.fill(cbX + 3, cbY + 3, cbX + CHECKBOX_W - 3, cbY + CHECKBOX_W - 3, 0xFF80E080);
        }

        // Color dot
        int dotX = cbX + CHECKBOX_W + 4;
        int dotY = y + 4;
        int color = 0xFF000000 | (wp.getColor() & 0xFFFFFF);
        g.fill(dotX, dotY, dotX + COLOR_DOT_W, dotY + COLOR_DOT_W, color);
        g.renderOutline(dotX, dotY, COLOR_DOT_W, COLOR_DOT_W, 0xFF202020);

        // Name
        String name = wp.getName() == null ? "(unnamed)" : wp.getName();
        if (!wp.isEnabled()) {
            name = "§7" + name; // grey if disabled
        }
        int textX = dotX + COLOR_DOT_W + 6;
        g.drawString(this.font, name, textX, y + 4, INK_PRIMARY, false);

        // Right-side metadata: coords + dim + distance
        String meta = formatMeta(wp);
        int metaW = this.font.width(meta);
        g.drawString(this.font, meta,
                x + w - metaW - 6, y + 5, INK_SECONDARY, false);
    }

    private String formatMeta(Waypoint wp) {
        String dim = wp.getPrimaryDimension();
        String dimShort = dim == null ? "?" : dim;
        int colon = dimShort.indexOf(':');
        if (colon >= 0) dimShort = dimShort.substring(colon + 1);

        var pos = wp.getBlockPos();
        String coords = pos == null
                ? "(?, ?, ?)"
                : "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
        return coords + " · " + dimShort;
    }

    // ── Mouse interaction ───────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && my >= listTop && my <= listBottom) {
            int rowIdx = (int) ((my - listTop + scrollOffset) / ROW_H);
            if (rowIdx >= 0 && rowIdx < visible.size()) {
                Waypoint wp = visible.get(rowIdx);
                // Checkbox region click vs row click
                if (mx >= 6 && mx <= 6 + CHECKBOX_W + 4) {
                    toggleSelection(wp);
                    return true;
                }
                // Click on color dot → cycle color (preset palette)
                int colorDotStart = 6 + 2 + CHECKBOX_W + 4;
                if (mx >= colorDotStart && mx < colorDotStart + COLOR_DOT_W) {
                    cycleColor(wp);
                    return true;
                }
                // Row click (not on widgets) → toggle enabled
                if (mx > colorDotStart + COLOR_DOT_W + 6) {
                    wp.setEnabled(!wp.isEnabled());
                    WaypointService.save(wp);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (my >= listTop && my <= listBottom) {
            scrollOffset -= (int) (sy * ROW_H * 2);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            return super.keyPressed(key, scan, mods);
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    // ── Actions ─────────────────────────────────────────────────

    private void toggleSelection(Waypoint wp) {
        String guid = wp.getGuid();
        if (!selected.remove(guid)) {
            selected.add(guid);
        }
    }

    private void selectAllVisible() {
        for (Waypoint wp : visible) selected.add(wp.getGuid());
    }

    private void deleteSelected() {
        if (selected.isEmpty()) return;
        List<Waypoint> all = new ArrayList<>(WaypointService.getAll());
        int deleted = 0;
        for (Waypoint wp : all) {
            if (selected.contains(wp.getGuid())) {
                WaypointService.remove(wp);
                deleted++;
            }
        }
        selected.clear();
        refresh();
    }

    private void toggleSelectedEnabled() {
        if (selected.isEmpty()) return;
        List<Waypoint> all = new ArrayList<>(WaypointService.getAll());
        for (Waypoint wp : all) {
            if (selected.contains(wp.getGuid())) {
                wp.setEnabled(!wp.isEnabled());
                WaypointService.save(wp);
            }
        }
        refresh();
    }

    /**
     * Cycle through a fixed 8-color palette. Click the color dot to advance.
     * Saves on each step so changes persist immediately.
     */
    private static final int[] COLOR_PALETTE = {
            0xFFFFFF, 0xFF5555, 0xFFAA00, 0xFFFF55,
            0x55FF55, 0x55FFFF, 0x5555FF, 0xFF55FF,
    };
    private void cycleColor(Waypoint wp) {
        int current = wp.getColor() & 0xFFFFFF;
        int idx = 0;
        for (int i = 0; i < COLOR_PALETTE.length; i++) {
            if (COLOR_PALETTE[i] == current) { idx = i + 1; break; }
        }
        wp.setColor(COLOR_PALETTE[idx % COLOR_PALETTE.length]);
        WaypointService.save(wp);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Empty - we paint our own panel in render() so no vanilla dim.
    }
}
