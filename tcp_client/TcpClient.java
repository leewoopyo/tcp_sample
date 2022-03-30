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

    /**
     * 특정 문자를 leftPad 하는 메소드
     * @param str       : 원문 문자열
     * @param size      : padding 이후의 최종 문자의 길이
     * @param padStr    : padding 할 문자
     * @return
     */
    public static String leftPad(String str, int size, String padStr) {
        // 원본 문자열이 null 이면 null을 반환
        if (str == null) {
            return null;
        }
        // padding 문자열이 없을 떄는 ' '으로 처리
        if ((CharSequence)padStr == null || ((CharSequence)padStr).length() == 0) {
            padStr = " ";
        }
        int padLen = padStr.length();   // padding할 문자의 길이
        int strLen = str.length();      // 원본 문자의 길이
        int pads = size - strLen;       // padding이 필요한 길이

        // padding 할 길이가 없을 떄, 원분 문자열을 반환
        if (pads <= 0) {
            return str; 
        }

        // padding 할 문자가 1글자일 경우, char[]를 만들어 빈칸을 채운 후 원본 문자 concat
        if (padLen == 1) {
            char[] buf = new char[pads];
            for (int i = pads - 1; i >= 0; i--) {
                buf[i] = padStr.charAt(0);
            }
            return new String(buf).concat(str);
        }

        // padding할 문자와 padding이 필요한 길이가 같을 떄, padding 문자와 원문 문자 concat 
        if (pads == padLen) {
            return padStr.concat(str);
        // padding할 문자의 길이가 padding이 필요한 길이보다 길 때, padding 문자를 subString으로 자른 후 원본 문자열하고 concat
        } else if (pads < padLen) {
            return padStr.substring(0, pads).concat(str);
        // 그 외의 경우, padding할 문자열을 char[]로 바꾼 후 for문으로 순차적으로 한글자씩 padding이 필요한 길이만큼 붙인 다음, 원분 문자열 concat
        } else {
            char[] padding = new char[pads];
            char[] padChars = padStr.toCharArray();
            for (int i = 0; i < pads; i++) {
                padding[i] = padChars[i % padLen];
            }
            return new String(padding).concat(str);
        }
    }

    /**
     * 1. Tcp Server에 byte[] Message 요청(write), 후 응답(read)
     * 2. lengthType 변수에 따라 4byte의 length의 형태가 달라짐(String or Binary)
     * @param address       : 서버 주소 
     * @param port          : 서버 포트
     * @param lengthType    : 4byte 길이의 형태
     * @param msg           : 길이를 제외한 전문 메시지
     */
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

            // 1. lengthType이 String이면 
            if ("String".equals(lengthType)) {

                // 1-1. write_length 추출 : msg의 길이 구한 후 leftpad를 활용하여 4byte length부 추출
                byte[] byteWriteLength = leftPad(Integer.toString(msg.length), 4, "0").getBytes();
                System.out.println("[Tcp_Client] 1. write_length : [" + new String(byteWriteLength) + "]");

                // 1-2. write_msg  : length와 msg를 결합해서 서버로 write를 하는 writeMsg 구성
                byte[] writeMsg = new byte[4 + msg.length];
                System.arraycopy(byteWriteLength, 0, writeMsg, 0, 4);
                System.arraycopy(msg, 0, writeMsg, 4, msg.length);
                System.out.println("[Tcp_Client] 2. write_msg : [" + new String(writeMsg) + "]");

                // 1-3. 구성한 writeMsg를 TcpServer에 write
                dos.write(writeMsg, 0, writeMsg.length);
                dos.flush();

                // 1-4. TcpServer에서 응답받은 4byte의 length를 추출
                byte[] byteLength = new byte[4];
                dis.readFully(byteLength, 0, 4);
                int length =  Integer.parseInt(new String(byteLength));
                System.out.println("[Tcp_Client] 2. read_length : [" + length + "]");

                // 1-5. TcpServer에서 응답받은 echoMsg를 추출
                if (length > 0) {
                    byte[] readMsg = new byte[length];
                    dis.readFully(readMsg, 0, length);

                    byte[] echoMsg = new byte[4 + length];
                    System.arraycopy(byteLength, 0, echoMsg, 0, 4);
                    System.arraycopy(readMsg, 0, echoMsg, 4, length);
                    System.out.println("[Tcp_Client] 3. read_echo_msg : [" + new String(echoMsg) + "]");
                }
            }

            // 2. lengthType이 Binary이면 
            if ("Binary".equals(lengthType)) {

                // 2-1. TcpServer에 length와 msg를 write
                dos.writeInt(msg.length);
                dos.write(msg, 0, msg.length);
                dos.flush();

                // 2-2. TcpServer로부터 length를 read함
                int length = dis.readInt();
                System.out.println("[Tcp_Client] 1. read length : [" + length + "]");

                // 2-3. TcpServer로부터 echoMsg를 
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

    /**
     * Main 메소드
     * @param args
     */
    public static void main(String[] args) {

        // 소켓 정보 및 lengthType 초기화
        String TCP_SVR_ADDRESS = "127.0.0.1";
        int TCP_SVR_PORT = 11111;
        String lengthType = null;
        byte[] msg = " wpleeTest_Binary".getBytes();

        // 1. main 실행 시 lengthType에 해당하는 arguments 가 있다면 lengthType에 해당 argument를 대입.
        // 2. arguments가 없다면 lengthType에 "String" 값을 넣는다.
        // 3. Tcp Client 로직 실행.
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