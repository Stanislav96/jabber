import java.util.Scanner;

public class Main {
  public static void main(String[] args) {
    Jabber jabber = new Jabber();
    while (jabber.isConnected()) {
      Scanner scanner = new Scanner(System.in);
      System.out.print("<< ");
      String req = scanner.next();
      if (req.equals("input")) {
        String user = scanner.next();
        String pass = scanner.next();
        try {
          jabber.login(user, pass);
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
          jabber.close();
          e.printStackTrace();
          return;
        }
        continue;
      }
      if (req.equals("status")) {
        String status = scanner.next();
        try {
          jabber.sendStatus(status);
        } catch (Exception e) {
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
