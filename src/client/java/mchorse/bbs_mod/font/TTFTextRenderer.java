package mchorse.bbs_mod.font;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Custom text renderer that uses TTFRenderer for rendering text.
 * This wraps around a fallback TextRenderer but uses STBTruetype for actual rendering.
 */
public class TTFTextRenderer
{
    private final TTFRenderer ttfRenderer;
    private final TextRenderer fallbackRenderer;
    
    // Glyph texture cache
    private final java.util.Map<Integer, GlyphTexture> glyphCache = new java.util.HashMap<>();

    public TTFTextRenderer(TTFRenderer ttfRenderer, TextRenderer fallbackRenderer)
    {
        this.ttfRenderer = ttfRenderer;
        this.fallbackRenderer = fallbackRenderer;
    }

    /**
     * Draw text at the specified position with shadow.
     */
    public int drawWithShadow(MatrixStack matrices, String text, int x, int y, int color)
    {
        // Draw shadow
        this.draw(matrices, text, x + 1, y + 1, (color & 0xFCFCFC) >> 2 | color & 0xFF000000);
        
        // Draw main text
        return this.draw(matrices, text, x, y, color);
    }

    /**
     * Draw text at the specified position.
     */
    public int draw(MatrixStack matrices, String text, float x, float y, int color)
    {
        if (text == null || text.isEmpty())
        {
            return 0;
        }

        float currentX = x;
        
        // Extract ARGB color components (normalized to 0-1)
        float alpha = ((color >> 24) & 0xFF) / 255.0f;
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;

        // Set up rendering state
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(red, green, blue, alpha);

        for (int i = 0; i < text.length(); i++)
        {
            int codePoint = text.codePointAt(i);
            
            // Get or create glyph texture
            GlyphTexture glyphTex = this.getOrCreateGlyphTexture(codePoint);
            
            if (glyphTex != null && glyphTex.textureId > 0)
            {
                // Draw the glyph
                this.drawGlyph(matrices, glyphTex, currentX, y);
                currentX += glyphTex.advanceWidth;
            }
            else
            {
                // Fallback for missing glyphs (e.g., spaces)
                TTFRenderer.GlyphData glyphData = this.ttfRenderer.renderGlyph(codePoint);
                if (glyphData != null)
                {
                    currentX += glyphData.advanceWidth;
                }
            }
        }

        // Restore rendering state
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();

        return (int) (currentX - x);
    }

    /**
     * Get or create a texture for a glyph.
     */
    private GlyphTexture getOrCreateGlyphTexture(int codePoint)
    {
        // Check cache
        if (this.glyphCache.containsKey(codePoint))
        {
            return this.glyphCache.get(codePoint);
        }

        // Render glyph
        TTFRenderer.GlyphData glyphData = this.ttfRenderer.renderGlyph(codePoint);
        
        if (glyphData == null || glyphData.image == null)
        {
            // Cache null for whitespace characters
            this.glyphCache.put(codePoint, null);
            return null;
        }

        // Upload to GPU
        int textureId = this.uploadGlyphTexture(glyphData.image);
        
        GlyphTexture glyphTex = new GlyphTexture(
            textureId,
            glyphData.advanceWidth,
            glyphData.bearingX,
            glyphData.bearingY,
            glyphData.width,
            glyphData.height
        );
        
        this.glyphCache.put(codePoint, glyphTex);
        
        // Clean up the NativeImage (texture is now on GPU)
        glyphData.image.close();
        
        return glyphTex;
    }

    /**
     * Upload a glyph image to GPU and return texture ID.
     */
    private int uploadGlyphTexture(NativeImage image)
    {
        int textureId = com.mojang.blaze3d.platform.GlStateManager._genTexture();
        
        RenderSystem.bindTexture(textureId);
        
        // Set texture parameters
        RenderSystem.texParameter(3553, 10241, 9728); // GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST
        RenderSystem.texParameter(3553, 10240, 9728); // GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST
        
        // Upload texture data
        image.upload(0, 0, 0, false);
        
        return textureId;
    }

    /**
     * Draw a single glyph quad using immediate mode rendering.
     */
    private void drawGlyph(MatrixStack matrices, GlyphTexture glyph, float x, float y)
    {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        float x1 = x + glyph.bearingX;
        float y1 = y + glyph.bearingY;
        float x2 = x1 + glyph.width;
        float y2 = y1 + glyph.height;

        // Bind glyph texture
        RenderSystem.setShaderTexture(0, glyph.textureId);

        // Use position_tex shader
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);

        // Draw textured quad
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        
        bufferBuilder.vertex(matrix, x1, y1, 0).texture(0, 0).next();
        bufferBuilder.vertex(matrix, x1, y2, 0).texture(0, 1).next();
        bufferBuilder.vertex(matrix, x2, y2, 0).texture(1, 1).next();
        bufferBuilder.vertex(matrix, x2, y1, 0).texture(1, 0).next();
        
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    /**
     * Get width of text using our TTF metrics.
     */
    public int getWidth(String text)
    {
        return this.ttfRenderer.getTextWidth(text);
    }

    /**
     * Get font height using our TTF metrics.
     */
    public int getFontHeight()
    {
        return this.ttfRenderer.getLineHeight();
    }

    /**
     * Get the underlying fallback TextRenderer.
     */
    public TextRenderer getFallbackRenderer()
    {
        return this.fallbackRenderer;
    }

    /**
     * Clean up glyph textures.
     */
    public void cleanup()
    {
        for (GlyphTexture glyph : this.glyphCache.values())
        {
            if (glyph != null && glyph.textureId > 0)
            {
                com.mojang.blaze3d.platform.GlStateManager._deleteTexture(glyph.textureId);
            }
        }
        this.glyphCache.clear();
    }

    /**
     * Cached glyph texture data.
     */
    private static class GlyphTexture
    {
        final int textureId;
        final int advanceWidth;
        final int bearingX;
        final int bearingY;
        final int width;
        final int height;

        GlyphTexture(int textureId, int advanceWidth, int bearingX, int bearingY, int width, int height)
        {
            this.textureId = textureId;
            this.advanceWidth = advanceWidth;
            this.bearingX = bearingX;
            this.bearingY = bearingY;
            this.width = width;
            this.height = height;
        }
    }
}

