package ru.codeunited.wmq.handler;

import ru.codeunited.wmq.ExecutionContext;
import ru.codeunited.wmq.cli.ConsoleWriter;

/**
 * codeunited.ru
 * konovalov84@gmail.com
 * Created by ikonovalov on 19.02.15.
 */
public abstract class CommonMessageHandler<T> implements MessageHandler<T> {

    private final ExecutionContext context;

    private ConsoleWriter console;

    protected CommonMessageHandler(ExecutionContext context, ConsoleWriter console) {
        this.context = context;
        this.console = console;
    }

    public ExecutionContext getContext() {
        return context;
    }

    public ConsoleWriter getConsole() {
        return console;
    }
}
