import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.StringJoiner;

public class Jabber {
  private Socket socket;
  private BufferedWriter writer;
  private InputStream in;
  private String user;
  private String pass;
  private String receiver = "";
  volatile private boolean logged = false;
  private boolean connected = false;
  private boolean closed = false;

  private class ListenerThread extends Thread {
    private InputStream inForParser;
    private InputStream in;
    private boolean message = false;
    private boolean body = false;
    private boolean presence = false;
    private boolean show = false;
    private boolean mechanisms = false;
    private boolean mechanism = false;
    private boolean hasMechanisms = false;
    private boolean hasPlain = false;
    private boolean features = false;
    private String sender;
    private SAXParser parser;

    private class Listener extends DefaultHandler {
      @Override
      public void skippedEntity(String name) throws SAXException {}

      @Override
      public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws
          SAXException {
        System.out.println("begin Elem: "+qName);
        if (qName.equals("message")) {
          message = true;
          sender = atts.getValue("from");
          if (sender == null) {
            throw new SAXException("no from");
          }
          return;
        }
        if (qName.equals("body") && message) {
          body = true;
          return;
        }
        if (qName.equals("presence")) {
          presence = true;
          sender = atts.getValue("from");
          if (sender == null) {
            throw new SAXException("no from");
          }
          return;
        }
        if (qName.equals("show") && presence) {
          body = true;
          return;
        }
        if (qName.equals("stream:features")) {
          features = true;
          return;
        }
        if (qName.equals("starttls") && features) {
          try {
            writer.write("<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>");
            writer.flush();
          } catch (IOException e) {
            e.printStackTrace();
            close();
          }
          return;
        }
        if (qName.equals("proceed")) {
          try {
            parser.reset();
            writer.write("<?xml version=\"1.0\"?>" +
                "<stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" " +
                "version=\"1.0\" "+
                "xmlns=\"jabber:client\" " +
                "to=\"jabber.zone\" " +
                "xml:lang=\"ru\" " +
                "xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">");
            writer.flush();
          } catch (IOException e) {
            e.printStackTrace();
            close();
          }
          return;
        }
        if (qName.equals("mechanisms") && features) {
          hasMechanisms = true;
          mechanisms = true;
          return;
        }
        if (qName.equals("mechanism") && mechanisms) {
          mechanism = true;
          return;
        }
        if (qName.equals("success")) {
          logged = true;
        }
      }

      @Override
      public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        System.out.println("end Elem: "+ qName);
        if (qName.equals("message")) {
          message = false;
          return;
        }
        if (qName.equals("body") && message) {
          body = false;
          return;
        }
        if (qName.equals("presence")) {
          presence = false;
          return;
        }
        if (qName.equals("show") && presence) {
          show = false;
          return;
        }
        if (qName.equals("stream:features")) {
          features = false;
          if (!hasMechanisms) {
            logged = true;
          }
          return;
        }
        if (qName.equals("mechanisms") && features) {
          if (!hasPlain) {
            throw new SAXException("no plain auth");
          }
          mechanisms = false;
          return;
        }
        if (qName.equals("mechanism") && mechanisms) {
          mechanism = false;
          return;
        }
        if (qName.equals("success")) {
          try {
            parser.reset();
            writer.write("<?xml version=\"1.0\"?>" +
                "<stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" " +
                "version=\"1.0\" "+
                "xmlns=\"jabber:client\" " +
                "to=\"jabber.zone\" " +
                "xml:lang=\"ru\" " +
                "xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">");
            writer.flush();
          } catch (IOException e) {
            e.printStackTrace();
            close();
            return;
          }
          logged = true;
        }
      }

      @Override
      public void characters(char[] ch, int start, int length) throws SAXException {
        System.out.println("characters: "+new String(ch, start, length));
        if (body) {
          System.out.println("<< new msg from" + sender + "\n" + "<< " + new String(ch, start, length));
          return;
        }
        if (show) {
          System.out.println("<< " + sender + " is now " + new String(ch, start, length));
          return;
        }
        if (mechanism) {
          if (new String(ch, start, length).equals("PLAIN")) {
            hasPlain = true;
            Charset charset = Charset.forName("US-ASCII");
            String code = Base64.getEncoder()
                .encodeToString(charset.encode(user.split("@")[0]).put((byte) 0).put(charset.encode(pass)).array());
            try {
              writer.write("<auth " +
                  "xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" " +
                  "mechanism=\"PLAIN\">" +
                  code +
                  "</auth>");
              writer.flush();
            } catch (IOException e) {
              e.printStackTrace();
              close();
            }
          }
        }
      }
    }

    ListenerThread(InputStream in) {
      this.in = in;
    }

    public void run() {
      SAXParserFactory spf = SAXParserFactory.newInstance();
      //XMLReader reader;
      try {
        parser = spf.newSAXParser();
        //reader = XMLReaderFactory.createXMLReader();
        //reader.setContentHandler(new Listener());
      } catch (SAXException | ParserConfigurationException e) {
        e.printStackTrace();
        close();
        return;
      }
      try {
        //reader.parse(new InputSource(in));
        parser.parse(in, new Listener());
      } catch (SAXException | IOException e) {
        e.printStackTrace();
        close();
      }
    }
  }

  public Jabber() {
    try {
      socket = new Socket("jabber.zone", 5222);
      in = socket.getInputStream();
      writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    } catch (IOException e) {
      e.printStackTrace();
      close();
      return;
    }
    connected = true;
  }

  public boolean isConnected() {
    return connected;
  }

  public void login(String user, String pass) throws Exception {
    this.user = user;
    this.pass = pass;
    writer.write("<?xml version=\"1.0\"?>" +
        "<stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" " +
        "version=\"1.0\" "+
        "xmlns=\"jabber:client\" " +
        "to=\"jabber.zone\" " +
        "xml:lang=\"ru\" " +
        "xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">");
    writer.flush();
    Thread thr = new ListenerThread(in);
    thr.setDaemon(true);
    thr.start();
  }

  public void receiver(String user) throws Exception {
    receiver = user;
  }

  public void sendMsg(String msg) throws Exception {
    if (!logged) {
      throw new Exception("not logged in");
    }
    if (receiver.isEmpty()) {
      throw new Exception("no receiver");
    }
    writer.write("<message" +
        "from = >" + user +
        " to = " + receiver +
        "<body>" +
        msg +
        "</body>" +
        "</message>");
    writer.flush();
  }

  public void sendStatus(String status) throws Exception {
    if (!logged) {
      throw new Exception("not logged in");
    }
    writer.write("<presence>" +
        "<show>" +
        status +
        "</show>" +
        "</presence>");
    writer.flush();
  }

  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
   try {
     if (connected) {
       connected = false;
       writer.write("</stream>");
       writer.flush();
     }
   } catch (IOException e) {
     e.printStackTrace();
   } finally {
     try {
       if (socket != null) {
         socket.close();
       }
     } catch (IOException e) {
       e.printStackTrace();
     }
   }
  }
}
/*
input baklanov.stas@jabber.zone 123ghj
 */