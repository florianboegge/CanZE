/*
    CanZE
    Take a closer look at your ZE car

    Copyright (C) 2015 - The CanZE Team
    http://canze.fisch.lu

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or any
    later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package lu.fisch.canze.activities;


import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;

import lu.fisch.canze.R;
import lu.fisch.canze.actors.Ecu;
import lu.fisch.canze.actors.Ecus;
import lu.fisch.canze.actors.Field;
import lu.fisch.canze.actors.Fields;
import lu.fisch.canze.interfaces.FieldListener;


// Jeroen

public class FirmwareActivity extends CanzeActivity implements FieldListener {

    // There are hardware dependencies here (i.e. different CLIMA ECU's with different software loads) that we do not yet fully understand.

    //                                             EVC     TCU     LBC     PEB     AIBAG   USM     CLUSTER EPS     ABS     UBP     BCM     CLIM    UPA     BCB     LBC2    LINSCH
//  private static final int [] zoeVersions     = {0x0203, 0x0767, 0x0751, 0x0a06, 0x0470, 0xc630, 0x0507, 0x014a, 0x1178, 0x1523, 0x140e, 0x0702, 0x0017, 0x0210, 0x0751,      0};
//  private static final int [] fluenceVersions = {0x0202, 0x0172, 0x7505, 0x02ba, 0x0305, 0x0907, 0x0026, 0x014a, 0x8160,      0, 0x140e, 0x0007, 0x0024, 0xd300, 0x5c0a,      0};
//  private static final int [] kangooVersions  = {0x0201, 0x1011, 0x7505, 0x0205,      1,      1, 0x003d,      1,      1,      1, 0x0003,      1,      1, 0xd300, 0x5c0a,      1};
//  private static final int [] x10Versions     = zoeVersions;
    private static final int [] zoeVersions     = {0x0790, 0x1203, 0x5000, 0x0602, 0x7004, 0x3024, 0x0705, 0x4a08, 0x780e, 0x160c, 0x0000, 0x0515, 0x1700, 0x0000, 0x5000, 0x0000};
    private static final int [] fluenceVersions = {0x0830, 0x0000, 0x0000, 0x1668, 0x0000, 0x0400, 0x0400, 0x0000, 0x4009, 0x0000, 0x0000, 0xe504, 0x0000, 0x3066, 0x0000, 0x0000};
    private static final int [] kangooVersions  = {0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000};
    private static final int [] x10Versions     = zoeVersions;

    private static int versions [] = null;


    private ArrayList<Field> subscribedFields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firmware);

        subscribedFields = new ArrayList<>();

        switch (MainActivity.car) {
            case MainActivity.CAR_FLUENCE:
                versions = fluenceVersions;
                break;
            case MainActivity.CAR_KANGOO:
                versions = kangooVersions;
                break;
            case MainActivity.CAR_X10:
                versions = x10Versions;
                break;
            default:
                versions = zoeVersions;
                break;
        }

        for (Ecu ecu : Ecus.getInstance().getAllEcus()) {
            // ensure we are only selecting true (as in physical boxes) and reachable (as in, i.e. skipping R-LINK) ECU's
            if (ecu.getFromId() > 0 && ecu.getFromId() < 0x800) {
                TextView tv;
                tv = (TextView) findViewById(getResources().getIdentifier("lEcu" + Integer.toHexString (ecu.getFromId()).toLowerCase(), "id", getPackageName()));
                if (tv != null) {
                    tv.setText(ecu.getMnemonic() + " (" + ecu.getName() + ")");
                } else {
                    MainActivity.toast("No view with id 'lEcu" + Integer.toHexString (ecu.getFromId()).toLowerCase() + "'");
                }
                tv = (TextView) findViewById(getResources().getIdentifier("vEcu" + Integer.toHexString (ecu.getFromId()).toLowerCase(), "id", getPackageName()));
                if (tv != null) {
                    tv.setText("-");
                } else {
                    MainActivity.toast("No view with id 'vEcu" + Integer.toHexString (ecu.getFromId()).toLowerCase() + "'");
                }
            }
        }

        TextView textView = (TextView) findViewById(R.id.link);
        textView.setText(Html.fromHtml("Learn more about the car's computers <a href='http://canze.fisch.lu/computers/'>here</a>."));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
     }

    @Override
    protected void onResume() {
        super.onResume();

        // initialise the widgets
        initListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeListeners();
    }

    private void initListeners() {
        subscribedFields = new ArrayList<>();
        for (Ecu ecu : Ecus.getInstance().getAllEcus()) {
            if (ecu.getFromId() > 0 && ecu.getFromId() < 0x800) {
                addListener(Integer.toHexString (ecu.getFromId()).toLowerCase() + ".6180.144");
            }
        }
    }

    private void removeListeners () {
        // empty the query loop
        MainActivity.device.clearFields();
        // free up the listeners again
        for (Field field : subscribedFields) {
            field.removeListener(this);
        }
        subscribedFields.clear();
    }

    private void addListener(String sid) {
        Field field;
        field = MainActivity.fields.getBySID(sid);
        if (field != null) {
            field.addListener(this);
            if(MainActivity.device!=null)
                MainActivity.device.addActivityField(field);
            subscribedFields.add(field);
        } else {
            MainActivity.toast("sid " + sid + " does not exist in class Fields");
        }
    }

    // This is the event fired as soon as this the registered fields are
    // getting updated by the corresponding reader class.
    @Override
    public void onFieldUpdateEvent(final Field field) {
        // the update has to be done in a separate thread
        // otherwise the UI will not be repainted
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String fieldId = field.getSID();
                TextView tv = null;
                int refVersion = 0;

                // get the text field
                switch (fieldId) {

                    case "7ec.6180.144":
                        tv = (TextView) findViewById(R.id.vEcu7ec);
                        refVersion = versions [0];
                        break;
                    case "7da.6180.144":
                        tv = (TextView) findViewById(R.id.vEcu7da);
                        refVersion = versions [1];
                        break;
                    case "7bb.6180.144":
                        tv = (TextView) findViewById(R.id.vEcu7bb);
                        refVersion = versions [2];
                        break;
                    case "77e.6180.144":
                        tv = (TextView) findViewById(R.id.vEcu77e);
                        refVersion = versions [3];
                        break;
                    case "772.6180.144":
                        tv = (TextView) findViewById(R.id.vEcu772);
                        refVersion = versions [4];
                        break;
                    case "76d.6180.144":
                        tv = (TextView) findViewById(R.id.vEcu76d);
                        refVersion = versions [5];
                        break;
                    case "763.6180.144":
                        tv = (TextView) findViewById(R.id.vEcu763);
                        refVersion = versions [6];
                        break;
                    case "762.6180.144":
                        tv = (TextView) findViewById(R.id.vEcu762);
                        refVersion = versions [7];
                        break;
                    case "760.6180.144":
                        tv = (TextView) findViewById(R.id.vEcu760);
                        refVersion = versions [8];
                        break;
                    case "7bc.6180.144":
                        tv = (TextView) findViewById(R.id.vEcu7bc);
                        refVersion = versions [9];
                        break;
                    case "765.6180.144":
                        tv = (TextView) findViewById(R.id.vEcu765);
                        refVersion = versions [10];
                        break;
                    case "764.6180.144":
                        tv = (TextView) findViewById(R.id.vEcu764);
                        refVersion = versions [11];
                        break;
                    case "76e.6180.144":
                        tv = (TextView) findViewById(R.id.vEcu76e);
                        refVersion = versions [12];
                        break;
                    case "793.6180.144":
                        tv = (TextView) findViewById(R.id.vEcu793);
                        refVersion = versions [13];
                        break;
                    case "7b6.6180.144":
                        tv = (TextView) findViewById(R.id.vEcu7b6);
                        refVersion = versions [14];
                        break;
                }

                // set regular new content, all exeptions handled above
                if (tv != null) {
                    int curVersion = (int) field.getValue();
                    if (curVersion != 0) {
                        String hexCurVersion = Integer.toHexString(curVersion);
                        hexCurVersion = ("0000" + hexCurVersion).substring(hexCurVersion.length());
                        if (refVersion != 0) {
                            String hexRefVersion = Integer.toHexString(refVersion);
                            hexRefVersion = ("0000" + hexRefVersion).substring(hexRefVersion.length());
                            if (curVersion > refVersion) {
                                hexCurVersion += " > " + hexRefVersion;
                            } else if (curVersion < refVersion) {
                                hexCurVersion += " < " + hexRefVersion;
                            }
                        }
                        tv.setText(hexCurVersion);
                    }
                }

                tv = (TextView) findViewById(R.id.textDebug);
                tv.setText(fieldId);
            }
        });
    }
}
