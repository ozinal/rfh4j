package ru.codeunited.wmq.messaging;

import com.ibm.mq.*;

import java.io.IOException;
import java.io.InputStream;

import static com.ibm.mq.constants.CMQC.*;

/**
 * codeunited.ru
 * konovalov84@gmail.com
 * Created by ikonovalov on 30.10.14.
 */
public class MessageProducerImpl implements MessageProducer {

    private final MQQueue queue;

    private final MQPutMessageOptions defaultPutSpec = new MQPutMessageOptions();

    public MessageProducerImpl(String queueName, MQQueueManager queueManager) throws MQException {
        this.queue = queueManager.accessQueue(queueName, MQOO_OUTPUT | MQOO_FAIL_IF_QUIESCING);
        initialize();
    }

    private void initialize() {
        defaultPutSpec.options = defaultPutSpec.options | MQPMO_NEW_MSG_ID | MQPMO_NO_SYNCPOINT;
    }

    private byte[] putWithOptions(MQQueue mqQueue, MQMessage mqMessage, MQPutMessageOptions options) throws MQException {
        mqQueue.put(mqMessage, options);
        return mqMessage.messageId;
    }

    @Override
    public byte[] send(String messageText, MQPutMessageOptions options) throws IOException, MQException {
        final MQMessage message = MessageTools.createUTFMessage();
        MessageTools.writeStringToMessage(messageText, message);
        putWithOptions(queue, message, options);
        return message.messageId;
    }

    @Override
    public byte[] send(InputStream stream, MQPutMessageOptions options) throws IOException, MQException {
        final MQMessage message = MessageTools.createUTFMessage();
        MessageTools.writeStreamToMessage(stream, message);
        putWithOptions(queue, message, options);
        return message.messageId;
    }

    @Override
    public byte[] send(InputStream fileStream) throws IOException, MQException {
        return send(fileStream, defaultPutSpec);
    }

    @Override
    public byte[] send(String text) throws IOException, MQException {
        return send(text, defaultPutSpec);
    }
}
