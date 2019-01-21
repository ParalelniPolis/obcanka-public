/*
 * Copyright 2019 Paralelni Polis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.paralelnipolis.obcanka.android.app;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.acs.smartcard.Reader;
import cz.paralelnipolis.obcanka.android.lib.AndroidCardInterface;
import cz.paralelnipolis.obcanka.core.HexUtils;
import cz.paralelnipolis.obcanka.core.card.Card;
import cz.paralelnipolis.obcanka.core.certificates.Certificate;
import cz.paralelnipolis.obcanka.core.certificates.IdentificationCertificate;
import cz.paralelnipolis.obcanka.core.communication.CardException;
import cz.paralelnipolis.obcanka.core.communication.ICardInterface;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivityFragment extends Fragment {
    private static final String ACTION_USB_PERMISSION = "cz.paralelnipolis.obcanka.android.USB_PERMISSION";
    private UsbManager mManager;
    private Reader mReader;
    private PendingIntent mPermissionIntent;
    private ICardInterface ci;
    private Card cm;


    public MainActivityFragment() {
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // Open reader
                            logMsg("Opening reader: " + device.getDeviceName()+ "...");
                            new OpenTask().execute(device);
                        }
                    } else {
                        logMsg("Permission denied for device " + device.getDeviceName());
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (mReader.isSupported(device)) {
                        if (device != null && device.equals(mReader.getDevice())) {
                            // Close reader
                            logMsg("Opening reader: " + device.getDeviceName()+ "...");
                            new OpenTask().execute(device);
                        }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null && device.equals(mReader.getDevice())) {
                        // Close reader
                        logMsg("Closing reader...");
                        new CloseTask().execute();
                    }
                }
            }
        }
    };

    private void logMsg(String msg) {
        DateFormat dateFormat = new SimpleDateFormat("[dd-MM-yyyy HH:mm:ss]: ");
        Date date = new Date();
        System.out.println(dateFormat.format(date) + msg);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mManager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);

        mReader = new Reader(mManager);
        mReader.setOnStateChangeListener(new Reader.OnStateChangeListener() {
            @Override
            public void onStateChange(int slotNum, int prevState, int currState) {
                if (prevState < Reader.CARD_UNKNOWN || prevState > Reader.CARD_SPECIFIC) {
                    prevState = Reader.CARD_UNKNOWN;
                }

                if (currState < Reader.CARD_UNKNOWN || currState > Reader.CARD_SPECIFIC) {
                    currState = Reader.CARD_UNKNOWN;
                }

                if (prevState == Reader.CARD_ABSENT && currState == Reader.CARD_PRESENT) {
                    logMsg("Card inserted lets do something.");
                    if (mReader.isOpened()) {
                        readInformationFromTheCard();
                    }
                }else{
                    logMsg("Reader state changed: " + prevState + " " + currState);
                }
            }
        });

        // Register receiver for USB permission
        mPermissionIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        getContext().registerReceiver(mReceiver, filter);

        for (UsbDevice device : mManager.getDeviceList().values()) {
            // If device name is found
            if (mReader.isSupported(device)) {
                // Request permission
                mManager.requestPermission(device, mPermissionIntent);
                break;
            }
        }
    }

    private void readInformationFromTheCard() {
        try {
            String cardID = cm.getCardNumber();
            System.out.println("cardID = " + cardID);
            byte[] serialNumber = cm.getSerialNumber();
            System.out.println("serialNumber = " + HexUtils.bytesToHexStringWithSpacesAndAscii(serialNumber));
            byte[] x = cm.getData(0xC0, 1); // returns: 43 37 c9
            System.out.println("x = " + HexUtils.bytesToHexStringWithSpacesAndAscii(x));
            byte[] y = cm.getData(0xC1, 1); // returns: 00 00 00 00 00 00 00 00
            System.out.println("y = " + HexUtils.bytesToHexStringWithSpacesAndAscii(y));


            System.out.println("dokState = " + cm.getDokState());
            System.out.println("cm.getDokMaxTryLimit() = " + cm.getDokMaxTryLimit());
            System.out.println("cm.getDokTryLimit() = " + cm.getDokTryLimit());

            System.out.println("iokState = " + cm.getIokState());
            System.out.println("cm.getIokMaxTryLimit() = " + cm.getIokMaxTryLimit());
            System.out.println("cm.getIokTryLimit() = " + cm.getIokTryLimit());

            final IdentificationCertificate certificate = (IdentificationCertificate)cm.getCertificate(Certificate.CertificateType.IDENTIFICATION);
            System.out.println("certificate = " + certificate);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(),certificate+"",Toast.LENGTH_LONG).show();
                }
            });

        } catch (CardException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }


    private class OpenTask extends AsyncTask<UsbDevice, Void, Exception> {

        @Override
        protected Exception doInBackground(UsbDevice... params) {
            Exception result = null;
            try {
                mReader.open(params[0]);
                ci = AndroidCardInterface.create(mReader,0);
                cm = new Card(ci);
            } catch (Exception e) {
                result = e;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {
                logMsg(result.toString());
            } else {
                logMsg("Reader name: " + mReader.getReaderName());

                int numSlots = mReader.getNumSlots();
                logMsg("Number of slots: " + numSlots);
            }
        }
    }

    private class CloseTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            mReader.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }


    @Override
    public void onDestroy() {
        // Close reader
        mReader.close();
        // Unregister receiver
        getContext().unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
