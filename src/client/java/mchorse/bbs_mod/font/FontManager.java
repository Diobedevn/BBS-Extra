package mchorse.bbs_mod.font;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Font manager singleton that manages custom fonts
 * Loads fonts from bbs/assets/font directory
 */
public class FontManager
{
    private static FontManager instance;

    private Map<String, Font> fontCache = new HashMap<>();
    private List<Link> availableFonts = new ArrayList<>();

    public static FontManager get()
    {
        if (instance == null)
        {
            instance = new FontManager();
        }

        return instance;
    }

    private FontManager()
    {
        this.scanFonts();
    }

    /**
     * Scan for available fonts
     * - Minecraft built-in fonts (default, alt, uniform)
     * - Custom fonts in bbs/assets/font directory
     */
    private void scanFonts()
    {
        // Add Minecraft built-in fonts
        this.availableFonts.add(Link.create("minecraft:default")); // Default font
        this.availableFonts.add(Link.create("minecraft:alt"));
        this.availableFonts.add(Link.create("minecraft:uniform"));

        // Scan custom fonts from bbs/assets/font
        Link fontFolder = Link.assets("font");
        
        try
        {
            for (Link link : BBSMod.getProvider().getLinksFromPath(fontFolder))
            {
                String path = link.path.toLowerCase();
                
                // Support .ttf font files
                if (path.endsWith(".ttf"))
                {
                    this.availableFonts.add(link);
                }
            }
        }
        catch (Exception e)
        {
            // Font folder might not exist, that's okay
        }
    }

    /**
     * Get list of all available fonts
     */
    public List<Link> getAvailableFonts()
    {
        return this.availableFonts;
    }

    /**
     * Get or load a font by its link
     * @param link Font link (null for default font)
     * @return Font instance
     */
    public Font getFont(Link link)
    {
        String key = link == null ? "default" : link.toString();

        if (!this.fontCache.containsKey(key))
        {
            Font font = new Font(link);
            this.fontCache.put(key, font);
        }

        return this.fontCache.get(key);
    }

    /**
     * Get FontRenderer for a given font link
     * @param link Font link (null for default font)
     * @return FontRenderer instance
     */
    public FontRenderer getFontRenderer(Link link)
    {
        Font font = this.getFont(link);
        FontRenderer renderer = new FontRenderer();
        renderer.setRenderer(font.getRenderer());
        renderer.setFont(font);  // Store Font object for TTF support
        return renderer;
    }

    /**
     * Get default font renderer (Minecraft's default)
     */
    public FontRenderer getDefaultFontRenderer()
    {
        FontRenderer renderer = new FontRenderer();
        renderer.setRenderer(MinecraftClient.getInstance().textRenderer);
        return renderer;
    }

    /**
     * Clear font cache (for resource pack reloading)
     */
    public void clearCache()
    {
        this.fontCache.clear();
        this.availableFonts.clear();
        this.scanFonts();
    }

    /**
     * Refresh available fonts list (rescans font folder)
     */
    public void refreshFonts()
    {
        this.availableFonts.clear();
        this.scanFonts();
    }

    /**
     * Get font directory
     */
    public static File getFontFolder()
    {
        return BBSMod.getAssetsPath("font");
    }
}
