package ru.codeunited.wmq.handler;

import com.google.inject.Singleton;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import ru.codeunited.wmq.ExecutionContext;
import ru.codeunited.wmq.cli.ConsoleWriter;
import ru.codeunited.wmq.format.FormatterFactory;
import ru.codeunited.wmq.format.RootFormatFactory;
import ru.codeunited.wmq.cli.TableColumnName;

import javax.inject.Inject;
import java.io.IOException;

import static com.ibm.mq.constants.CMQC.MQFMT_ADMIN;
import static com.ibm.mq.constants.CMQC.MQFMT_STRING;

/**
 * codeunited.ru
 * konovalov84@gmail.com
 * Created by ikonovalov on 19.02.15.
 */
@Singleton
public class PrintStreamHandler extends CommonMessageHandler<Void> {

    private static final TableColumnName[] TABLE_HEADER = {
            TableColumnName.INDEX,
            TableColumnName.ACTION,
            TableColumnName.QMANAGER,
            TableColumnName.QUEUE,
            TableColumnName.MESSAGE_ID,
            TableColumnName.CORREL_ID,
            TableColumnName.OUTPUT
    };

    @Inject @RootFormatFactory
    private FormatterFactory formatterFactory;

    @Inject
    public PrintStreamHandler(ExecutionContext context, ConsoleWriter console) {
        super(context, console);
    }

    @Override
    public Void onMessage(MessageEvent messageEvent) throws NestedHandlerException {
        final String messageFormat = messageEvent.getMessageFormat();
        final MQMessage message = messageEvent.getMessage();
        try {
            switch (messageFormat) {
                case MQFMT_STRING: /* attach info table before body content output */
                    getConsole().createTable(TABLE_HEADER)
                            .append(
                                    String.valueOf(messageEvent.getMessageIndex()),
                                    messageEvent.getOperation().name(),
                                    getContext().getLink().getOptions().getQueueManagerName(),
                                    messageEvent.getEventSource().getName(),
                                    messageEvent.getHexMessageId(),
                                    messageEvent.getHexCorrelationId(),
                                    "<stream>"
                            ).make();
                case MQFMT_ADMIN:
                default:
                    final String formatterOutput = (String) formatterFactory.formatterFor(message).format(message);
                    if (formatterOutput.length() > 0) {
                        getConsole().writeln(formatterOutput).flush();
                    }
            }
        } catch (MQException | IOException e) {
            throw NestedHandlerException.nest(e);
        }
        return null;
    }
}
