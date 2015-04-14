package ru.codeunited.wmq.messaging;

import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQPutMessageOptions;
import com.ibm.mq.headers.MQHeader;
import com.ibm.mq.headers.MQHeaderIterator;
import com.ibm.mq.headers.MQRFH2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.codeunited.wmq.ContextModule;
import ru.codeunited.wmq.ExecutionContext;
import ru.codeunited.wmq.QueueingCapability;
import ru.codeunited.wmq.commands.CommandsModule;
import ru.codeunited.wmq.frame.ContextInjection;
import ru.codeunited.wmq.frame.GuiceContextTestRunner;
import ru.codeunited.wmq.frame.GuiceModules;
import ru.codeunited.wmq.messaging.impl.MessageConsumerImpl;
import ru.codeunited.wmq.messaging.impl.MessageProducerImpl;

import java.io.IOException;

import static com.ibm.mq.constants.MQConstants.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * codeunited.ru
 * konovalov84@gmail.com
 * Created by ikonovalov on 13.04.15.
 */
@RunWith(GuiceContextTestRunner.class)
@GuiceModules({ContextModule.class, CommandsModule.class})
public class MessagingProducer2Test extends QueueingCapability {

    @After @Before
    public void cleanUp() throws Exception {
        cleanupQueue("RFH.QTEST.QGENERAL1");
    }

    @Test
    @ContextInjection(cli = "-Q DEFQM -c JVM.DEF.SVRCONN --transport=client")
    public void doIt() throws Exception {
        communication(new QueueWork() {

            private final String THE_QUEUE = "RFH.QTEST.QGENERAL1";

            @Override
            public void work(ExecutionContext context) throws MQException, IOException, NoMessageAvailableException {
                try (
                        final MessageProducer producer = new MessageProducerImpl(THE_QUEUE, context.getLink());
                        final MessageConsumer consumer = new MessageConsumerImpl(THE_QUEUE, context.getLink())
                ) {
                    final MQMessage sentMessage = producer.send(new CustomSendAdjuster() {
                        @Override
                        public void setup(MQMessage message) throws IOException, MQException {
                            message.setStringProperty("myVar", "S13");
                            message.writeString("OK");
                            message.persistence = MQPER_NOT_PERSISTENT;
                        }

                        @Override
                        public void setup(MQPutMessageOptions options) {
                            options.options = MQPMO_NEW_MSG_ID | MQPMO_NO_SYNCPOINT;
                        }
                    });

                    final byte[] messageId = sentMessage.messageId;

                    final MQMessage selectedMessage = consumer.select(new MessageSelector() {
                        @Override
                        public void setup(MQGetMessageOptions messageOptions, MQMessage message) {
                            messageOptions.options = MQGMO_FAIL_IF_QUIESCING | MQGMO_NO_SYNCPOINT | MQGMO_NO_WAIT | MQGMO_PROPERTIES_FORCE_MQRFH2;
                            messageOptions.matchOptions =  MQMO_MATCH_MSG_ID;
                            message.messageId = messageId;
                        }
                    });

                    MQHeaderIterator headerIterator = new MQHeaderIterator(selectedMessage);
                    int totalHeader = 0;
                    boolean hasRHF2Header = false;
                    while (headerIterator.hasNext ())
                    {
                        MQHeader header = headerIterator.nextHeader();
                        if (header instanceof MQRFH2) {
                            MQRFH2 mqrfh2 = (MQRFH2) header;
                            String passedValue = (String) mqrfh2.getFieldValue("usr", "myVar");
                            assertThat("Passed via RFH2 header usr properties not match", passedValue, equalTo("S13"));
                            hasRHF2Header = true;
                        }
                        totalHeader++;
                    }
                    assertThat("Where is not header or MQGMO_PROPERTIES_FORCE_MQRFH2 not set or PROPCTL not setup properly for a forcing RHF2", totalHeader > 0, is(true));
                    assertThat("Where is no RFH2 header", hasRHF2Header, is(true));
                    assertThat("Sent message id not equals selected message id", sentMessage.messageId, equalTo(selectedMessage.messageId));
                }
            }
        });
    }
}