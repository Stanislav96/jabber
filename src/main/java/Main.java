public class Main {
  public static void main(String[] args) {
    Jabber jabber = new Jabber();
    while (true) {
      String req = System.console().readLine(">>");
      if (req.equals("input")) {
        String serv = System.console().readLine();
        String user = System.console().readLine();
        String pass = new String(System.console().readPassword());
        try {
          jabber.login(user, pass, serv);
        } catch (Exception e) {
          e.printStackTrace();
          return;
        }
        continue;
      }
      if (req.equals("chat")) {
        String user = System.console().readLine();
        jabber.chat(user);
      }
      if (req.equals("msg")) {
        String msg = System.console().readLine();
        try {
          jabber.sendMsg(msg);
        } catch (Exception e) {
          e.printStackTrace();
          return;
        }
      }
      if (req.equals("status")) {
        String status = System.console().readLine();
        try {
          jabber.sendStatus(status);
        } catch (Exception e) {
          e.printStackTrace();
          return;
        }
      }
      if (req.equals("exit")) {
        return;
      }
    }
  }
}
