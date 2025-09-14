/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cs324.election.without.register;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * Implementation of the Node interface for LCR leader election. This class
 * handles the core LCR protocol logic, where nodes forward or replace election
 * messages based on ID comparisons. The highest ID circulates the ring,
 * declares itself leader, and announces to all.
 *
 * @author sione.likiliki
 */
public class NodeImpl extends UnicastRemoteObject implements Node {

    private final int id;
    private int leaderId = -1;
    private Node nextNode;
    private boolean isLeader = false;
    private boolean hasVoted = false;
    private boolean isAlive = true;
    private final long networkDelay = 3000; // 3 seconds for demo visibility
    private final Object electionLock = new Object();
    private boolean electionComplete = false;

    /**
     * Constructor for a node with configurable network delay.
     *
     * @param nodeId The unique ID of the node.
     * @throws RemoteException If export fails.
     */
    public NodeImpl(int nodeId) throws RemoteException {
        this.id = nodeId;
    }

    @Override
    public int getId() throws RemoteException {
        return id;
    }

    @Override
    public void setNextNode(Node nextNode) throws RemoteException {
        this.nextNode = nextNode;
    }

    @Override
    public void setAlive(boolean alive) throws RemoteException {
        isAlive = alive;
    }

    @Override
    public int receiveElection(int uid, int initiatorId) throws RemoteException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String timestamp = sdf.format(new Date());
        System.out.println(timestamp + ": Node " + id + " received ELECTION(" + uid + ") from initiator " + initiatorId);

        try {
            Thread.sleep(networkDelay); // Simulate network delay for demo
        } catch (InterruptedException e) {
            return -1;
        }

        if (nextNode == null || !isAlive) {
            return 1; // Skip dead nodes or null successors silently
        }

        if (uid == id) {
            System.out.println(timestamp + ": Node " + id + " (started by " + initiatorId + ") completed circuit. Declaring leader " + uid + ".");
            leaderId = uid;
            isLeader = true;
            electionComplete = true;
            nextNode.receiveLeader(uid, id);
            return 1;
        } else if (uid < id) {
            System.out.println(timestamp + ": Node " + id + " replacing ELECTION(" + uid + ") with " + id + " and forwarding.");
            nextNode.receiveElection(id, initiatorId);
        } else {
            System.out.println(timestamp + ": Node " + id + " forwarding higher ELECTION(" + uid + ").");
            nextNode.receiveElection(uid, initiatorId);
        }
        return 1;
    }

    @Override
    public int receiveLeader(int leaderId, int originId) throws RemoteException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String timestamp = sdf.format(new Date());
        System.out.println(timestamp + ": Node " + id + " received LEADER(" + leaderId + ") from origin " + originId);

        if (nextNode == null || !isAlive) {
            return 1; // Skip dead nodes or null successors silently
        }

        if (this.id == originId) {
            System.out.println(timestamp + ": Node " + id + " completed leader announcement.");
            electionComplete = true;
            return 1;
        }

        this.leaderId = leaderId;
        if (this.id == leaderId) {
            isLeader = true;
        }
        nextNode.receiveLeader(leaderId, originId);
        return 1;
    }

    @Override
    public void initiateElection() throws RemoteException {
        synchronized (electionLock) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String timestamp = sdf.format(new Date());
            System.out.println(timestamp + ": Node " + id + " initiating election.");
            if (nextNode != null) {
                nextNode.receiveElection(id, id);
            }
        }
    }

    private void handleUserInput() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Type 'start' to initiate the election, or 'exit' to quit:");
            String command = scanner.nextLine().trim();

            if (command.equalsIgnoreCase("start")) {
                try {
                    initiateElection();
                } catch (RemoteException e) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    String timestamp = sdf.format(new Date());
                    System.out.println(timestamp + ": Node " + id + ": Error initiating election: " + e.getMessage());
                }
            } else if (command.equalsIgnoreCase("exit")) {
                System.out.println("Exiting...");
                System.exit(0);
            } else {
                System.out.println("Invalid command.");
            }
            if (electionComplete) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                String timestamp = sdf.format(new Date());
                System.out.println(timestamp + ": Election completed. Leader is " + leaderId + ".");
                electionComplete = false;
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: java cs324.election.without.register.NodeImpl <nodeId> <nextNodeName> <registryPort> <nextPort>");
            return;
        }

        try {
            int nodeId = Integer.parseInt(args[0]);
            String nextNodeName = args[1];
            int registryPort = Integer.parseInt(args[2]);
            int nextPort = Integer.parseInt(args[3]);

            NodeImpl node = new NodeImpl(nodeId);

            Registry registry = LocateRegistry.createRegistry(registryPort);
            registry.bind("Node" + nodeId, node);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String timestamp = sdf.format(new Date());
            System.out.println(timestamp + ": Node " + nodeId + ": Registered");

            // Initial delay to allow all nodes to start and bind
            System.out.println(timestamp + ": Node " + nodeId + ": Waiting 10 seconds for all nodes to initialize...");
            Thread.sleep(10000); // 10-second delay

            // Retry Mechanism for looking up the next node
            int retries = 10;
            int delay = 2000;

            while (retries > 0) {
                try {
                    Registry nextRegistry = LocateRegistry.getRegistry("localhost", nextPort);
                    node.setNextNode((Node) nextRegistry.lookup(nextNodeName));
                    System.out.println(timestamp + ": Node " + nodeId + ": Connected to " + nextNodeName);
                    break;
                } catch (Exception e) {
                    retries--;
                    System.err.println(timestamp + ": Node " + nodeId + ": Failed to lookup next node: " + e.getMessage());
                    if (retries > 0) {
                        System.out.println(timestamp + ": Node " + nodeId + ": Retrying in " + (delay / 1000) + " seconds...");
                        Thread.sleep(delay);
                    } else {
                        System.err.println(timestamp + ": Node " + nodeId + ": Max retries reached. Exiting...");
                        System.exit(1);
                    }
                }
            }

            node.handleUserInput();
        } catch (Exception e) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String timestamp = sdf.format(new Date());
            System.err.println(timestamp + ": Error in main for Node " + args[0] + ": " + e.getMessage());
        }
    }
}
