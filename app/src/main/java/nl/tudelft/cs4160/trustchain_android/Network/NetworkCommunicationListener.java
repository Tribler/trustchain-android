package nl.tudelft.cs4160.trustchain_android.Network;

import java.io.IOException;
import java.net.InetSocketAddress;

import nl.tudelft.cs4160.trustchain_android.appToApp.PeerAppToApp;
import nl.tudelft.cs4160.trustchain_android.appToApp.PeerHandler;
import nl.tudelft.cs4160.trustchain_android.appToApp.connection.messages.BlockMessage;
import nl.tudelft.cs4160.trustchain_android.appToApp.connection.messages.IntroductionRequest;
import nl.tudelft.cs4160.trustchain_android.appToApp.connection.messages.IntroductionResponse;
import nl.tudelft.cs4160.trustchain_android.appToApp.connection.messages.Message;
import nl.tudelft.cs4160.trustchain_android.appToApp.connection.messages.MessageException;
import nl.tudelft.cs4160.trustchain_android.appToApp.connection.messages.Puncture;
import nl.tudelft.cs4160.trustchain_android.appToApp.connection.messages.PunctureRequest;
import nl.tudelft.cs4160.trustchain_android.connection.CommunicationListener;

/**
 * Created by michiel on 12-1-2018.
 */

public interface NetworkCommunicationListener extends CommunicationListener {
    void updateInternalSourceAddress(String address);
    void updatePeerLists();
    void updateWan(Message message) throws MessageException;
    void updateConnectionType(int connectionType, String typename, String subtypename);
    PeerAppToApp getOrMakePeer(String id, InetSocketAddress address, boolean incoming);
    PeerHandler getPeerHandler();
}
