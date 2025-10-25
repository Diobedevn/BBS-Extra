package mchorse.bbs_mod.graphics.texture;

import mchorse.bbs_mod.resources.Link;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages video texture lifecycle and caching.
 * Singleton pattern to ensure one manager instance.
 */
public class VideoTextureManager
{
    private static VideoTextureManager INSTANCE;
    
    private final Map<Link, VideoTexture> loadedVideos;
    private final Set<Link> preloadingVideos;
    private int currentTick = 0;
    
    private VideoTextureManager()
    {
        this.loadedVideos = new HashMap<>();
        this.preloadingVideos = new HashSet<>();
    }
    
    /**
     * Get singleton instance
     */
    public static VideoTextureManager getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new VideoTextureManager();
        }
        
        return INSTANCE;
    }
    
    /**
     * Get or load a video texture.
     * Called by BillboardFormRenderer.
     */
    public VideoTexture getVideoTexture(Link videoPath)
    {
        // Check if already loaded
        VideoTexture video = this.loadedVideos.get(videoPath);
        
        if (video != null && video.isLoaded())
        {
            return video;
        }
        
        // Load video
        System.out.println("[VIDEO] Loading: " + videoPath);
        try
        {
            video = new VideoTexture(videoPath);
            video.load();
            this.loadedVideos.put(videoPath, video);
            
            System.out.println("[VIDEO] Loaded successfully");
            return video;
        }
        catch (Exception e)
        {
            System.err.println("[VIDEO] Failed to load: " + videoPath);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Preload video texture in background.
     * Used by Films.java before playback starts.
     */
    public void preloadVideoTexture(Link videoPath)
    {
        // Skip if already loaded or preloading
        if (this.loadedVideos.containsKey(videoPath) || this.preloadingVideos.contains(videoPath))
        {
            return;
        }
        
        this.preloadingVideos.add(videoPath);
        
        // Load in background thread to avoid blocking
        Thread preloadThread = new Thread(() -> {
            try
            {
                VideoTexture video = new VideoTexture(videoPath);
                video.load();
                
                this.loadedVideos.put(videoPath, video);
                System.out.println("Video preloaded: " + videoPath);
            }
            catch (Exception e)
            {
                System.err.println("Failed to preload video: " + videoPath);
                e.printStackTrace();
            }
            finally
            {
                this.preloadingVideos.remove(videoPath);
            }
        }, "Video-Preload-" + videoPath.toString());
        
        preloadThread.setDaemon(true);
        preloadThread.start();
    }
    
    /**
     * Check if a video is being preloaded
     */
    public boolean isPreloading(Link videoPath)
    {
        return this.preloadingVideos.contains(videoPath);
    }
    
    /**
     * Check if a video is loaded
     */
    public boolean isLoaded(Link videoPath)
    {
        VideoTexture video = this.loadedVideos.get(videoPath);
        return video != null && video.isLoaded();
    }
    
    /**
     * Get loading progress for preloading.
     * Returns value between 0.0 (not started) and 1.0 (complete).
     */
    public float getLoadingProgress(Set<Link> videosToLoad)
    {
        if (videosToLoad.isEmpty())
        {
            return 1.0f;
        }
        
        int loaded = 0;
        
        for (Link videoPath : videosToLoad)
        {
            if (this.isLoaded(videoPath))
            {
                loaded++;
            }
        }
        
        return (float) loaded / videosToLoad.size();
    }
    
    /**
     * Update current timeline tick.
     * Used by BaseFilmController to track playback position.
     */
    public void setCurrentTick(int tick)
    {
        this.currentTick = tick;
    }
    
    public int getCurrentTick()
    {
        return this.currentTick;
    }
    
    /**
     * Unload a specific video texture
     */
    public void unloadVideoTexture(Link videoPath)
    {
        VideoTexture video = this.loadedVideos.remove(videoPath);
        
        if (video != null)
        {
            video.delete();
            System.out.println("Video texture unloaded: " + videoPath);
        }
    }
    
    /**
     * Unload all video textures.
     * Called on disconnect/world change.
     */
    public void unloadAll()
    {
        System.out.println("Unloading all video textures (" + this.loadedVideos.size() + ")...");
        
        for (VideoTexture video : this.loadedVideos.values())
        {
            video.delete();
        }
        
        this.loadedVideos.clear();
        this.preloadingVideos.clear();
        this.currentTick = 0;
    }
}
