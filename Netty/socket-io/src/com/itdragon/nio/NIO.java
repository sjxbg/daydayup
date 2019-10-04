package com.itdragon.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * NIO 也称 New IO， Non-Block IO，非阻塞同步通信方式
 * 从BIO的阻塞到NIO的非阻塞，这是一大进步。功归于Buffer，Channel，Selector三个设计实现。
 * Buffer   ： 缓冲区。NIO的数据操作都是在缓冲区中进行。缓冲区实际上是一个数组。而BIO是将数据直接写入或读取到Stream对象。
 * Channel  ：  通道。NIO可以通过Channel进行数据的读，写和同时读写操作。
 * Selector ：  多路复用器。NIO编程的基础。多路复用器提供选择已经就绪状态任务的能力。
 * 
 * NIO过人之处：
 * 客户端和服务器通过Channel连接，而这些Channel都要注册在Selector。Selector通过一个线程不停的轮询这些Channel。找出已经准备就绪的Channel执行IO操作。
 * NIO通过一个线程轮询，实现千万个客户端的请求，这就是非阻塞NIO的特点。
 * 
 * @author itdragon
 *
 */
public class ITDragonNIOServer implements Runnable{  
    
  private final int BUFFER_SIZE = 1024; // 缓冲区大小  
  private final int PORT = 8888; 		// 监听的端口  
  private Selector selector;  			// 多路复用器，NIO编程的基础，负责管理通道Channel 
  // 缓冲区Buffer，和BIO的一个重要区别（NIO读写数据是在缓冲区中进行，而BIO是通过流的形式）  
  private ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);  
    
  public ITDragonNIOServer() {  
      startServer();  
  }  

  private void startServer() {  
      try {  
          // 1.开启多路复用器  
          selector = Selector.open();  
          // 2.打开服务器通道(网络读写通道)  
          ServerSocketChannel channel = ServerSocketChannel.open();  
          // 3.设置服务器通道为非阻塞模式，true为阻塞，false为非阻塞  
          channel.configureBlocking(false);  
          // 4.绑定端口  
          channel.socket().bind(new InetSocketAddress(PORT));  
          // 5.把通道注册到多路复用器上，并监听阻塞事件  
          /** 
           * SelectionKey.OP_READ 	: 表示关注读数据就绪事件  
           * SelectionKey.OP_WRITE 	: 表示关注写数据就绪事件  
           * SelectionKey.OP_CONNECT: 表示关注socket channel的连接完成事件  
           * SelectionKey.OP_ACCEPT : 表示关注server-socket channel的accept事件  
           */  
          channel.register(selector, SelectionKey.OP_ACCEPT);  
          System.out.println("Server start >>>>>>>>> port :" + PORT);  
      } catch (IOException e) {  
          e.printStackTrace();  
      }  
  }  
    
  // 需要一个线程负责Selector的轮询  
  @Override  
  public void run() {  
      while (true) {  
          try {  
              /** 
               * a.select() 阻塞到至少有一个通道在你注册的事件上就绪  
               * b.select(long timeOut) 阻塞到至少有一个通道在你注册的事件上就绪或者超时timeOut 
               * c.selectNow() 立即返回。如果没有就绪的通道则返回0  
               * select方法的返回值表示就绪通道的个数。 
               */  
              // 1.多路复用器监听阻塞  
              selector.select();  
              // 2.多路复用器已经选择的结果集  
              Iterator<SelectionKey> selectionKeys = selector.selectedKeys().iterator();  
              // 3.不停的轮询  
              while (selectionKeys.hasNext()) {  
                  // 4.获取一个选中的key  
                  SelectionKey key = selectionKeys.next();  
                  // 5.获取后便将其从容器中移除  
                  selectionKeys.remove();  
                  // 6.只获取有效的key  
                  if (!key.isValid()){  
                      continue;  
                  }  
                  // 阻塞状态处理  
                  if (key.isAcceptable()){  
                      accept(key);  
                  }  
                  // 可读状态处理  
                  if (key.isReadable()){  
                      read(key);  
                  }  
              }  
          } catch (IOException e) {  
              e.printStackTrace();  
          }  
      }  
  }  
    
  // 设置阻塞，等待Client请求。在传统IO编程中，用的是ServerSocket和Socket。在NIO中采用的ServerSocketChannel和SocketChannel  
  private void accept(SelectionKey selectionKey) {  
      try {  
          // 1.获取通道服务  
          ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();  
          // 2.执行阻塞方法  
          SocketChannel socketChannel = serverSocketChannel.accept();  
          // 3.设置服务器通道为非阻塞模式，true为阻塞，false为非阻塞  
          socketChannel.configureBlocking(false);  
          // 4.把通道注册到多路复用器上，并设置读取标识  
          socketChannel.register(selector, SelectionKey.OP_READ);  
      } catch (IOException e) {  
          e.printStackTrace();  
      }  
  }  
    
  private void read(SelectionKey selectionKey) {  
      try {  
          // 1.清空缓冲区数据  
          readBuffer.clear();  
          // 2.获取在多路复用器上注册的通道  
          SocketChannel socketChannel = (SocketChannel) selectionKey.channel();  
          // 3.读取数据，返回  
          int count = socketChannel.read(readBuffer);  
          // 4.返回内容为-1 表示没有数据  
          if (-1 == count) {  
              selectionKey.channel().close();  
              selectionKey.cancel();  
              return ;  
          }  
          // 5.有数据则在读取数据前进行复位操作  
          readBuffer.flip();  
          // 6.根据缓冲区大小创建一个相应大小的bytes数组，用来获取值  
          byte[] bytes = new byte[readBuffer.remaining()];  
          // 7.接收缓冲区数据  
          readBuffer.get(bytes);  
          // 8.打印获取到的数据  
          System.out.println("NIO Server : " + new String(bytes)); // 不能用bytes.toString()  
      } catch (IOException e) {  
          e.printStackTrace();  
      }  
  }  
    
  public static void main(String[] args) {  
      new Thread(new ITDragonNIOServer()).start();  
  } 
}

=======================================================================================================
package com.itdragon.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ITDragonNIOClient {
	
	private final static int PORT = 8888;  
    private final static int BUFFER_SIZE = 1024;  
    private final static String IP_ADDRESS = "127.0.0.1";  
  
    // 从代码中可以看出，和传统的IO编程很像，很大的区别在于数据是写入缓冲区  
    public static void main(String[] args) {  
    	clientReq();
    }  
    
    private static void clientReq() {
    	// 1.创建连接地址  
        InetSocketAddress inetSocketAddress = new InetSocketAddress(IP_ADDRESS, PORT);  
        // 2.声明一个连接通道  
        SocketChannel socketChannel = null;  
        // 3.创建一个缓冲区  
        ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);  
        try {  
            // 4.打开通道  
            socketChannel = SocketChannel.open();  
            // 5.连接服务器  
            socketChannel.connect(inetSocketAddress);  
            while(true){  
                // 6.定义一个字节数组，然后使用系统录入功能：  
                byte[] bytes = new byte[BUFFER_SIZE];  
                // 7.键盘输入数据  
                System.in.read(bytes);  
                // 8.把数据放到缓冲区中  
                byteBuffer.put(bytes);  
                // 9.对缓冲区进行复位  
                byteBuffer.flip();  
                // 10.写出数据  
                socketChannel.write(byteBuffer);  
                // 11.清空缓冲区数据  
                byteBuffer.clear();  
            }  
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally {  
            if (null != socketChannel) {  
                try {  
                    socketChannel.close();  
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            }  
        } 
    }
}

=======================================================================================================
package com.itdragon.nio;

public class ITDragonNIODoubleClient {
	
	private static String DEFAULT_HOST = "127.0.0.1";
	private static Integer DEFAULT_PORT = 8888;
	private static ITDragonNIODoubleClientHandler clientHandle;

	public static void start() {
		start(DEFAULT_HOST, DEFAULT_PORT);
	}

	public static synchronized void start(String ip, int port) {
		if (clientHandle != null) {
			clientHandle.stop();
		}
		clientHandle = new ITDragonNIODoubleClientHandler(ip, port);
		new Thread(clientHandle, "Server").start();
	}

	// 向服务器发送消息
	public static boolean sendMsg(String msg) throws Exception {
		if (msg.equals("q"))
			return false;
		clientHandle.sendMsg(msg);
		return true;
	}
}

=======================================================================================================
package com.itdragon.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class ITDragonNIODoubleClientHandler implements Runnable {

	private String host;
	private Integer port;
	private Selector selector;
	private SocketChannel socketChannel;
	private volatile boolean started;

	public ITDragonNIODoubleClientHandler(String ip, Integer port) {
		this.host = ip;
		this.port = port;
		try {
			selector = Selector.open();
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
			started = true;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void stop() {
		started = false;
	}

	@Override
	public void run() {
		doConnect();
		while (started) {
			try {
				selector.select();
				Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
				SelectionKey key = null;
				while (iterator.hasNext()) {
					key = iterator.next();
					iterator.remove();
					handleInput(key);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (null != selector) {
			try {
				selector.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void handleInput(SelectionKey key) throws IOException {
		if (key.isValid()) {
			SocketChannel socketChannel = (SocketChannel) key.channel();
			if (key.isConnectable()) {
				if (!socketChannel.finishConnect()) {
					System.exit(1);
				}
			}
			if (key.isReadable()) {
				ByteBuffer buffer = ByteBuffer.allocate(1024);
				int readBytes = socketChannel.read(buffer);
				if (0 < readBytes) {
					buffer.flip();
					byte[] bytes = new byte[buffer.remaining()];
					buffer.get(bytes);
					String result = new String(bytes, "UTF-8");
					System.out.println("客户端收到消息：" + result);
				} else if (0 > readBytes) {
					key.cancel();
					socketChannel.close();
				}
			}
		}
	}

	private void doWrite(SocketChannel channel, String request) throws IOException {
		byte[] bytes = request.getBytes();
		ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
		writeBuffer.put(bytes);
		writeBuffer.flip();
		channel.write(writeBuffer);
	}

	private void doConnect() {
		try {
			if (!socketChannel.connect(new InetSocketAddress(host, port))) {
				socketChannel.register(selector, SelectionKey.OP_CONNECT);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendMsg(String msg) throws Exception {
		socketChannel.register(selector, SelectionKey.OP_READ);
		doWrite(socketChannel, msg);
	}
}

=======================================================================================================
package com.itdragon.nio;

import java.util.Scanner;

public class ITDragonNIODoubleMain {
	
	public static void main(String[] args) throws Exception{  
        // 启动服务器  
		ITDragonNIODoubleServer.start();  
        Thread.sleep(100);  
        // 启动客户端   
        ITDragonNIODoubleClient.start();  
        while(ITDragonNIODoubleClient.sendMsg(new Scanner(System.in).nextLine()));  
    }  
}

=======================================================================================================
package com.itdragon.nio;

public class ITDragonNIODoubleServer {
	
	private static Integer DEFAULT_PORT = 8888;
	private static ITDragonNIODoubleServerHandler serverHandle;

	public static void start() {
		start(DEFAULT_PORT);
	}

	public static synchronized void start(Integer port) {
		if (serverHandle != null) {
			serverHandle.stop();
		}
		serverHandle = new ITDragonNIODoubleServerHandler(port);
		new Thread(serverHandle, "Server").start();
	}
}

=======================================================================================================
package com.itdragon.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import com.itdragon.util.CalculatorUtil;

public class ITDragonNIODoubleServerHandler implements Runnable{  
	
    private Selector selector;  				// 多路复用选择器
    private ServerSocketChannel serverChannel;  // 服务器通道
    private volatile boolean started;  			// 服务器状态，避免服务器关闭后还继续循环
    private final int BUFFER_SIZE = 1024; 		// 缓冲区大小  
    
	public ITDragonNIODoubleServerHandler(int port) {
		try {
			// 创建多路复用器
			selector = Selector.open();
			// 打开监听通道
			serverChannel = ServerSocketChannel.open();
			// 设置服务器通道为非阻塞模式，true为阻塞，false为非阻塞
			serverChannel.configureBlocking(false);// 开启非阻塞模式
			// 绑定端口
			serverChannel.socket().bind(new InetSocketAddress(port));
			// 通道注册到多路复用器上，并监听阻塞事件
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			// 标记服务器已开启
			started = true;
			System.out.println("服务器已启动 >>>>>>>> 端口号：" + port);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1); // 非正常退出程序
		}
	} 
    
    public void stop(){  
        started = false;  
    }  
    
    /** 
     * selector.select() 				阻塞到至少有一个通道在你注册的事件上就绪  
     * selector.select(long timeOut) 	阻塞到至少有一个通道在你注册的事件上就绪或者超时timeOut 
     * selector.selectNow() 			立即返回。如果没有就绪的通道则返回0  
     * select方法的返回值表示就绪通道的个数。 
     */  
    @Override  
    public void run() {  
		while (started) {		// 循环遍历selector
			try {
				// 多路复用器监听阻塞
				selector.select();
				// 多路复用器已经选择的结果集
				Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
				SelectionKey key = null;
				while (iterator.hasNext()) {
					key = iterator.next();
					iterator.remove();
					handleInput(key);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		if (null != selector) {	// 释放资源
			try {
				selector.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
    }  
    
    private void handleInput(SelectionKey key) throws IOException{ 
		if (!key.isValid()) {
			return;
		}
		// 处理新接入的请求消息
		if (key.isAcceptable()) {
			// 获取通道服务
			ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
			// 通过ServerSocketChannel的accept创建SocketChannel实例
			SocketChannel socketChannel = serverSocketChannel.accept();
			// 设置服务器通道为非阻塞模式，true为阻塞，false为非阻塞
			socketChannel.configureBlocking(false);
			// 把通道注册到多路复用器上，并设置读取标识
			socketChannel.register(selector, SelectionKey.OP_READ);
		}
		// 可读状态消息
		if (key.isReadable()) {
			SocketChannel sc = (SocketChannel) key.channel();
			// 创建ByteBuffer，并开辟一个1M的缓冲区
			ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
			// 读取请求码流，返回读取到的字节数
			int readBytes = sc.read(buffer);
			// 读取到字节，对字节进行编解码
			if (0 < readBytes) {
				// 将缓冲区数据复位
				buffer.flip();
				// 根据缓冲区可读字节数创建字节数组
				byte[] bytes = new byte[buffer.remaining()];
				// 将缓冲区可读字节数组复制到新建的数组中
				buffer.get(bytes);
				String expression = new String(bytes, "UTF-8");
				System.out.println("服务器收到消息：" + expression);
				// 处理数据
				String result = CalculatorUtil.cal(expression).toString();
				// 返回数据消息
				doWrite(sc, result);
			} else if (0 > readBytes) {
				key.cancel();
				sc.close();
			}
		}
    }  
    
    // 异步发送应答消息  
    private void doWrite(SocketChannel channel, String response) throws IOException{  
		// 将消息编码为字节数组
		byte[] bytes = response.getBytes();
		// 根据数组容量创建ByteBuffer
		ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
		// 将字节数组复制到缓冲区
		writeBuffer.put(bytes);
		// 将缓冲区数据复位
		writeBuffer.flip();
		// 发送缓冲区的字节数组
		channel.write(writeBuffer);
    }  
}
=======================================================================================================
