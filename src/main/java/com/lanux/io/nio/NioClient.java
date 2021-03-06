package com.lanux.io.nio;

import com.lanux.io.NetConfig;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by lanux on 2017/8/6.
 */
public class NioClient extends NioBasic implements Closeable {

    private Selector selector;
    public SocketChannel channel;

    public volatile boolean connected;

    public NioClient() {
        try {
            channel = SocketChannel.open();
            channel.configureBlocking(false);//设置 SocketChannel 为异步模式, 这样connect, read, write 都是异步的
            channel.connect(new InetSocketAddress(NetConfig.SERVER_IP, NetConfig.SERVER_PORT));
            /**
             *  非阻塞模式 connect立即返回，可能还未连接成功，所以一般用while来等待
             *  while(! channel.finishConnect() ){
             *    //wait, or do something else...
             *  }
             */
            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_CONNECT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen() {
        while (true) {
            try {
                if (selector.select(NetConfig.SO_TIMEOUT) == 0) {
                    continue;
                }
                Iterator<SelectionKey> ite = selector.selectedKeys().iterator();
                while (ite.hasNext()) {
                    SelectionKey key = ite.next();
                    //删除已选的key，防止重复处理
                    ite.remove();
                    if (key.isValid() && key.isConnectable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        if (channel.isConnectionPending()) {
                            channel.finishConnect();
                        }
                        channel.register(selector, SelectionKey.OP_READ);
                        connected = true;
                        System.out.println("nio client connected");
                    } else if (key.isReadable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        handleRead(channel);
                    } else if (key.isWritable()) {
                        handleWrite(key);
//                        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE); //取消注册写监听
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void write(String value) throws IOException {
        writeMsg(channel, value);
    }

    @Override
    public void close() throws IOException {
        try {
            connected = false;
            if (channel != null) {
                channel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
