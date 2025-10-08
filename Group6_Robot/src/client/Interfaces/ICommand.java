package client.Interfaces;

import client.Handlers.CommandHandler;

public interface ICommand {
    void execute(String[] args, CommandHandler context);
}