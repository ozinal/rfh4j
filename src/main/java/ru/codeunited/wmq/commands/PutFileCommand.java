package ru.codeunited.wmq.commands;

import com.ibm.mq.*;
import ru.codeunited.wmq.cli.ConsoleWriter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static com.ibm.mq.constants.CMQC.*;

/**
 * Created by ikonovalov on 22.10.14.
 */
public class PutFileCommand extends AbstractCommand {

    private static long writeStreamToMessage(InputStream stream, MQMessage message) throws IOException {
        final byte[] buffer = new byte[1024];
        int readCount = 0;
        long totalBytes = 0;
        while ((readCount = stream.read(buffer)) != -1) {
            message.write(buffer, 0, readCount);
            totalBytes += readCount;
        }
        return totalBytes;
    }

    @Override
    public ReturnCode work() throws CommandGeneralException {
        final ExecutionContext context = getExecutionContext();
        final ConsoleWriter console = getConsoleWriter();
        try {
            final MQQueueManager manager = context.getQueueManager();
            final MQQueue queue = manager.accessQueue("MFC.APPLICATION_OUT", MQOO_OUTPUT);
            MQMessage message = new MQMessage();
            //message.format = MQFMT_STRING;
            message.characterSet = 1208;

            try {
                final long totalSize = writeStreamToMessage(
                        new FileInputStream(
                                "/home/ikonovalov/Yandex.Disk/Work/Buyakov/ParkingMFC/message_samples/mfc_v5/Parkings/043301.xml"),
                        message);
                console.writeln("File with size " + totalSize + "b stored in a message.");
            } catch (IOException e) {
                e.printStackTrace();
            }

            MQPutMessageOptions putSpec = new MQPutMessageOptions();
            putSpec.options =  putSpec.options | MQPMO_NEW_MSG_ID | MQPMO_NO_SYNCPOINT;
            queue.put(message, putSpec);
            console.writeln("Message PUT with messageId = " + UUID.nameUUIDFromBytes(message.messageId));
        } catch (MQException e) {
            LOG.severe(e.getMessage());
            console.errorln(e.getMessage());
            throw new CommandGeneralException(e);
        }
        return getState();
    }

    @Override
    public boolean resolve() {
        return true;
    }
}
