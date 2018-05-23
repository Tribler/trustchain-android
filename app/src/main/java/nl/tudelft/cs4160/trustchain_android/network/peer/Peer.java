package nl.tudelft.cs4160.trustchain_android.network.peer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import nl.tudelft.cs4160.trustchain_android.main.OverviewConnectionsActivity;

/**
 * The peer object that is used to find other connected peers in the network.
 * The peer is identified by its unique peer id, which is the chosen username, and keeps track of the last send and receive time.
 */
public class Peer implements Serializable {
    final private static int TIMEOUT = 15000;
    final private static int REMOVE_TIMEOUT = 25000;
    private InetSocketAddress address;
    private String peerId;
    private boolean hasReceivedData; // still here for backwards compatibility
    private boolean hasSentData; // still here for backwards compatibility
    private int connectionType = -1;
    private String networkOperator = ""; // still here for backwards compatibility
    private long lastSendTime = -1;
    private long lastReceiveTime = -1;
    private long creationTime;
    static final long serialVersionUID = 3246968294284429472L; // for backwards compatibility, note that this will cause crashes in the deserialization function if not properly updated on changes

    /**
     * Create a peer.
     *
     * @param peerId  its unique id.
     * @param address its address.
     */
    public Peer(String peerId, InetSocketAddress address) {
        this.peerId = peerId;
        this.address = address;
        this.creationTime = System.currentTimeMillis();
    }

    public long getCreationTime() {
        return creationTime;
    }

    public int getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(int connectionType) {
        this.connectionType = connectionType;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    /**
     * If the lastReceiveTime set is set to some time we have received something
     * @return
     */
    public boolean isReceivedFrom() {
        return lastReceiveTime != -1;
    }

    /**
     * If the lastSentTime set is set to some time we have sent something
     * @return
     */
    public boolean isSentTo() {
        return lastSendTime != -1;
    }

    public int getPort() {
        return address.getPort();
    }

    public InetAddress getExternalAddress() {
        if (address != null) {
            return address.getAddress();
        }
        return null;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    /**
     * Method called when data is sent to this peer.
     */
    public void sentData() {
        lastSendTime = System.currentTimeMillis();
    }

    /**
     * Method called when data is received from this peer.
     *
     * @param buffer the received data.
     */
    public void received(ByteBuffer buffer) {
        lastReceiveTime = System.currentTimeMillis();
    }

    /**
     * Calculates whether this peer is alive: the peer is alive when the peer hasn't send data yet, or when data is received within the
     * timeout after sending data.
     *
     * @return
     */
    public boolean isAlive() {
        if (isReceivedFrom()) {
            return System.currentTimeMillis() - lastReceiveTime < TIMEOUT;
        }
        return true;
    }

    /**
     * If a peer has sent data, but the last time it has sent is longer ago than the remove timeout, it can be removed.
     * If we are trying to connect to a peer, but we haven't gotten a response within the given timeout, it can be removed.
     * Never remove the bootstrap peer.
     * @return
     */
    boolean canBeRemoved() {
        if(isBootstrap()) {
            return false;
        }
        if (isReceivedFrom()) {
            return System.currentTimeMillis() - lastReceiveTime > REMOVE_TIMEOUT;
        }
        if (isSentTo()) {
            return System.currentTimeMillis() - creationTime > REMOVE_TIMEOUT;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Peer{" +
                "address=" + address +
                ", peerId='" + peerId + '\'' +
                ", isReceivedFrom=" + isReceivedFrom() +
                ", connectionType=" + connectionType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Peer peer = (Peer) o;

        if (address != null ? !address.equals(peer.address) : peer.address != null) return false;
        return peerId != null ? peerId.equals(peer.peerId) : peer.peerId == null;

    }

    @Override
    public int hashCode() {
        int result = address != null ? address.hashCode() : 0;
        result = 31 * result + (peerId != null ? peerId.hashCode() : 0);
        return result;
    }

    public long getLastSentTime() {
        return lastSendTime;
    }

    public long getLastReceivedTime() {
        return lastReceiveTime;
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    public static Peer deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return (Peer) is.readObject();
    }

    /**
     * Checks if this peer is the bootstrap address.
     * @return
     */
    public boolean isBootstrap() {
        try {
            if(InetAddress.getByName(OverviewConnectionsActivity.CONNECTABLE_ADDRESS).equals(address.getAddress())){
                return true;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return false;
    }
}
