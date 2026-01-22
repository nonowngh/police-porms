package crash.commands.base2

import mb.fw.atb.util.command.NetCommand
import org.crsh.cli.Argument
import org.crsh.cli.Command
import org.crsh.cli.Usage

@Usage("Network check commands")
class net {

    NetCommand cmd = new NetCommand();

    @Usage("Check TCP/IP connection : chk <host> <port>")
    @Command
    def chk(@Argument String host, @Argument int port) {
        return cmd.tcpConnectCheck(host, port, 30000);
    }

    @Usage("Check if the service port of the current server is listening : lschk <port>")
    @Command
    def lschk(@Argument int port) {
        return cmd.portListenCheck(port);
    }

}
