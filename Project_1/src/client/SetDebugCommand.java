package client;

public class SetDebugCommand implements Command {

    @Override
    public void execute(String[] args, CommandHandler handler) {
        if (args.length > 1) {
            ClientMain.DEBUG = "1".equals(args[1].trim());
            System.out.println("DEBUG mode set to: " + ClientMain.DEBUG);
        }
    }
}
