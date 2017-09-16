package com.lanux.io.bio;

import com.lanux.io.NetConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lanux on 2017/8/6.
 */
public class BioServer extends IoStream {

    private ServerSocket serverSocket;
    private volatile boolean running;

    private static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
        final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "server-thread-" + threadNumber.getAndIncrement());
        }
    });

    public BioServer() {
        try {
            serverSocket = new ServerSocket(NetConfig.SERVER_PORT);
            running = true;
            while (true) {
                final Socket socket = serverSocket.accept();
                executor.submit(() -> handle(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            running = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void handle(Socket socket) {
        try {
            //循环read write Stream，一直没有close可能会爆内存。
            //所以阻塞IO一般一个连接使用一次，不会循环利用。
            while (running) {
                byte[] bytes = readStream(socket.getInputStream());
                if (bytes != null && bytes.length > 0) {
                    String s = new String(bytes);
                    System.out.println(Thread.currentThread().getName() + " received " + bytes.length + " response : " + maxString(s, 50));
                    writeStream(socket.getOutputStream(), s.getBytes());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
