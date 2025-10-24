package mchorse.bbs_mod.ui.framework.elements.overlay;

import mchorse.bbs_mod.font.FontManager;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Font picker overlay panel
 * Allows users to select fonts from Minecraft built-in fonts and custom TTF fonts
 */
public class UIFontOverlayPanel extends UIStringOverlayPanel
{
    private static Set<String> getAvailableFonts()
    {
        // Refresh fonts to detect newly added TTF files
        FontManager.get().refreshFonts();
        
        Set<String> fonts = new HashSet<>();
        List<Link> fontLinks = FontManager.get().getAvailableFonts();

        for (Link link : fontLinks)
        {
            fonts.add(link.toString());
        }

        return fonts;
    }

    public UIFontOverlayPanel(Consumer<Link> callback)
    {
        super(UIKeys.OVERLAYS_FONTS_MAIN, getAvailableFonts(), null);

        this.callback((str) ->
        {
            if (callback != null)
            {
                // Handle empty string or null as null (no custom font)
                Link link = (str == null || str.isEmpty()) ? null : Link.create(str);
                System.out.println("[Font Picker] Selected font string: '" + str + "' -> Link: " + link);
                callback.accept(link);
            }
        });
    }

    /**
     * Set initial selected font
     */
    public UIFontOverlayPanel setFont(Link font)
    {
        // If font is null or minecraft:default, select minecraft:default
        String fontStr = (font == null) ? "minecraft:default" : font.toString();
        this.set(fontStr);

        return this;
    }
}
