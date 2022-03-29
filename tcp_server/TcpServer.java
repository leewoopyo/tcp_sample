import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TcpServer
 */
public class TcpServer {

    private static final int TCP_SVR_PORT = 11111 ;

    private class Worker extends Thread {
		private Socket socket;

		public Worker(Socket sock) {
			this.socket = sock;
		}

		public void run() {

            boolean flag = true;

			try (					
                OutputStream out = this.socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(out);
            
                InputStream in = this.socket.getInputStream();
                DataInputStream dis = new DataInputStream(in);
                ) 
            {
				while(flag) {

                    byte[] byteLengthType = new byte[6];
                    dis.readFully(byteLengthType, 0, 6);
                    String lengthType = new String(byteLengthType);
                    System.out.println("[Tcp_Server] 0. length_type : [" + lengthType + "]");

                    // #1 
                    if ("String".equals(lengthType)) {
                        byte[] byteLength = new byte[4];
                        dis.readFully(byteLength, 0, byteLength.length);
                        int length = Integer.parseInt(new String(byteLength));
                        System.out.println("[Tcp_Server] 1. read_length : [" + length + "]");

                        byte readMsg[]  = new byte[length];
                        dis.readFully(readMsg, 0, length);
                        System.out.println("[Tcp_Server] 2. read_msg : [" + new String(readMsg) + "]");

                        byte[] echoMsg = new byte[4 + length];
                        System.arraycopy(byteLength, 0, echoMsg, 0, 4);
                        System.arraycopy(readMsg, 0, echoMsg, 4, length);
                        System.out.println("[Tcp_Server] 3. echo_msg : [" + new String(echoMsg) + "]");

                        dos.write(echoMsg, 0, echoMsg.length);
                        dos.flush();
                    }

                    if ("Binary".equals(lengthType)) {
                        int  length = dis.readInt();
                        System.out.println("[Tcp_Server] 1. read_length : [" + length + "]");

                        byte readMsg[]  = new byte[length];
                        dis.readFully(readMsg, 0, length);
                        System.out.println("[Tcp_Server] 2. read_msg : [" + new String(readMsg) + "]");

                        byte[] echoMsg = new byte[length];
                        System.arraycopy(readMsg, 0, echoMsg, 0, length);
                        System.out.println("[Tcp_Server] 3. echo_msg : [" + new String(echoMsg) + "]");

                        dos.writeInt(echoMsg.length);
                        dos.write(echoMsg, 0, echoMsg.length);
                        dos.flush();
                    }

			    	System.out.println("[Tcp_Server] Tcp Server Response Echo Message Ok........");

				}
			} catch (Exception e) {
				//e.printStackTrace();
                flag = false;
			} finally {
				try { 
					if (this.socket != null) {
						System.out.println("[Tcp_Server] disconnected :"+this.socket.getInetAddress().getHostAddress()+"/"+this.socket.getPort());
						this.socket.close(); 
					}
				} catch(Exception e) {}
			}
		}
	}

    public TcpServer () throws Exception {
        // 소켓 인스턴스 생성
        ServerSocket serverSocket = new ServerSocket();
        // serReuseAddress 설정을 해줌 (TCP 접속을 닫은 후 일정시간동안 타임아웃 상태일 시 동일 포트를 바인딩 못하는데, 이를 가능하게 하는 옵션)
        serverSocket.setReuseAddress(true);

        try {
            serverSocket.bind(new InetSocketAddress(TCP_SVR_PORT));

            System.out.println("starting tcp server : "+TCP_SVR_PORT);

            while (true) {
                Socket socket = serverSocket.accept();

                System.out.println("connected : "+socket.getLocalPort());

                System.out.println(socket.getPort());

                //Worker w = new Worker(ts);

                System.out.println("12345");
                Worker w = new Worker(socket);
				w.start();

            }
        } catch (Exception e) {
            e.printStackTrace();
            serverSocket.close();
        }

    }

    public static void main(String[] args) {
        System.out.println("Hello World!!!");
        try {
            new TcpServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}