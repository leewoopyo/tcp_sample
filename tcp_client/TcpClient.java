import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;

/**
 * TcpClient
 */
public class TcpClient {

    public static String leftPad(final String str, final int size, String padStr) {
        if (str == null) {
            return null;
        }
        if ((CharSequence)padStr == null || ((CharSequence)padStr).length() == 0) {
            padStr = " ";
        }
        int padLen = padStr.length();
        int strLen = str.length();
        int pads = size - strLen;

        if (pads <= 0) {
            return str; // returns original String when possible
        }

        if (padLen == 1) {
            char[] buf = new char[pads];
            for (int i = pads - 1; i >= 0; i--) {
                buf[i] = padStr.charAt(0);
            }
            return new String(buf).concat(str);
        }

        if (pads == padLen) {
            return padStr.concat(str);
        } else if (pads < padLen) {
            return padStr.substring(0, pads).concat(str);
        } else {
            char[] padding = new char[pads];
            char[] padChars = padStr.toCharArray();
            for (int i = 0; i < pads; i++) {
                padding[i] = padChars[i % padLen];
            }
            return new String(padding).concat(str);
        }
    }

    public static void call(String address, int port, String lengthType, byte[] msg) {
        
        // try ~ catch ~ resource
        try (Socket socket = new Socket(address, port);
            OutputStream out = socket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(out);
            InputStream in = socket.getInputStream();
            DataInputStream dis = new DataInputStream(in);
            ) 
        {   
            // 0. 서버에 lengthType을 먼저 전달
            System.out.println("[Tcp_Client] 0. length_type : [" + lengthType + "]");
            dos.write(lengthType.getBytes(), 0, lengthType.getBytes().length);
            dos.flush();

            // 1-1. lengthType이 String이면 
            if ("String".equals(lengthType)) {

                byte[] byteWriteLength = leftPad(Integer.toString(msg.length), 4, "0").getBytes();
                System.out.println("[Tcp_Client] 1. write_length : [" + new String(byteWriteLength) + "]");

                byte[] writeMsg = new byte[4 + msg.length];
                System.arraycopy(byteWriteLength, 0, writeMsg, 0, 4);
                System.arraycopy(msg, 0, writeMsg, 4, msg.length);
                System.out.println("[Tcp_Client] 2. write_msg : [" + new String(writeMsg) + "]");

                dos.write(writeMsg, 0, writeMsg.length);
                dos.flush();

                byte[] byteLength = new byte[4];
                dis.readFully(byteLength, 0, 4);
                int length =  Integer.parseInt(new String(byteLength));

                System.out.println("[Tcp_Client] 2. read_length : [" + length + "]");

                if (length > 0) {
                    byte[] readMsg = new byte[length];
                    dis.readFully(readMsg, 0, length);

                    byte[] echoMsg = new byte[4 + length];
                    System.arraycopy(byteLength, 0, echoMsg, 0, 4);
                    System.arraycopy(readMsg, 0, echoMsg, 4, length);

                    System.out.println("[Tcp_Client] 3. read_echo_msg : [" + new String(echoMsg) + "]");
                }
            }

            if ("Binary".equals(lengthType)) {

                dos.writeInt(msg.length);
                dos.write(msg, 0, msg.length);
                dos.flush();

                int length = dis.readInt();

                System.out.println("[Tcp_Client] 1. read length : [" + length + "]");

                if (length > 0) {
                    byte[] readMsg = new byte[length];
                    dis.readFully(readMsg, 0, length);

                    byte[] echoMsg = new byte[length];
                    System.arraycopy(readMsg, 0, echoMsg, 0, length);

                    System.out.println("[Tcp_Client] 2. read_echo_msg : [" + new String(echoMsg) + "]");
                }
            }

            System.out.println("[Tcp_Client] Tcp Client Request Message Ok........");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        // 소켓 정보 및 lengthType 초기화
        String TCP_SVR_ADDRESS = "127.0.0.1";
        int TCP_SVR_PORT = 11111;
        String lengthType = null;
        byte[] msg = " wpleeTest_Binary".getBytes();

        // 1. main 실행 시 lengthType에 해당하는 arguments 가 있다면 lengthType에 해당 argument를 대입.
        // 2. arguments가 없다면 lengthType에 "String" 값을 넣는다.
        // Tcp Client 로직 실행.
        try {
            if (args[0] != null) {
                lengthType = args[0];
                System.out.println("main args[0] ===> [" + args[0] + "]");
            } else {
                lengthType = "String";
            }
            TcpClient.call(TCP_SVR_ADDRESS, TCP_SVR_PORT, lengthType, msg);    
        } catch (ArrayIndexOutOfBoundsException aiobe) {
            lengthType = "String";
            TcpClient.call(TCP_SVR_ADDRESS, TCP_SVR_PORT, lengthType, msg);
        }
        
    }
}