package com.itdragon.aio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Random;

public class ITDragonAIOClient implements Runnable{  
  
	private static Integer PORT = 8888;  
    private static String IP_ADDRESS = "127.0.0.1";
    private AsynchronousSocketChannel asynSocketChannel ;  
      
    public ITDragonAIOClient() throws Exception {  
        asynSocketChannel = AsynchronousSocketChannel.open();  // 打开通道  
    }  
      
    public void connect(){  
        asynSocketChannel.connect(new InetSocketAddress(IP_ADDRESS, PORT));  // 创建连接 和NIO一样  
    }  
      
    /**
     * 可能会报异常：java.nio.channels.NotYetConnectedException 
     * 因为异步请求不会管你有没有连接成功，都会继续执行
     */
    public void write(String request){  
        try {  
            asynSocketChannel.write(ByteBuffer.wrap(request.getBytes())).get();  
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);  
            asynSocketChannel.read(byteBuffer).get();  
            byteBuffer.flip();  
            byte[] respByte = new byte[byteBuffer.remaining()];  
            byteBuffer.get(respByte); // 将缓冲区的数据放入到 byte数组中  
            System.out.println(new String(respByte,"utf-8").trim());  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    }  
  
    @Override  
    public void run() {  
        while(true){  
              
        }  
    }  
      
    public static void main(String[] args) throws Exception {  
    	for (int i = 0; i < 10; i++) {
    		ITDragonAIOClient myClient = new ITDragonAIOClient();  
    		myClient.connect();  
    		new Thread(myClient, "myClient").start(); 
    		String []operators = {"+","-","*","/"};
    		Random random = new Random(System.currentTimeMillis());  
    		String expression = random.nextInt(10)+operators[random.nextInt(4)]+(random.nextInt(10)+1);
    		myClient.write(expression);  
		}
    }  

}

-------------------------------------------------------------------------------------------------
package com.itdragon.aio;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AIO, 也叫 NIO2.0 是一种异步非阻塞的通信方式
 * NIO 通过多路复用器轮询注册的通道，从而实现非阻塞效果，同时也说明了NIO是同步的。既然AIO是非阻塞的，哪它又是如何实现异步非阻塞的呢？
 * AIO 引入了异步通道的概念 AsynchronousServerSocketChannel 和 AsynchronousSocketChannel
 * 
 * @author itdragon
 *
 */
public class ITDragonAIOServer {
	  
    private ExecutorService executorService;  		// 线程池
    private AsynchronousChannelGroup threadGroup;  	// 通道组
    public AsynchronousServerSocketChannel asynServerSocketChannel;  // 服务器通道 
      
    public void start(Integer port){  
        try {  
            // 1.创建一个缓存池  
            executorService = Executors.newCachedThreadPool();  
            // 2.创建通道组  
            threadGroup = AsynchronousChannelGroup.withCachedThreadPool(executorService, 1);  
            // 3.创建服务器通道  
            asynServerSocketChannel = AsynchronousServerSocketChannel.open(threadGroup);  
            // 4.进行绑定  
            asynServerSocketChannel.bind(new InetSocketAddress(port));  
            System.out.println("server start , port : " + port);  
            // 5.等待客户端请求  
            asynServerSocketChannel.accept(this, new ITDragonAIOServerHandler());  
            // 一直阻塞 不让服务器停止，真实环境是在tomcat下运行，所以不需要这行代码  
            Thread.sleep(Integer.MAX_VALUE);  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    }  
      
    public static void main(String[] args) {  
    	ITDragonAIOServer server = new ITDragonAIOServer();  
        server.start(8888);  
    }  
}
-------------------------------------------------------------------------------------------------
package com.itdragon.aio;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;

import com.itdragon.util.CalculatorUtil;

public class ITDragonAIOServerHandler implements CompletionHandler<AsynchronousSocketChannel, ITDragonAIOServer> {  
    
  private final Integer BUFFER_SIZE = 1024;  

  @Override  
  public void completed(AsynchronousSocketChannel asynSocketChannel, ITDragonAIOServer attachment) {  
      // 当有下一个客户端接入的时候 直接调用Server的accept方法，这样反复执行下去，保证多个客户端都可以阻塞  
      attachment.asynServerSocketChannel.accept(attachment, this);  
      read(asynSocketChannel);  
  }  

  //读取数据  
  private void read(final AsynchronousSocketChannel asynSocketChannel) {  
      ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);  
      asynSocketChannel.read(byteBuffer, byteBuffer, new CompletionHandler<Integer, ByteBuffer>() {  
          @Override  
          public void completed(Integer resultSize, ByteBuffer attachment) {  
              //进行读取之后,重置标识位  
              attachment.flip();  
              //获取读取的数据  
              String resultData = new String(attachment.array()).trim();  
              System.out.println("Server -> " + "收到客户端的数据信息为:" + resultData);  
              String response = resultData + " = " + CalculatorUtil.cal(resultData);  
              write(asynSocketChannel, response);  
          }  
          @Override  
          public void failed(Throwable exc, ByteBuffer attachment) {  
              exc.printStackTrace();  
          }  
      });  
  }  
    
  // 写入数据
  private void write(AsynchronousSocketChannel asynSocketChannel, String response) {  
      try {  
          // 把数据写入到缓冲区中  
          ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);  
          buf.put(response.getBytes());  
          buf.flip();  
          // 在从缓冲区写入到通道中  
          asynSocketChannel.write(buf).get();  
      } catch (InterruptedException e) {  
          e.printStackTrace();  
      } catch (ExecutionException e) {  
          e.printStackTrace();  
      }  
  }  
    
  @Override  
  public void failed(Throwable exc, ITDragonAIOServer attachment) {  
      exc.printStackTrace();  
  }  

}
