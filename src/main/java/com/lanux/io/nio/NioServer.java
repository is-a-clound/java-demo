package com.lanux.io.nio;

import com.lanux.io.NetConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by lanux on 2017/9/16.
 */
public class NioServer extends NioBasic {
    Selector selector = null;
    ServerSocketChannel ssc = null;

    public NioServer() {
        try {
            selector = Selector.open();
            ssc = ServerSocketChannel.open();
            ssc.socket().bind(new InetSocketAddress(NetConfig.SERVER_IP, NetConfig.SERVER_PORT));
            ssc.configureBlocking(false);// Channel 要注册到 Selector 中, 那么这个 Channel 必须是非阻塞的
            /**
             *
             * Connect, 即连接事件(TCP 连接), 对应于SelectionKey.OP_CONNECT
             * Accept, 即确认事件, 对应于SelectionKey.OP_ACCEPT
             * Read, 即读事件, 对应于SelectionKey.OP_READ, 表示 buffer 可读.
             * Write, 即写事件, 对应于SelectionKey.OP_WRITE, 表示 buffer 可写.
             *
             * 我们可以使用或运算|来组合多个事件, 例如:int interestSet = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
             */
            SelectionKey register = ssc.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                if (selector.select(NetConfig.SO_TIMEOUT) == 0) {
                    continue;
                }
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (!key.isValid()) {
                        // 选择键无效
                        continue;
                    }
                    handleKey(key);
                    // 这里不能用异步
//                    executor.submit(()-> handleKey(key));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (selector != null) {
                    selector.close();
                }
                if (ssc != null) {
                    ssc.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleKey(SelectionKey key) {
        try {
            if (key.isAcceptable()) {
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                serverSocketChannel
                        .accept()
                        .configureBlocking(false)
                        .register(selector, SelectionKey.OP_READ);
//                        .register(selector,SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                //一般来说，你不应该注册写事件。
                // 写操作的就绪条件为底层缓冲区有空闲空间，而写缓冲区绝大部分时间都是有空闲空间的，所以当你注册写事件后，写操作一直是就绪的，选择处理线程全占用整个CPU资源。
                // 所以，只有当你确实有数据要写时再注册写操作，并在写完以后马上取消注册。
                //  key.interestOps(SelectionKey.OP_WRITE);  //注册写监听
                //  key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE); //取消注册写监听
            }
            if (key.isReadable()) {
                SocketChannel sc = (SocketChannel) key.channel();
                String value = handleRead(sc);
                writeMsg(sc, value);
            }
            if (key.isWritable()) {
                handleWrite(key);
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);//取消注册写监听
            }
            if (key.isConnectable()) {
                System.out.println("is connect able");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
