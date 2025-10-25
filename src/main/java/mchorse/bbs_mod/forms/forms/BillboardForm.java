package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.misc.ValueVector4f;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.utils.colors.Color;
import org.joml.Vector4f;

public class BillboardForm extends Form
{
    public final ValueLink texture = new ValueLink("texture", null);
    public final ValueBoolean billboard = new ValueBoolean("billboard", false);
    public final ValueBoolean linear = new ValueBoolean("linear", false);
    public final ValueBoolean mipmap = new ValueBoolean("mipmap", false);
    public final ValueVector4f crop = new ValueVector4f("crop", new Vector4f(0, 0, 0, 0));
    public final ValueBoolean resizeCrop = new ValueBoolean( "resizeCrop", false);
    public final ValueColor color = new ValueColor("color", Color.white());
    public final ValueFloat offsetX = new ValueFloat("offsetX", 0F);
    public final ValueFloat offsetY = new ValueFloat("offsetY", 0F);
    public final ValueFloat rotation = new ValueFloat("rotation", 0F);
    public final ValueBoolean shading = new ValueBoolean("shading", true);
    
    /* Video playback properties */
    public final ValueBoolean videoPlaying = new ValueBoolean("videoPlaying", false);
    public final ValueFloat videoStartTime = new ValueFloat("videoStartTime", 0F);
    public final ValueFloat videoPlaybackSpeed = new ValueFloat("videoPlaybackSpeed", 1F);
    public final ValueBoolean videoLoop = new ValueBoolean("videoLoop", false);

    public BillboardForm()
    {
        super();

        this.linear.invisible();
        this.mipmap.invisible();
        this.resizeCrop.invisible();
        this.shading.invisible();

        this.register(this.texture);
        this.register(this.billboard);
        this.register(this.linear);
        this.register(this.mipmap);
        this.register(this.crop);
        this.register(this.resizeCrop);
        this.register(this.color);
        this.register(this.offsetX);
        this.register(this.offsetY);
        this.register(this.rotation);
        this.register(this.shading);
        this.register(this.videoPlaying);
        this.register(this.videoStartTime);
        this.register(this.videoPlaybackSpeed);
        this.register(this.videoLoop);
    }
    
    /**
     * Check if texture is a video file
     */
    public boolean isVideoTexture()
    {
        Link link = this.texture.get();
        
        if (link == null)
        {
            return false;
        }
        
        String path = link.path.toLowerCase();
        return path.endsWith(".mp4") || path.endsWith(".mov") || 
               path.endsWith(".avi") || path.endsWith(".webm") || 
               path.endsWith(".mkv");
    }

    @Override
    public String getDefaultDisplayName()
    {
        Link link = this.texture.get();

        return link == null ? "none" : link.toString();
    }
}