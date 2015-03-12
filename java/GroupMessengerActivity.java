package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.concurrent.TimeoutException;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko, avinav sharan
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String KEY_FIELD = "key";
    static final String VALUE_FIELD = "value";
    static final String SERVER_PORT = "10000";
    static final String[] REMOTE_PORTS_VAL = {"11108","11112", "11116", "11120", "11124"};
    static ArrayList<String> REMOTE_PORTS = new ArrayList<String>(Arrays.asList(REMOTE_PORTS_VAL));
    static int NO_OF_PROCS = 5;
    static final int REQ_TIMEOUT = 10000;
    static final String AGREE = "agree", ACK = "ack", NEW = "new", DELIVER = "deliver";

    static int MY_PID , MY_EID = 0;
    static int id = -1;
    static final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

    static HashSet<String> msgSet = new HashSet<String>();
    static HashSet<String> deliveredSet = new HashSet<String>();
    static HashMap<String, Message1> idMap = new HashMap<String,Message1>();
    static HashMap<String, HashSet<Message1>> agreedMap = new HashMap<String, HashSet<Message1>>();
    static PriorityQueue<Message1> msgQueue = new PriorityQueue<Message1>(10, new MessageSort());


    Object lock1 = new Object();
    private static Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    public String getMid() {
        return String.valueOf(MY_EID) + "." + String.valueOf(MY_PID);
    }

    public static class Message1 implements Serializable {
        private static final long serialVersionUID = 1L;
        String msgTxt, mid, type;
        int pid, eid;

        public Message1(String txt, String mid, int eid, int pid, String type) {
            this.msgTxt = txt;
            this.mid = mid;
            this.eid = eid;
            this.pid = pid;
            this.type = type;
        }
    }

    public static class MessageSort implements Comparator<Message1> {
        public int compare(Message1 m1, Message1 m2) {
            int diff_eid = m1.eid - m2.eid;
            int diff_pid = m1.pid - m2.pid;
            if (diff_eid == 0)
                return diff_pid;
            else
                return diff_eid;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        MY_PID = Integer.parseInt(myPort);


        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        try {
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(SERVER_PORT));
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {
            Log.e(TAG, "Cannot create server port!");
        }
        final EditText editText = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String msg = editText.getText().toString() + "\n";
                        editText.setText("");
                        Log.e(TAG, "hello");
//                tv.append("\t" + msg);
//                        synchronized (lock1) {
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort, NEW);
//                        }
                    }
                });
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, Message1, Void> {
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try {
                while(true) {
                    Socket socket = serverSocket.accept();
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    Message1 m = (Message1) in.readObject();
                    publishProgress(m);


//                    synchronized (this) {

//                    }
                    /*Socket socket = serverSocket.accept();
                    InputStream in = socket.getInputStream();
                    DataInputStream dIn = new DataInputStream(in);
                    int i = 0;
                    StringBuffer sb = new StringBuffer();
                    while ((i = dIn.read()) != -1) {
                        sb.append((char)i);
                    }
                    publishProgress(sb.toString());
                    socket.close(); */
                }
            } catch (IOException e) {
                Log.e(TAG, "Server IO Exception!");
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Server ClassNotFound Exception!");
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(Message1... m) {
            Log.e(TAG,"Here in server progress!");
                new ProcessMessageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,m);



            /*String strReceived = strings[0];
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(strReceived + "\t\n");
            ContentValues contentValues = new ContentValues();
            contentValues.put(KEY_FIELD, Integer.toString(++ id));
            contentValues.put(VALUE_FIELD, strReceived);
            getContentResolver().insert(mUri, contentValues);*/
        }
    }

    private class ProcessMessageTask extends AsyncTask<Message1, String, Void> {
        @Override
        protected Void doInBackground(Message1... msg) {
            Log.e(TAG,"Here in process message task!");
            Message1 m = msg[0];
            if (!idMap.containsKey(m.mid)) {
                idMap.put(m.mid, m);
            }
            HashSet<Message1> set = agreedMap.get(m.mid);
            if (set == null) {
                set = new HashSet<Message1>();
                agreedMap.put(m.mid,set);
            }
//            idMap.put(m.mid, m);
            if (NEW.equals(m.type)) {// && m.pid != MY_PID) {
                Log.e(TAG, "Here in processMsg new!");
//                if (!msgSet.contains(m.mid)) {
                    ++MY_EID;
                    m.eid = MY_EID;
                    msgSet.add(m.mid);
                    idMap.put(m.mid, m);

                    set.add(m);
                    agreedMap.put(m.mid,set);
//                    Integer count = agreedMap.get(m.mid);
//                    if (count != null) {
//                        agreedMap.put(m.mid, count + 1);
//                    }
//                    else {
//                        agreedMap.put(m.mid, 1);
//                    }
                    msgQueue.offer(m);
//                                synchronized (lock1) {
                    if (m.pid != MY_PID) {
                        publishProgress(ACK, m.mid, m.msgTxt);
                    }
//                                }
//                }
            } else if (ACK.equals(m.type) && m.pid != MY_PID) {
                Log.e(TAG, "Here in processMsg ack!");
//                Message1 old_msg = idMap.get(m.mid);
//                if (old_msg != null) {
//                    if (m.eid > old_msg.eid ||
//                            (m.eid == old_msg.eid && m.pid > old_msg.pid)) {
//                        update(m.mid, m.eid, m.pid);
//                        deliver();
//                    }
                    set.add(m);
                    agreedMap.put(m.mid, set);
                    Integer count = agreedMap.get(m.mid).size();
                    if(count != null) {
                        if (count == NO_OF_PROCS) {
//                                        synchronized(lock1) {
//                        updateQueue(m.mid, m.eid, m.pid);
                            set = agreedMap.get(m.mid);
                            int temp_eid = m.eid, temp_pid =m.pid;
                            for(Message1 ms : set) {
                                if (ms.eid > temp_eid ||
                                        (ms.eid == temp_eid && ms.pid > temp_pid)) {
                                    temp_eid = ms.eid;
                                    temp_pid = ms.pid;
                                }
                            }
                            update(m.mid, temp_eid, temp_pid);
                            publishProgress(AGREE,m.mid,m.msgTxt);
//                                        }
                        }
                    }
//                    else {
//                        agreedMap.put(m.mid,1);
//                    }
//                }

            } else if (AGREE.equals(m.type)) {
                Log.e(TAG, "Here in processMsg agree!");
                MY_EID = Math.max(MY_EID, m.eid);
                update(m.mid, m.eid, m.pid);
                deliveredSet.add(m.mid);
                deliver();
            }
            return null;
        }

        public void update(String mid, int eid, int pid) {
            // update idMap and msgQueue
            Message1 m = idMap.get(mid);
            msgQueue.remove(m);
            m.eid = eid;
            m.pid = pid;
            msgQueue.offer(m);
        }
/*
        public void updateQueue(String mid, int eid, int pid) {
            Message1 m = idMap.get(mid);
            msgQueue.remove(m);
            m.eid = eid;
            m.pid = pid;
            msgQueue.offer(m);
        }
*/
        public void deliver() {
            //while queue is not empty or deliveredSet.contains(queue.peek())
            //queue.pop() and display message
            Log.e(TAG, "Here in deliver!");
            Log.e(TAG, "Queue: " +msgQueue.peek().mid);
            for (String s : deliveredSet) {
                Log.e(TAG, "delSet:" + s);
            }
            while(!msgQueue.isEmpty()&&deliveredSet.contains(msgQueue.peek().mid)) {
                Log.e(TAG, "publishing deliver!");
                Message1 m = msgQueue.poll();
                publishProgress(DELIVER,m.mid,m.msgTxt);

            }
        }

        protected void onProgressUpdate(String...strings) {
            Log.e(TAG, "Here in processMsg progress!");
            String type = strings[0];
            String mid = strings[1];
            String msgTxt = strings[2];
            if (DELIVER.equals(type)) {
                Log.e(TAG, "on progress deliver!");
                TextView tv = (TextView) findViewById(R.id.textView1);
                tv.append(msgTxt + "\t\n");
                ContentValues contentValues = new ContentValues();
                contentValues.put(KEY_FIELD, String.valueOf(++id));
                contentValues.put(VALUE_FIELD, msgTxt);
                getContentResolver().insert(mUri, contentValues);
            } else if (ACK.equals(type)) {
                Log.e(TAG, "Here in processMsg progress ack!");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTxt, "", type, mid);
            } else if (AGREE.equals(type)) {
                Log.e(TAG, "Here in processMsg progress agree!");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTxt, "", type, mid);
            }
        }

    }

    private class ClientTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... msgs) {
            //msg_txt = msgs[0], myPort = msgs[1], type = msgs[2], mid = msgs[3]
            Log.e(TAG,"Here in client Task!");
            send(msgs);
            return null;
        }
        public void send(String... msgs) {
            String type = msgs[2];
            if (NEW.equals(type)) {
                Log.e(TAG,"Here in client Task new!");
                String msg = msgs[0];
//                ++MY_EID;
                Message1 m = new Message1(msg,getMid(),MY_EID,MY_PID,type);
//                agreedMap.put(m.mid, 1);
//                msgSet.add(m.mid);
//                idMap.put(m.mid, m);
                //queue add
//                msgQueue.offer(m);
//                outMulticast(m,msgs);
                bMulticast(m,0,msgs);
            }
            else if (ACK.equals(type)) {
                Log.e(TAG,"Here in client Task ack!");
                String mid = msgs[3];
                Message1 m = idMap.get(mid);
                if (m== null) {
                    Log.e(TAG,"MID:" +mid);
                }
                int remotePort = m.pid;
                m.type = type;
                m.pid = MY_PID;
                sendAck(m,remotePort);
            }
            else if (AGREE.equals(type)) {
                Log.e(TAG,"Here in client Task agree!");
                String mid = msgs[3];
                Message1 m = idMap.get(mid);
                MY_EID = Math.max(m.eid, MY_EID);
                m.type = type;
                bMulticast(m,0,msgs);
            }
        }
        public void sendAck(Message1 m, int remotePort) {
            try {
                Log.e(TAG,"Here in send Ack!");
//                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
//                        remotePort);
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        remotePort),REQ_TIMEOUT);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(m);
                socket.close();
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "Client Socket Timeout exception Ack!");
                if (!REMOTE_PORTS.isEmpty()) {
                    REMOTE_PORTS.remove(String.valueOf(remotePort));
                    NO_OF_PROCS--;
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "Client Unknown host exception!");
            } catch (IOException e) {
                Log.e(TAG, "Client Socket io exception!");
                e.printStackTrace();
            }
        }
        /*public void outMulticast(Message1 m, String... msgs) {
            try {
                Log.e(TAG,"Here in outMulticast!");
                Socket socket;
                ObjectOutputStream out;
                for(String remotePort : REMOTE_PORTS) {
                    if (Integer.parseInt(remotePort) != MY_PID) {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    OutputStream outS = socket.getOutputStream();
                    out = new ObjectOutputStream(outS);
                    out.writeObject(m);
                    out.close();
                    socket.close();
                    }
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "Client Unknown host exception!");
            } catch (IOException e) {
                Log.e(TAG, "Client Socket io exception!");
                e.printStackTrace();
            }
        }*/

        public void bMulticast(Message1 m, int count, String... msgs) {
            String remotePort = null;
            try {
                Log.e(TAG,"Here in client Task bmulti!");
                Socket socket;
//                OutputStream out;
//                DataOutputStream dOut;
                ObjectOutputStream out;
//                String ownPort = msgs[1];
                for(;count < REMOTE_PORTS.size(); count++) {
                    remotePort = REMOTE_PORTS.get(count);
//                    if (!ownPort.equals(remotePort)) {
//                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
//                            Integer.parseInt(remotePort));

                    socket = new Socket();
                    socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort)), REQ_TIMEOUT);
//                    byte[] msgToSend = msgs[0].getBytes();
//                    dOut = new DataOutputStream(out);
//                    dOut.write(msgToSend);
                    OutputStream outS = socket.getOutputStream();
                    out = new ObjectOutputStream(outS);
                    out.writeObject(m);
                    out.close();
                    socket.close();
//                    }
                }
            }  catch (SocketTimeoutException e) {
                Log.e(TAG, "Client Socket Timeout exception!");
                if (remotePort != null && !REMOTE_PORTS.isEmpty()) {
                    REMOTE_PORTS.remove(remotePort);
                    NO_OF_PROCS --;
//                    bMulticast(m,++count,msgs);
                }
            }  catch (UnknownHostException e) {
                Log.e(TAG, "Client Unknown host exception!");
            } catch (IOException e) {
                Log.e(TAG, "Client Socket io exception!");
                e.printStackTrace();
            }
        }



    }
}
