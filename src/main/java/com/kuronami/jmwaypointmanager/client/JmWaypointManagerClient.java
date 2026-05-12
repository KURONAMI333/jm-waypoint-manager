package com.kuronami.jmwaypointmanager.client;

import com.kuronami.jmwaypointmanager.JmWaypointManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Client setup: keybinding to open the Waypoint Manager screen.
 *
 * <p>Default key: {@code Y} — avoids the usual JourneyMap binds ({@code J}
 * fullscreen, {@code M} minimap, {@code B} create-waypoint) and the vanilla
 * advancement screen ({@code L}). Rebindable via the Controls menu.
 */
@EventBusSubscriber(modid = JmWaypointManager.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class JmWaypointManagerClient {

    public static final KeyMapping OPEN_MANAGER = new KeyMapping(
            "key.jmwaypointmanager.open_manager",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            "key.categories.jmwaypointmanager");

    private JmWaypointManagerClient() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MANAGER);
    }

    /**
     * Tick handler lives on the GAME bus, so it needs its own subscriber class.
     * Consumes pending keypresses and opens the screen once per press.
     */
    @EventBusSubscriber(modid = JmWaypointManager.MODID, value = Dist.CLIENT)
    public static final class TickHandler {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            while (OPEN_MANAGER.consumeClick()) {
                openManager();
            }
        }

        private static void openManager() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            mc.setScreen(new WaypointManagerScreen());
        }
    }
}
