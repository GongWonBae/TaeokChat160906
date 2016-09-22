package univ.anyang.taeokchat160906;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;



public class MainActivity extends Activity implements BeaconConsumer {


    // private static int port = 5001;
    // private static final String ipText = "192.168.0.7"; // IP지정으로 사용시에 쓸 코드
    String streammsg = "";
    String Str_LoginJson;
    TextView showText;
    TextView sendText_View;
    TextView beaconText_View;

    Button connectBtn;
    Button Button_send;
    Button login_Btn;
    Button beacon_Btn;
    Button search_Btn;

    EditText ip_EditText;
    EditText port_EditText;
    EditText editText_massage;
    EditText sid_Text;
    EditText pw_Text;

    Handler msghandler;

    SocketClient client;
    ReceiveThread receive;
    SendThread send;
    Socket socket;

    Boolean LoginBoolean;

    PipedInputStream sendstream = null;
    PipedOutputStream receivestream = null;

    LinkedList<SocketClient> threadList;
    private BeaconManager beaconManager;
    // 감지된 비콘들을 임시로 담을 리스트
    private List<Beacon> beaconList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LoginBoolean = false;
        ////////////////////////////비콘 관련 변수 복붙 /////////////////////////////

        // 실제로 비콘을 탐지하기 위한 비콘매니저 객체를 초기화
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconText_View = (TextView)findViewById(R.id.BeaconList_View);

        // 여기가 중요한데, 기기에 따라서 setBeaconLayout 안의 내용을 바꿔줘야 하는듯 싶다.
        // 필자의 경우에는 아래처럼 하니 잘 동작했음.
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));

        // 비콘 탐지를 시작한다. 실제로는 서비스를 시작하는것.
        beaconManager.bind(this);
        ///////////////////////////비콘관련 변수 복붙.//////////////////////////////

        ip_EditText = (EditText) findViewById(R.id.ip_EditText);
        port_EditText = (EditText) findViewById(R.id.port_EditText);
        connectBtn = (Button) findViewById(R.id.connect_Button);
        showText = (TextView) findViewById(R.id.showText_TextView);
        editText_massage = (EditText) findViewById(R.id.editText_massage);
        Button_send = (Button) findViewById(R.id.Button_send);

        sid_Text=(EditText)findViewById(R.id.ID_Text);
        pw_Text=(EditText) findViewById(R.id.PW_Text);
        login_Btn =(Button) findViewById(R.id.login_btn);
        sendText_View= (TextView) findViewById(R.id.sendText_TextView);
        beaconText_View=(TextView)findViewById(R.id.BeaconList_View);
        search_Btn=(Button)findViewById(R.id.Serch_btn);
        threadList = new LinkedList<MainActivity.SocketClient>();


        ip_EditText.setText("121.139.168.55");
        port_EditText.setText("8888");

        // ReceiveThread를통해서 받은 메세지를 Handler로 MainThread에서 처리(외부Thread에서는 UI변경이불가)
        msghandler = new Handler() {
            @Override
            public void handleMessage(Message hdmsg) {
                if (hdmsg.what == 1111) {
                    showText.setText(hdmsg.obj.toString() );
                    if(hdmsg.obj.toString().contains("TRUE"))
                    {
                        Intent intent = new Intent(getApplicationContext(),HomeActivity.class);
                        startActivityForResult(intent,1001);
                    }
                }
            }
        };

        // 연결버튼 클릭 이벤트
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                //Client 연결부
                client = new SocketClient(ip_EditText.getText().toString(),
                        port_EditText.getText().toString());
                threadList.add(client);
                client.start();
            }
        });

        //전송 버튼 클릭 이벤트
        Button_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                //SendThread 시작
                if (editText_massage.getText().toString() != null) {
                    send = new SendThread(socket);
                    send.start();

                    //시작후 edittext 초기화
                    editText_massage.setText("");
                }
            }
        });
        login_Btn.setOnClickListener(new View.OnClickListener() {
            DataOutputStream output;
            @Override
            public void onClick(View v) {
                String str_id = sid_Text.getText().toString();
                String str_pw = pw_Text.getText().toString();
                try
                {
                    LoginJson LJ0 = new LoginJson(str_id,str_pw);
                    Str_LoginJson = LJ0.str;

                }catch(JSONException e) { }
                try {
                    output = new DataOutputStream(socket.getOutputStream());
                } catch (Exception e) {
                }
                if (output != null) {
                    if (Str_LoginJson != null) {
                        try {
                            output.writeUTF(Str_LoginJson);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        search_Btn.setOnClickListener(new View.OnClickListener() {
            DataOutputStream output;
            String Str_search = "{\"SEARCH\":["
                    +"]}";
            @Override
            public void onClick(View v) {
                try {
                    output = new DataOutputStream(socket.getOutputStream());
                } catch (Exception e) {}
                if (output != null) {

                        try {
                            output.writeUTF(Str_search);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                }
            }
        });
    }
//비콘 인터페이스의 메소드
    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            // 비콘이 감지되면 해당 함수가 호출된다. Collection<Beacon> beacons에는 감지된 비콘의 리스트가,
            // region에는 비콘들에 대응하는 Region 객체가 들어온다.
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    beaconList.clear();
                    for (Beacon beacon : beacons) {
                        beaconList.add(beacon);
                    }
                }
            }

        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
    }
    public void OnButtonClicked(View view){
        // 아래에 있는 handleMessage를 부르는 함수. 맨 처음에는 0초간격이지만 한번 호출되고 나면
        // 1초마다 불러온다.
        handler.sendEmptyMessage(0);
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            beaconText_View.setText("");

            // 비콘의 아이디와 거리를 측정하여 textView에 넣는다.
            for(Beacon beacon : beaconList){
                beaconText_View.append("ID : " + beacon.getId1() + " / " + "Distance : " + Double.parseDouble(String.format("%.3f", beacon.getDistance())) + "m\n");
            }

            // 자기 자신을 1초마다 호출
            handler.sendEmptyMessageDelayed(0, 1000);
        }
    };


    class SocketClient extends Thread {
        boolean threadAlive;
        String ip;
        String port;
        String mac;

        //InputStream inputStream = null;
        OutputStream outputStream = null;
        BufferedReader br = null;

        private DataOutputStream output = null;

        public SocketClient(String ip, String port) {
            threadAlive = true;
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void run() {

            try {
                // 연결후 바로 ReceiveThread 시작
                socket = new Socket(ip, Integer.parseInt(port));
                //inputStream = socket.getInputStream();
                output = new DataOutputStream(socket.getOutputStream());
                receive = new ReceiveThread(socket);
                receive.start();

                //mac주소를 받아오기위해 설정
                WifiManager mng = (WifiManager) getSystemService(WIFI_SERVICE);
                WifiInfo info = mng.getConnectionInfo();
                mac = info.getMacAddress();

                //mac 전송
                //output.writeUTF(mac);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ReceiveThread extends Thread {
        private Socket socket = null;
        DataInputStream input;

        public ReceiveThread(Socket socket) {
            this.socket = socket;
            try{
                input = new DataInputStream(socket.getInputStream());
            }catch(Exception e){
            }
        }
        // 메세지 수신후 Handler로 전달
        public void run() {
            try {
                while (input != null) {

                    String msg = input.readUTF();
                    if (msg != null) {
                        Log.d(ACTIVITY_SERVICE, "test");

                        Message hdmsg = msghandler.obtainMessage();
                        hdmsg.what = 1111;
                        hdmsg.obj = msg;
                        msghandler.sendMessage(hdmsg);
                        Log.d(ACTIVITY_SERVICE,hdmsg.obj.toString());

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class SendThread extends Thread {
        private Socket socket;

        String sendmsg = editText_massage.getText().toString();
        DataOutputStream output;
        String Beacon_json = "{" +
                "  \"BEACON\": [" +
                "    {" +
                "      \"SID\": \"201131046\"," +
                "      \"PHONE\": \"1062928744\"," +
                "      \"NOWTIME\": \"16/05/07/13/57\"," +
                "      \"CLASS_CODE\": \"AN0044\"," +
                "      \"CLASS_NO\": \"01\"," +
                "      \"CLASSROOM\":\"A607\","+
                "      \"WEEK\" : \"13\","+
                "      \"BEACON_CNT\": 4," +
                "      \"BEACON_INFO\": [" +
                "        {" +
                "          \"UUID\": \"e2c56db5-dffb-48d2-b060-d0f5a71096e0\"," +
                "          \"MAJOR\": \"1000\"," +
                "          \"MINOR\": \"2000\"," +
                "          \"DISTANCE\": \"15\"" +
                "        }," +
                "        {" +
                "          \"UUID\": \"2221\"," +
                "          \"MAJOR\": \"2222\"," +
                "          \"MINOR\": \"2223\"," +
                "          \"DISTANCE\": \"2224\"" +
                "        }," +
                "        {" +
                "          \"UUID\": \"3331\"," +
                "          \"MAJOR\": \"3332\"," +
                "          \"MINOR\": \"3333\"," +
                "          \"DISTANCE\": \"3334\"" +
                "        }," +
                "        {" +
                "          \"UUID\": \"4441\"," +
                "          \"MAJOR\": \"4442\"," +
                "          \"MINOR\": \"4443\"," +
                "          \"DISTANCE\": \"4444\"" +
                "        }" +
                "      ]" +
                "    }" +
                "  ]" +
                "}";


        public SendThread(Socket socket) {
            this.socket = socket;
            try {
                output = new DataOutputStream(socket.getOutputStream());
            } catch (Exception e) {
            }
        }

        public void run() {

            try {

                // 메세지 전송부 (누군지 식별하기위한 방법으로 mac를 사용)
                Log.d(ACTIVITY_SERVICE, "11111");
                String mac = null;
                WifiManager mng = (WifiManager) getSystemService(WIFI_SERVICE);
                WifiInfo info = mng.getConnectionInfo();
                mac = info.getMacAddress();

                if (output != null) {
                    if (sendmsg != null) {
                        output.writeUTF(Beacon_json);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException npe) {
                npe.printStackTrace();

            }
        }
    }
}
