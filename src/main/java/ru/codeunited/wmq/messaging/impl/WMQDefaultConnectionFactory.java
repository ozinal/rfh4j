package ru.codeunited.wmq.messaging.impl;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import ru.codeunited.wmq.messaging.ConnectionOptions;
import ru.codeunited.wmq.messaging.MQLink;
import ru.codeunited.wmq.messaging.WMQConnectionFactory;

/**
 * This is a not thread-safe implementation.
 * Created by ikonovalov on 22.10.14.
 */
public class WMQDefaultConnectionFactory implements WMQConnectionFactory {

    public WMQDefaultConnectionFactory() {

    }

    @Override
    public MQLink connectQueueManager(ConnectionOptions connectionOptions) throws MQException {
        MQQueueManager mqQueueManager = new MQQueueManager(
                connectionOptions.getQueueManagerName(),
                connectionOptions.getOptions()
        );

        QueueManagerImpl queueManager = new QueueManagerImpl(mqQueueManager);
        MQLink link = new MQLinkImpl(connectionOptions, queueManager);
        queueManager.setParentLink(link);
        return link;
    }
}
