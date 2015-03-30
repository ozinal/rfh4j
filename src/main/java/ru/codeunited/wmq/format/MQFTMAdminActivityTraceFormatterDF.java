package ru.codeunited.wmq.format;

import com.ibm.mq.MQMessage;
import com.ibm.mq.pcf.MQCFGR;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFParameter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import static com.ibm.mq.constants.MQConstants.*;

import static ru.codeunited.wmq.messaging.pcf.PCFUtilService.*;

/**
 * codeunited.ru
 * konovalov84@gmail.com
 * Created by ikonovalov on 02.02.15.
 */
public class MQFTMAdminActivityTraceFormatterDF extends MQPCFMessageAbstractFormatter<String> {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyyMMdd HHmmss");

    private static final SimpleDateFormat TIME_REFORMATED = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    MQFTMAdminActivityTraceFormatterDF() {
        super();
    }


    static interface Filter {
        boolean allowed(PCFParameter code);
    }

    /**
     * Filter for allowed operations of MQXF.
     */
    static final class OperationFilter implements Filter {

        private final Set<Integer> WHITE_LIST;

        OperationFilter() {
            final Set<Integer> whiteList = new HashSet<>();
            whiteList.add(MQXF_PUT);
            whiteList.add(MQXF_PUT1);
            whiteList.add(MQXF_GET);
            WHITE_LIST = Collections.unmodifiableSet(whiteList);
        }

        @Override
        public boolean allowed(PCFParameter checkIt) {
            return WHITE_LIST.contains(checkIt.getValue());
        }

    }

    private final static Filter OPERATION_FILTER = new OperationFilter();


    @Override
    public String format(PCFMessage pcfMessage, MQMessage mqMessage) {
        final StringBuffer buffer = new StringBuffer(1024);

        // print MQFTM_ADMIN
        if (pcfMessage.getCommand() != MQCMD_ACTIVITY_TRACE)
            return String.format("Can't handled with %s", MQFTMAdminActivityTraceFormatter.class.getName());

        boolean allowOutput = false;

        // scan for MQGACF_ACTIVITY_TRACE
        Enumeration<PCFParameter> parametersL1 = pcfMessage.getParameters();
        while (parametersL1.hasMoreElements()) {
            final PCFParameter parameter = parametersL1.nextElement();

            // skip non activity trace records
            if (parameter.getParameter() != MQGACF_ACTIVITY_TRACE)
                continue;

            // process activity trace elements (MQGACF_ACTIVITY_TRACE is always grouped as MQCFGR)
            MQCFGR trace = (MQCFGR) parameter; // => MQGACF_ACTIVITY_TRACE

            final PCFParameter mqiacfOperation = parameterOf(trace, MQIACF_OPERATION_ID);
            if (parameterOf(trace, MQIACF_COMP_CODE).getValue().equals(MQCC_OK) // => skip failed operations
                    && OPERATION_FILTER.allowed(mqiacfOperation)) { // => skip not interesting operations

                // timestamp;msgId;queueName;operation(put/get);QMGRNAme;size;<Строка заголовков - вида name=value>;
                boolean xmitExchange = "MQXMIT".equals(decodedParameter(trace, MQCACH_FORMAT_NAME));
                final String putDateTime = String.format("%s %s", decodedParameter(trace, MQCACF_PUT_DATE), decodedParameter(trace, MQCACF_PUT_TIME));
                try {
                    buffer.append(TIME_REFORMATED.format(TIME_FORMAT.parse(putDateTime)));
                } catch (ParseException e) {
                    buffer.append(putDateTime);
                } finally {
                    buffer.append(';');
                }
                buffer.append( /* append message id */
                        xmitExchange ?
                                decodedParameter(trace, MQBACF_XQH_MSG_ID) :
                                decodedParameter(trace, MQBACF_MSG_ID))
                        .append(';');

                buffer.append( /* append queue name */
                        xmitExchange ?
                                String.format("%s;%s;",
                                        coalesce(trace, MQCACF_OBJECT_NAME, MQCACF_RESOLVED_Q_NAME, MQCACF_RESOLVED_LOCAL_Q_NAME),
                                        decodedParameter(trace, MQCACF_XQH_REMOTE_Q_NAME)
                                ) :
                                String.format("%s;;",
                                        coalesce(trace, MQCACF_OBJECT_NAME, MQCACF_RESOLVED_LOCAL_Q_NAME, MQCACF_RESOLVED_LOCAL_Q_NAME)
                                )
                );

                buffer.append(decodeValue(mqiacfOperation)).append(';');

                buffer.append( /* append queuemanager name */
                        xmitExchange ?
                                String.format("%s;%s;",
                                        coalesce(trace, MQCACF_OBJECT_Q_MGR_NAME, MQCACF_RESOLVED_Q_MGR, MQCACF_RESOLVED_LOCAL_Q_MGR),
                                        decodedParameter(trace, MQCACF_XQH_REMOTE_Q_MGR)
                                ) :
                                String.format("%s;;",
                                        coalesce(trace, MQCACF_OBJECT_Q_MGR_NAME, MQCACF_RESOLVED_Q_MGR, MQCACF_RESOLVED_LOCAL_Q_MGR)
                                )
                );

                buffer.append(decodedParameter(trace, MQIACF_MSG_LENGTH)).append(';');
                buffer.append(decodedParameter(pcfMessage, MQIA_APPL_TYPE)).append(';');
                buffer.append(decodedParameter(pcfMessage, MQCACF_APPL_NAME)).append(';');
                buffer.append(decodedParameter(pcfMessage, MQCACF_USER_IDENTIFIER));
                //buffer.append(decodedParameter(trace, MQBACF_MESSAGE_DATA)).append(';');

                buffer.append('\n');
                allowOutput = true;
            }

        }
        if (!allowOutput) { // drop buffer in it contains nothing interesting.
            buffer.setLength(0);
        }
        return buffer.toString().trim();
    }



}
