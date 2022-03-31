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

    // 서버 포트
    private static final int TCP_SVR_PORT = 11111 ;

    // 이너 클래스, worker thread
    private class Worker extends Thread {
		private Socket socket;

		public Worker(Socket sock) {
			this.socket = sock;
		}
        // thread 실행 로직
		public void run() {

            boolean flag = true;
            // try ~ catch ~ resource
			try (					
                OutputStream out = this.socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(out);
            
                InputStream in = this.socket.getInputStream();
                DataInputStream dis = new DataInputStream(in);
                ) 
            {
				while(flag) {
                    // lengthType을 client에서 받음 
                    byte[] byteLengthType = new byte[6];
                    dis.readFully(byteLengthType, 0, 6);
                    String lengthType = new String(byteLengthType);
                    System.out.println("[Tcp_Server] 0. length_type : [" + lengthType + "]");

                    // 1. lengthType이 'String'일 때 
                    if ("String".equals(lengthType)) {
                        // 1-1. length 부분 추출 : 빈 4바이트 생성 후 길이부 read
                        byte[] byteLength = new byte[4];
                        dis.readFully(byteLength, 0, byteLength.length);
                        int length = Integer.parseInt(new String(byteLength));
                        System.out.println("[Tcp_Server] 1. read_length : [" + length + "]");

                        // 1-2. readMsg 추출 : client에서 보낸 바이트 데이터를 read
                        byte readMsg[]  = new byte[length];
                        dis.readFully(readMsg, 0, length);
                        System.out.println("[Tcp_Server] 2. read_msg : [" + new String(readMsg) + "]");

                        // 1-3. echoMsg 구성 : arraycopy를 사용해서 echoMsg를 구성
                        byte[] echoMsg = new byte[4 + length];
                        System.arraycopy(byteLength, 0, echoMsg, 0, 4);
                        System.arraycopy(readMsg, 0, echoMsg, 4, length);
                        System.out.println("[Tcp_Server] 3. echo_msg : [" + new String(echoMsg) + "]");

                        // 1-4. echoMsg write
                        dos.write(echoMsg, 0, echoMsg.length);
                        dos.flush();
                    }

                    // #2 lengthType이 'Binary'일 때 
                    if ("Binary".equals(lengthType)) {
                        // 2-1. length 부분 추출 : readInt 메소드로 길이부 추출
                        int  length = dis.readInt();
                        System.out.println("[Tcp_Server] 1. read_length : [" + length + "]");

                        // 2-2. readMsg 추출 : client에서 보낸 바이트 데이터를 read
                        byte readMsg[]  = new byte[length];
                        dis.readFully(readMsg, 0, length);
                        System.out.println("[Tcp_Server] 2. read_msg : [" + new String(readMsg) + "]");

                        // 2-3. echoMsg 구성 : arraycopy를 사용해서 echoMsg를 구성
                        byte[] echoMsg = new byte[length];
                        System.arraycopy(readMsg, 0, echoMsg, 0, length);
                        System.out.println("[Tcp_Server] 3. echo_msg : [" + new String(echoMsg) + "]");

                        // 1-4. echoMsg write
                        dos.writeInt(echoMsg.length);
                        dos.write(echoMsg, 0, echoMsg.length);
                        dos.flush();
                    }

			    	System.out.println("[Tcp_Server] Tcp Server Response Echo Message Ok........");
				}
            // EOFException 발생 시
			} catch (EOFException eofe) {
				//e.printStackTrace();
                flag = false;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
				try { 
					if (this.socket != null) {
						System.out.println("[Tcp_Server] disconnected :"+this.socket.getInetAddress().getHostAddress()+"/"+this.socket.getPort());
						this.socket.close(); 
					}
				} catch(Exception e) {
                    e.printStackTrace();
                }
			}
		}
	}

    // 생성자 메소드 에서 서버 소켓 생성
    public TcpServer () throws Exception {
        // 소켓 인스턴스 생성
        ServerSocket serverSocket = new ServerSocket();
        // serReuseAddress 설정을 해줌 (TCP 접속을 닫은 후 일정시간동안 타임아웃 상태일 시 동일 포트를 바인딩 못하는데, 이를 가능하게 하는 옵션)
        serverSocket.setReuseAddress(true);

        try {
            // 소켓에 포트 바인딩
            serverSocket.bind(new InetSocketAddress(TCP_SVR_PORT));
            System.out.println("starting tcp server : "+TCP_SVR_PORT);

            while (true) {
                // 서버에서 요청 accept 대기
                Socket socket = serverSocket.accept();
                System.out.println("connected : "+socket.getLocalPort());

                // accept를 받으면 worker Thread 시작
                Worker w = new Worker(socket);
				w.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            serverSocket.close();
        }

    }

    /**
     * main 메소드 실행
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("Hello World!!!");
        try {
            //서버 인스턴스 실행
            new TcpServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}