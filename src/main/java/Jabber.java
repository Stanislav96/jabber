import org.xml.sax.*;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Base64;

class Jabber {
  private Socket socket;
  private BufferedWriter writer;
  private InputStream in;
  private String user;
  private String pass;
  private String receiver = "";
  volatile private boolean logged = false;
  volatile private boolean closed = false;
  volatile private String userRes;

  private class ListenerThread extends Thread {
    private boolean message = false;
    private boolean body = false;
    private boolean presence = false;
    private boolean show = false;
    private boolean mechanisms = false;
    private boolean mechanism = false;
    private boolean hasMechanisms = false;
    private boolean hasPlain = false;
    private boolean features = false;
    private boolean auth = false;
    private boolean bind = false;
    private boolean jid = false;
    private String sender;
    //private SAXParser parser;

    /*private class JabberInputStream extends InputStream {
      private final byte[] closeTag;
      private int index;

      public JabberInputStream() throws IOException {
        closeTag = ("</stream:stream>").getBytes();
        index = closeTag.length;
      }

      public void startCloseTag() {
        index = 0;
      }

      public int read(byte[] c, int start, int length) throws IOException {
        int i;
        for (i = index; i < closeTag.length && i < index + length; ++i) {
          System.out.print((char) closeTag[i]);
          c[start + i - index] = closeTag[i];
          if (i == closeTag.length - 1) {
            shouldReset = true;
          }
        }
        int numRead = i - index;
        int r = in.read(c, start + numRead, length - numRead);
        for (int j = start + numRead; j < start + numRead + r; j++) {
          System.out.print((char) c[j]);
        }
        index = i;
        return r + numRead;
      }

      public int read() throws IOException {
        if (index == closeTag.length) {
          int r = in.read();
          System.out.print((char) r);
          return r;
          //return in.read();
        }
        if (index == closeTag.length - 1) {
          shouldReset = true;
        }
        System.out.print((char) closeTag[index]);
        return closeTag[index++];
      }
    }*/

    private class /*MyParserWithBlackjackAndWhores*/ /*MyParserWithoutStupidChecksWhetherXMLIsWellFormed*/
    JabberXMLReader implements XMLReader {
      private ContentHandler handler;

      public void parse(String systemId) throws IOException, SAXException {
        throw new SAXException("not supported");
      }
      public void parse(InputSource in) throws IOException, SAXException {
        throw new SAXException("not supported");
      }
      public boolean getFeature(String name)
          throws SAXNotRecognizedException, SAXNotSupportedException {
        return false;
      }
      public void setFeature(String name, boolean value)
          throws SAXNotRecognizedException, SAXNotSupportedException {
      }
      public ContentHandler getContentHandler() {
        return handler;
      }
      public void setContentHandler(ContentHandler handler) {
        this.handler = handler;
      }
      public DTDHandler getDTDHandler() {
        return null;
      }
      public void setDTDHandler(DTDHandler handler) {
      }
      public EntityResolver getEntityResolver() {
        return null;
      }
      public void setEntityResolver(EntityResolver resolver) {
      }
      public ErrorHandler getErrorHandler() {
        return null;
      }
      public void setErrorHandler(ErrorHandler handler) {
      }
      public Object getProperty(String name)
          throws SAXNotRecognizedException, SAXNotSupportedException {
        return null;
      }
      public void setProperty(String name, Object value)
          throws SAXNotRecognizedException, SAXNotSupportedException {}

      public void parse() throws IOException, SAXException {
        while (!closed) {
          int avail = in.available();
          while (avail == 0) {
            avail = in.available();
          }
          byte[] buf = new byte[avail];
          if (in.read(buf, 0, avail) != avail) {
            throw new SAXException("can't read correctly");
          }
          parseString(new String(buf, 0, avail));
        }
      }

      public void parseString(String s) throws SAXException {
        if (s != null) {
          handler.startDocument();
          //System.out.print(s);
          parse(s, 0);
          handler.endDocument();
        }
      }

      public int parse(String s, int begin) throws SAXException {
        int index = begin;
        if (s == null) {
          return 0;
        }
        while (index != s.length()) {
          index = this.skipWhitespaces(s, index);
          if (index > s.length() - 1) {
            break;
          }
          if (s.charAt(index) == '<') {
            ++index;
            if (s.charAt(index) == '?') {
              index = s.indexOf('>', index) + 1;
            }
            else if (s.charAt(index) == '/') {
              int beg = index;
              index = s.indexOf('>', index) + 1;
              String tagName = s.substring(beg + 1, index - 1);
              handler.endElement(tagName, tagName, tagName);
            }
            else if (s.substring(index, index + 3).equals("!--")) {
              index = s.indexOf("-->", index) + 3;
            }
            else if (s.substring(index, index + 8).equals("![CDATA[")) {
              index += 8;
              int end = s.indexOf("]]>", index);
              String charNode = processValue(s.substring(index, end));
              handler.characters(charNode.toCharArray(), 0, charNode.length());

              index = end + 3;
            }
            else if (s.charAt(index) == '!') {
              int l = 1;
              while (l > 0) {
                if (s.charAt(index) == '<') {
                  l++;
                }
                if (s.charAt(index) == '>') {
                  l--;
                }
                index++;
              }
            }
            else {
              String tagName = getCharName(s, index);
              if (tagName == null || tagName.length() == 0) {
                return 0;
              }
              else {
                index += tagName.length();
                index = skipWhitespaces(s, index);
                AttributesImpl params = new AttributesImpl();
                while (s.charAt(index) != '/' && s.charAt(index) != '>') {
                  String paramName = getCharName(s, index);
                  index += paramName.length();
                  index = this.skipWhitespaces(s, index);
                  if (s.charAt(index) != '=') {
                    throw new SAXException("no =");
                  }
                  ++index;
                  index = this.skipWhitespaces(s, index);
                  if (s.charAt(index) != '\'') {
                    throw new SAXException("no '");
                  }
                  ++index;
                  int paramEnd = s.indexOf("'", index);
                  String paramVal = this.processValue(s.substring(index, paramEnd));
                  index = this.skipWhitespaces(s, paramEnd + 1);
                  params.addAttribute(paramName, paramName, paramName, null, paramVal);
                }
                index = s.indexOf('>', index) + 1;
                handler.startElement(tagName, tagName, tagName, params);
                if (s.charAt(index - 2) == '/') {
                  handler.endElement(tagName, tagName, tagName);
                }
                else {
                  index = parse(s, index);
                }

              }
            }
          }
          else {
            int end = s.indexOf('<', index);
            String charValue = processValue(s.substring(index, end));
            handler.characters(charValue.toCharArray(), 0, charValue.length());

            index = end;
          }
        }
        return index;
      }
      int skipWhitespaces(String str, int begin) {
        int i = begin;
        while (i < str.length() && (str.charAt(i) == '\n' || str.charAt(i) == '\r' || str.charAt(i) == '\t' || str
            .charAt(i) == ' ' || str.charAt(i) == '\u00ff' || str.charAt(i) == '\u00fe' || str
            .charAt(i) == '\ufffe' || str.charAt(i) == '\ufeff')) {
          ++i;
        }
        return i;
      }
      String getCharName(String str, int begin) {
        int i = begin;
        while (i < str.length() && !(str.charAt(i) == '\n' || str.charAt(i) == '\r' || str.charAt(i) == '\"' || str
            .charAt(i) == '\'' || str.charAt(i) == '\t' || str.charAt(i) == '/' || str.charAt(i) == '>' || str
            .charAt(i) == '<' || str.charAt(i) == '=' || str.charAt(i) == ' ')) {
          ++i;
        }
        return str.substring(begin, i);
      }
      String processValue(String str) {
        return str.replaceAll("&lt;", "<").
            replaceAll("&gt;", ">").
            replaceAll("&quot;", "\"").
            replaceAll("&apos;", "\'").
            replaceAll("&amp;", "&");
      }
    }

    private class ListeningParser extends DefaultHandler {
      @Override
      public void startElement(final String namespaceURI, final String localName, final String qName, final
      Attributes atts) throws
          SAXException {
        //System.out.println("begin Elem: "+qName);
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
          show = true;
          return;
        }
        if (qName.equals("stream:features")) {
          features = true;
          return;
        }
        /*if (qName.equals("starttls") && features) {
          try {
            writer.write("<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>");
            writer.flush();
          } catch (final IOException e) {
            e.printStackTrace();
            close();
          }
          return;
        }*/
        if (qName.equals("mechanisms") && features) {
          hasMechanisms = true;
          mechanisms = true;
          return;
        }
        if (qName.equals("mechanism") && mechanisms) {
          mechanism = true;
          return;
        }
        if (qName.equals("jid") && bind) {
          jid = true;
          return;
        }
        if (qName.equals("error") && bind) {
          throw new SAXException("error");
        }
      }

      @Override
      public void endElement(final String namespaceURI, final String localName, final String qName) throws
          SAXException {
        //System.out.println("end Elem: "+ qName);
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
        if (qName.equals("bind") && auth && !bind) {
          try {
            writer.write("<iq type=\"set\" id=\"bind\">" +
                "<bind xmlns=\"urn:ietf:params:xml:ns:xmpp-bind\">" +
                "<resource>app</resource>" +
                "</bind>" +
                "</iq>");
            writer.flush();
          } catch (final IOException e) {
            e.printStackTrace();
            close();
            return;
          }
          bind = true;
          return;
          //logged = true;
        }
        if (qName.equals("bind") && bind) {
          try {
            writer.write("<iq type=\"set\" id=\"ses\">" +
                "<session xmlns=\"urn:ietf:params:xml:ns:xmpp-session\"/>" +
                "</iq>");
            writer.write("<presence></presence>");
            writer.flush();
          } catch (final IOException e) {
            e.printStackTrace();
            close();
            return;
          }
          return;
        }
        /*if (qName.equals("proceed")) {
          try {
            writer.write("<?xml version=\"1.0\"?>" +
                "<stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" " +
                "version=\"1.0\" " +
                "xmlns=\"jabber:client\" " +
                "to=\"jabber.zone\" " +
                "xml:lang=\"ru\" " +
                "xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">");
            writer.flush();
            System.out.println("proceed:"+in.read());
            System.out.println("proceed:"+in.read());
          } catch (final IOException e) {
            e.printStackTrace();
            close();
          }
          return;
        }*/
        if (qName.equals("success")) {
          try {
            writer.write("<?xml version=\"1.0\"?>" +
                "<stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" " +
                "version=\"1.0\" " +
                "xmlns=\"jabber:client\" " +
                "to=\"jabber.ru\" " +
                "xml:lang=\"ru\" " +
                "xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">");
            writer.flush();
          } catch (final IOException e) {
            e.printStackTrace();
            close();
            return;
          }
          auth = true;
        }
        if (qName.equals("jid")) {
          jid = false;
        }
        if (qName.equals("iq") && auth) {
          logged = true;
        }
      }

      @Override
      public void characters(final char[] ch, final int start, final int length) throws SAXException {
        //System.out.println("characters: "+new String(ch, start, length));
        if (body) {
          System.out.println("<< new msg from " + sender + "\n" + "<< " + new String(ch, start, length));
          return;
        }
        if (show) {
          System.out.println("<< " + sender + " changed presence to " + new String(ch, start, length));
          return;
        }
        if (mechanism) {
          if (new String(ch, start, length).equals("PLAIN")) {
            hasPlain = true;
            //Charset charset = Charset.forName("US-ASCII");
            String id = user.split("@")[0];
            //ByteBuffer buf = ByteBuffer.allocate(id.length() + user.length() + pass.length() + 5);
            String code = Base64.getEncoder()
                .encodeToString((user + "\0" + id + "\0" + pass).getBytes());
            try {
              writer.write("<auth " +
                  "xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" " +
                  "mechanism=\"PLAIN\">" +
                  code +
                  "</auth>");
              writer.flush();
            } catch (final IOException e) {
              e.printStackTrace();
              close();
            }
          }
        }
        if (jid) {
          userRes = new String(ch, start, length);
        }
      }
    }

    public void run() {
      //final SAXParserFactory spf = SAXParserFactory.newInstance();
      final JabberXMLReader reader = new JabberXMLReader();
      //try {
      //parser = spf.newSAXParser();
      //reader = XMLReaderFactory.createXMLReader();
      reader.setContentHandler(new ListeningParser());
      //} catch (final SAXException | ParserConfigurationException e) {
      //e.printStackTrace();
      //close();
      //return;
      //}
      try {
        //reader.parse(new InputSource(in));
        //while (!closed) {
        reader.parse();
        //parser.parse(jin = new JabberInputStream(), new ListeningParser());
        //}
      } catch (final SAXException | IOException e) {
        e.printStackTrace();
        close();
      }
    }
  }

  public Jabber() {
    try {
      socket = new Socket("jabber.ru", 5222);
      in = new BufferedInputStream(socket.getInputStream());
      writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    } catch (final IOException e) {
      e.printStackTrace();
      close();
    }
  }

  public boolean isConnected() {
    return socket.isConnected();
  }

  public void login(final String user, final String pass) throws Exception {
    this.user = user;
    this.pass = pass;
    writer.write("<?xml version=\"1.0\"?>" +
        "<stream:stream xmlns:stream=\"http://etherx.jabber.org/streams\" " +
        "version=\"1.0\" " +
        "xmlns=\"jabber:client\" " +
        "to=\"jabber.ru\" " +
        "xml:lang=\"ru\" " +
        "xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">");
    writer.flush();
    final Thread thr = new ListenerThread();
    thr.setDaemon(true);
    thr.start();
  }

  public void receiver(final String user) {
    receiver = user;
  }

  public void sendMsg(final String msg) throws Exception {
    if (!logged) {
      throw new Exception("not logged in");
    }
    if (receiver.isEmpty()) {
      throw new Exception("no receiver");
    }
    writer.write("<message " +
        "to=\"" + receiver +
        "\" from=\"" + userRes +
        "\" type=\"chat\"><body>" +
        msg +
        "</body>" +
        "</message>");
    writer.flush();
  }

  public void sendStatus(final String status) throws Exception {
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
      if (socket.isConnected()) {
        writer.write("<presence type=\"unavailable\"/>");
        writer.write("</stream>");
        writer.flush();
      }
    } catch (final IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (socket != null) {
          socket.close();
        }
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }
  }
}
/*
input baklanov.stas@jabber.ru 12345ghj
 */