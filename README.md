# Leader Election using Ring Topology (Java RMI)

## Overview
This project demonstrates a **Leader Election Algorithm** in a **ring topology** using **Java RMI**.  
Unlike centralized approaches, this implementation does **not use a peer registry**. Nodes communicate directly with their successor in the ring, passing election and leader messages until a leader is chosen.

The algorithm is based on the **LCR (Le Lann–Chang–Roberts)** protocol, where nodes in a unidirectional ring pass messages containing node IDs, and the node with the highest ID becomes the leader.

## Project Structure
- **Node.java**  
  RMI interface for node communication. Declares remote methods for sending election and leader messages between nodes.

- **NodeImpl.java**  
  Implements the `Node` interface. Each node:
  - Holds a unique ID.
  - Maintains a reference to its successor node in the ring.
  - Initiates and forwards election messages.
  - Declares itself leader if its ID wins, then propagates the leader message.

## Features
- Distributed leader election using **ring topology**.
- No central registry — nodes connect directly to each other.
- RMI-based communication for remote method invocation.
- Supports dynamic triggering of elections.
- Each node only needs to know its **successor** in the ring.

## Requirements
- Java JDK 8 or newer
- NetBeans IDE or any Java IDE
- RMI Registry (`rmiregistry`)

## How to Compile and Run
1. **Compile**
   ```bash
   javac Node.java NodeImpl.java
   ```

2. **Start RMI Registry** (in a separate terminal):
   ```bash
   rmiregistry 1099 &
   ```

3. **Start multiple nodes**  
   Launch several `NodeImpl` instances, each with a unique ID and knowledge of its successor. Example:
   ```bash
   java NodeImpl 1 rmi://localhost:1099/Node2
   java NodeImpl 2 rmi://localhost:1099/Node3
   java NodeImpl 3 rmi://localhost:1099/Node1
   ```

   In this example:
   - Node 1’s successor is Node 2
   - Node 2’s successor is Node 3
   - Node 3’s successor is Node 1 (closing the ring)

4. **Trigger an Election**  
   One node can start the election:
   ```bash
   java NodeImpl 1 rmi://localhost:1099/Node2 --election
   ```

5. **Observe Leader Announcement**  
   Election messages circulate until the node with the **highest ID** declares itself leader and broadcasts the result.

## Example Workflow
- 3 nodes form a ring: 1 → 2 → 3 → 1.
- Node 1 starts an election.
- IDs circulate: (1, 2, 3).
- Node 3 has the highest ID, becomes leader.
- Leader message circulates so all nodes know Node 3 is leader.

## .gitignore (Recommended)
```
/build/
/dist/
/nbproject/private/
/*.class
```

## Author
**Sione Likiliki**  

---

This project is for educational purposes in **Distributed Systems**, focusing on leader election protocols using **RMI without a central registry**.
