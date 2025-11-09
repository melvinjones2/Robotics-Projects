package client.commands;

import client.network.CommandHandler;

public interface ICommand {
    void execute(String[] args, CommandHandler context);
}