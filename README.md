# Emergency Service Platform

## Overview
This project implements an **Emergency Service Platform** using the **STOMP** protocol. It consists of a server (in Java) and a client (in C++) to facilitate emergency event reporting and real-time communication across different emergency service channels, such as police, fire, and medical services.

## Features
### Server
- **STOMP Protocol Support**:
  - Handles `CONNECT`, `SEND`, `SUBSCRIBE`, `UNSUBSCRIBE`, and `DISCONNECT` frames.
- Supports two modes of operation:
  - **Thread-per-Client (TPC)**: Each client is handled by a dedicated thread.
  - **Reactor**: Event-driven architecture for efficient handling of multiple clients.
- Manages client subscriptions and message distribution based on channels.

### Client
- Multi-threaded design:
  - Handles user commands via keyboard input.
  - Listens to server messages concurrently.
- Supports commands:
  - **Login**: Connect to the server with username and password.
  - **Join/Exit Channels**: Subscribe or unsubscribe from specific emergency channels.
  - **Report**: Send emergency events from a JSON file.
  - **Summary**: Generate and save a summary of events for a specific channel.
  - **Logout**: Gracefully disconnect from the server.

## Prerequisites
### General
- Java 11 or higher
- C++ compiler with support for C++11 or higher
- Maven (for building the server)
- Make (for building the client)

### Libraries
- C++: JSON parsing library (included in the provided files)

## Installation
### Server
1. Navigate to the `server/` directory.
2. Build the project using Maven:
   ```bash
   mvn compile
   ```
3. Run the server:
   ```bash
   java -jar StompServer1.1.jar <port> <mode>
   ```
   Example:
   ```bash
   java -jar StompServer1.1.jar 7777 tpc
   ```

### Client
1. Navigate to the `client/` directory.
2. Build the client:
   ```bash
   make
   ```
3. Run the client:
   ```bash
   ./bin/StompEMIClient
   ```

## Usage
### Server
Run the server in one of two modes:
- `tpc` for Thread-per-Client.
- `reactor` for Reactor-based implementation.

Example:
```bash
java -jar StompServer1.1.jar 7777 reactor
```

### Client Commands
#### Login
```bash
login <host:port> <username> <password>
```
Example:
```bash
login 127.0.0.1:7777 user1 pass123
```

#### Join Emergency Channel
```bash
join <channel_name>
```
Example:
```bash
join police
```

#### Report Events
```bash
report <file>
```
Example:
```bash
report events1.json
```

#### Summary
```bash
summary <channel_name> <user> <file>
```
Example:
```bash
summary police user1 summary.txt
```

#### Logout
```bash
logout
```

## File Structure
```
project-root/
|-- server/
|   |-- src/
|   |-- pom.xml
|   |-- StompServer1.1.jar
|-- client/
|   |-- src/
|   |-- include/
|   |-- bin/
|   |-- Makefile
|-- data/
|   |-- events1.json
|   |-- events1_out.txt
```

## Development Notes
1. **Concurrency**: Both the server and client utilize multi-threading to handle simultaneous operations.
2. **Protocol Compliance**: Ensure that all STOMP frames adhere to the STOMP 1.2 specification.
3. **Testing**:
   - Use the provided `.jar` and `.exe` files for testing before full implementation.
   - Test edge cases, such as malformed frames, invalid commands, and disconnections.

## Example Workflow
1. Start the server:
   ```bash
   java -jar StompServer1.1.jar 7777 tpc
   ```
2. Start the client:
   ```bash
   ./bin/StompEMIClient
   ```
3. Login and interact:
   ```bash
   login 127.0.0.1:7777 user1 pass123
   join police
   report events1.json
   summary police user1 events1_out.txt
   logout
   ```

## Author
Arik shvarts
Aviv Eli
