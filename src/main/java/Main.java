import java.util.Scanner;

class Main {
  public static void main(final String[] args) {
    final Jabber jabber = new Jabber();
    while (jabber.isConnected()) {
      final Scanner scanner = new Scanner(System.in);
      String req = scanner.next();
      if (req.equals("input")) {
        String user = scanner.next();
        String pass = scanner.next();
        try {
          jabber.login(user, pass);
        } catch (final Exception e) {
          jabber.close();
          e.printStackTrace();
          return;
        }
        continue;
      }
      if (req.equals("to")) {
        String user = scanner.next();
        try {
          jabber.receiver(user);
        } catch (final Exception e) {
          jabber.close();
          e.printStackTrace();
          return;
        }
        continue;
      }
      if (req.equals("msg")) {
        String msg = scanner.next();
        try {
          jabber.sendMsg(msg);
        } catch (final Exception e) {
          jabber.close();
          e.printStackTrace();
          return;
        }
        continue;
      }
      if (req.equals("presence")) {
        String status = scanner.next();
        try {
          jabber.sendStatus(status);
        } catch (final Exception e) {
          jabber.close();
          e.printStackTrace();
          return;
        }
        continue;
      }
      if (req.equals("exit")) {
        jabber.close();
        return;
      }
    }
  }
}
