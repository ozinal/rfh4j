package ru.codeunited.wmq.messaging.impl;

import com.ibm.mq.MQException;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.ibm.mq.pcf.PCFParameter;
import ru.codeunited.wmq.messaging.MQLink;
import ru.codeunited.wmq.messaging.ManagerInspector;
import ru.codeunited.wmq.messaging.QueueInspector;
import ru.codeunited.wmq.messaging.pcf.InquireCommand;
import ru.codeunited.wmq.messaging.pcf.PCFUtilService;
import ru.codeunited.wmq.messaging.pcf.Queue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static com.ibm.mq.constants.MQConstants.*;
/**
 * codeunited.ru
 * konovalov84@gmail.com
 * Created by ikonovalov on 19.11.14.
 */
public class ManagerInspectorImpl implements ManagerInspector {

    private final PCFMessageAgent pcfAgent;

    private final MQLink link;

    public ManagerInspectorImpl(MQLink link) throws MQException {
        this.pcfAgent = new PCFMessageAgent(link.getManager().get());
        this.link = link;
    }

    @Override
    public List<Queue> listLocalQueues() throws MQException, IOException {
        return selectLocalQueues("*");
    }

    private PCFMessage[] query(PCFMessage request) throws MQException, IOException {
        return pcfAgent.send(request);
    }

    @Override
    public void managerAttributes() throws MQException, IOException {
        PCFMessage request = new PCFMessage (InquireCommand.QMGR.object());
        // MQIACF_Q_MGR_ATTRS is a MQCFIL type (IL -> integer list)
        request.addParameter(MQIACF_Q_MGR_ATTRS, new int[]{MQIACF_ALL});
        PCFMessage[] responses = query(request);
        PCFMessage response = responses[0];
        Enumeration<PCFParameter> parameterEnumeration = response.getParameters();
        while(parameterEnumeration.hasMoreElements()) {
            PCFParameter parameter = parameterEnumeration.nextElement();
            System.out.println(parameter.getParameterName() + " -> " + PCFUtilService.decodeValue(parameter));
        }
        System.out.println(responses.length);
    }

    @Override
    public List<Queue> selectLocalQueues(String filter) throws MQException, IOException {
        final PCFMessage request = new PCFMessage (InquireCommand.QUEUE.object());

        request.addParameter(MQCA_Q_NAME, filter);
        request.addParameter(MQIA_Q_TYPE, MQQT_LOCAL);

        final PCFMessage[] responses = query(request);
        final String[] names = (String[]) responses[0].getParameterValue(MQCACF_Q_NAMES);
        final List<Queue> queues = new ArrayList<>(names.length);
        for (String queueName : names) {
            final Queue queue = new Queue(queueName);
            try (final QueueInspector inspector = new QueueInspectorImpl(queueName, link)) {
                queue.setDepth(inspector.depth());
                queue.setMaxDepth(inspector.maxDepth());
                queue.setInputCount(inspector.openInputCount());
                queue.setOutputCount(inspector.opentOutputCount());
                queues.add(queue);
            }
        }
        return queues;
    }

    @Override
    public void close() throws IOException {
        // PCF Agent disconnect() are performing drop single connection with a QM
    }
}