package mchorse.bbs_mod.forms.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.font.FontManager;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.LabelForm;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

public class LabelFormRenderer extends FormRenderer<LabelForm>
{
    public static void fillQuad(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a)
    {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();

        /* 1 - BR, 2 - BL, 3 - TL, 4 - TR */
        builder.vertex(matrix4f, x1, y1, z1).color(r, g, b, a).texture(0F, 0F).next();
        builder.vertex(matrix4f, x2, y2, z2).color(r, g, b, a).texture(0F, 0F).next();
        builder.vertex(matrix4f, x3, y3, z3).color(r, g, b, a).texture(0F, 0F).next();
        builder.vertex(matrix4f, x1, y1, z1).color(r, g, b, a).texture(0F, 0F).next();
        builder.vertex(matrix4f, x3, y3, z3).color(r, g, b, a).texture(0F, 0F).next();
        builder.vertex(matrix4f, x4, y4, z4).color(r, g, b, a).texture(0F, 0F).next();
    }

    public LabelFormRenderer(LabelForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        // Use custom font if specified, otherwise use default batcher font
        mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer fontRenderer = (this.form.font.get() != null)
            ? FontManager.get().getFontRenderer(this.form.font.get())
            : context.batcher.getFont();
        
        // Debug: Log what font is being used
        if (this.form.font.get() != null)
        {
            System.out.println("[LabelForm] Custom font link: " + this.form.font.get());
            System.out.println("[LabelForm] FontRenderer.getFont() is null? " + (fontRenderer.getFont() == null));
            if (fontRenderer.getFont() != null)
            {
                System.out.println("[LabelForm] Font display name: " + fontRenderer.getFont().getDisplayName());
                System.out.println("[LabelForm] Is TTF: " + fontRenderer.getFont().isTTFFont());
            }
        }
        
        int color = this.form.color.get().getARGBColor();
        String text = StringUtils.processColoredText(this.form.text.get());
        List<String> wrap = fontRenderer.wrap(text, x2 - x1 - 4);

        int th = fontRenderer.getHeight();
        int lineHeight = th + 4;
        int h = th + (wrap.size() - 1) * lineHeight;
        int y = (y2 + y1) / 2 - h / 2;

        for (String s : wrap)
        {
            // Use TTF text renderer if available, otherwise use standard renderer
            if (fontRenderer.getFont() != null && fontRenderer.getFont().isTTFFont() && fontRenderer.getFont().getTTFTextRenderer() != null)
            {
                System.out.println("[LabelForm] ✓ Using TTF renderer for: " + s);
                fontRenderer.getFont().getTTFTextRenderer().drawWithShadow(context.batcher.getContext().getMatrices(), s, x1 + 2, y, color);
            }
            else
            {
                if (fontRenderer.getFont() == null)
                {
                    System.out.println("[LabelForm] ✗ Font object is NULL for: " + s);
                }
                else if (!fontRenderer.getFont().isTTFFont())
                {
                    System.out.println("[LabelForm] ✗ Not TTF font (is " + fontRenderer.getFont().getDisplayName() + ") for: " + s);
                }
                else
                {
                    System.out.println("[LabelForm] ✗ TTF renderer is NULL for: " + s);
                }
                
                context.batcher.getContext().drawText(fontRenderer.getRenderer(), s, x1 + 2, y, color, true);
            }

            y += lineHeight;
        }
        
        context.batcher.getContext().draw();
    }

    @Override
    public void render3D(FormRenderingContext context)
    {
        context.stack.push();

        if (this.form.billboard.get())
        {
            Matrix4f modelMatrix = context.stack.peek().getPositionMatrix();
            Vector3f scale = Vectors.TEMP_3F;

            modelMatrix.getScale(scale);

            modelMatrix.m00(1).m01(0).m02(0);
            modelMatrix.m10(0).m11(1).m12(0);
            modelMatrix.m20(0).m21(0).m22(1);

            modelMatrix.scale(scale);

            context.stack.peek().getNormalMatrix().identity();
        }

        // Use custom font if specified, otherwise use default Minecraft font
        TextRenderer renderer = (this.form.font.get() != null)
            ? FontManager.get().getFontRenderer(this.form.font.get()).getRenderer()
            : MinecraftClient.getInstance().textRenderer;
        
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        float scale = 1F / 16F;
        int light = context.light;

        MatrixStackUtils.scaleStack(context.stack, scale, -scale, scale);

        RenderSystem.disableCull();

        if (context.isPicking())
        {
            CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
            {
                this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                RenderSystem.setShader(BBSShaders::getPickerModelsProgram);
            });

            light = 0;
        }

        if (this.form.max.get() <= 10)
        {
            this.renderString(context, consumers, renderer, light);
        }
        else
        {
            this.renderLimitedString(context, consumers, renderer, light);
        }

        CustomVertexConsumerProvider.clearRunnables();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        context.stack.pop();
    }

    private void renderString(FormRenderingContext context, CustomVertexConsumerProvider consumers, TextRenderer renderer, int light)
    {
        String content = StringUtils.processColoredText(this.form.text.get());
        float transition = context.getTransition();
        int w = renderer.getWidth(content) - 1;
        int h = renderer.fontHeight - 2;
        int x = (int) (-w * this.form.anchorX.get());
        int y = (int) (-h * this.form.anchorY.get());

        Color shadowColor = this.form.shadowColor.get().copy();
        Color color = this.form.color.get().copy();

        color.mul(context.color);
        shadowColor.mul(context.color);

        if (shadowColor.a > 0)
        {
            context.stack.push();
            context.stack.translate(0F, 0F, -0.1F);
            renderer.draw(
                content,
                x + this.form.shadowX.get(),
                y + this.form.shadowY.get(),
                shadowColor.getARGBColor(), false,
                context.stack.peek().getPositionMatrix(),
                consumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                light
            );
            context.stack.pop();
        }

        renderer.draw(
            content,
            x,
            y,
            color.getARGBColor(), false,
            context.stack.peek().getPositionMatrix(),
            consumers,
            TextRenderer.TextLayerType.NORMAL,
            0,
            light
        );

        RenderSystem.enableDepthTest();

        consumers.draw();

        this.renderShadow(context, x, y, w, h);
    }

    private void renderLimitedString(FormRenderingContext context, CustomVertexConsumerProvider consumers, TextRenderer renderer, int light)
    {
        float transition = context.getTransition();
        int w = 0;
        int h = renderer.fontHeight - 2;
        String content = StringUtils.processColoredText(this.form.text.get());
        List<String> lines = FontRenderer.wrap(renderer, content, this.form.max.get());

        if (lines.size() <= 1)
        {
            this.renderString(context, consumers, renderer, light);

            return;
        }

        for (int i = 0; i < lines.size(); i++)
        {
            lines.set(i, lines.get(i).trim());
        }

        for (String line : lines)
        {
            w = Math.max(renderer.getWidth(line) - 1, w);
            h += 12;
        }

        h -= 12;

        int x = (int) (-w * this.form.anchorX.get());
        int y = (int) (-h * this.form.anchorY.get());
        int y2 = y;

        Color shadowColor = this.form.shadowColor.get().copy();

        shadowColor.mul(context.color);

        if (shadowColor.a > 0)
        {
            context.stack.push();
            context.stack.translate(0F, 0F, -0.1F);

            for (String line : lines)
            {
                int x2 = x + (this.form.anchorLines.get() ? (int) ((w - renderer.getWidth(line)) * this.form.anchorX.get()) : 0);

                renderer.draw(
                    line,
                    x2 + this.form.shadowX.get(),
                    y2 + this.form.shadowY.get(),
                    shadowColor.getARGBColor(), false,
                    context.stack.peek().getPositionMatrix(),
                    consumers,
                    TextRenderer.TextLayerType.NORMAL,
                    0,
                    light
                );

                y2 += 12;
            }

            context.stack.pop();

            y2 = y;
        }

        Color cColor = this.form.color.get();

        cColor.mul(context.color);

        int color = cColor.getARGBColor();

        for (String line : lines)
        {
            int x2 = x + (this.form.anchorLines.get() ? (int) ((w - renderer.getWidth(line)) * this.form.anchorX.get()) : 0);

            renderer.draw(
                line,
                x2,
                y2,
                color, false,
                context.stack.peek().getPositionMatrix(),
                consumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                light
            );

            y2 += 12;
        }

        consumers.draw();

        RenderSystem.enableDepthTest();

        this.renderShadow(context, x, y, w, h);
    }

    private void renderShadow(FormRenderingContext context, int x, int y, int w, int h)
    {
        float offset = this.form.offset.get();
        Color color = this.form.background.get().copy();

        color.mul(context.color);

        if (color.a <= 0)
        {
            return;
        }

        context.stack.push();
        context.stack.translate(0, 0, -0.2F);

        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE);

        fillQuad(
            builder, context.stack,
            x + w + offset, y - offset, 0,
            x - offset, y - offset, 0,
            x - offset, y + h + offset, 0,
            x + w + offset, y + h + offset, 0,
            color.r, color.g, color.b, color.a
        );

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(builder.end());
        context.stack.pop();
    }
}