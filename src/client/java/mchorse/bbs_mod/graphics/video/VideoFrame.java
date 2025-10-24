package mchorse.bbs_mod.graphics.video;

import mchorse.bbs_mod.utils.resources.Pixels;

/**
 * Container for a decoded video frame.
 * Uses BBS's existing Pixels class for image data.
 */
public class VideoFrame
{
    public final Pixels pixels;
    public final int frameNumber;
    
    public VideoFrame(Pixels pixels, int frameNumber)
    {
        this.pixels = pixels;
        this.frameNumber = frameNumber;
    }
    
    public int getWidth()
    {
        return this.pixels.width;
    }
    
    public int getHeight()
    {
        return this.pixels.height;
    }
}
