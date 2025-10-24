package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.font.Font;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FontRenderer
{
    private TextRenderer renderer;
    private Font font;  // Store the Font object for TTF support

    public static List<String> wrap(TextRenderer renderer, String string, int width)
    {
        return renderer.wrapLines(Text.literal(string), width).stream().map((ot) ->
        {
            StringBuilder builder = new StringBuilder();
            StyleHolder holder = new StyleHolder(Style.EMPTY);

            ot.accept((a, b, c) ->
            {
                if (!Objects.equals(b, holder.style))
                {
                    styleToString(builder, b);

                    holder.style = b;
                }

                builder.appendCodePoint(c);

                return true;
            });

            return builder.toString();
        }).collect(Collectors.toList());
    }

    private static void styleToString(StringBuilder b, Style style)
    {
        /* Ew... */
        if (!style.isEmpty())
        {
            b.append("\u00A7r");
        }

        if (style.getColor() != null)
        {
            switch (style.getColor().getName())
            {
                case "black": b.append("\u00A70"); break;
                case "dark_blue": b.append("\u00A71"); break;
                case "dark_green": b.append("\u00A72"); break;
                case "dark_aqua": b.append("\u00A73"); break;
                case "dark_red": b.append("\u00A74"); break;
                case "dark_purple": b.append("\u00A75"); break;
                case "gold": b.append("\u00A76"); break;
                case "gray": b.append("\u00A77"); break;
                case "dark_gray": b.append("\u00A78"); break;
                case "blue": b.append("\u00A79"); break;
                case "green": b.append("\u00A7a"); break;
                case "aqua": b.append("\u00A7b"); break;
                case "red": b.append("\u00A7c"); break;
                case "light_purple": b.append("\u00A7d"); break;
                case "yellow": b.append("\u00A7e"); break;
                case "white": b.append("\u00A7f"); break;
            }
        }

        if (style.isObfuscated()) b.append("\u00A7k");
        if (style.isBold()) b.append("\u00A7l");
        if (style.isStrikethrough()) b.append("\u00A7m");
        if (style.isUnderlined()) b.append("\u00A7n");
        if (style.isItalic()) b.append("\u00A7o");
    }

    public void setRenderer(TextRenderer renderer)
    {
        this.renderer = renderer;
    }

    public TextRenderer getRenderer()
    {
        return this.renderer;
    }

    /**
     * Set the Font object (for TTF support)
     */
    public void setFont(Font font)
    {
        this.font = font;
    }

    /**
     * Get the Font object
     */
    public Font getFont()
    {
        return this.font;
    }

    public int getWidth(String string)
    {
        return this.renderer.getWidth(string);
    }

    public int getHeight()
    {
        return this.renderer.fontHeight - 2;
    }

    public List<String> wrap(String string, int width)
    {
        return wrap(this.renderer, string, width);
    }

    public String limitToWidth(String str, int width)
    {
        return limitToWidth(str, "...", width);
    }

    public String limitToWidth(String str, String suffix, int width)
    {
        if (str.isEmpty())
        {
            return str;
        }

        int w = this.renderer.getWidth(str);

        if (w < width)
        {
            return str;
        }

        int sw = this.renderer.getWidth(suffix);
        int i = str.length() - 1;

        while (w + sw >= width && i > 0)
        {
            w -= this.renderer.getWidth(String.valueOf(str.charAt(i)));
            i -= 1;
        }

        str = str.substring(0, i);

        return str.isEmpty() ? str : str + suffix;
    }

    private static class StyleHolder
    {
        public Style style;

        public StyleHolder(Style style)
        {
            this.style = style;
        }
    }
}