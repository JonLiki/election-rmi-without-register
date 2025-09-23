/*
 * Click https://netbeans.org/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click https://netbeans.org/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cs324.election.without.register;

/**
 * Remote interface for nodes in the LCR leader election protocol. Enhanced for
 * demo presentation with additional status and control methods.
 */
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Node extends Remote {

    int receiveElection(int uid, int initiatorId) throws RemoteException;

    int receiveLeader(int leaderId, int originId) throws RemoteException;

    int getId() throws RemoteException;

    void setNextNode(Node nextNode) throws RemoteException;

    void initiateElection() throws RemoteException;

    void setAlive(boolean alive) throws RemoteException;

    // Demo enhancement methods
    String getStatus() throws RemoteException;

    void recover() throws RemoteException;

    void printDetailedStatus() throws RemoteException;

    boolean isElectionInProgress() throws RemoteException;

    boolean isElectionCompleted() throws RemoteException;
}
