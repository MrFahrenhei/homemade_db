package storage;

import memory.BufferPoolManager;
import memory.Page;

import java.util.logging.Logger;

public class BTreeIndex {
    private static final Logger logger = Logger.getLogger(BTreeIndex.class.getName());

    private static final int PAGE_HEADER_SIZE = 64;
    private static final int MAX_TUPLES_PER_PAGE = (Page.PAGE_SIZE - PAGE_HEADER_SIZE) / Tuple.TUPLE_SIZE;

    private final BufferPoolManager bufferPoolManager;
    private final int rootPageId;

    public BTreeIndex(BufferPoolManager bufferPoolManager){
        this.bufferPoolManager = bufferPoolManager;
        this.rootPageId = 0;
        logger.info("BTreeIndex initialized. Max Tuples per leaf page: "+MAX_TUPLES_PER_PAGE);
    }

    public boolean insert(Tuple tuple){
        Page targetPage = bufferPoolManager.fetchPage(rootPageId);

        try{
            targetPage.getData().position(0);
            int currentRecords = targetPage.getData().getInt();

            if(currentRecords >= MAX_TUPLES_PER_PAGE){
                logger.info("B-Tree Leaf Node is full. Split requires (Out of scope for POC");
                return false;
            }
            int insertOffset = PAGE_HEADER_SIZE + (currentRecords* Tuple.TUPLE_SIZE);
            tuple.serialize(targetPage.getData(), insertOffset);

            targetPage.getData().position(0);
            targetPage.getData().putInt(currentRecords + 1);

            logger.info("Inserted Tuple ID: "+tuple.getId()+" at offset: "+insertOffset);
            return true;
        } finally {
            bufferPoolManager.unpinPage(rootPageId, true);
        }
    }

    public boolean delete(long targetId){
        Page targetPage = bufferPoolManager.fetchPage(rootPageId);
        try{
            targetPage.getData().position(0);
            int currentRecords = targetPage.getData().getInt();
            Tuple tempTuple = new Tuple();

            for (int i = 0; i < currentRecords; i++){
                int readOffset = PAGE_HEADER_SIZE + (i * Tuple.TUPLE_SIZE);
                tempTuple.deserialize(targetPage.getData(), readOffset);

                if (tempTuple.getId() == targetId) {
                    targetPage.getData().position(readOffset);
                    for (int b = 0; b < Tuple.TUPLE_SIZE; b++) {
                        targetPage.getData().put((byte) 0x00);
                    }

                    logger.info("Tombstone placed for ID: " + targetId + " at offset: " + readOffset);
                    return true;
                }
            }
            logger.info("Delete failed: ID" + targetId + " not found.");
            return false;
        }finally {
            bufferPoolManager.unpinPage(rootPageId, true);
        }
    }

    public boolean update(long targetId, String newPayloadStr){
        Page targetPage = bufferPoolManager.fetchPage(rootPageId);
        try {
            targetPage.getData().position(0);
            int currentRecords = targetPage.getData().getInt();

            Tuple tempTuple = new Tuple();

            for (int i = 0; i < currentRecords; i++) {
                int readOffset = PAGE_HEADER_SIZE + (i*Tuple.TUPLE_SIZE);
                tempTuple.deserialize(targetPage.getData(), readOffset);

                if(tempTuple.getId() == targetId){
                    long newTimestamp = System.currentTimeMillis();
                    Tuple updatedTuple = new Tuple(targetId, newTimestamp, newPayloadStr);
                    updatedTuple.serialize(targetPage.getData(), readOffset);
                    logger.info("In-Place update executed for ID: "+targetId+" at offset: "+readOffset);
                    return true;
                }
            }
            logger.info("Update Failed: ID "+ targetId + " not found.");
            return false;
        } finally {
            bufferPoolManager.unpinPage(rootPageId, true);
        }
    }

    public String sequentialScan() {
        Page targetPage = bufferPoolManager.fetchPage(rootPageId);
        StringBuilder result = new StringBuilder();
        int activeRecords = 0;

        try {
            targetPage.getData().position(0);
            int currentRecords = targetPage.getData().getInt();
            Tuple tempTuple = new Tuple();

            for (int i = 0; i < currentRecords; i++) {
                int readOffset = PAGE_HEADER_SIZE + (i * Tuple.TUPLE_SIZE);
                tempTuple.deserialize(targetPage.getData(), readOffset);

                if (tempTuple.getId() != -1L) {
                    result.append("\n  [ID: ").append(tempTuple.getId()).append("] => ")
                            .append(tempTuple.getPayloadAsString());
                    activeRecords++;
                }
            }

            if (activeRecords == 0) {
                return "TABLE IS EMPTY";
            }

            return "Found " + activeRecords + " active records:" + result.toString();

        } finally {
            bufferPoolManager.unpinPage(rootPageId, false);
        }
    }

    public Tuple pointQuery(long searchId) {
        Page targetPage = bufferPoolManager.fetchPage(rootPageId);

        try {
            targetPage.getData().position(0);
            int currentRecords = targetPage.getData().getInt();

            Tuple tempTuple = new Tuple();

            for (int i = 0; i < currentRecords; i++) {
                int readOffset = PAGE_HEADER_SIZE + (i * Tuple.TUPLE_SIZE);
                tempTuple.deserialize(targetPage.getData(), readOffset);

                if (tempTuple.getId() == searchId) {
                    logger.info("Point Query HIT for ID: " + searchId);
                    return tempTuple;
                }
            }

            logger.info("Point Query MISS for ID: " + searchId);
            return null;

        } finally {
            bufferPoolManager.unpinPage(rootPageId, false);
        }
    }




}
