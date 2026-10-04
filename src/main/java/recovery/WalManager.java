package recovery;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class WalManager {
    private static final Logger logger = Logger.getLogger(WalManager.class.getName());

    private final FileChannel logChannel;
    private final AtomicInteger currentLsn;
    private final ByteBuffer walBuffer;

    public WalManager(String logFile){
        try{
            Path path = Paths.get(logFile);
            this.logChannel = FileChannel.open(
                    path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );
            this.currentLsn = new AtomicInteger(0);
            this.walBuffer = ByteBuffer.allocateDirect(4096);
            logger.info("WalManager initialize. Target file: "+logFile);
        }catch (Exception e){
           throw new RuntimeException("System halt: Failed to open WAL file.", e);
        }
    }
    public long append(byte opCode, int pageId, byte[] payload){
        long lsn = currentLsn.get();

        walBuffer.clear();
        walBuffer.putLong(lsn);
        walBuffer.put(opCode);
        walBuffer.putInt(pageId);
        walBuffer.putInt(payload.length);
        walBuffer.put(payload);

        try{
            while (walBuffer.hasRemaining()){
                int ignored = logChannel.write(walBuffer);
            }
        }catch(IOException e){
           throw new RuntimeException("Wal write error at LSN: " + lsn, e);
        }
        return lsn;
    }

    public void flush(){
        try{
            logChannel.force(true);
            logger.info("WAL flushed to disk successfully");
        }catch (IOException e){
            throw new RuntimeException("Failed to fsync WAL. ", e);
        }
    }
    public void shutDown(){
        try{
            flush();
            logChannel.close();
            logger.info("WalManager graceful shut down.");
        }catch(IOException e){
            throw new RuntimeException("Failed to close WAL channel.", e);
        }
    }
}
