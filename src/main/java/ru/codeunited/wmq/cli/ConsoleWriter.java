package ru.codeunited.wmq.cli;

import com.ibm.mq.MQMessage;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * codeunited.ru
 * konovalov84@gmail.com
 * Created by ikonovalov on 22.10.14.
 */
public class ConsoleWriter implements Closeable {

    private final PrintWriter normalWriter;

    private final PrintWriter errorWriter;

    private static final char NL = '\n'; // next line

    private static final char TAB = '\t';

    private static final String BORDER = "<--------------PAYLOAD-BOARDER-------------------->";

    public ConsoleWriter(PrintStream printWriter, PrintStream errorWriter) {
        this.errorWriter = new PrintWriter(errorWriter);
        this.normalWriter = new PrintWriter(printWriter);
    }

    public ConsoleTable createTable() {
        return new ConsoleTable(this);
    }

    /**
     * Create console table with header.
     * @param head
     * @return
     */
    public ConsoleTable createTable(TableColumnName...head) {
        return createTable().head(head);
    }

    /**
     * Use same stream for normal output and errors.
     *
     * @param printWriter
     */
    public ConsoleWriter(PrintStream printWriter) {
        this(printWriter, printWriter);
    }

    public void writef(String leftAlignFormat, String...rowResized) {
        write(String.format(leftAlignFormat, rowResized));
    }

    public ConsoleWriter printf(String format, String string) {
        normalWriter.printf(format, string);
        return this;
    }

    public ConsoleWriter write(String string) {
        normalWriter.write(string);
        return this;
    }

    public ConsoleWriter end() {
        write(NL);
        return this;
    }

    public ConsoleWriter write(char ch) {
        normalWriter.write(ch);
        return this;
    }

    public ConsoleWriter writeln(String string) {
        return write(string).end();
    }

    public ConsoleWriter write(MQMessage message) throws IOException {
        writeln(BORDER);
        writeln(message.readStringOfByteLength(message.getDataLength()));
        writeln(BORDER);
        end();
        return this;
    }

    public ConsoleWriter error(String string) {
        errorWriter.write(string);
        return this;
    }

    public ConsoleWriter flush() {
        this.errorWriter.flush();
        this.normalWriter.flush();
        return this;
    }

    public ConsoleWriter errorln(String message) {
        return error(message).end();
    }

    @Override
    public void close() throws IOException {
        flush();
        this.errorWriter.close();
        this.normalWriter.close();
    }
}
