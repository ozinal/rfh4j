package ru.codeunited.wmq.format;

import com.google.inject.Injector;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.pcf.PCFMessage;
import ru.codeunited.wmq.ExecutionContext;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import static com.ibm.mq.constants.MQConstants.*;

/**
 * codeunited.ru
 * konovalov84@gmail.com
 * Created by ikonovalov on 08.02.15.
 */
@Singleton
class MQFMTAdminFormatFactory implements FormatterFactory {

    private final ExecutionContext context;

    @Inject @MQCMDActivityTrace
    private MessageFormatter activityTraceFormatter;

    @Inject @MQFMTAdmin
    private MessageFormatter defaultAdminMessageFormatter;

    @Inject
    MQFMTAdminFormatFactory(ExecutionContext context) {
        this.context = context;
    }

    @Override
    public MessageFormatter formatterFor(MQMessage message) throws MQException, IOException {
        final PCFMessage pcfMessage = new PCFMessage(message);
        final int commandCode = pcfMessage.getCommand();
        final MessageFormatter formatter;
        switch (commandCode) {
            case MQCMD_ACTIVITY_TRACE:
                formatter = activityTraceFormatter;
                break;
            default:
                formatter = defaultAdminMessageFormatter;
        }
        return formatter;
    }

}
