package mchorse.bbs_mod.graphics.video;

import java.io.File;

/**
 * Interface for video decoding implementations
 */
public interface VideoDecoder
{
    /**
     * Open a video file and prepare for decoding
     */
    void open(File videoFile) throws Exception;
    
    /**
     * Get total number of frames in the video
     */
    int getTotalFrames();
    
    /**
     * Get video framerate (frames per second)
     */
    float getFramerate();
    
    /**
     * Get video duration in seconds
     */
    float getDuration();
    
    /**
     * Get video width in pixels
     */
    int getWidth();
    
    /**
     * Get video height in pixels
     */
    int getHeight();
    
    /**
     * Get a specific frame by index
     * @param frameIndex Frame number (0-based)
     * @return VideoFrame containing the decoded frame data, or null if failed
     */
    VideoFrame getFrame(int frameIndex) throws Exception;
    
    /**
     * Close the video file and free resources
     */
    void close();
    
    /**
     * Check if decoder is ready
     */
    boolean isOpen();
}
