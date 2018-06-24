package nl.tudelft.cs4160.trustchain_android.stresstest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import nl.tudelft.cs4160.trustchain_android.crypto.DualSecret;
import nl.tudelft.cs4160.trustchain_android.crypto.Key;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;
import nl.tudelft.cs4160.trustchain_android.network.Network;
import nl.tudelft.cs4160.trustchain_android.network.NetworkStatusListener;
import nl.tudelft.cs4160.trustchain_android.peer.Peer;
import nl.tudelft.cs4160.trustchain_android.peer.PeerHandler;
import nl.tudelft.cs4160.trustchain_android.peer.PeerListener;
import nl.tudelft.cs4160.trustchain_android.statistics.StatisticsServer;
import nl.tudelft.cs4160.trustchain_android.storage.sharedpreferences.BootstrapIPStorage;

public class StressTestNode implements PeerListener, NetworkStatusListener {

    private static final String TAG = StressTestNode.class.getName();
    private Network network;
    private PeerHandler peerHandler;
    private String wan = "";
    public final static String CONNECTABLE_ADDRESS = "130.161.211.254";
    public final static int DEFAULT_PORT = 1873;
    private final static int BUFFER_SIZE = 65536;
    private Context context;

    public String userName;
    private DualSecret keyPair;
    private boolean networkRunning;

    int port;

    public void startNode () {
        initVariables();
        addInitialPeer();
        startListenThread();
        startSendThread();
        StatisticsServer.getInstance().start(this);

        Runnable refreshTask = () -> {
            while(true) {
                peerHandler.removeDeadPeers();
                peerHandler.splitPeerList();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(refreshTask).start();
    }

    public StressTestNode (Context context, int port) {
        this.context = context;
        this.userName = UsernameGenerator.getUsername();
        this.keyPair = Key.createNewKeyPair();
        this.port = port;
    }

    /**
     * Add the initial hard-coded connectable inboxItem to the inboxItem list.
     */
    public void addInitialPeer() {
        String address = BootstrapIPStorage.getIP(context);
        CreateInetSocketAddressTask createInetSocketAddressTask = new CreateInetSocketAddressTask(this);
        try {
            if (address != null && !address.equals("")) {
                createInetSocketAddressTask.execute(address, String.valueOf(DEFAULT_PORT));
            } else {
                createInetSocketAddressTask.execute(CONNECTABLE_ADDRESS, String.valueOf(DEFAULT_PORT));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


//    /**
//     * Initialize the inboxItem lists.
//     */
//    private void initPeerLists() {
//        ListView incomingPeerConnectionListView = findViewById(R.id.incoming_peer_connection_list_view);
//        ListView outgoingPeerConnectionListView = findViewById(R.id.outgoing_peer_connection_list_view);
//        incomingPeerAdapter = new PeerListAdapter(getApplicationContext(), R.layout.peer_connection_list_item, peerHandler.getIncomingList(), (CoordinatorLayout) findViewById(R.id.myCoordinatorLayout));
//        incomingPeerConnectionListView.setAdapter(incomingPeerAdapter);
//        outgoingPeerAdapter = new PeerListAdapter(getApplicationContext(), R.layout.peer_connection_list_item, peerHandler.getOutgoingList(), (CoordinatorLayout) findViewById(R.id.myCoordinatorLayout));
//        outgoingPeerConnectionListView.setAdapter(outgoingPeerAdapter);
//    }
    /**
     * Initialize all local variables
     * If this activity is opened with a saved instance state
     * we load the list of peers from this saved state.
//     * @param savedInstanceState
     */
    private void initVariables() {
        networkRunning = true;
        peerHandler = new PeerHandler(keyPair.getPublicKeyPair(), userName);
        getPeerHandler().setPeerListener(this);

        network = new Network(userName, keyPair.getPublicKeyPair(), context, port);
        network.getMessageHandler().setPeerHandler(getPeerHandler());
        network.setNetworkStatusListener(this);
        updateConnectionType(network.getConnectionTypeString((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)));
//        ((TextView) findViewById(R.id.peer_id)).setText(peerHandler.getHashId());
    }

    /**
//     * Start the thread send thread responsible for sending a {@link IntroductionRequest} to a random inboxItem every 5 seconds.
     */
    private void startSendThread() {
        Thread sendThread = new Thread(() -> {
            boolean snackbarVisible = false;
//            View view = findViewById(android.R.id.content);
//            Snackbar networkUnreachableSnackbar = Snackbar.make(view, "Network unavailable", Snackbar.LENGTH_INDEFINITE);

            // wait max one second for the CreateInetSocketAddressTask to finish, indicated by that the bootstrap is added to the peerlist
            int t = 0;
            while(peerHandler.size() == 0 && t < 100) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                t++;
            }

            while(!Thread.interrupted() && networkRunning) {
                try {
                    // update connection type and internal ip address
                    updateConnectionType(network.getConnectionTypeString((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)));
                    network.showLocalIpAddress();
                    if (peerHandler.size() > 0) {
                        // select 10 random peers to send an introduction request to
                        int limit = 10;
                        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
                        lock.readLock().lock();
                        List<Peer> connectedPeers = new ArrayList<>(peerHandler.getPeerList());
                        lock.readLock().unlock();
                        if(connectedPeers.size() <= limit) {
                            for(Peer peer : connectedPeers){
                                network.sendIntroductionRequest(peer);
                            }
                        } else {
                            Random rand = new Random();
                            for (int i = 0; i < limit; i++) {
                                int index = rand.nextInt(connectedPeers.size());
                                network.sendIntroductionRequest(connectedPeers.get(index));
                                connectedPeers.remove(index);
                            }
                        }
                    }
                    // if the network is reachable again, remove the snackbar
                } catch (SocketException e) {
                    Log.i(TAG, "network unreachable");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "Send thread stopped");
        });
        sendThread.start();
        Log.d(TAG, "Send thread started");
    }


    /**
     * Update the showed inboxItem lists.
     * First split into new peers and the active list
     * Then remove the peers that aren't responding for a long time.
     */
    @Override
    public void updatePeerLists() {
//        new Handler(Looper.getMainLooper()).post(new Runnable() {
//            @Override
//            public void run() {
//                synchronized (this) {
//
//                }
//            }
//        });
    }

    /**
     * Start the listen thread. The thread opens a new {@link DatagramChannel} and calls {@link Network#dataReceived(Context, ByteBuffer,
     * InetSocketAddress)} for each incoming datagram.
     */
    private void startListenThread() {
        Thread listenThread = new Thread(() -> {
            try {
                ByteBuffer inputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
                while (!Thread.interrupted() && networkRunning) {
                    inputBuffer.clear();
                    SocketAddress address = network.receive(inputBuffer);
                    inputBuffer.flip();
                    network.dataReceived(context, inputBuffer, (InetSocketAddress) address);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "Listen thread stopped");
        });
        listenThread.start();
        Log.d(TAG, "Listen thread started");
    }

    /**
     * Update wan address
     * @param message a message that was received, the destination is our wan address
     */
    public void updateWan(MessageProto.Message message) throws UnknownHostException {
        byte[] da = message.getDestinationAddress().toByteArray();
        InetAddress addr = InetAddress.getByAddress(da);
        int port = message.getDestinationPort();
        InetSocketAddress socketAddress = new InetSocketAddress(addr, port);

        if (peerHandler.getWanVote().vote(socketAddress)) {
            wan = peerHandler.getWanVote().getAddress().toString();
        }
    }

    @Override
    public void updateConnectionType(String connectionTypeStr) {
        // No UI -> do nothing
    }

    @Override
    public void updateInternalSourceAddress(String address) {
        // No UI -> do nothing
    }

//
//    @Override
//    public void handleIntroductionRequest(Peer peer, MessageProto.IntroductionRequest request) throws IOException {
//        messagesReceived++;
//        introductionRequestsReceived++;
//
//        peer.setConnectionType((int) request.getConnectionType());
//        if (getPeerHandler().size() > 1) {
//            Peer invitee = getPeerHandler().getEligiblePeer(peer);
//            if (invitee != null) {
//                network.sendIntroductionResponse(peer, invitee);
//                messagesSent++;
//                introductionResponsesSent++;
//                statistics.messageSent();
//                network.sendPunctureRequest(invitee, peer);
//                messagesSent++;
//                puncturesSent++;
//                statistics.messageSent();
//                Log.d("Network", "Introducing " + invitee.getAddress() + " to " + peer.getAddress());
//            }
//        } else {
//            Log.d("Network", "Peerlist too small, can't handle introduction request");
//            network.sendIntroductionResponse(peer, null);
//            messagesSent++;
//            introductionResponsesSent++;
//            statistics.messageSent();
//        }
//    }
//
//    @Override
//    public void handleIntroductionResponse(Peer peer, MessageProto.IntroductionResponse response) throws Exception {
//        messagesReceived++;
//        introductionResponsesReceived++;
//        statistics.messageReceived();
//
//        peer.setConnectionType((int) response.getConnectionType());
//        peer.setNetworkOperator(response.getNetworkOperator());
//        List<ByteString> pex = response.getPexList();
//        for (ByteString pexPeer : pex) {
//            Peer p = Peer.deserialize(pexPeer.toByteArray());
//            Log.d(TAG, "From " + peer + " | found peer in pexList: " + p);
//
//            getPeerHandler().getOrMakePeer(p.getPeerId(), p.getAddress());
//        }
//    }
//
//    @Override
//    public void handlePunctureRequest(Peer peer, MessageProto.PunctureRequest request) throws IOException {
//        messagesReceived++;
//        puncturesReceived++;
//        statistics.messageReceived();
//
//        Peer puncturePeer = null;
//        try {
//            puncturePeer = Peer.deserialize(request.getPuncturePeer().toByteArray());
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//        if (!getPeerHandler().peerExistsInList(puncturePeer)) {
//            network.sendPuncture(puncturePeer);
//            puncturesSent++;
//            statistics.messageSent();
//        }
//    }
//
//    @Override
//    public void handleReceivedBlock(Peer peer, MessageProto.TrustChainBlock block) throws IOException {
//        // Blocks are ignored
//        messagesReceived++;
//        blockMessagesReceived++;
//        statistics.messageReceived();
//    }
//
//    @Override
//    public void handleCrawlRequest(Peer peer, MessageProto.CrawlRequest request) throws IOException {
//        // Crawl requests are ignored
//        messagesReceived++;
//        crawlRequestsReceived++;
//        statistics.messageReceived();
//    }
//
//    @Override
//    public void handlePuncture(Peer peer, MessageProto.Puncture puncture) throws IOException {
//        messagesReceived++;
//        puncturesReceived++;
//        statistics.messageReceived();
//    }

    /**
     * Return the peer handler object.
     * @return
     */
    @Override
    public PeerHandler getPeerHandler() {
        return peerHandler;
    }

    @Override
    public String getName() {
        return userName;
    }

    @Override
    public void updateActivePeers() {

    }

    @Override
    public void updateNewPeers() {

    }

    /**
     * Asynctask to create the inetsocketaddress since network stuff can no longer happen on the main thread in android v3 (honeycomb).
     */
    private static class CreateInetSocketAddressTask extends AsyncTask<String, Void, InetSocketAddress> {
        private WeakReference<StressTestNode> activityReference;

        CreateInetSocketAddressTask(StressTestNode context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected InetSocketAddress doInBackground(String... params) {
            InetSocketAddress inetSocketAddress = null;
            StressTestNode activity = activityReference.get();
            if (activity == null) return null;

            try {
                InetAddress connectableAddress = InetAddress.getByName(params[0]);
                int port = Integer.parseInt(params[1]);
                inetSocketAddress = new InetSocketAddress(connectableAddress, port);

                activity.peerHandler.addPeer(inetSocketAddress, null,null);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            return inetSocketAddress;
        }
    }

    public void stopNode() {
        network.closeChannel();
        networkRunning = false;
    }
}
