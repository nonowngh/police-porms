package mb.fw.atb.util.command;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

@Slf4j
public class NetCommand {
    /**
     * Check if the TCP/IP connection is established
     */
    public String tcpConnectCheck(String host, int port, int timeout) throws IOException {

        try (Socket socket = new Socket()) {
            SocketAddress socketAddress = new InetSocketAddress(host, port);
            socket.connect(socketAddress, timeout);
            return "Connection established";
        } catch (IOException e) {
            log.warn("[{}:{}] Connection failed:{}", host, port, e.getMessage());
            return "[" + host + ":" + port + "] Connection failed:" + e.getMessage();
        }
    }

    public String portListenCheck(int port) {
        String os = System.getProperty("os.name").toLowerCase();
        String command;

        if (os.contains("win")) {
            command = "netstat -an | findstr :" + port;
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            command = "netstat -an | grep :" + port;
        } else {
            return "Unsupported operating system: " + os;
        }

        try {
            log.info("Executing command: " + command);
            ProcessBuilder processBuilder;
            if (os.contains("win")) {
                processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                processBuilder = new ProcessBuilder("sh", "-c", command);
            }
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("Output line: " + line);
                if (line.contains("LISTENING") || line.contains("LISTEN")) {
                    return "Port " + port + " is listening";
                }
            }
            return "Port " + port + " is not listening";
        } catch (Exception e) {
            return "Error checking port: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        NetCommand netCommand = new NetCommand();
//        try {
//            log.info(netCommand.tcpConnectCheck("localhost", 8080, 5000));
//        } catch (IOException e) {
//            log.error("Error: " + e.getMessage());
//        }

        log.info(netCommand.portListenCheck(2000));
    }
}