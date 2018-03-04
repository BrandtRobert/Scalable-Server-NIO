# Scalable Server Using Java NIO

This project demonstrates the construction of a scalable server that can handle up to 100 clients simultaneous using the java.nio framework. Mainly, the client receives 8 KB messages from clients at a given rate and returns SHA-1 hashes for those messages.

## Packages

### Client

Contains code to support the client side of the application.

#### Client

Sends *n* messages per second to the server, and verifies hash codes sent back by the server. Stores computed hash codes of each sent message in the HashList collection.

java Client \<server-ip> \<port> \<num-messages-per-second>

#### ClientRunner

Allows you to run multiple clients in one program by spawning a thread for each client.

java ClientRunner \<num-clients> \<server-ip> \<server-port> \<num-messages-per-second>

#### HashList

A synchronized list structure for storing hash codes computed by the client and verify hash codes received from the server.

### Concurrent

#### ThreadPool

Implementation of a java thread pool. This is a fixed threadpool created to a size of *n* threads. Call the threadpool initialization function to start the threads in the pool. The thread pool accepts Tasks and completes when a thread becomes available.

### Server

Contains code to manage the server side of the application.

#### ClientConnection

An object that holds data pertinent for the server to know about a client. Allows you to test whether a client is alive, and keeps a count of the throughput for this client. Holds messages that need to be hashed for a client, and a list of hashes to be sent out to a client.

#### Server

An nio based server that accepts new connections, tracks client connections, and submits "HashMessage" tasks to the thread pool. In this implementation the server handles all reading and writing for client connections.

### Tasks

Contains different task objects. A task represents work to be done by the threadpool.

#### Task

An abstract class that defines code for polling whether a task is finished and waiting for a task to finish. All tasks should extend the Task class.

#### HashMessage

Take an 8KB message from the client connections list and hashes it using SHA-1. After performing a hash of the clients message it changes the client key's interest set to write. This helps balance how much reading and writing you are doign per client.

### Util

#### Statistics Collector and Display

Computes the basic statistics for the server throughput, mean throughput, number of connected clients, and standard deviation of throughputs.