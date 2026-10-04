package storage;

import memory.Page;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Manages reading and writing fixed-size pages between memory and a database file on disk
 */
public class DiskManager {
    private static final Logger logger = Logger.getLogger(DiskManager.class.getName());
    private final FileChannel fileChannel;
    /**
     * ID that will be assigned to the next allocated page.
     *
     * <p>Backed by an {@link AtomicInteger} so concurrent callers always receive
     * unique IDs without external locking.
     */
    private final AtomicInteger nextPageId;

    /**
     * ID that will be assigned to the next allocated page.
     *
     * <p>Backed by an {@link AtomicInteger} so concurrent callers always receive
     * unique IDs without external locking.
     */
    public DiskManager(String dbFile){
        try{
            Path path = Paths.get(dbFile);
            this.fileChannel = FileChannel.open(
                    path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE
            );
            this.nextPageId = new AtomicInteger((int) (fileChannel.size() / Page.PAGE_SIZE));
            logger.info("DISKManager initialized. Target file: " + dbFile);
        }catch (IOException e){
            throw new RuntimeException("System halt: Failed to open DB file. ", e);
        }
    }

    /**
     * Reads a page from disk into the given in-memory page.
     *
     * <p>The page's buffer is cleared before reading. On return, the buffer is
     * flipped and ready for reading if data was found. If {@code pageId} lies
     * beyond the end of the file, the buffer is left cleared (empty state)
     * rather than throwing.
     *
     * @param pageId ID of the page to read; must be non-negative
     * @param page   destination whose buffer receives the data; its contents
     *               are overwritten
     * @throws RuntimeException if an I/O error occurs while reading
     */
    public void readPage(int pageId, Page page){
        long offset = (long) pageId * Page.PAGE_SIZE;
        try{
            page.getData().clear();
            int bytesRead = fileChannel.read(page.getData(), offset);
            if(bytesRead == -1) {
                page.getData().clear();
            }else {
                page.getData().flip();
            }
        }catch(Exception e){
            throw new RuntimeException("I/O Error: Failed to read page "+pageId, e);
        }
    }

    /**
     * Writes the given page to disk at the position derived from its page ID
     * and forces the data to the storage device.
     *
     * <p>The page's buffer is rewound before writing, so the whole buffer is
     * written. The call flushes file content (not metadata) to disk before
     * returning, so the write is durable once this method completes.
     *
     * @param page the page to persist; its {@link Page#getPageId() page ID}
     *             determines the file offset
     * @throws RuntimeException if an I/O error occurs while writing
     */
    public void writePage(Page page){
        long offset = (long) page.getPageId() * Page.PAGE_SIZE;
        try{
           page.getData().rewind();
           int ignore = fileChannel.write(page.getData(), offset);
           fileChannel.force(false);
        }catch (IOException e){
            throw new RuntimeException("I/O Error: Failed to write page" + page.getPageId(), e);
        }
    }

    /**
     * Reserves and returns a new, unused page ID.
     *
     * <p>This only hands out an ID; it does not write anything to disk. The
     * file grows when the page is first passed to {@link #writePage(Page)}.
     * This method is thread-safe, and each call returns a distinct ID.
     *
     * @return the newly allocated page ID
     */
    public int allocatePage(){
        int newPageId = nextPageId.getAndIncrement();
        logger.info("Allocated new physical page with ID: " + newPageId);
        return newPageId;
    }

    /**
     * Closes the underlying file channel and releases its resources.
     *
     * <p>The manager must not be used after this method is called.
     *
     * @throws RuntimeException if the channel cannot be closed
     */
    public void shutDown(){
        try{
            fileChannel.close();
            logger.info("DiskManager gracefully shutdown");
        } catch (IOException e){
            throw new RuntimeException("Failed to close channel. ", e);
        }
    }
}
