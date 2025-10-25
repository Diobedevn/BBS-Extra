package mchorse.bbs_mod.font;

import net.minecraft.client.texture.NativeImage;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Simple TTF font renderer using STBTruetype.
 * This bypasses Minecraft's FontLoader/Font system to avoid 1.19.2→1.20.4 API issues.
 */
public class TTFRenderer
{
    private final File fontFile;
    private ByteBuffer fontData;
    private STBTTFontinfo fontInfo;
    private float fontSize;
    private float scale;
    private int ascent;
    private int descent;
    private int lineGap;
    
    private boolean initialized = false;

    public TTFRenderer(File fontFile, float fontSize)
    {
        this.fontFile = fontFile;
        this.fontSize = fontSize;
    }

    /**
     * Initialize the TTF font using STBTruetype.
     */
    public boolean initialize()
    {
        if (this.initialized)
        {
            return true;
        }

        try
        {
            // Load font file into ByteBuffer
            this.fontData = this.loadFontData(this.fontFile);
            if (this.fontData == null)
            {
                System.err.println("[TTFRenderer] Failed to load font data from: " + this.fontFile);
                return false;
            }

            // Initialize STBTruetype font info
            this.fontInfo = STBTTFontinfo.create();
            if (!STBTruetype.stbtt_InitFont(this.fontInfo, this.fontData))
            {
                System.err.println("[TTFRenderer] Failed to initialize STBTruetype for: " + this.fontFile);
                return false;
            }

            // Calculate scale for desired font size
            this.scale = STBTruetype.stbtt_ScaleForPixelHeight(this.fontInfo, this.fontSize);

            // Get font metrics
            int[] ascent = new int[1];
            int[] descent = new int[1];
            int[] lineGap = new int[1];
            STBTruetype.stbtt_GetFontVMetrics(this.fontInfo, ascent, descent, lineGap);
            this.ascent = Math.round(ascent[0] * this.scale);
            this.descent = Math.round(descent[0] * this.scale);
            this.lineGap = Math.round(lineGap[0] * this.scale);

            this.initialized = true;
            System.out.println("[TTFRenderer] Successfully initialized TTF: " + this.fontFile.getName());
            System.out.println("[TTFRenderer] Font size: " + this.fontSize + ", Scale: " + this.scale);
            System.out.println("[TTFRenderer] Metrics - Ascent: " + this.ascent + ", Descent: " + this.descent + ", LineGap: " + this.lineGap);
            return true;
        }
        catch (Exception e)
        {
            System.err.println("[TTFRenderer] Exception during initialization:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load font file into a ByteBuffer.
     */
    private ByteBuffer loadFontData(File file) throws IOException
    {
        try (FileInputStream fis = new FileInputStream(file);
             FileChannel fc = fis.getChannel())
        {
            ByteBuffer buffer = BufferUtils.createByteBuffer((int) fc.size());
            fc.read(buffer);
            buffer.flip();
            return buffer;
        }
    }

    /**
     * Get the width of a text string when rendered with this font.
     */
    public int getTextWidth(String text)
    {
        if (!this.initialized || text == null || text.isEmpty())
        {
            return 0;
        }

        int width = 0;
        for (int i = 0; i < text.length(); i++)
        {
            int codePoint = text.codePointAt(i);
            int glyphIndex = STBTruetype.stbtt_FindGlyphIndex(this.fontInfo, codePoint);
            
            int[] advanceWidth = new int[1];
            int[] leftSideBearing = new int[1];
            STBTruetype.stbtt_GetGlyphHMetrics(this.fontInfo, glyphIndex, advanceWidth, leftSideBearing);
            
            width += Math.round(advanceWidth[0] * this.scale);

            // Add kerning if there's a next character
            if (i + 1 < text.length())
            {
                int nextCodePoint = text.codePointAt(i + 1);
                int nextGlyphIndex = STBTruetype.stbtt_FindGlyphIndex(this.fontInfo, nextCodePoint);
                int kern = STBTruetype.stbtt_GetGlyphKernAdvance(this.fontInfo, glyphIndex, nextGlyphIndex);
                width += Math.round(kern * this.scale);
            }
        }

        return width;
    }

    /**
     * Get the line height of this font.
     */
    public int getLineHeight()
    {
        return this.ascent - this.descent + this.lineGap;
    }

    /**
     * Render a glyph to a NativeImage.
     * This is what would be used to create glyph textures for Minecraft.
     */
    public GlyphData renderGlyph(int codePoint)
    {
        if (!this.initialized)
        {
            return null;
        }

        int glyphIndex = STBTruetype.stbtt_FindGlyphIndex(this.fontInfo, codePoint);
        if (glyphIndex == 0)
        {
            return null; // Glyph not found
        }

        // Get glyph metrics
        int[] advanceWidth = new int[1];
        int[] leftSideBearing = new int[1];
        STBTruetype.stbtt_GetGlyphHMetrics(this.fontInfo, glyphIndex, advanceWidth, leftSideBearing);

        // Get glyph bounding box
        int[] x0 = new int[1], y0 = new int[1], x1 = new int[1], y1 = new int[1];
        STBTruetype.stbtt_GetGlyphBitmapBox(this.fontInfo, glyphIndex, this.scale, this.scale, x0, y0, x1, y1);

        int width = x1[0] - x0[0];
        int height = y1[0] - y0[0];

        if (width <= 0 || height <= 0)
        {
            // Whitespace character or empty glyph
            return new GlyphData(null, Math.round(advanceWidth[0] * this.scale), 0, 0, 0, 0);
        }

        // Render glyph to bitmap
        ByteBuffer bitmap = BufferUtils.createByteBuffer(width * height);
        STBTruetype.stbtt_MakeGlyphBitmap(this.fontInfo, bitmap, width, height, width, this.scale, this.scale, glyphIndex);

        // Convert to NativeImage (RGBA format for Minecraft)
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, false);
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                int alpha = bitmap.get(y * width + x) & 0xFF;
                // White glyph with alpha channel
                int color = (alpha << 24) | 0x00FFFFFF;
                image.setColor(x, y, color);
            }
        }

        return new GlyphData(
            image,
            Math.round(advanceWidth[0] * this.scale),
            x0[0],
            y0[0],
            width,
            height
        );
    }

    /**
     * Clean up resources.
     */
    public void close()
    {
        if (this.fontInfo != null)
        {
            this.fontInfo.free();
            this.fontInfo = null;
        }
        this.initialized = false;
    }

    /**
     * Data class to hold rendered glyph information.
     */
    public static class GlyphData
    {
        public final NativeImage image; // Can be null for whitespace
        public final int advanceWidth;
        public final int bearingX;
        public final int bearingY;
        public final int width;
        public final int height;

        public GlyphData(NativeImage image, int advanceWidth, int bearingX, int bearingY, int width, int height)
        {
            this.image = image;
            this.advanceWidth = advanceWidth;
            this.bearingX = bearingX;
            this.bearingY = bearingY;
            this.width = width;
            this.height = height;
        }
    }
}
