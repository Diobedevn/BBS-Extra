package mchorse.bbs_mod.graphics.texture;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.graphics.video.JavaCVVideoDecoder;
import mchorse.bbs_mod.graphics.video.VideoDecoder;
import mchorse.bbs_mod.graphics.video.VideoFrame;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.resources.Pixels;

import java.io.File;

/**
 * Texture wrapper for video playback.
 * Handles frame updates and timeline synchronization.
 */
public class VideoTexture
{
    private final Link videoPath;
    private Texture texture;
    private VideoDecoder decoder;
    
    private int width;
    private int height;
    private int totalFrames;
    private float framerate;
    private float duration;
    
    private int lastRenderedFrame = -1;
    
    // Video properties from BillboardForm
    private boolean playing = false;
    private float startTime = 0f;
    private float playbackSpeed = 1f;
    private boolean loop = false;
    
    private boolean loaded = false;
    
    public VideoTexture(Link videoPath)
    {
        this.videoPath = videoPath;
    }
    
    /**
     * Load video file and initialize decoder
     */
    public void load() throws Exception
    {
        // Use BBS's AssetProvider to get the File from Link
        File videoFile = BBSMod.getProvider().getFile(this.videoPath);
        
        if (!videoFile.exists())
        {
            throw new RuntimeException("Video file not found: " + videoFile.getAbsolutePath());
        }
        
        // Initialize JavaCV decoder
        this.decoder = new JavaCVVideoDecoder();
        this.decoder.open(videoFile);
        
        // Get video metadata
        this.width = this.decoder.getWidth();
        this.height = this.decoder.getHeight();
        this.totalFrames = this.decoder.getTotalFrames();
        this.framerate = this.decoder.getFramerate();
        this.duration = this.decoder.getDuration();
        
        // Load first frame to create OpenGL texture
        VideoFrame firstFrame = this.decoder.getFrame(0);
        if (firstFrame == null)
        {
            throw new RuntimeException("Failed to decode first frame");
        }
        
        // Create BBS texture from first frame
        this.texture = new Texture();
        
        // Set format before uploading
        Pixels pixels = firstFrame.pixels;
        this.texture.setFormat(pixels.bits == 4 ? TextureFormat.RGBA_U8 : TextureFormat.RGB_U8);
        
        // Force dimensions to 0 so updateTexture uses the slow path (glTexImage2D) for first frame
        this.texture.width = 0;
        this.texture.height = 0;
        
        // CRITICAL: Bind texture before uploading (Texture class expects manual binding)
        this.texture.bind();
        
        // Now updateTexture will properly allocate the texture with glTexImage2D
        this.texture.updateTexture(firstFrame.pixels);
        
        // Unbind to restore GL state
        this.texture.unbind();
        
        this.lastRenderedFrame = 0;
        this.loaded = true;
    }
    
    /**
     * Update texture with a new frame
     */
    public void updateFrame(int frameNumber) throws Exception
    {
        if (!this.loaded || this.decoder == null || !this.decoder.isOpen())
        {
            return;
        }
        
        // Clamp frame number
        frameNumber = Math.max(0, Math.min(frameNumber, this.totalFrames - 1));
        
        // Skip if already rendered (optimization: no-op if same frame)
        if (frameNumber == this.lastRenderedFrame)
        {
            return;
        }
        
        // Get frame from decoder (uses cache internally)
        VideoFrame frame = this.decoder.getFrame(frameNumber);
        
        if (frame == null)
        {
            System.err.println("Failed to decode frame " + frameNumber + " for video: " + this.videoPath);
            return;
        }
        
        // CRITICAL: Bind texture before updating (Texture class expects manual binding)
        this.texture.bind();
        
        // Update OpenGL texture with new frame data
        this.texture.updateTexture(frame.pixels);
        
        // Unbind to restore GL state
        this.texture.unbind();
        
        this.lastRenderedFrame = frameNumber;
    }
    
    /**
     * Calculate frame number from timeline tick.
     * Used by BillboardFormRenderer to sync video with timeline.
     * 
     * @param currentTick Current timeline tick
     * @param tickRate Ticks per second (usually 20)
     * @return Frame number to render
     */
    public int calculateFrameFromTick(int currentTick, int tickRate)
    {
        if (!this.playing)
        {
            // If not playing, return first frame
            return 0;
        }
        
        // Convert tick to seconds
        float currentTime = (float) currentTick / tickRate;
        
        // Apply start time offset
        float videoTime = currentTime - this.startTime;
        
        // Apply playback speed
        videoTime *= this.playbackSpeed;
        
        // Handle negative time (before video starts)
        if (videoTime < 0f)
        {
            return 0;
        }
        
        // Handle looping
        if (this.loop && videoTime > this.duration)
        {
            videoTime = videoTime % this.duration;
        }
        
        // Clamp to video duration if not looping
        if (videoTime > this.duration)
        {
            return this.totalFrames - 1;
        }
        
        // Calculate frame number
        int frameNumber = (int) (videoTime * this.framerate);
        return Math.max(0, Math.min(frameNumber, this.totalFrames - 1));
    }
    
    /**
     * Set video playback properties from BillboardForm
     */
    public void setVideoProperties(boolean playing, float startTime, float playbackSpeed, boolean loop)
    {
        this.playing = playing;
        this.startTime = startTime;
        this.playbackSpeed = Math.max(0.1f, playbackSpeed); // Prevent negative/zero speed
        this.loop = loop;
    }
    
    /**
     * Get the underlying OpenGL texture
     */
    public Texture getTexture()
    {
        return this.texture;
    }
    
    public boolean isLoaded()
    {
        return this.loaded;
    }
    
    public int getWidth()
    {
        return this.width;
    }
    
    public int getHeight()
    {
        return this.height;
    }
    
    public int getTotalFrames()
    {
        return this.totalFrames;
    }
    
    public float getFramerate()
    {
        return this.framerate;
    }
    
    public float getDuration()
    {
        return this.duration;
    }
    
    public Link getVideoPath()
    {
        return this.videoPath;
    }
    
    /**
     * Free OpenGL texture and decoder resources
     */
    public void delete()
    {
        if (this.texture != null)
        {
            this.texture.delete();
            this.texture = null;
        }
        
        if (this.decoder != null)
        {
            this.decoder.close();
            this.decoder = null;
        }
        
        this.loaded = false;
        
        System.out.println("Video texture deleted: " + this.videoPath);
    }
}
