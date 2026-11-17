package com.example.bountyhighlighter;

import com.example.bountyhighlighter.mixin.HandledScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client-side only. Reads the Bounties GUI you already have open, remembers when
 * YOUR client first saw each player, and tints that slot by age.
 * Middle-click a head to copy the name. Right-click prints debug info to chat.
 */
public class BountyHighlighter implements ClientModInitializer {
    static final long GREEN_UNTIL_MIN  = 5;   // < 5 min   -> green
    static final long YELLOW_UNTIL_MIN = 20;  // 5-20 min  -> yellow
                                              // >= 20 min -> red
    static final int GREEN  = 0x8855FF55;
    static final int YELLOW = 0x88FFFF55;
    static final int RED    = 0x88FF5555;

    static final Pattern NAME = Pattern.compile("[A-Za-z0-9_.]{3,16}");
    static final Set<String> STOP = Set.of("bounty", "bounties", "player", "click", "page",
            "next", "previous", "head", "the", "reward", "amount", "for", "name", "back");
    static final Map<String, Long> FIRST_SEEN = new HashMap<>();
    static final Map<String, Long> LAST_SEEN = new HashMap<>();
    static final long FORGET_AFTER_MS = 45L * 60_000L;

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof HandledScreen<?> hs) || !isBountyScreen(screen.getTitle())) return;

            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
                HandledScreenAccessor acc = (HandledScreenAccessor) hs;
                MinecraftClient mc = MinecraftClient.getInstance();
                long now = System.currentTimeMillis();
                int containerSlots = hs.getScreenHandler().slots.size() - 36;
                for (int i = 0; i < containerSlots; i++) {
                    Slot slot = hs.getScreenHandler().slots.get(i);
                    String name = nameOf(slot.getStack());
                    if (name == null) continue;
                    FIRST_SEEN.putIfAbsent(name, now);
                    LAST_SEEN.put(name, now);
                    long ageMin = (now - FIRST_SEEN.get(name)) / 60_000L;
                    int color = ageMin < GREEN_UNTIL_MIN ? GREEN : ageMin < YELLOW_UNTIL_MIN ? YELLOW : RED;
                    int x = acc.bh$getX() + slot.x, y = acc.bh$getY() + slot.y;
                    ctx.fill(x, y, x + 16, y + 16, color);
                    ctx.drawText(mc.textRenderer, ageMin + "m", x + 1, y + 8, 0xFFFFFFFF, true);
                }
                LAST_SEEN.entrySet().removeIf(e -> {
                    boolean stale = now - e.getValue() > FORGET_AFTER_MS;
                    if (stale) FIRST_SEEN.remove(e.getKey());
                    return stale;
                });
            });

            // Middle click = copy name. Right click = print debug info. Cancels the vanilla click.
            ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> {
                int btn = click.button();
                if (btn != 2 && btn != 1) return true;
                Slot slot = ((HandledScreenAccessor) hs).bh$getSlotAt(click.x(), click.y());
                if (slot == null || slot.getStack().isEmpty()) return true;
                MinecraftClient mc = MinecraftClient.getInstance();
                ItemStack stack = slot.getStack();
                String name = nameOf(stack);

                if (btn == 1) {
                    if (mc.player == null) return false;
                    mc.player.sendMessage(Text.literal("[BH] item name: " + stack.getName().getString()
                            + " | detected: " + name), false);
                    List<Text> tip = stack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC);
                    for (Text t : tip) mc.player.sendMessage(Text.literal("[BH] " + t.getString()), false);
                    return false;
                }
                if (name == null) return true;
                mc.keyboard.setClipboard(name);
                if (mc.player != null) mc.player.sendMessage(Text.literal("Copied " + name), true);
                return false;
            });
        });
    }

    static boolean isBountyScreen(Text title) {
        return title.getString().toLowerCase().contains("bount");
    }

    /** Pulls a username from a player-head item's display name. */
    static String nameOf(ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(Items.PLAYER_HEAD)) return null;
        String plain = Formatting.strip(stack.getName().getString());
        if (plain == null) return null;
        Matcher m = NAME.matcher(plain);
        while (m.find()) {
            String token = m.group();
            if (!STOP.contains(token.toLowerCase())) return token;
        }
        return null;
    }
}
