package mchorse.bbs_mod.graphics.video;

import mchorse.bbs_mod.utils.resources.Pixels;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JavaCV-based video decoder for high-performance frame extraction.
 * Much faster than FFmpeg CLI approach - no process spawning or temp files.
 */
public class JavaCVVideoDecoder implements VideoDecoder
{
    private FFmpegFrameGrabber grabber;
    private Java2DFrameConverter converter;
    
    private int width;
    private int height;
    private float framerate;
    private int totalFrames;
    private float duration;
    private boolean open = false;
    
    private final FrameCache cache;
    private final ExecutorService executor;
    private final Map<Integer, Future<VideoFrame>> pendingFrames;
    private int lastRequestedFrame = -1;
    
    public JavaCVVideoDecoder()
    {
        this(60); // Cache 60 frames (~1 second at 60fps) - reduced for lower memory usage
    }
    
    public JavaCVVideoDecoder(int cacheSize)
    {
        this.cache = new FrameCache(cacheSize);
        this.converter = new Java2DFrameConverter();
        this.pendingFrames = new ConcurrentHashMap<>();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "JavaCV-Frame-Decoder");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1); // Slightly lower priority
            return t;
        });
    }
    
    @Override
    public void open(File videoFile) throws Exception
    {
        if (videoFile == null || !videoFile.exists())
        {
            throw new IllegalArgumentException("Video file does not exist: " + videoFile);
        }
        
        // Create FFmpeg frame grabber with GPU hardware acceleration
        this.grabber = new FFmpegFrameGrabber(videoFile);
        
        // Try to enable hardware decoding (GPU acceleration)
        // Priority: NVDEC (NVIDIA) > QSV (Intel) > CPU fallback
        String hwAccelMethod = "CPU";
        
        try
        {
            // NVIDIA GPU acceleration (GTX 1650 supports this!)
            this.grabber.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
            this.grabber.setOption("hwaccel", "cuda");
            this.grabber.setOption("hwaccel_output_format", "cuda");
            this.grabber.start();
            hwAccelMethod = "NVIDIA CUDA";
        }
        catch (Exception e)
        {
            try
            {
                // Intel Quick Sync Video (if NVIDIA fails)
                this.grabber = new FFmpegFrameGrabber(videoFile);
                this.grabber.setOption("hwaccel", "qsv");
                this.grabber.start();
                hwAccelMethod = "Intel QSV";
            }
            catch (Exception e2)
            {
                // CPU fallback (no hardware acceleration)
                this.grabber = new FFmpegFrameGrabber(videoFile);
                this.grabber.start();
                hwAccelMethod = "CPU";
            }
        }
        
        // Get video metadata
        this.width = this.grabber.getImageWidth();
        this.height = this.grabber.getImageHeight();
        this.framerate = (float) this.grabber.getFrameRate();
        this.totalFrames = this.grabber.getLengthInFrames();
        this.duration = this.totalFrames / this.framerate;
        
        if (this.width <= 0 || this.height <= 0 || this.framerate <= 0)
        {
            throw new IllegalStateException("Invalid video metadata: " + 
                this.width + "x" + this.height + " @ " + this.framerate + " fps");
        }
        
        this.open = true;
        
        System.out.println("[FFMPEG] Opened: " + videoFile.getName() + 
            " (" + this.width + "x" + this.height + ", " + 
            this.totalFrames + " frames @ " + this.framerate + " fps, " + hwAccelMethod + ")");
    }
    
    @Override
    public VideoFrame getFrame(int frameIndex) throws Exception
    {
        if (!this.open)
        {
            throw new IllegalStateException("Decoder is not open");
        }
        
        // Check cache first
        VideoFrame cached = this.cache.get(frameIndex);
        if (cached != null)
        {
            // Start preloading next frames
            this.preloadFrames(frameIndex);
            return cached;
        }
        
        // Check if frame is being decoded in background
        Future<VideoFrame> pending = this.pendingFrames.get(frameIndex);
        if (pending != null && pending.isDone())
        {
            try
            {
                VideoFrame frame = pending.get();
                this.pendingFrames.remove(frameIndex);
                this.cache.put(frameIndex, frame);
                this.preloadFrames(frameIndex);
                return frame;
            }
            catch (Exception e)
            {
                this.pendingFrames.remove(frameIndex);
                throw e;
            }
        }
        
        // Frame not in cache and not being decoded - decode synchronously (will block!)
        VideoFrame frame = this.decodeFrameSync(frameIndex);
        
        // Handle decode failure gracefully
        if (frame == null)
        {
            System.err.println("[FFMPEG] Failed to decode frame " + frameIndex);
            return null;
        }
        
        this.cache.put(frameIndex, frame);
        
        // Start preloading next frames
        this.preloadFrames(frameIndex);
        
        return frame;
    }
    
    /**
     * Preload upcoming frames in background
     */
    private void preloadFrames(int currentFrame)
    {
        // Only preload if playing forward
        if (currentFrame <= this.lastRequestedFrame)
        {
            this.lastRequestedFrame = currentFrame;
            return;
        }
        
        this.lastRequestedFrame = currentFrame;
        
        // Submit background decode tasks for next 5 frames (reduced for lower-end PCs)
        for (int i = 1; i <= 5; i++)
        {
            int nextFrame = currentFrame + i;
            if (nextFrame >= this.totalFrames)
            {
                break;
            }
            
            // Skip if already cached or being decoded
            if (this.cache.get(nextFrame) != null || this.pendingFrames.containsKey(nextFrame))
            {
                continue;
            }
            
            // Submit background decode task with error handling
            final int frameToLoad = nextFrame;
            try
            {
                Future<VideoFrame> future = this.executor.submit(() -> this.decodeFrameSync(frameToLoad));
                this.pendingFrames.put(nextFrame, future);
            }
            catch (Exception e)
            {
                System.err.println("Failed to submit preload task for frame " + frameToLoad);
            }
        }
    }
    
    /**
     * Decode frame synchronously (blocks until decoded)
     */
    private synchronized VideoFrame decodeFrameSync(int frameIndex)
    {
        try
        {
            // Clamp frame index
            frameIndex = Math.max(0, Math.min(frameIndex, this.totalFrames - 1));
            
            // Seek to the frame
            this.grabber.setFrameNumber(frameIndex);
            
            // Grab the frame
            Frame frame = this.grabber.grabImage();
            if (frame == null)
            {
                System.err.println("Failed to grab frame " + frameIndex + " (null frame)");
                return null;
            }
            
            // Convert JavaCV Frame to BBS Pixels
            Pixels pixels = this.convertFrameToPixels(frame);
            
            // Create video frame
            return new VideoFrame(pixels, frameIndex);
        }
        catch (OutOfMemoryError e)
        {
            System.err.println("OUT OF MEMORY decoding frame " + frameIndex + "! Clearing cache...");
            this.cache.clear();
            System.gc(); // Request garbage collection
            return null;
        }
        catch (Exception e)
        {
            System.err.println("Error decoding frame " + frameIndex + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert JavaCV Frame (BGR) to BBS Pixels (RGBA)
     */
    private Pixels convertFrameToPixels(Frame frame) throws Exception
    {
        // Convert to BufferedImage
        BufferedImage image = this.converter.convert(frame);
        
        if (image == null)
        {
            throw new RuntimeException("Failed to convert frame to image");
        }
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Create ByteBuffer for RGBA pixels (4 bytes per pixel)
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        buffer.order(ByteOrder.nativeOrder());
        
        // Get image data
        if (image.getType() == BufferedImage.TYPE_INT_RGB || 
            image.getType() == BufferedImage.TYPE_INT_ARGB)
        {
            // Direct pixel access for INT_RGB/ARGB
            int[] imageData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            
            for (int i = 0; i < imageData.length; i++)
            {
                int pixel = imageData[i];
                
                // Extract RGB components
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                int a = (pixel >> 24) & 0xFF;
                
                // If no alpha channel, set to opaque
                if (image.getType() == BufferedImage.TYPE_INT_RGB)
                {
                    a = 0xFF;
                }
                
                // Write as RGBA bytes
                buffer.put((byte) r);
                buffer.put((byte) g);
                buffer.put((byte) b);
                buffer.put((byte) a);
            }
        }
        else
        {
            // Slower path for other image types
            for (int y = 0; y < height; y++)
            {
                for (int x = 0; x < width; x++)
                {
                    int pixel = image.getRGB(x, y);
                    
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    int a = (pixel >> 24) & 0xFF;
                    
                    // Write as RGBA bytes
                    buffer.put((byte) r);
                    buffer.put((byte) g);
                    buffer.put((byte) b);
                    buffer.put((byte) a);
                }
            }
        }
        
        buffer.flip();
        return new Pixels(buffer, width, height, 4);
    }
    
    @Override
    public int getTotalFrames()
    {
        return this.totalFrames;
    }
    
    @Override
    public float getFramerate()
    {
        return this.framerate;
    }
    
    @Override
    public float getDuration()
    {
        return this.duration;
    }
    
    @Override
    public int getWidth()
    {
        return this.width;
    }
    
    @Override
    public int getHeight()
    {
        return this.height;
    }
    
    @Override
    public void close()
    {
        // Cancel all pending decode tasks
        for (Future<VideoFrame> future : this.pendingFrames.values())
        {
            future.cancel(true);
        }
        this.pendingFrames.clear();
        
        // Shutdown executor
        if (this.executor != null)
        {
            this.executor.shutdownNow();
        }
        
        this.cache.clear();
        
        if (this.grabber != null)
        {
            try
            {
                this.grabber.stop();
                this.grabber.release();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        
        this.open = false;
    }
    
    @Override
    public boolean isOpen()
    {
        return this.open;
    }
    
    /**
     * Simple LRU cache for decoded frames
     */
    private static class FrameCache
    {
        private final LinkedHashMap<Integer, VideoFrame> cache;
        private final int maxSize;
        
        public FrameCache(int maxSize)
        {
            this.maxSize = maxSize;
            this.cache = new LinkedHashMap<Integer, VideoFrame>(maxSize, 0.75f, true)
            {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, VideoFrame> eldest)
                {
                    // Just evict from cache - memory managed by texture lifecycle
                    // Frames are read-only after creation, texture handles cleanup
                    return size() > FrameCache.this.maxSize;
                }
            };
        }
        
        public VideoFrame get(int frameIndex)
        {
            return this.cache.get(frameIndex);
        }
        
        public void put(int frameIndex, VideoFrame frame)
        {
            this.cache.put(frameIndex, frame);
        }
        
        public void clear()
        {
            // Just clear cache - memory managed by texture lifecycle
            this.cache.clear();
        }
    }
}
