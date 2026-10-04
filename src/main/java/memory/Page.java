package memory;

import java.nio.ByteBuffer;

/**
 * An in-memory copy of one fixed-size disk page
 */
public class Page {
    public static final int PAGE_SIZE = 4096;
    private int pageId;
    private final ByteBuffer data;
    private boolean isDirt;

    public Page(int pageId){
        this.pageId = pageId;
        this.data = ByteBuffer.allocateDirect(PAGE_SIZE);
        this.isDirt = false;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public ByteBuffer getData() {
        return data;
    }

    public boolean isDirt() {
        return isDirt;
    }

    public void setDirt(boolean dirt) {
        isDirt = dirt;
    }

}
