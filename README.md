# LCR Ring Election using Java RMI

This project implements the **Le Lann–Chang–Roberts (LCR)** leader election algorithm over a **ring topology** using Java RMI.

Each node is a separate Java process bound to its own RMI registry. The nodes form a logical ring and elect the node with the largest ID as the leader.

## Overview
This project demonstrates distributed leader election using the LCR algorithm.  
It is designed as a coursework assignment for distributed systems, showing how nodes in a ring topology coordinate through message passing over Java RMI.


## Requirements

- Java 11 or newer
- NetBeans or any Java IDE (optional)
- 4 terminals (or processes) to simulate the nodes
- Classes compiled to `target/classes`

## Setup
1. Install Java 11+ and ensure `java` and `javac` are on your PATH.
2. Compile the source code:
```bash
javac -d target/classes src/main/java/cs324/election/without/register/Node.java src/main/java/cs324/election/without/register/*.java
```
## Visual Feedback (Color & Emoji Support)

For best presentation results, enable UTF-8 output in each terminal before starting the nodes. This ensures that ANSI colors and emojis display correctly.

In **PowerShell**, run:
```
chcp 65001
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

```
Now when you run the nodes, log messages will include **color-coded text and emojis** for clearer visual feedback.

## Running the Simulation

Open **4 terminals**, one per node.

### Terminal 1
```bash
cd <project-root>
java -cp target/classes cs324.election.without.register.NodeImpl 5 Node11 1099 1199
```
<img width="1086" height="489" alt="image" src="https://github.com/user-attachments/assets/530ac367-6f6b-44dc-b755-32c60dc970e8" />


### Terminal 2
```bash
cd <project-root>
java -cp target/classes cs324.election.without.register.NodeImpl 11 Node2 1199 1299
```
<img width="1085" height="483" alt="image" src="https://github.com/user-attachments/assets/c95879e7-e9bf-4710-a43e-42700783cb88" />

### Terminal 3
```bash
cd <project-root>
java -cp target/classes cs324.election.without.register.NodeImpl 2 Node7 1299 1399
```
<img width="1093" height="482" alt="image" src="https://github.com/user-attachments/assets/cb0b548e-3df7-4134-8065-4453e0cd3e7e" />

### Terminal 4
```bash
cd <project-root>
java -cp target/classes cs324.election.without.register.NodeImpl 7 Node5 1399 1099
```
<img width="1091" height="481" alt="image" src="https://github.com/user-attachments/assets/62c9730d-4da5-448d-8aee-5b8387abae25" />

### Arguments Meaning
1. **Node ID** – unique integer identifier of the node.
2. **Successor Node Name** – the registry name of the next node in the ring.
3. **Local Registry Port** – port this node’s RMI registry listens on.
4. **Successor Registry Port** – port used to connect to successor’s RMI registry.

### Example Ring Topology
```
Node 5 → Node 11 → Node 2 → Node 7 → Node 5
```
```
   [Node 5] ---> [Node 11]
       ^             |
       |             v
   [Node 7] <--- [Node 2]
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
<img width="1918" height="1030" alt="image" src="https://github.com/user-attachments/assets/ab84572b-ddb8-479a-a0d9-57c365f06b2c" />

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

Complexity: **O(n²)** messages for the election plus **O(n)** for the leader announcement.

## Troubleshooting

- java.rmi.ConnectException – The successor node or its RMI registry is not running. Start the successor first.

- java.rmi.NotBoundException – The given successor name is not yet registered. Check the name.

- **Election does not complete** – Ensure all 4 nodes are started and form a complete ring.

## Group Members (CS324)

- Sione Likiliki
- Seiloni Utuone
- Fasi Tangataevaha
- Lanuongo Guttenbeil

Assignment: Election RMI using Ring Topology (LCR).

---

## License
For academic use in distributed systems coursework.
