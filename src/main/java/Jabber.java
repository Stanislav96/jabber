import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import java.util.Collection;

public class Jabber {
  private AbstractXMPPConnection conn;
  private Chat chat = null;

  private class Listener implements MessageListener {
    public void processMessage(Chat chat, Message message) {
      System.out.println("<< " + message);
    }
  }

  public Jabber() {
    ChatManager.getInstanceFor(conn).addChatListener((chat, createdLocally) -> {
      if (!createdLocally) {
        chat.addMessageListener(new Listener());
      }
    });
    //Roster.getInstanceOf().
    conn.getRoster().addRosterListener(new RosterListener() {
      public void entriesAdded(Collection<String> addresses) {}
      public void entriesDeleted(Collection<String> addresses) {}
      public void entriesUpdated(Collection<String> addresses) {}
      public void presenceChanged(Presence presence) {
        System.out.println("<< " + presence.getFrom() + " " + presence);
      }
    });
  }

  public void login(String user, String pass, String serv) throws Exception {
    conn = new XMPPTCPConnection(user, pass, serv);
    conn.connect().login();
  }

  public void chat(String user) {
    chat = ChatManager.getInstanceFor(conn).createChat(user, new Listener());
  }

  public void sendMsg(String msg) throws Exception {
    if (chat != null) {
      chat.sendMessage(msg);
    }
  }

  public void sendStatus(String status) throws Exception {
    if (status.equals("here")) {
      //conn.sendStanza(new Presence(Presence.Type.available));
      conn.sendPacket(new Presence(Presence.Type.available));
    }
    if (status.equals("away")) {
      //conn.sendStanza(new Presence(Presence.Type.unavailable));
      conn.sendPacket(new Presence(Presence.Type.unavailable));
    }
  }
}
