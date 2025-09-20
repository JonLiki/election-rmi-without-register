# LCR Ring Election using Java RMI

This project implements the **Le Lann–Chang–Roberts (LCR)** leader election algorithm over a **ring topology** using Java RMI.

Each node is a separate Java process bound to its own RMI registry. The nodes form a logical ring and elect the node with the largest ID as the leader.

## Requirements

- Java 11 or newer
- 4 terminals (or processes) to simulate the nodes
- Classes compiled to `target/classes`

Compile:
```bash
javac -d target/classes src/main/java/cs324/election/without/register/Node.java src/main/java/cs324/election/without/register/*.java
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
