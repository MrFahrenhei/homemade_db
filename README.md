# Homemade DB

A minimal, single-node database engine written in Java. It stores fixed-size records in 4 KB pages, keeps I/O off the JVM heap, and guarantees durability through a Write-Ahead Log.

## Table of Contents

- [Functional Requirements](#functional-requirements)
- [Non-Functional Requirements](#non-functional-requirements)
- [Scope](#scope)
- [Design Numbers](#design-numbers)

## Functional Requirements

### Data Serialization (Write Path)

The system receives an `INSERT` command, serializes the primitive values (`Long`, `String`, `Integer`) into a byte array, and stores it at the next free offset of a 4 KB page.

> [!NOTE]
> **What is a page?**
> Pages are the basic unit used to organize data inside database files.

### Point Queries (Read Path)

The system supports looking up a single record by its primary key (a *point query*). It determines which page holds the record, loads that page into RAM, and extracts the record's bytes.

### Boot & Crash Recovery

On startup, the system reads the metadata files, maps the pages stored on disk, and reads the Write-Ahead Log (WAL). It then replays any committed transactions that were not yet applied to the data files.

> [!NOTE]
> **What is a WAL?**
> Write-Ahead Logging is a technique that ensures data integrity by recording every change in a log *before* it is applied to the database.

## Non-Functional Requirements

### Zero-GC Memory Management

The JVM's garbage collector can cause unpredictable pauses, which is a problem for a database. To avoid this, the I/O path does not allocate objects: all reads and writes go through off-heap memory allocated with `ByteBuffer.allocateDirect()`, keeping the garbage collector out of the hot path.

### ACID Durability via Kernel Syscalls

An `INSERT` returns `SUCCESS` to the client only after the data has been flushed to physical storage (via `fsync`).

> [!NOTE]
> **What is ACID?**
> ACID stands for Atomicity, Consistency, Isolation, and Durability: the properties that ensure database transactions are processed reliably.

### Fine-Grained Concurrency (Page Latches)

The system does not lock the whole database. Synchronization happens at the page level:

- Multiple threads can read the same page simultaneously (**shared latch**).
- Only one thread can hold the **exclusive latch** needed to modify a page.

## Scope

### What it does

Clients connect over a TCP socket. Instead of text commands, they send a compact binary message with this layout:

```
[ 1 byte: OpCode ][ 8 bytes: Key ][ 50 bytes: Payload ]
```

### What it won't do

| Feature | Details |
|---|---|
| SQL | No lexer, parser, or AST (Abstract Syntax Tree) |
| Clustering | No Raft or Paxos |
| Auth / RBAC | No roles or grants |

## Design Numbers

### Disk

Data is organized in pages of **4096 bytes**, matching the typical disk block size.

### Record Layout

| Field | Type | Size |
|---|---|---|
| ID | `long` | 8 bytes |
| Timestamp | `long` | 8 bytes |
| Name | fixed-length `String` | 48 bytes |
| **Total** | | **64 bytes** |

Records per page: `4032 bytes / 64 bytes = 63 records`

(Of the 4096 bytes in a page, 4032 are usable for records.)

### Buffer Pool

The buffer pool lives off-heap, outside the garbage collector's control. If the process exceeds its memory limit, the OS may kill it.

| Setting | Value |
|---|---|
| RAM limit | 1 GB |
| Page capacity | 1 GB / 4096 bytes = **262,144 pages** |
| Record capacity | 262,144 × 63 ≈ **16.5 million records** |

When the pool is full, the least recently used (LRU) page is evicted and written to disk.

### I/O

Insert speed is limited by how fast the system can `fsync` the Write-Ahead Log.

| Metric | Value |
|---|---|
| WAL on NVMe SSD | ~500 MB/s |
| Size of one WAL entry | ~100 bytes |