package com.example.bountyhighlighter;

import com.example.bountyhighlighter.mixin.HandledScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client-side only. In the Bounties GUI, middle-click a player head to copy that
 * player's name to your clipboard. Sends nothing to the server.
 */
public class BountyHighlighter implements ClientModInitializer {
    static final Pattern NAME = Pattern.compile("[A-Za-z0-9_.]{3,16}");
    static final Set<String> STOP = Set.of("bounty", "bounties", "player", "click", "page",
            "next", "previous", "head", "the", "reward", "amount", "for", "name", "back");

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof HandledScreen<?> hs) || !isBountyScreen(screen.getTitle())) return;

            // Middle click = copy name. Returning false cancels the vanilla click.
            ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> {
                if (click.button() != 2) return true;
                Slot slot = ((HandledScreenAccessor) hs).bh$getSlotAt(click.x(), click.y());
                if (slot == null) return true;
                String name = nameOf(slot.getStack());
                if (name == null) return true;
                MinecraftClient mc = MinecraftClient.getInstance();
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
