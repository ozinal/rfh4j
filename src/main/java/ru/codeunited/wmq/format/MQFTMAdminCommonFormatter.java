package ru.codeunited.wmq.format;

import com.ibm.mq.MQException;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.*;

import static com.ibm.mq.constants.MQConstants.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * codeunited.ru
 * konovalov84@gmail.com
 * Created by ikonovalov on 02.02.15.
 */
public class MQFTMAdminCommonFormatter extends MQFTMAdminAbstractFormatter<String> {

    public MQFTMAdminCommonFormatter(PCFMessage pcfMessage) {
        super(pcfMessage);
    }

    private void boarder(final StringBuffer buffer) {
        buffer.append("<--------------MQFTM_ADMIN------------------------>").append('\n');
    }

    @Override
    public String format() throws IOException, MQException {
        final StringBuffer buffer = new StringBuffer();

        // print MQFTM_ADMIN
        boarder(buffer);

        int paramCount = pcfMessage.getParameterCount();

        buffer.append(String.format("Command: %d\n", pcfMessage.getCommand()));
        buffer.append(String.format("Parameters count: %d\n", paramCount));

        final Enumeration<PCFParameter> parametersEnum = pcfMessage.getParameters();
        final StringBuffer parametersBuffer = formatParameters(parametersEnum, 1);
        buffer.append(parametersBuffer);

        return buffer.toString();
    }

    private String depthStringOffset(int depth) {
        char[] offset = new char[depth];
        Arrays.fill(offset, '\t');
        return new String(offset);
    }

    private StringBuffer formatParameters(Enumeration<PCFParameter> parameters, int depth) {
        final String offset = depthStringOffset(depth);
        final StringBuffer buffer = new StringBuffer();
        int pIndex = 0;
        while(parameters.hasMoreElements()) {
            PCFParameter pcfParameter = parameters.nextElement(); // MQGACF_ACTIVITY_TRACE,  "MQI Operation"
            final int paramCode = pcfParameter.getParameter();
            switch (paramCode) { // resolve grouped parameters
                case MQGACF_ACTIVITY_TRACE: // activity trace
                    final MQCFGR parameterGroup = (MQCFGR) pcfParameter;
                    pIndex = formatSingleParameter(offset, buffer, pIndex, parameterGroup);
                    final StringBuffer parameterGroupBuffer = formatParameters(
                            (Enumeration<PCFParameter>) parameterGroup.getParameters(),
                            depth + 1
                    );
                    buffer.append(parameterGroupBuffer);
                    break;
                default:
                    pIndex = formatSingleParameter(offset, buffer, pIndex, pcfParameter);

            }
        }
        return buffer;
    }

    private int formatSingleParameter(String offset, StringBuffer buffer, int pIndex, PCFParameter pcfParameter) {
        final String pName = pcfParameter.getParameterName();
        final String pStringValue = decodeValue(pcfParameter);
        buffer.append(String.format("%sP_%s:%d [%d][%s][%s]\n",
                offset, pcfParameter.getClass().getSimpleName(), pIndex++, pcfParameter.getParameter(), pName, pStringValue
        ));
        return pIndex;
    }

    private String decodeValue(PCFParameter pcfParameter) {
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
