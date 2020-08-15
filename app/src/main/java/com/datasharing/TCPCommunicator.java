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

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
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

    @SuppressLint("NewApi")
    void sendFiles(ArrayList<SendFileModel> list) {

        if (list.size() == 0) return;
        for (TCPListener listener : allListeners)
            listener.CreateProgressDialog();
        try {
            Runnable runnable = () -> {
                try {
                    try {
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).type == null || list.get(i).type.equals(JsonHelper.FILE)) {
                                try {

                                    JSONObject jsonObject = new JSONObject();
                                    jsonObject.put(JsonHelper.FILE, true);
                                    try {
                                        String outMsg = jsonObject.toString() + System.getProperty("line.separator");
                                        out.write(outMsg);
                                        out.flush();
                                        Thread.sleep(1000);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                SendFileModel sendFileModel = list.get(i);
                                ArrayList<File> files = new ArrayList<>();
                                files.addAll(list.get(i).files);
                                long totalSize = 0;
                                float sendSize = 0;
                                for (int j = 0; j < files.size(); j++) {
                                    totalSize = totalSize + files.get(j).length();

                                    DataOutputStream dataOS = new DataOutputStream(s.getOutputStream());
                                    OutputStream out = s.getOutputStream();
                                    dataOS.writeLong(totalSize);
                                    dataOS.writeInt(files.size());
                                    File file = files.get(j);
                                    int size = (int) (file.length());
                                    dataOS.writeInt(size);
                                    dataOS.writeUTF(file.getName());
                                    dataOS.writeUTF(file.getParentFile().getName());
                                    byte[] bytes = new byte[16 * 1024];
                                    InputStream in = new FileInputStream(file);
                                    int count;
                                    while ((count = in.read(bytes)) > 0) {
                                        sendSize = sendSize + count;
                                        out.write(bytes, 0, count);
                                        float per = (sendSize / totalSize) * 100;
                                        for (TCPListener listener : allListeners)
                                            listener.updatePercentage((int) per);
                                    }
                                }
                                out.flush();

                                sendFileModel.isSend = true;
                                ArrayList<SendFileModel> sendFileModels = (ArrayList<SendFileModel>) ((MyApplicationClass) (context.getApplicationContext())).getFilesList().clone();
                                sendFileModels.set(0, sendFileModel);
                                ((MyApplicationClass) (context.getApplicationContext())).setMap(sendFileModels);

                                for (TCPListener listener : allListeners)
                                    listener.updateList();
                            }

                        }
                        for (TCPListener listener : allListeners)
                            listener.dismissDialog();

                        ((MyApplicationClass) (context.getApplicationContext())).reset();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };

            Thread thread = new Thread(runnable);
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

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


                        if (inMsg.contains(JsonHelper.FILE)) {
                            receiveFiles(dataOS);
                        }  else {
                            for (TCPListener listener : allListeners)
                                listener.onTCPMessageReceived(inMsg);
                        }

                }

            } catch (IOException e) {
                e.printStackTrace();
                for (TCPListener listener : allListeners)
                    listener.onErrorMessage(e.getMessage());
            }

            return null;

        }

    }



    void receiveFiles(DataInputStream dataOS) throws IOException {
        OutputStream outputStream;
        for (TCPListener listener : allListeners)
            listener.showProgressDialog();
        long totalSize = dataOS.readLong();
        int totalFile = dataOS.readInt();
        float readSize = 0;
        for (int i = 0; i < totalFile; i++) {
            int len = dataOS.readInt();
            String fileName = dataOS.readUTF();
            String folderName = dataOS.readUTF();
            String path1 = Environment.getExternalStorageDirectory().getPath() + "/DataSharing/" + folderName;
            File file = new File(path1);
            if (!file.exists()) {
                file.mkdirs();
            }
            File file1 = new File(file, fileName);
            byte[] bytes = new byte[16 * 1024];

            outputStream = new FileOutputStream(file1);
            int count;

            while (len > 0 && (count = dataOS.read(bytes, 0, Math.min(bytes.length, len))) > 0) {
                len = len - count;
                outputStream.write(bytes, 0, count);
                outputStream.flush();
                readSize = readSize + count;
                for (TCPListener listener : allListeners) {
                    float percentage = (readSize / totalSize) * 100;
                    listener.updatePercentage((int) (percentage));
                }
            }

            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file1)));
            MediaScannerConnection.scanFile(
                    context,
                    new String[]{file1.getAbsolutePath()},
                    null,
                    (path, uri) -> {
                        long id = WifiHotSpots.getSongIdFromMediaStore(file1.getPath(), context);
                        for (TCPListener listener : allListeners) {
                            listener.fileReceived(id, file1);
                        }
                    });
        }
        for (TCPListener listener : allListeners)
            listener.dismissDialog();


    }



}
