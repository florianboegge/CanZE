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

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import lu.fisch.canze.R;
import lu.fisch.canze.actors.Field;
import lu.fisch.canze.actors.TripState;
import lu.fisch.canze.interfaces.FieldListener;

// If you want to monitor changes, you must add a FieldListener to the fields.
// For the simple activity, the easiest way is to implement it in the actitviy itself.
public class FlorianActivity extends CanzeActivity implements FieldListener {

    public static final String SID_AvEnergy                         = "427.49";
    public static final String SID_RealSoC                          = "7bb.6103.192";
    public static final String SID_PreciseOdometer = "5d7.16"; //  (EVC)

    double avEnergyMultiplicator = 0;
    double socOnCreate = 0;
    double actualSoc = 0;
    double lastKnownSoc = 0;
    double actualAvEnergy = 0;
    double avEnergyOnCreate = 0;
    double actualPreciseOdometer = 0;
    double preciseOdometerOnCreate = 0;
    double lastSavedPreciseOdometer = 0;
    int resetPointer = 0;
    boolean allInitialized = false;

    private ArrayList<Field> subscribedFields;
    private ArrayList<TripState> tripStates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_florian);
        LinearLayout layoutReset = (LinearLayout)findViewById(R.id.layoutReset);
        layoutReset.setOnClickListener(myListener);
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
        tripStates = new ArrayList<>();

        addListener(SID_RealSoC);
        addListener(SID_AvEnergy);
        addListener(SID_PreciseOdometer);
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

                // get the text field
                switch (fieldId) {

                    case SID_PreciseOdometer:
                        if(!Double.isNaN(field.getValue())) {
                            actualPreciseOdometer = field.getValue();
                        }
                        if(!allInitialized) {
                            initialize();
                        } else {
                            if ((actualPreciseOdometer - lastSavedPreciseOdometer) >= 0.1) {
                                TripState newTripState = new TripState(actualSoc, actualPreciseOdometer, actualPreciseOdometer - preciseOdometerOnCreate);
                                tripStates.add(newTripState);
                                updateKilometerInfo();
                                lastSavedPreciseOdometer = actualPreciseOdometer;
                                //tv = (TextView) findViewById(R.id.textLastSavedPreciseOdometer);
                                //tv.setText("" + lastSavedPreciseOdometer);
                            }
                        }
                        //tv = (TextView) findViewById(R.id.textActualPreciseOdometer);
                        break;
                    case SID_RealSoC:
                        if(!Double.isNaN(field.getValue())) {
                            actualSoc = field.getValue();
                        }
                        if(!allInitialized) {
                            initialize();
                        } else {
                            if (lastKnownSoc == 0 || lastKnownSoc != actualSoc) {
                                lastKnownSoc = actualSoc;
                                updateDriveInfo();
                            }
                        }
                        //tv = (TextView) findViewById(R.id.textActualSoc);
                        break;
                    case SID_AvEnergy:
                        if(!Double.isNaN(field.getValue())) {
                            actualAvEnergy = field.getValue();
                        }
                        if(!allInitialized) {
                            initialize();
                        }
                        break;
                }
                // set regular new content, all exeptions handled above
                if (tv != null) {
                    tv.setText("" + field.getValue());
                }

                tv = (TextView) findViewById(R.id.textDebug);
                tv.setText(fieldId);
            }
        });

    }

    private void updateDriveInfo() {
        TextView tv = null;
        LinearLayout myLayout = null;
        double usedEnergy;
        double usedEnergyReset;
        double usedEnergyProjected;
        double usedEnergyProjectedReset;
        TripState tripState;
        TripState tripStateCompare;
        TripState tripStateReset;

        if (tripStates.size() >= 1) {

            tripState = tripStates.get(tripStates.size() - 1); //der letzte
            tripStateCompare = tripStates.get(0); // der erste
            tripStateReset = tripStates.get(resetPointer);

            usedEnergy = calcUsedEnergy(tripStateCompare.getActualSoc(), tripState.getActualSoc());
            tv = (TextView) findViewById(R.id.textUsedEnergySinceStart);
            if (usedEnergy > 0) {
                tv.setText("" + (Math.round(usedEnergy * 10.0) / 10.0) + " Wh");
            }

            usedEnergyReset = calcUsedEnergy(tripStateReset.getActualSoc(), tripState.getActualSoc());
            tv = (TextView) findViewById(R.id.textUsedEnergySinceReset);
            if (usedEnergyReset > 0) {
                tv.setText("" + (Math.round(usedEnergyReset * 10.0) / 10.0) + " Wh");
            }

            tv = (TextView) findViewById(R.id.textDrivenDistance);
            tv.setText("" + (Math.round(tripState.getDrivenDistance() * 10.0) / 10.0) + " km");

            tv = (TextView) findViewById(R.id.textDrivenDistanceSinceReset);
            tv.setText("" + (Math.round((tripState.getDrivenDistance() - tripStateReset.getDrivenDistance())  * 10.0) / 10.0) + " km");

            usedEnergyProjected = calcProjectedEnergy(usedEnergy, tripState.getDrivenDistance());
            tv = (TextView) findViewById(R.id.textUsedEnergySinceStartProjectedTo100km);
            if (usedEnergyProjected > 0 && usedEnergyProjected < 100) {
                tv.setText("" + (Math.round(usedEnergyProjected * 10.0) / 10.0) + " kWh");
            }

            usedEnergyProjectedReset = calcProjectedEnergy(usedEnergyReset, (tripState.getDrivenDistance() - tripStateReset.getDrivenDistance()));
            tv = (TextView) findViewById(R.id.textUsedEnergySinceResetProjectedTo100km);
            if (usedEnergyProjectedReset > 0 && usedEnergyProjectedReset < 100) {
                tv.setText("" + (Math.round(usedEnergyProjectedReset * 10.0) / 10.0) + " kWh");
            }

            myLayout = (LinearLayout) findViewById(R.id.layoutStart);
            setConsumptionColor(myLayout, usedEnergyProjected);

            myLayout = (LinearLayout) findViewById(R.id.layoutReset);
            setConsumptionColor(myLayout, usedEnergyProjectedReset);
            }

    }

    private void updateKilometerInfo() {

        LinearLayout myLayout = null;
        TextView tv = null;
        double usedEnergy;
        double usedEnergyProjected;
        TripState tripState;
        TripState tripStateCompare;

            if (tripStates.size() >= 11) {
                tripState = tripStates.get(tripStates.size() - 1); //bei 11: index 10
                tripStateCompare = tripStates.get(tripStates.size() - 11); //bei 11: index 0
                usedEnergy = calcUsedEnergy(tripStateCompare.getActualSoc(), tripState.getActualSoc());
                usedEnergyProjected = calcProjectedEnergy(usedEnergy, 1);
                tv = (TextView) findViewById(R.id.textUsedEnergyLast1km);
                tv.setText("" + (Math.round(usedEnergy * 10.0) / 10.0) + " Wh");
                tv = (TextView) findViewById(R.id.textUsedEnergyLast1kmProjectedTo100km);
                tv.setText("" + (Math.round(usedEnergyProjected * 10.0) / 10.0) + " kWh");
                myLayout = (LinearLayout) findViewById(R.id.layout1Km);
                setConsumptionColor(myLayout, usedEnergyProjected);
            }
            if (tripStates.size() >= 21) {
                tripState = tripStates.get(tripStates.size() - 1); //bei 21: index 20
                tripStateCompare = tripStates.get(tripStates.size() - 21); //bei 21: index 0
                usedEnergy = calcUsedEnergy(tripStateCompare.getActualSoc(), tripState.getActualSoc());
                usedEnergyProjected = calcProjectedEnergy(usedEnergy, 2);
                tv = (TextView) findViewById(R.id.textUsedEnergyLast2km);
                tv.setText("" + (Math.round(usedEnergy * 10.0) / 10.0) + " Wh");
                tv = (TextView) findViewById(R.id.textUsedEnergyLast2kmProjectedTo100km);
                tv.setText("" + (Math.round(usedEnergyProjected * 10.0) / 10.0) + " kWh");
                myLayout = (LinearLayout) findViewById(R.id.layout2Km);
                setConsumptionColor(myLayout, usedEnergyProjected);
            }
            if (tripStates.size() >= 31) {
                tripState = tripStates.get(tripStates.size() - 1);
                tripStateCompare = tripStates.get(tripStates.size() - 31);
                usedEnergy = calcUsedEnergy(tripStateCompare.getActualSoc(), tripState.getActualSoc());
                usedEnergyProjected = calcProjectedEnergy(usedEnergy, 3);
                tv = (TextView) findViewById(R.id.textUsedEnergyLast3km);
                tv.setText("" + (Math.round(usedEnergy * 10.0) / 10.0) + " Wh");
                tv = (TextView) findViewById(R.id.textUsedEnergyLast3kmProjectedTo100km);
                tv.setText("" + (Math.round(usedEnergyProjected * 10.0) / 10.0) + " kWh");
                myLayout = (LinearLayout) findViewById(R.id.layout3Km);
                setConsumptionColor(myLayout, usedEnergyProjected);
            }

    }

    private void setConsumptionColor(LinearLayout myLayout, double usedEnergyProjected) {

        int red = 0;
        int green = 0;

        if (usedEnergyProjected < 10) {
            green = 200;
            red = 0;
        } else if (usedEnergyProjected < 20) {
            green = Math.abs(-400+(20 * (int) usedEnergyProjected));
            red = -200 + (20* (int) usedEnergyProjected);
        } else {
            green = 0;
            red = 200;
        }
        myLayout.setBackgroundColor(Color.rgb(red, green, 0));
    }

    private double calcUsedEnergy(double earlierSoc, double laterSoc) {
        double usedEnergy = ((earlierSoc - laterSoc) * avEnergyMultiplicator) * 1000;
        return usedEnergy;
    }

    private double calcProjectedEnergy(double usedEnergy, double drivenDistance) {
        double projectedEnergy = usedEnergy / (drivenDistance * 10);
        return  projectedEnergy;
    }

    private void initialize() {

        TextView tv = null;

        if (actualPreciseOdometer != 0) {
            preciseOdometerOnCreate = actualPreciseOdometer;
            //tv = (TextView) findViewById(R.id.textPreciseOdometerOnCreate);
            //tv.setText("" + preciseOdometerOnCreate);
            lastSavedPreciseOdometer = actualPreciseOdometer;
            //tv = (TextView) findViewById(R.id.textLastSavedPreciseOdometer);
            //tv.setText("" + lastSavedPreciseOdometer);
            if (actualSoc != 0) {
                socOnCreate = actualSoc;
                //tv = (TextView) findViewById(R.id.textSocOnCreate);
                //tv.setText("" + (Math.round(socOnCreate * 10.0) / 10.0));
                if (actualAvEnergy != 0) {
                    avEnergyOnCreate = actualAvEnergy;
                    avEnergyMultiplicator = avEnergyOnCreate / socOnCreate;
                    TripState newTripState = new TripState(actualSoc, actualPreciseOdometer, 0);
                    tripStates.add(newTripState);
                    allInitialized = true;
                }
            }
        }

    }

    // Create an anonymous implementation of OnClickListener
    private View.OnClickListener myListener = new View.OnClickListener() {
        public void onClick(View v) {

            TextView tv = null;
            LinearLayout myLayout = null;
            myLayout = (LinearLayout) findViewById(R.id.layoutReset);
            tv = (TextView) findViewById(R.id.textUsedEnergyLast1km);

            switch (v.getId() /*to get clicked view id**/) {
                case R.id.layoutReset:

                    if (tripStates.size() >= 1) {
                        resetPointer = tripStates.size() - 1;
                        tv = (TextView) findViewById(R.id.textUsedEnergySinceReset);
                        tv.setText("-");
                        tv = (TextView) findViewById(R.id.textDrivenDistanceSinceReset);
                        tv.setText("-");
                        tv = (TextView) findViewById(R.id.textUsedEnergySinceResetProjectedTo100km);
                        tv.setText("-");
                        myLayout.setBackgroundColor(Color.rgb(138, 138, 138));
                    }

                    break;
                default:
                    break;
            }
        }
    };

}