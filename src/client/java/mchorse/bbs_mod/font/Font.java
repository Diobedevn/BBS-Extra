package mchorse.bbs_mod.font;

import mchorse.bbs_mod.resources.Link;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.Identifier;

/**
 * Font wrapper that can represent either:
 * 1. Minecraft built-in fonts (minecraft:default, minecraft:alt, minecraft:uniform)
 * 2. Custom TTF fonts loaded from bbs/assets/font directory
 */
public class Font
{
    private Link link;
    private TextRenderer renderer;
    private TTFRenderer ttfRenderer;  // For custom TTF fonts
    private TTFTextRenderer ttfTextRenderer;  // Wrapper that uses TTFRenderer
    private String displayName;
    private boolean isMinecraftFont;
    private boolean isTTFFont;  // True if using custom TTF
    private Identifier minecraftFontId;

    /**
     * Create a Font from a Link
     * @param link The font link (e.g., "minecraft:default" or "bbs:assets/font/MyFont.ttf")
     */
    public Font(Link link)
    {
        this.link = link;
        this.isMinecraftFont = this.checkIfMinecraftFont();
        this.loadFont();
        this.generateDisplayName();
    }

    /**
     * Check if this font is a Minecraft built-in font
     */
    private boolean checkIfMinecraftFont()
    {
        if (this.link == null)
        {
            return true; // null = default font
        }

        String path = this.link.path;
        
        // Check for minecraft: scheme fonts
        if (this.link.source.equals("minecraft"))
        {
            return true;
        }

        // Check for common Minecraft font identifiers
        return path.equals("default") || path.equals("alt") || path.equals("uniform");
    }

    /**
     * Load the font renderer
     */
    private void loadFont()
    {
        MinecraftClient client = MinecraftClient.getInstance();

        System.out.println("[Font.loadFont()] Loading font: " + this.link + " (isMinecraft=" + this.isMinecraftFont + ")");

        if (this.link == null)
        {
            // Null link = use default font
            this.renderer = client.textRenderer;
            System.out.println("[Font.loadFont()] -> Using default font (null link)");
            return;
        }

        if (this.isMinecraftFont)
        {
            // Load Minecraft built-in font
            String fontName = this.link.path;
            
            // Handle minecraft:font_name format
            if (this.link.source.equals("minecraft"))
            {
                fontName = this.link.path;
            }

            System.out.println("[Font.loadFont()] Minecraft font name: " + fontName);

            // Map font names to Minecraft identifiers and get the actual font
            if (fontName.equals("default") || fontName.isEmpty())
            {
                this.minecraftFontId = new Identifier("default");
                this.renderer = client.textRenderer;
                System.out.println("[Font.loadFont()] -> Using default font");
            }
            else if (fontName.equals("alt"))
            {
                this.minecraftFontId = new Identifier("alt");
                // Access the alt font from Minecraft's font manager using reflection
                try
                {
                    // Get the fontManager from MinecraftClient
                    java.lang.reflect.Field fontManagerField = MinecraftClient.class.getDeclaredField("fontManager");
                    fontManagerField.setAccessible(true);
                    net.minecraft.client.font.FontManager fontManager = 
                        (net.minecraft.client.font.FontManager) fontManagerField.get(client);
                    
                    // Get the fontStorages map from FontManager
                    java.lang.reflect.Field fontStoragesField = fontManager.getClass().getDeclaredField("fontStorages");
                    fontStoragesField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    java.util.Map<Identifier, net.minecraft.client.font.FontStorage> fontStorages = 
                        (java.util.Map<Identifier, net.minecraft.client.font.FontStorage>) fontStoragesField.get(fontManager);
                    
                    // Get the alt font storage
                    net.minecraft.client.font.FontStorage fontStorage = fontStorages.get(this.minecraftFontId);
                    if (fontStorage != null)
                    {
                        this.renderer = new net.minecraft.client.font.TextRenderer(
                            (id) -> fontStorage,
                            false
                        );
                        System.out.println("[Font.loadFont()] -> Successfully loaded ALT font using reflection!");
                    }
                    else
                    {
                        this.renderer = client.textRenderer;
                        System.out.println("[Font.loadFont()] -> Alt font storage not found, using default");
                    }
                }
                catch (Exception e)
                {
                    this.renderer = client.textRenderer; // Fall back to default
                    System.out.println("[Font.loadFont()] -> Alt font failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            else if (fontName.equals("uniform"))
            {
                this.minecraftFontId = new Identifier("uniform");
                // Access the uniform font from Minecraft's font manager using reflection
                try
                {
                    // Get the fontManager from MinecraftClient
                    java.lang.reflect.Field fontManagerField = MinecraftClient.class.getDeclaredField("fontManager");
                    fontManagerField.setAccessible(true);
                    net.minecraft.client.font.FontManager fontManager = 
                        (net.minecraft.client.font.FontManager) fontManagerField.get(client);
                    
                    // Get the fontStorages map from FontManager
                    java.lang.reflect.Field fontStoragesField = fontManager.getClass().getDeclaredField("fontStorages");
                    fontStoragesField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    java.util.Map<Identifier, net.minecraft.client.font.FontStorage> fontStorages = 
                        (java.util.Map<Identifier, net.minecraft.client.font.FontStorage>) fontStoragesField.get(fontManager);
                    
                    // Get the uniform font storage
                    net.minecraft.client.font.FontStorage fontStorage = fontStorages.get(this.minecraftFontId);
                    if (fontStorage != null)
                    {
                        this.renderer = new net.minecraft.client.font.TextRenderer(
                            (id) -> fontStorage,
                            false
                        );
                        System.out.println("[Font.loadFont()] -> Successfully loaded UNIFORM font using reflection!");
                    }
                    else
                    {
                        this.renderer = client.textRenderer;
                        System.out.println("[Font.loadFont()] -> Uniform font storage not found, using default");
                    }
                }
                catch (Exception e)
                {
                    this.renderer = client.textRenderer; // Fall back to default
                    System.out.println("[Font.loadFont()] -> Uniform font failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            else
            {
                // Unknown Minecraft font, use default
                this.renderer = client.textRenderer;
                System.out.println("[Font.loadFont()] -> Unknown minecraft font, using default");
            }
        }
        else
        {
            // Custom TTF font - load using STBTruetype directly
            try
            {
                // Resolve file path relative to assets/font
                java.io.File fontFolder = FontManager.getFontFolder();
                String path = this.link.path;
                // If path starts with "font/" remove that prefix
                if (path.startsWith("font/"))
                {
                    path = path.substring("font/".length());
                }

                java.io.File fontFile = new java.io.File(fontFolder, path);
                System.out.println("[Font.loadFont()] Attempting to load TTF file: " + fontFile.getAbsolutePath());

                if (fontFile.exists())
                {
                    // Use our custom TTFRenderer with STBTruetype
                    System.out.println("[Font.loadFont()] -> Loading TTF using STBTruetype...");
                    
                    // Create TTF renderer with default font size (we'll scale it later)
                    this.ttfRenderer = new TTFRenderer(fontFile, 16.0f); // 16px default size
                    
                    if (this.ttfRenderer.initialize())
                    {
                        System.out.println("[Font.loadFont()] -> TTF loaded successfully!");
                        System.out.println("[Font.loadFont()] -> Font path: " + fontFile.getAbsolutePath());
                        System.out.println("[Font.loadFont()] -> Font size: 16.0");
                        
                        // Create custom text renderer that uses our TTF
                        this.ttfTextRenderer = new TTFTextRenderer(this.ttfRenderer, client.textRenderer);
                        this.isTTFFont = true;
                        
                        // Store fallback renderer (will be wrapped by our TTF renderer)
                        this.renderer = client.textRenderer;
                        
                        System.out.println("[Font.loadFont()] -> TTFTextRenderer created! isTTFFont=" + this.isTTFFont);
                        System.out.println("[Font.loadFont()] -> ttfTextRenderer null? " + (this.ttfTextRenderer == null));
                    }
                    else
                    {
                        System.out.println("[Font.loadFont()] -> TTF initialization failed, using default");
                        this.ttfRenderer = null;
                        this.ttfTextRenderer = null;
                        this.isTTFFont = false;
                        this.renderer = client.textRenderer;
                    }
                }
                else
                {
                    System.out.println("[Font.loadFont()] -> TTF file not found: " + fontFile.getAbsolutePath());
                    this.renderer = client.textRenderer;
                }
            }
            catch (Exception e)
            {
                this.renderer = client.textRenderer; // Fall back to default
                System.out.println("[Font.loadFont()] -> TTF font loading failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Generate display name for UI
     */
    private void generateDisplayName()
    {
        if (this.link == null)
        {
            this.displayName = "Default";
            return;
        }

        if (this.isMinecraftFont)
        {
            String fontName = this.link.path;
            
            if (fontName.equals("default") || fontName.isEmpty())
            {
                this.displayName = "Default";
            }
            else if (fontName.equals("alt"))
            {
                this.displayName = "Alt";
            }
            else if (fontName.equals("uniform"))
            {
                this.displayName = "Uniform";
            }
            else
            {
                this.displayName = fontName.substring(0, 1).toUpperCase() + fontName.substring(1);
            }
        }
        else
        {
            // Extract filename without extension for TTF fonts
            String path = this.link.path;
            int lastSlash = path.lastIndexOf('/');
            int lastDot = path.lastIndexOf('.');
            
            if (lastSlash >= 0 && lastDot > lastSlash)
            {
                this.displayName = path.substring(lastSlash + 1, lastDot);
            }
            else if (lastDot > 0)
            {
                this.displayName = path.substring(0, lastDot);
            }
            else
            {
                this.displayName = path;
            }
        }
    }

    public Link getLink()
    {
        return this.link;
    }

    public TextRenderer getRenderer()
    {
        return this.renderer;
    }

    /**
     * Get the TTF text renderer if this font is using a custom TTF.
     * @return TTFTextRenderer or null if not a TTF font
     */
    public TTFTextRenderer getTTFTextRenderer()
    {
        return this.ttfTextRenderer;
    }

    /**
     * Check if this font is using a custom TTF renderer.
     */
    public boolean isTTFFont()
    {
        return this.isTTFFont;
    }

    public String getDisplayName()
    {
        return this.displayName;
    }

    public boolean isMinecraftFont()
    {
        return this.isMinecraftFont;
    }

    public Identifier getMinecraftFontId()
    {
        return this.minecraftFontId;
    }
}
