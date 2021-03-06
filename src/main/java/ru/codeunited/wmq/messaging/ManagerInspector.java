package ru.codeunited.wmq.messaging;

import com.ibm.mq.MQException;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * codeunited.ru
 * konovalov84@gmail.com
 * Created by ikonovalov on 19.11.14.
 */
public interface ManagerInspector extends Closeable {

    List<Queue> listLocalQueues() throws MQException, IOException;

    QueueManagerAttributes managerAttributes() throws MQException, IOException;

    List<Queue> selectLocalQueues(String filter) throws MQException, IOException;

}
