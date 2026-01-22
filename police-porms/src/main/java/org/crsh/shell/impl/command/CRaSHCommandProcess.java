package org.crsh.shell.impl.command;

import lombok.extern.slf4j.Slf4j;
import org.crsh.keyboard.KeyHandler;
import org.crsh.shell.ErrorKind;
import org.crsh.shell.ShellProcessContext;
import org.crsh.shell.ShellResponse;
import org.crsh.shell.impl.async.AsyncProcess;
import org.crsh.shell.impl.command.spi.CommandException;
import org.crsh.shell.impl.command.spi.CommandInvoker;
import org.crsh.util.Utils;

import java.util.UUID;

/**
 * @author Julien Viet
 */
@Slf4j
class CRaSHCommandProcess extends CRaSHProcess {

    /**
     * .
     */
    private final CRaSHSession session;

    /**
     * .
     */
    private final CommandInvoker<Void, ?> command;

    public CRaSHCommandProcess(CRaSHSession session, String request, CommandInvoker<Void, ?> command) {
        super(session, request);

        //
        this.session = session;
        this.command = command;
    }

    @Override
    public KeyHandler getKeyHandler() {
        return command.getKeyHandler();
    }

    @Override
    ShellResponse doInvoke(final ShellProcessContext context) throws InterruptedException {
        CRaSHProcessContext invocationContext = new CRaSHProcessContext(session, context);

        //로그인 오류를 감지하기위한 sessionId
        String sessionId = UUID.randomUUID().toString();
        log.info("sessionId: {}", sessionId);
        try {
            session.put("sessionId", sessionId);
            command.invoke(invocationContext);
            return ShellResponse.ok();
        } catch (CommandException e) {
            String message = e.getMessage();
            if (message.equals(sessionId)) {
                log.info("logout Exception : {}", sessionId);
                return ShellResponse.close();
            } else {
                return build(e);
            }
        } catch (Throwable t) {
            log.error("Throwable: ", t);
            return build(t);
        } finally {
            Utils.close(invocationContext);
        }
    }

    private ShellResponse.Error build(Throwable throwable) {
        ErrorKind errorType;
        if (throwable instanceof CommandException) {
            CommandException ce = (CommandException) throwable;
            errorType = ce.getErrorKind();
            Throwable cause = throwable.getCause();
            if (cause != null) {
                throwable = cause;
            }
        } else {
            errorType = ErrorKind.INTERNAL;
        }
        String result;
        String msg = throwable.getMessage();
        if (throwable instanceof CommandException) {
            if (msg == null) {
                result = request + ": failed";
            } else {
                result = request + ": " + msg;
            }
            return ShellResponse.error(errorType, result, throwable);
        } else {
            if (msg == null) {
                msg = throwable.getClass().getSimpleName();
            }
            if (throwable instanceof RuntimeException) {
                result = request + ": exception: " + msg;
            } else if (throwable instanceof Exception) {
                result = request + ": exception: " + msg;
            } else if (throwable instanceof Error) {
                result = request + ": error: " + msg;
            } else {
                result = request + ": unexpected throwable: " + msg;
            }
            return ShellResponse.error(errorType, result, throwable);
        }
    }
}
