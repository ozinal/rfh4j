package ru.codeunited.wmq.format;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static com.ibm.mq.constants.MQConstants.*;

/**
 * codeunited.ru
 * konovalov84@gmail.com
 * Created by ikonovalov on 02.02.15.
 */
public class MQFTMAdminActivityTraceFormatter extends MQFTMAdminAbstractFormatter<String> {

    MQFTMAdminActivityTraceFormatter(PCFMessage pcfMessage) {
        super(pcfMessage);
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
    public String format() throws IOException, MQException {
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

            final PCFParameter mqiacfOperation = trace.getParameter(MQIACF_OPERATION_ID);
            if (parameterOf(trace, MQIACF_COMP_CODE).getValue().equals(MQCC_OK) // => skip failed operations
                    && OPERATION_FILTER.allowed(mqiacfOperation)) { // => skip not interesting operations

                // timestamp;msgId;queueName;operation(put/get);QMGRNAme;size;<Строка заголовков - вида name=value>;

                buffer.append(String.format("%s %s;", decodedParameter(trace, MQCACF_OPERATION_DATE), decodedParameter(trace, MQCACF_OPERATION_TIME)));
                buffer.append(decodedParameter(trace, MQBACF_MSG_ID)).append(';');
                buffer.append(decodedParameter(trace, MQCACF_RESOLVED_Q_NAME)).append(';');
                buffer.append(decodeValue(mqiacfOperation)).append(';');
                buffer.append(decodedParameter(trace, MQCACF_RESOLVED_Q_MGR)).append(';');
                buffer.append(decodedParameter(trace, MQIACF_MSG_LENGTH)).append(';');
                buffer.append(decodedParameter(trace, MQBACF_MESSAGE_DATA)).append(';');

                final Integer operationValue = (Integer) mqiacfOperation.getValue();

                switch (operationValue) {
                    case MQXF_PUT:
                    case MQXF_GET:
                        /*buffer.append(
                                String.format(" obj:[%s] mid:[%s] cid:[%s] len:[%s] dat:[%s]",


                                        decodedParameter(trace, MQIACF_MSG_LENGTH),
                                        decodedParameter(trace, MQBACF_MESSAGE_DATA)
                                        ));*/
                }
                buffer.append('\n');
                allowOutput = true;
            }

        }
        if (!allowOutput) { // drop buffer in it contains nothing interesting.
            buffer.setLength(0);
        }

        return buffer.toString();
    }

    private PCFParameter parameterOf(PCFContent content, int paramCode) {
        return content.getParameter(paramCode);
    }

    private String decodedParameter(PCFContent content, int paramCode) {
        String value = decodeValue(parameterOf(content, paramCode));
        if (value != null)
            value = value.trim();
        return value;
    }

    private String decodeValue(final PCFParameter pcfParameter) {
        if (pcfParameter == null)
            return "";
        final int code = pcfParameter.getParameter();
        final Object value = pcfParameter.getValue();
        switch (code) { //http://www-01.ibm.com/support/knowledgecenter/SSFKSJ_7.5.0/com.ibm.mq.ref.dev.doc/q090210_.htm
            case MQIACF_OPERATION_ID:
                return MQConstants.lookup(value, "MQXF_.*");
            case MQIACF_COMP_CODE:
                return MQConstants.lookup(value, "MQCC_.*");
            case MQIA_PLATFORM:
                return MQConstants.lookup(value, "MQPL_.*");
            case MQIA_APPL_TYPE:
                return MQConstants.lookup(value, "MQAT_.*");
            case MQBACF_MESSAGE_DATA:
                return new String(((MQCFBS) pcfParameter).getString(), Charset.forName("UTF-8"));
            default:
                return pcfParameter.getStringValue();
        }

    }
}
