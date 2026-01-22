package crash.commands.base2

import mb.fw.atb.configuration.CrashConfig
import org.crsh.CrashLogin
import org.crsh.cli.Command
import org.crsh.cli.Usage
import org.crsh.command.InvocationContext

class auth {

    CrashLogin login = new CrashLogin();

    //java to groovy
    @Usage("login to system")
    @Command
    def main(InvocationContext<String> invContext) {
        CrashConfig config = context.attributes.beans["crashConfig"];
        //system properties에서 username을 가져온다
        def sysUsername = config.getUsername();
        def sysPassword = config.getPassword();
        login.setLogin(Thread.currentThread().getId(), false);
        long nowId = Thread.currentThread().getId();
        def username = invContext.readLine("username: ", false)
        def password = invContext.readLine("password: ", false);

        if (sysUsername.equals(username) && sysPassword.equals(password)) {
            login.setLogin(nowId, true);
            invContext.append("Login success\n");
        } else {
            invContext.append("Login failed\n")
            String sessionId = invContext.session.get("sessionId");
            Systout.println("sessionId: " + sessionId);
            throw new RuntimeException(sessionId);
        }

        String printline = """\

 _______  _______  _______    _______  _______  __    _  _______  _______  ___      _______ 
|   _   ||       ||  _    |  |       ||       ||  |  | ||       ||       ||   |    |       |
|  |_|  ||_     _|| |_|   |  |       ||   _   ||   |_| ||  _____||   _   ||   |    |    ___|
|       |  |   |  |       |  |       ||  | |  ||       || |_____ |  | |  ||   |    |   |___ 
|       |  |   |  |  _   |   |      _||  |_|  ||  _    ||_____  ||  |_|  ||   |___ |    ___|
|   _   |  |   |  | |_|   |  |     |_ |       || | |   | _____| ||       ||       ||   |___ 
|__| |__|  |___|  |_______|  |_______||_______||_|  |__||_______||_______||_______||_______|

Welcome to $username !
It is ${new Date()} now
""";
        invContext.append(printline)
        invContext.flush();
    }

}
