package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.utils.colors.Color;

public class LabelForm extends Form
{
    public final ValueString text = new ValueString("text", "Hello, World!");
    public final ValueBoolean billboard = new ValueBoolean("billboard", false);
    public final ValueColor color = new ValueColor("color", Color.white());
    public final ValueLink font = new ValueLink("font", null);

    public final ValueInt max = new ValueInt("max", -1);
    public final ValueFloat anchorX = new ValueFloat("anchorX", 0.5F);
    public final ValueFloat anchorY = new ValueFloat("anchorY", 0.5F);
    public final ValueBoolean anchorLines = new ValueBoolean("anchorLines", false);

    /* Shadow properties */
    public final ValueFloat shadowX = new ValueFloat("shadowX", 1F);
    public final ValueFloat shadowY = new ValueFloat("shadowY", 1F);
    public final ValueColor shadowColor = new ValueColor("shadowColor", new Color(0, 0, 0, 0));

    /* Background */
    public final ValueColor background = new ValueColor("background", new Color(0, 0, 0, 0));
    public final ValueFloat offset = new ValueFloat("offset", 3F);

    public LabelForm()
    {
        super();

        this.register(this.text);
        this.register(this.billboard);
        this.register(this.color);
        this.register(this.font);
        this.register(this.max);
        this.register(this.anchorX);
        this.register(this.anchorY);
        this.register(this.anchorLines);
        this.register(this.shadowX);
        this.register(this.shadowY);
        this.register(this.shadowColor);
        this.register(this.background);
        this.register(this.offset);
    }

    @Override
    public String getDefaultDisplayName()
    {
        return this.text.get();
    }
}