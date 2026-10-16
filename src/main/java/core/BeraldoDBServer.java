package core;

import memory.BufferPoolManager;
import network.NioSocketServer;
import network.ProtocolParser;
import recovery.WalManager;
import storage.BTreeIndex;
import storage.DiskManager;

import java.util.logging.Logger;

public class BeraldoDBServer {

    private static final Logger logger = Logger.getLogger(BeraldoDBServer.class.getName());

    public static void main(String[] args) {
        logger.info("Booting BeraldoDB_Engine...");

        DiskManager diskManager = new DiskManager("beraldodb.db");
        WalManager walManager = new WalManager("beraldodb.log");

        BufferPoolManager bufferPool = new BufferPoolManager(diskManager, 1024);

        BackgroundWorker bgWorker = new BackgroundWorker(bufferPool);
        bgWorker.start();
        BTreeIndex index = new BTreeIndex(bufferPool);

        ProtocolParser parser = new ProtocolParser(index, walManager);
        NioSocketServer server = new NioSocketServer(5432, parser);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("SIGTERM received from OS. Initiating graceful shutdown...");
            bgWorker.shutDown();
            bufferPool.flushAllPages();
            walManager.shutDown();
            diskManager.shutDown();
            logger.info("System safely halted.");
        }));

        server.start();
    }
}
