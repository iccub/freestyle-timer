package bucci.dev.freestyle;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class demoService extends IntentService {
    public demoService() {
        super("demoService");
    }

    Messenger messenger;
    CountDownTimer countdown;

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("xxxx", "tu weszlo");

        messenger = (Messenger) intent.getExtras().get("incomingHandler");
        countdown = new CountDownTimer(180000, 500) {

            @Override
            public void onTick(long l) {
                Log.d("xxxx", "tick");
                Message msg = Message.obtain();
                Bundle data = new Bundle();
                data.putString("result", formatLongToTimerText(l));
                msg.setData(data);

                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    Log.d("xxxx", "error: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void onFinish() {
                Message msg = Message.obtain();
                Bundle data = new Bundle();
                data.putString("result", "0:00");
                msg.setData(data);

                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    String formatLongToTimerText(long l) {
        int seconds = (int) (l / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        return String.format("%d:%02d", minutes, seconds);
    }
}

