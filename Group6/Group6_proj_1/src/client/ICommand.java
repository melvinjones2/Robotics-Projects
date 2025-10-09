package client;

public interface ICommand {
    void execute(String[] args, CommandHandler context);
}