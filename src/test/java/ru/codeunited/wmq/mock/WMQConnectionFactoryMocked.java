package ru.codeunited.wmq.mock;


import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import ru.codeunited.wmq.WMQConnectionFactory;

import java.util.Properties;

import static org.mockito.Mockito.*;
/**
 * codeunited.ru
 * konovalov84@gmail.com
 * Created by ikonovalov on 26.10.14.
 */
public class WMQConnectionFactoryMocked implements WMQConnectionFactory {

    @Override
    public MQQueueManager connectQueueManager(String queueManagerName, Properties properties) throws MQException {
        MQQueueManager manager = mock(MQQueueManager.class);

        // return valid mqm name
        when(manager.getName()).thenReturn(queueManagerName + "-mocked");

        // first - for check "is connected", second - for "is disconnected"
        when(manager.isConnected()).thenReturn(true, false);

        return manager;
    }
}