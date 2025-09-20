<<<<<<< HEAD
# LCR Ring Election using Java RMI

This project implements the **Le Lann–Chang–Roberts (LCR)** leader election algorithm over a **ring topology** using Java RMI.

Each node is a separate Java process bound to its own RMI registry. The nodes form a logical ring and elect the node with the largest ID as the leader.

## Requirements

- Java 11 or newer
- 4 terminals (or processes) to simulate the nodes
- Classes compiled to `target/classes`

Compile:
```bash
javac -d target/classes src/main/java/cs324/election/without/register/Node.java src/main/java/cs324/election/without/register/NodeImpl.java
```

## Running the Simulation

Open **4 terminals**, one per node.

### Terminal 1
```bash
cd <project-root>
java -cp target/classes cs324.election.without.register.NodeImpl 5 Node11 1099 1199
```

### Terminal 2
```bash
cd <project-root>
java -cp target/classes cs324.election.without.register.NodeImpl 11 Node2 1199 1299
```

### Terminal 3
```bash
cd <project-root>
java -cp target/classes cs324.election.without.register.NodeImpl 2 Node7 1299 1399
```

### Terminal 4
```bash
cd <project-root>
java -cp target/classes cs324.election.without.register.NodeImpl 7 Node5 1399 1099
```

### Arguments Meaning
1. **Node ID** – unique integer identifier of the node.
2. **Successor Node Name** – the registry name of the next node in the ring.
3. **Local Registry Port** – port this node’s RMI registry listens on.
4. **Successor Registry Port** – port used to connect to successor’s RMI registry.

### Example Ring Topology
```
Node 5 → Node 11 → Node 2 → Node 7 → Node 5
```

Each node registers itself and connects to its successor to form the ring.

## Running the Election

1. Wait until all 4 terminals show that their RMI registries are connected and the ring is established.
2. Each terminal will present an **interactive prompt**.
3. On any terminal, type:
   ```
   start
   ```
   to begin the leader election.
4. Messages will circulate around the ring.
5. At the end, the node with the **highest ID** will declare itself as **leader** and broadcast this result.

## Example Output
```
[Node-5] initiating election with UID=5
[Node-11] received ELECTION(5), replaces with 11
[Node-2] received ELECTION(11), forwards unchanged
[Node-7] received ELECTION(11), forwards unchanged
[Node-11] received its own UID -> declares LEADER
[Node-11] broadcasting LEADER(11)
All nodes: leaderId = 11, electionCompleted = true
```

## Notes

- Each node has its own RMI registry.
- If one node is not started, the ring will not form.
- Only unique IDs are valid. Duplicate IDs break the algorithm.
- The ring is fixed and does not bypass failed nodes.

## Commands (interactive prompt)

- `start` – start an election
- `status` – print node status
- `exit` – terminate node process

## Algorithm Summary

- **Election rule**: Forward the larger of (incoming UID, local ID).
- **Leader detection**: When a node receives its own UID back, it declares itself leader.
- **Leader announcement**: The leader sends a `LEADER(id)` message around the ring until it returns.

---

## License
For academic use in distributed systems coursework.
=======
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
>>>>>>> e293926 (Update Node and NodeImpl with fixed LCR implementation)
