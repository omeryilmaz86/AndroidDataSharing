package com.datasharing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;


public class TCPCommunicator {
    private static TCPCommunicator uniqInstance;
    private static String serverHost;
    private static int serverPort;
    private static Set<TCPListener> allListeners;
    private static BufferedWriter out;
    private static BufferedReader in;
    private static Socket s;
    private static Handler UIHandler;
    private Activity context;
    Handler handler = new Handler();

    private TCPCommunicator() {
        allListeners = new HashSet<>();
    }

    public static TCPCommunicator getInstance() {
        if (uniqInstance == null) {
            uniqInstance = new TCPCommunicator();
        }
        return uniqInstance;
    }

    public static void removeListener(TCPListener tcpListener) {
        if (allListeners != null)
            allListeners.remove(tcpListener);
    }


    public void init(String host, int port) {
        setServerHost(host);
        setServerPort(port);
        InitTCPClientTask task = new InitTCPClientTask();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    static void writeToSocket(final String obj, Handler handle) {
        UIHandler = handle;
        Runnable runnable = () -> {
            try {
                String outMsg = obj + System.getProperty("line.separator");
                out.write(outMsg);
                out.flush();
            } catch (Exception e) {
                UIHandler.post(e::printStackTrace);
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();

    }

    static void addListener(TCPListener listener) {
        if (allListeners != null) {
            allListeners.clear();
            allListeners.add(listener);
        }
    }

    static void removeAllListeners() {
        if (allListeners != null)
            allListeners.clear();
    }

    static void closeStreams() {
        try {
            s.close();
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static String getServerHost() {
        return serverHost;
    }

    private static void setServerHost(String serverHost) {
        TCPCommunicator.serverHost = serverHost;
    }

    private static int getServerPort() {
        return serverPort;
    }

    private static void setServerPort(int serverPort) {
        TCPCommunicator.serverPort = serverPort;
    }

    public void setContext(Activity context) {
        this.context = context;
    }


    @SuppressLint("StaticFieldLeak")
    public class InitTCPClientTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {

                s = new Socket();
                s.setKeepAlive(true);
                s.setSoLinger(true, 1000);
                SocketAddress remoteAddress = new InetSocketAddress(getServerHost(), getServerPort());
                Log.d("connect to server", "inside");
                s.connect(remoteAddress);
                Log.d("connect to server", "true");
                in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                Log.d("Server time out ", s.getSoTimeout() + "");
                DataInputStream dataOS = new DataInputStream(s.getInputStream());
                for (TCPListener listener : allListeners)
                    listener.onTCPConnectionStatusChanged(true);
                String inMsg;
                while (in != null && (inMsg = in.readLine()) != null) {

                        for (TCPListener listener : allListeners)
                            listener.onTCPMessageReceived(inMsg);

                }

            } catch (IOException e) {
                e.printStackTrace();
                for (TCPListener listener : allListeners)
                    listener.onErrorMessage(e.getMessage());
            }

            return null;

        }

    }






}
