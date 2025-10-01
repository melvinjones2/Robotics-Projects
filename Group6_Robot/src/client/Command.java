package client;

public interface Command {
    void execute(String[] args, CommandHandler context);
}