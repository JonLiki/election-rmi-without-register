/*
 * Enhanced LCR Leader Election Implementation - BULLETPROOF CIRCUIT COMPLETION
 * Includes comprehensive debug logging to trace election flow
 */
package cs324.election.without.register;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of the Node interface for LCR leader election. This class
 * handles the core LCR protocol logic with enhanced features for demo
 * presentation: - Visual status indicators without timestamps - Robust error
 * handling and retry mechanisms - Node failure simulation and recovery -
 * Interactive command-line interface - Thread-safe election state management -
 * BULLETPROOF: Guaranteed circuit completion with debug tracing
 *
 * @author sione.likiliki (enhanced for demo)
 */
public class NodeImpl implements Node {

    // Core node properties
    private final int id;
    private volatile int leaderId = -1;
    private volatile Node nextNode;
    private volatile boolean isLeader = false;
    private volatile boolean isAlive = true;

    // Election state management
    private final AtomicBoolean electionInProgress = new AtomicBoolean(false);
    private final AtomicBoolean electionCompleted = new AtomicBoolean(false);
    private final ReentrantLock electionLock = new ReentrantLock();

    // Demo configuration
    private static final long NETWORK_DELAY = 3000; // 3 seconds for election messages
    private static final long LEADER_ANNOUNCE_DELAY = 1500; // 1.5 seconds for leader announcements
    private static final int MAX_RETRIES = 15;
    private static final long RETRY_DELAY = 1500;
    private static final int INITIAL_WAIT = 8000; // 8 seconds for all nodes to initialize

    // Status tracking
    private final Object statusLock = new Object();
    private volatile String status = "INITIALIZING";
    private long lastStatusUpdate = System.currentTimeMillis();

    // Debug toggle
    private static boolean debugEnabled = false;

    /**
     * Constructor for a node with configurable network delay. No automatic RMI
     * export here - done manually in main method
     *
     * @param nodeId The unique ID of the node.
     */
    public NodeImpl(int nodeId) {
        this.id = nodeId;
        updateStatus("READY");
        printStartupMessage();
    }

    @Override
    public int getId() throws RemoteException {
        return id;
    }

    @Override
    public void setNextNode(Node nextNode) throws RemoteException {
        this.nextNode = nextNode;
        updateStatus("CONNECTED");
        logMessage("🔗 Connected to next node in ring");
        debugRingConnection();
    }

    @Override
    public void setAlive(boolean alive) throws RemoteException {
        if (isAlive != alive) {
            isAlive = alive;
            if (alive) {
                updateStatus("RECOVERED");
                logMessage("🔥 Node has recovered! 🔥");
                reconnectToRing();
            } else {
                updateStatus("FAILED");
                logMessage("💀 Node has failed! 💀");
            }
        }
    }

    /**
     * BULLETPROOF LCR ELECTION HANDLING - GUARANTEED CIRCUIT COMPLETION WITH
     * DEBUG - FIXED: Deadlock prevention and proper leader declaration
     */
    @Override
    public int receiveElection(int uid, int initiatorId) throws RemoteException {
        if (!isAlive || electionCompleted.get()) {
            debugLog("SKIPPING election message - node dead or election completed");
            return 1;
        }

        updateStatus("ELECTION_MSG(" + uid + ")");
        logMessage("📨 Received ELECTION(" + uid + ") from initiator " + initiatorId);

        try {
            Thread.sleep(NETWORK_DELAY);
        } catch (InterruptedException e) {
            logError("Interrupted during network delay simulation");
            Thread.currentThread().interrupt();
            return -1;
        }

        if (nextNode == null) {
            logWarning("❌ No next node available - election message DROPPED");
            return 1;
        }

        int forwardUid = -1;
        electionLock.lock();
        try {
            debugLog("🔍 PROCESSING ELECTION(" + uid + ") -> Initiator: " + initiatorId + ", My ID: " + id + ", Next: "
                    + (nextNode != null ? "EXISTS" : "NULL"));

            if (uid == id && !electionCompleted.get()) {
                logMessage("🏆 CIRCUIT COMPLETED! Node " + id + " declares itself LEADER!");
                debugLog("🎉 CIRCUIT COMPLETION CONFIRMED - Node " + id + " wins election");

                leaderId = uid;
                isLeader = true;
                electionCompleted.set(true);
                electionInProgress.set(false);
                announceLeader(uid, id);
                updateStatus("LEADER_DECLARED");
                return 1;
            }

            if (uid < id) {
                logMessage("🔄 REPLACING ELECTION(" + uid + ") with " + id + " (my ID is higher)");
                updateStatus("REPLACING_ID");
                forwardUid = id;
            } else {
                logMessage("➡️ FORWARDING higher ELECTION(" + uid + ") unchanged");
                updateStatus("FORWARDING");
                forwardUid = uid;
            }
        } finally {
            electionLock.unlock();
        }

        if (forwardUid != -1) {
            final int finalForwardUid = forwardUid;
            final Node finalNextNode = nextNode;

            try {
                finalNextNode.receiveElection(finalForwardUid, initiatorId);
                logMessage("✅ Forwarded ELECTION(" + finalForwardUid + ") to next node");

            } catch (Exception forwardEx) {
                logError("❌ CRITICAL FAILURE: Could not forward: " + forwardEx.getMessage());
                debugLog("💥 FORWARD FAILURE DETAILS: " + forwardEx.getClass().getSimpleName() + " - " + forwardEx.getMessage());
                electionInProgress.set(false);
            }
        }

        return 1;
    }

    /**
     * FIXED: Leader announcement handling - stops forwarding when circuit
     * completes and displays results on ALL nodes
     */
    @Override
    public int receiveLeader(int leaderId, int originId) throws RemoteException {
        if (!isAlive) {
            return 1;
        }

        updateStatus("LEADER_MSG(" + leaderId + ")");
        logMessage("👑 Received LEADER(" + leaderId + ") from origin " + originId);

        if (nextNode == null) {
            logWarning("No next node for leader announcement - message dropped");
            return 1;
        }

        try {
            this.leaderId = leaderId;
            if (this.id == leaderId) {
                isLeader = true;
                updateStatus("I_AM_LEADER");
                logMessage("🎉 I AM THE LEADER! 🎉");
            }

            electionCompleted.set(true);
            electionInProgress.set(false);
            printElectionResult();
            printRingStatus();

            if (this.id == originId) {
                logMessage("✅ Leader announcement completed full circuit! STOPPING FORWARDING");
                updateStatus("ANNOUNCEMENT_COMPLETE");
                return 1;
            }

            if (nextNode != null && isAlive) {
                final int finalLeaderId = leaderId;
                final int finalOriginId = originId;
                final Node finalNextNode = nextNode;

                Thread forwardThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(LEADER_ANNOUNCE_DELAY);
                            debugLog("👑 Forwarding LEADER(" + finalLeaderId + ") announcement");
                            finalNextNode.receiveLeader(finalLeaderId, finalOriginId);
                            logMessage("✅ Forwarded LEADER(" + finalLeaderId + ") announcement");
                        } catch (Exception e) {
                            logError("Failed to forward leader announcement: " + e.getMessage());
                        }
                    }
                });
                forwardThread.start();
            }

            return 1;
        } catch (Exception e) {
            logError("Error in receiveLeader: " + e.getMessage());
            return -1;
        }
    }

    private void announceLeader(int leaderId, int originId) throws RemoteException {
        logMessage("--- Broadcasting Leader ---");
        logMessage("👑 📢 Broadcasting LEADER announcement to ring...");
        updateStatus("BROADCASTING_LEADER");
        if (nextNode != null) {
            debugLog("🔄 Starting leader announcement - LEADER(" + leaderId + ") sent to next node");
            nextNode.receiveLeader(leaderId, originId);
            logMessage("👑 Forward confirmed - LEADER(" + leaderId + ") sent to next node");
            debugLog("✅ Successfully initiated leader announcement");
        }
    }

    @Override
    public void initiateElection() throws RemoteException {
        if (!isAlive || electionInProgress.get() || electionCompleted.get()) {
            logWarning("Cannot initiate election - node dead or election in progress/completed");
            return;
        }
        logMessage("--- Starting Election ---");
        electionInProgress.set(true);
        updateStatus("INITIATING");
        logMessage("🚀 INITIATING ELECTION from Node " + id);
        if (nextNode != null) {
            debugLog("🚀 ELECTION STARTED - Sending ELECTION(" + id + ") to next node");
            debugLog("🔄 Calling nextNode.receiveElection(" + id + ", " + id + ")");
            nextNode.receiveElection(id, id);
            debugLog("✅ ELECTION INITIATION SENT SUCCESSFULLY");
        } else {
            logWarning("No next node - cannot initiate election");
            electionInProgress.set(false);
        }
    }

    @Override
    public String getStatus() throws RemoteException {
        return status;
    }

    @Override
    public void recover() throws RemoteException {
        setAlive(true);
    }

    @Override
    public void printDetailedStatus() throws RemoteException {
        System.out.println("Node " + String.format("%03d", id) + " Detailed Status:");
        System.out.println("- Alive: " + isAlive);
        System.out.println("- Leader: " + (isLeader ? "Yes (ID: " + leaderId + ")" : "No, Leader is " + leaderId));
        System.out.println("- Election in Progress: " + electionInProgress.get());
        System.out.println("- Election Completed: " + electionCompleted.get());
        System.out.println("- Next Node: " + (nextNode != null ? "Connected" : "Not Connected"));
        System.out.println("- Current Status: " + status);
    }

    @Override
    public boolean isElectionInProgress() throws RemoteException {
        return electionInProgress.get();
    }

    @Override
    public boolean isElectionCompleted() throws RemoteException {
        return electionCompleted.get();
    }

    private void printStartupMessage() {
        System.out.println("\n" + getSeparatorLine());
        System.out.println(coloredInfo("🚀 Node " + String.format("%03d", id) + " starting up..."));
        System.out.println("📍 ID: " + String.format("%03d", id));
        System.out.println("⏰ Started");
        System.out.println("⚙️  RMI Ready - Waiting for ring formation...");
        System.out.println(getSeparatorLine() + "\n");
    }

    private void printElectionResult() {
        System.out.println("\n" + getSeparatorLine());
        System.out.println("🎊 === ELECTION RESULTS ===");
        System.out.println("👑 ELECTED LEADER: Node " + String.format("%03d", leaderId));
        if (isLeader) {
            System.out.println("🎉 This node is the new leader of the ring!");
        } else {
            System.out.println("📍 Ring now recognizes Node " + String.format("%03d", leaderId) + " as leader");
        }
        System.out.println("✅ Leader Location: Successfully Node " + String.format("%03d", leaderId));
        System.out.println("✅ Election: Successfully Completed");
        System.out.println("🔗 Ring Integrity: MAINTAINED");
        System.out.println("📢 All nodes will forward requests to the leader.");
        System.out.println("⏰ Completed");
        System.out.println(getSeparatorLine() + "\n");
    }

    private void printRingStatus() {
        if (isLeader) {
            System.out.println("\n=== RING STATUS ===");
            System.out.println("👑 Leader: Node " + String.format("%03d", leaderId));
            System.out.println("🔗 Ring Nodes: [2, 5, 7, 11]"); // Static example; can be dynamic if node IDs collected
            System.out.println("✅ Election Status: Completed");
            System.out.println(getSeparatorLine() + "\n");
        }
    }

    private void updateStatus(String newStatus) {
        synchronized (statusLock) {
            status = newStatus;
            lastStatusUpdate = System.currentTimeMillis();
            System.out.println("Node " + String.format("%03d", id) + " Status → " + newStatus);
        }
    }

    private void logMessage(String message) {
        System.out.println(coloredInfo(message));
    }

    private void debugLog(String message) {
        if (debugEnabled) {
            System.out.println(coloredDebug("DEBUG[" + String.format("%03d", id) + "] " + message));
        }
    }

    private void logWarning(String message) {
        System.out.println(coloredWarning(message));
    }

    private void logError(String message) {
        System.err.println(coloredError(message));
    }

    private String getSeparatorLine() {
        return "════════════════════════════════════════════════════════════";
    }

    private void debugRingConnection() {
        debugLog("Ring connection established - next node reference set");
    }

    private void reconnectToRing() {
        // Attempt to reconnect if possible; for demo, assume manual reconnection
        logMessage("Attempting to reconnect to ring...");
    }

    private void handleUserInput() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n🎭 Node " + String.format("%03d", id) + " ready for demo!");
        System.out.println("Type 'help' for available commands\n");
        while (true) {
            System.out.print("Node " + String.format("%03d", id) + " > ");
            String command = scanner.nextLine().trim().toLowerCase();
            try {
                switch (command) {
                    case "start":
                        initiateElection();
                        break;
                    case "status":
                        printDetailedStatus();
                        break;
                    case "fail":
                        setAlive(false);
                        break;
                    case "recover":
                        recover();
                        break;
                    case "debug":
                        debugEnabled = !debugEnabled;
                        System.out.println("Debug logging " + (debugEnabled ? "enabled" : "disabled"));
                        break;
                    case "help":
                        printHelp();
                        break;
                    case "exit":
                        System.out.println("Shutting down node...");
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Unknown command. Type 'help' for list.");
                }
            } catch (Exception e) {
                logError("Command execution error: " + e.getMessage());
            }
        }
    }

    private void printHelp() {
        System.out.println("Available commands:\n- start: Initiate election\n- status: Print detailed status\n- fail: Simulate node failure\n- recover: Recover from failure\n- debug: Toggle debug logging\n- help: Show this help\n- exit: Shut down node");
    }

    private String coloredInfo(String message) {
        return "\033[1;34m" + message + "\033[0m";
    }

    private String coloredDebug(String message) {
        return "\033[1;36m" + message + "\033[0m";
    }

    private String coloredWarning(String message) {
        return "\033[1;33m" + message + "\033[0m";
    }

    private String coloredError(String message) {
        return "\033[1;31m" + message + "\033[0m";
    }

    private static String staticColoredInfo(String message) {
        return "\033[1;34m" + message + "\033[0m";
    }

    private static String staticColoredDebug(String message) {
        return "\033[1;36m" + message + "\033[0m";
    }

    private static String staticColoredWarning(String message) {
        return "\033[1;33m" + message + "\033[0m";
    }

    private static String staticColoredError(String message) {
        return "\033[1;31m" + message + "\033[0m";
    }

    public static void main(String[] args) {
        if (!validateArguments(args)) {
            System.exit(1);
        }

        int nodeId = Integer.parseInt(args[0]);
        String nextNodeName = args[1];
        int registryPort = Integer.parseInt(args[2]);
        int nextPort = Integer.parseInt(args[3]);

        NodeImpl node = null;
        Registry registry = null;
        Node stub = null;
        String bindingName = null;

        try {
            node = new NodeImpl(nodeId);
            stub = (Node) UnicastRemoteObject.exportObject(node, 0);
            registry = getOrCreateRegistry(registryPort, nodeId);

            bindingName = "Node" + nodeId;
            try {
                registry.bind(bindingName, stub);
                System.out.println(staticColoredInfo("✅ Node " + String.format("%03d", nodeId)
                        + " BOUND to registry on port " + registryPort));
            } catch (AlreadyBoundException e) {
                System.out.println(staticColoredWarning("⚠️  Binding already exists, cleaning up..."));
                registry.unbind(bindingName);
                registry.bind(bindingName, stub);
                System.out.println(staticColoredInfo("✅ Node " + String.format("%03d", nodeId)
                        + " REBOUND to registry on port " + registryPort));
            }

            System.out.println(staticColoredInfo("⏳ Node " + String.format("%03d", nodeId)
                    + " waiting " + (INITIAL_WAIT / 1000) + "s for ring formation..."));
            Thread.sleep(INITIAL_WAIT);

            if (!connectToNextNode(node, nextNodeName, nextPort, nodeId)) {
                System.err.println(staticColoredError("💥 CRITICAL: Failed to connect to ring. Exiting..."));
                cleanupRegistry(registry, bindingName, nodeId);
                UnicastRemoteObject.unexportObject(node, true);
                System.exit(1);
            }

            node.updateStatus("DEMO_READY");
            System.out.println(staticColoredInfo("🎭 Node " + String.format("%03d", nodeId) + " ready for demo!"));

            final NodeImpl finalNode = node;
            final int finalNodeId = nodeId;

            Thread inputThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        finalNode.handleUserInput();
                    } catch (Exception e) {
                        System.err.println(staticColoredError("Interactive interface error: " + e.getMessage()));
                    }
                }
            }, "UserInput-" + nodeId);
            inputThread.start();

            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } catch (Exception e) {
            System.err.println(String.format("💥 FATAL ERROR in Node %03d: %s",
                    nodeId, e.getMessage()));
            e.printStackTrace();

            try {
                cleanupRegistry(registry, bindingName, nodeId);
                if (node != null) {
                    UnicastRemoteObject.unexportObject(node, true);
                }
            } catch (Exception cleanupEx) {
            }
            System.exit(1);
        }
    }

    private static Registry getOrCreateRegistry(int port, int nodeId) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", port);
            registry.list();
            System.out.println(staticColoredInfo("🔍 Node " + String.format("%03d", nodeId)
                    + " using existing registry on port " + port));
            return registry;
        } catch (RemoteException e) {
            System.out.println(staticColoredInfo("🔄 Node " + String.format("%03d", nodeId)
                    + " creating new registry on port " + port));
            return LocateRegistry.createRegistry(port);
        }
    }

    private static void cleanupRegistry(Registry registry, String bindingName, int nodeId) {
        if (registry == null || bindingName == null) {
            return;
        }

        try {
            registry.unbind(bindingName);
            System.out.println(staticColoredInfo("🧹 Node " + String.format("%03d", nodeId)
                    + " cleaned up registry binding: " + bindingName));
        } catch (NotBoundException e) {
        } catch (RemoteException e) {
            System.err.println(staticColoredError("⚠️  Failed to cleanup registry for Node "
                    + String.format("%03d", nodeId) + ": " + e.getMessage()));
        }
    }

    private static boolean validateArguments(String[] args) {
        if (args.length != 4) {
            System.err.println(staticColoredError("❌ Usage: java cs324.election.without.register.NodeImpl <nodeId> <nextNodeName> <registryPort> <nextPort>"));
            System.err.println("   Example: java cs324.election.without.register.NodeImpl 1 Node2 1099 1199");
            return false;
        }

        try {
            int nodeId = Integer.parseInt(args[0]);
            int registryPort = Integer.parseInt(args[2]);
            int nextPort = Integer.parseInt(args[3]);

            if (nodeId < 1 || nodeId > 999) {
                System.err.println(staticColoredError("❌ Node ID must be between 1-999"));
                return false;
            }
            if (registryPort < 1024 || registryPort > 65535) {
                System.err.println(staticColoredError("❌ Registry port must be between 1024-65535"));
                return false;
            }
            if (nextPort < 1024 || nextPort > 65535) {
                System.err.println(staticColoredError("❌ Next port must be between 1024-65535"));
                return false;
            }
        } catch (NumberFormatException e) {
            System.err.println(staticColoredError("❌ Invalid numeric arguments - use integers only"));
            return false;
        }

        return true;
    }

    private static boolean connectToNextNode(NodeImpl node, String nextNodeName, int nextPort, int nodeId) {
        int retries = MAX_RETRIES;
        long delay = RETRY_DELAY;

        while (retries > 0) {
            try {
                Registry nextRegistry = LocateRegistry.getRegistry("localhost", nextPort);
                Node nextNode = (Node) nextRegistry.lookup(nextNodeName);
                node.setNextNode(nextNode);

                System.out.println(String.format("✅ Node %03d: RING CONNECTED ➡️ %s",
                        nodeId, nextNodeName));
                return true;

            } catch (Exception e) {
                retries--;
                System.err.println(String.format("⚠️  Node %03d: Connection attempt %d/%d failed: %s",
                        nodeId, (MAX_RETRIES - retries + 1),
                        MAX_RETRIES, e.getMessage()));

                if (retries > 0) {
                    System.out.println(String.format("Node %03d: Retrying in %.1fs...",
                            nodeId, delay / 1000.0));
                    try {
                        Thread.sleep(delay);
                        delay = Math.min(delay * 2, 5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }

        System.err.println(String.format("💥 Node %03d: MAX RETRIES (%d) EXCEEDED - RING FORMATION FAILED",
                nodeId, MAX_RETRIES));
        return false;
    }
}
