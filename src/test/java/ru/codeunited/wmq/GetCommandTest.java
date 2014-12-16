package ru.codeunited.wmq;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.junit.Test;
import ru.codeunited.wmq.cli.CLIExecutionContext;
import ru.codeunited.wmq.commands.CommandGeneralException;
import ru.codeunited.wmq.commands.IncompatibleOptionsException;
import ru.codeunited.wmq.commands.MQGetCommand;
import ru.codeunited.wmq.commands.MissedParameterException;

/**
 * codeunited.ru
 * konovalov84@gmail.com
 * Created by ikonovalov on 29.11.14.
 */
public class GetCommandTest extends CLITestSupport {

    @Test(expected = IncompatibleOptionsException.class)
    public void getIncompatibleParamsStreamAll() throws ParseException, IncompatibleOptionsException, CommandGeneralException, MissedParameterException {
        CommandLine cl = prepareCommandLine("-Q DEFQM --stream --all");
        ExecutionContext executionContext = new CLIExecutionContext(cl);
        MQGetCommand getCmd = (MQGetCommand) new MQGetCommand().setContext(executionContext);
        // should throw IncompatibleOptionsException here
        getCmd.execute();
    }

    @Test(expected = IncompatibleOptionsException.class)
    public void getIncompatibleParamsStreamLimit10() throws ParseException, IncompatibleOptionsException, CommandGeneralException, MissedParameterException {
        CommandLine cl = prepareCommandLine("-Q DEFQM --stream --limit 10");
        ExecutionContext executionContext = new CLIExecutionContext(cl);
        MQGetCommand getCmd = (MQGetCommand) new MQGetCommand().setContext(executionContext);
        // should throw IncompatibleOptionsException here
        getCmd.execute();
    }

    @Test(expected = MissedParameterException.class)
    public void getMissedParameterException() throws ParseException, IncompatibleOptionsException, CommandGeneralException, MissedParameterException {
        CommandLine cl = prepareCommandLine("-Q DEFQM --stream");
        ExecutionContext executionContext = new CLIExecutionContext(cl);
        MQGetCommand getCmd = (MQGetCommand) new MQGetCommand().setContext(executionContext);
        // should throw IncompatibleOptionsException here
        getCmd.execute();
    }
}