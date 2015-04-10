package ru.codeunited.wmq.commands;

import com.google.inject.Injector;
import com.ibm.mq.MQException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.codeunited.wmq.*;
import ru.codeunited.wmq.cli.CLIExecutionContext;
import ru.codeunited.wmq.handler.NestedHandlerException;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static ru.codeunited.wmq.CLITestSupport.prepareCommandLine;
import static ru.codeunited.wmq.RFHConstants.*;

/**
 * codeunited.ru
 * konovalov84@gmail.com
 * Created by ikonovalov on 29.11.14.
 */
public class GetCommandTest extends QueueingCapability {

    private final static String QUEUE = "RFH.QTEST.QGENERAL1";

    @Test(expected = IncompatibleOptionsException.class)
    public void getIncompatibleParamsStreamAll() throws ParseException, IncompatibleOptionsException, CommandGeneralException, MissedParameterException, NestedHandlerException {
        final CommandLine cl = prepareCommandLine(String.format("-Q DEFQM --%s --all", OPT_STREAM));
        final ExecutionContext executionContext = new CLIExecutionContext(cl);
        final MQGetCommand getCmd = (MQGetCommand) new MQGetCommand().setContext(executionContext);
        // should throw IncompatibleOptionsException here
        getCmd.execute();
    }

    @Test(expected = MissedParameterException.class) /* very synthetic test, not from real life */
    public void getMissedParameterExceptiontreamLimit10() throws ParseException, IncompatibleOptionsException, CommandGeneralException, MissedParameterException, NestedHandlerException {
        final CommandLine cl = prepareCommandLine(String.format("-Q DEFQM --%s --limit 10", OPT_STREAM));
        final ExecutionContext executionContext = new CLIExecutionContext(cl);
        final MQGetCommand getCmd = (MQGetCommand) new MQGetCommand().setContext(executionContext);
        assertThat("shouldWait", getCmd.shouldWait(), is(false));
        assertThat("getMessagesCountLimit", getCmd.getMessagesCountLimit(1), is(10));
        assertThat("waitTime", getCmd.waitTime(), is(-1));
        // should throw MissedParameterException here from the work() of MQGetCommand
        getCmd.execute();
    }

    @Test
    public void justGetOneMessageNOWaitAndExit() throws ParseException {
        final CommandLine cl = prepareCommandLine(String.format("-Q DEFQM --%1$s --srcq %2$s", OPT_STREAM, QUEUE));
        final ExecutionContext executionContext = new CLIExecutionContext(cl);
        final MQGetCommand getCmd = (MQGetCommand) new MQGetCommand().setContext(executionContext);
        assertThat(getCmd.isListenerMode(), is(false));
        assertThat(getCmd.shouldWait(), is(false));
        assertThat(getCmd.getMessagesCountLimit(1), is(1));
    }

    @Test
    public void justGetOneMessageWithWaitAndExit() throws ParseException {
        final CommandLine cl = prepareCommandLine(String.format("-Q DEFQM --%1$s --srcq %2$s --wait 1000", OPT_STREAM, QUEUE));
        final ExecutionContext executionContext = new CLIExecutionContext(cl);
        final MQGetCommand getCmd = (MQGetCommand) new MQGetCommand().setContext(executionContext);
        assertThat(getCmd.isListenerMode(), is(false));
        assertThat(getCmd.shouldWait(), is(true));
        assertThat(getCmd.getMessagesCountLimit(1), is(1));
        assertThat(getCmd.waitTime(), is(1000));
    }

    @Test(expected = MissedParameterException.class)
    public void getMissedParameterException() throws ParseException, IncompatibleOptionsException, CommandGeneralException, MissedParameterException, NestedHandlerException {
        final CommandLine cl = prepareCommandLine(String.format("-Q DEFQM --%s", OPT_STREAM));
        final ExecutionContext executionContext = new CLIExecutionContext(cl);
        final MQGetCommand getCmd = (MQGetCommand) new MQGetCommand().setContext(executionContext);
        // should throw IncompatibleOptionsException here
        getCmd.execute();
    }

    @Test(expected = MissedParameterException.class)
    public void streamOrPayloadMissed() throws ParseException, MissedParameterException, IncompatibleOptionsException, CommandGeneralException, NestedHandlerException {
        final CommandLine cl = prepareCommandLine("-Q DEFQM --srcq Q");
        final ExecutionContext executionContext = new CLIExecutionContext(cl);
        setup(executionContext);
        final ExecutionPlanBuilder executionPlanBuilder = injector.getInstance(ExecutionPlanBuilder.class);
        try {
            List<Command> commands = executionPlanBuilder.buildChain().getCommandChain();
            MQGetCommand getCmd = (MQGetCommand) commands.get(1);
            getCmd.execute();
        } catch (MissedParameterException missed) {
            assertThat(missed.getMessage(), equalTo(String.format("Option(s) [%s] [%s]  are missed.", OPT_PAYLOAD, OPT_STREAM)));
            throw missed;
        }
    }

    @Test
    public void initListenerMode() throws MissedParameterException, ParseException {
        CommandLine cl = prepareCommandLine(String.format("%1$s --srcq %2$s --stream --limit -1", "-Q DEFQM -c JVM.DEF.SVRCONN", QUEUE));
        ExecutionContext executionContext = new CLIExecutionContext(cl);
        setup(executionContext);

        ExecutionPlanBuilder executionPlanBuilder = injector.getInstance(ExecutionPlanBuilder.class);
        CommandChain chain = executionPlanBuilder.buildChain();
        List<Command> commands = chain.getCommandChain();
        MQGetCommand getCmd = (MQGetCommand) commands.get(1);

        assertThat("isListenerMode", getCmd.isListenerMode(), is(true));
        assertThat("shouldWait", getCmd.shouldWait(), is(true)); // engage MQGMO_WAIT
        assertThat("waitTime", getCmd.waitTime(), is(-1)); // engage MQWI_UNLIMITED
    }

    @Test(timeout = 20000)
    public void waitTwoMessages() throws ParseException, MissedParameterException, IncompatibleOptionsException, CommandGeneralException, ExecutionException, InterruptedException {

        branch(new Parallel.Branch() {
            @Override
            protected void perform() throws Exception {
                CommandLine cl = prepareCommandLine(String.format("%1$s --srcq %2$s --stream --limit 2 --wait 200", "-Q DEFQM -c JVM.DEF.SVRCONN", QUEUE));
                ExecutionContext executionContext = new CLIExecutionContext(cl);
                setup(executionContext);

                ExecutionPlanBuilder executionPlanBuilder = injector.getInstance(ExecutionPlanBuilder.class);
                CommandChain chain = executionPlanBuilder.buildChain();
                List<Command> commands = chain.getCommandChain();
                MQGetCommand getCmd = (MQGetCommand) commands.get(1);

                assertThat("isListenerMode", getCmd.isListenerMode(), is(false));
                assertThat("shouldWait", getCmd.shouldWait(), is(true));
                assertThat("waitTime", getCmd.waitTime(), is(200));

                chain.execute();
            }
        });

        branch(new Parallel.Branch(200) {
            @Override
            protected void perform() throws Exception {
                putToQueue(QUEUE);
            }
        });

        branch(new Parallel.Branch(300) {
            @Override
            protected void perform() throws Exception {
                putToQueue(QUEUE);
            }
        });

        parallel();

    }

    @Before
    @After
    public void cleanUp() throws MissedParameterException, IncompatibleOptionsException, CommandGeneralException, MQException, ParseException, NestedHandlerException {
        cleanupQueue(QUEUE);
    }

}
