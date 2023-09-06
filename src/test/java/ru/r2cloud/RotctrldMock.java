package ru.r2cloud;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ThreadPoolFactoryImpl;

public class RotctrldMock {

    private static final Logger LOG = LoggerFactory.getLogger(RotctrldMock.class);

    private final int port;

    private ServerSocket socket;
    private Thread thread;
    private RequestHandler handler;
    private ScheduledExecutorService executor = null;

    public RotctrldMock(int port) {
        this.port = port;
    }

    public void setHandler(RequestHandler handler) {
        this.handler = handler;
    }

    public void start() throws IOException {
        executor = new ThreadPoolFactoryImpl(1000).newScheduledThreadPool(2, new NamingThreadFactory("rotator-mock"));
        socket = new ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"));
        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Socket client = socket.accept();
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    handleClient(client);
                                } catch (IOException e) {
                                    LOG.error("unable to handle", e);
                                }
                            }
                        });
                    } catch (IOException e) {
                        return;
                    }
                }
                LOG.info("shutting down");
            }

        }, "client-handler");
        thread.start();
    }

    private void handleClient(Socket client) throws IOException {
        while (!Thread.currentThread().isInterrupted()) {
            BufferedReader r = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String request = r.readLine();
            if (request == null) {
                break;
            }
            if (handler != null) {
                String response = handler.handle(request);
                client.getOutputStream().write((response).getBytes(StandardCharsets.ISO_8859_1));
                client.getOutputStream().flush();
            }
        }
        client.close();
    }

    public void stop() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                LOG.error("unable to close socket", e);
            }
        }
        if (thread != null) {
            thread.interrupt();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

}
