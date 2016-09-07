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
import java.util.Date;

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

    long socTime;
    long kmTime;
    long startTime;
    long resetTime;
    long actualTime;
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
                            kmTime = new Date().getTime();
                        }
                        if(!allInitialized) {
                            initialize();
                        } else {
                            if (Math.abs(socTime - kmTime) < 1000) {
                                TripState newTripState = new TripState(actualSoc, actualPreciseOdometer, actualPreciseOdometer - preciseOdometerOnCreate);
                                tripStates.add(newTripState);
                                updateInfoByDistance(1, (LinearLayout) findViewById(R.id.layout1Km), (TextView) findViewById(R.id.textUsedEnergyLast1km), (TextView) findViewById(R.id.textUsedEnergyLast1kmProjectedTo100km));
                                updateInfoByDistance(2, (LinearLayout) findViewById(R.id.layout2Km), (TextView) findViewById(R.id.textUsedEnergyLast2km), (TextView) findViewById(R.id.textUsedEnergyLast2kmProjectedTo100km));
                                updateInfoByDistance(3, (LinearLayout) findViewById(R.id.layout3Km), (TextView) findViewById(R.id.textUsedEnergyLast3km), (TextView) findViewById(R.id.textUsedEnergyLast3kmProjectedTo100km));
                                updateInfoByIndex(tripStates.size() - 1, 0, false, (LinearLayout) findViewById(R.id.layoutStart), (TextView) findViewById(R.id.textUsedEnergySinceStart), (TextView) findViewById(R.id.textUsedEnergySinceStartProjectedTo100km), (TextView) findViewById(R.id.textDrivenDistance), (TextView) findViewById(R.id.textDrivenTime));
                                updateInfoByIndex(tripStates.size() - 1, resetPointer, true, (LinearLayout) findViewById(R.id.layoutReset), (TextView) findViewById(R.id.textUsedEnergySinceReset), (TextView) findViewById(R.id.textUsedEnergySinceResetProjectedTo100km), (TextView) findViewById(R.id.textDrivenDistanceSinceReset), (TextView) findViewById(R.id.textDrivenTimeSinceReset));
                                lastSavedPreciseOdometer = actualPreciseOdometer;
                            }
                        }
                        break;
                    case SID_RealSoC:
                        if(!Double.isNaN(field.getValue())) {
                            actualSoc = field.getValue();
                            socTime = new Date().getTime();
                        }
                        if(!allInitialized) {
                            initialize();
                        }
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

        private void updateInfoByDistance(double distance, LinearLayout layoutToUpdate, TextView tvConsumption, TextView tvProjectedConsumption) {

        TripState tripState = null;
        TripState tripStateCompare = null;
        double usedEnergy;
        double usedEnergyProjected;
        int counter = 2;
        boolean notFound = true;

        tripState = tripStates.get(tripStates.size() - 1);

        while (notFound) {
            tripStateCompare = tripStates.get(tripStates.size() - counter);

            if (tripState.getDrivenDistance() - tripStateCompare.getDrivenDistance() >= distance) {
                notFound = false;
                usedEnergy = calcUsedEnergy(tripStateCompare.getActualSoc(), tripState.getActualSoc());
                usedEnergyProjected = calcProjectedEnergy(usedEnergy, distance);
                if (usedEnergy > 0) {
                    tvConsumption.setText("" + (Math.round(usedEnergy * 10.0) / 10.0) + " Wh");
                }
                if (usedEnergyProjected > 0 && usedEnergyProjected < 100) {
                    tvProjectedConsumption.setText("" + (Math.round(usedEnergyProjected * 10.0) / 10.0) + " kWh");
                    setConsumptionColor(layoutToUpdate, usedEnergyProjected);
                }

            } else if (counter == tripStates.size()) {
                break;
            } else {
                counter++;
            }
        }

    }

    private void updateInfoByIndex(int tripIndex, int compareIndex, boolean sinceReset, LinearLayout layoutToUpdate, TextView tvConsumption, TextView tvProjectedConsumption, TextView tvDrivenDistance, TextView tvDrivenTime) {

        TripState tripState;
        TripState tripStateCompare;
        double usedEnergy;
        double usedEnergyProjected;
        long timeDifference;
        long diffSeconds;
        long diffMinutes;
        long diffHours;

        if (tripStates.size() >= (tripIndex + 1) && tripStates.size() >= (compareIndex + 1)) {
            tripState = tripStates.get(tripIndex);
            tripStateCompare = tripStates.get(compareIndex);

            usedEnergy = calcUsedEnergy(tripStateCompare.getActualSoc(), tripState.getActualSoc());

            actualTime = new Date().getTime();

            if (sinceReset) {
                usedEnergyProjected = calcProjectedEnergy(usedEnergy, tripState.getDrivenDistance() - tripStates.get(resetPointer).getDrivenDistance());
                timeDifference = actualTime - resetTime;
            } else {
                usedEnergyProjected = calcProjectedEnergy(usedEnergy, tripState.getDrivenDistance());
                timeDifference = actualTime - startTime;
            }

            diffSeconds = timeDifference / 1000 % 60;
            diffMinutes = timeDifference / (60 * 1000) % 60;
            diffHours = timeDifference / (60 * 60 * 1000) % 60;

            if (diffHours != 0) {
                tvDrivenTime.setText("" + String.format("%02d", diffHours) + ":" + String.format("%02d", diffMinutes) + ":" + String.format("%02d", diffSeconds));
            } else {
                tvDrivenTime.setText("" + String.format("%02d", diffMinutes) + ":" + String.format("%02d", diffSeconds));
            }

            if (usedEnergy > 0) {
                tvConsumption.setText("" + (Math.round(usedEnergy * 10.0) / 10.0) + " Wh");
            }
            if (usedEnergyProjected > 0 && usedEnergyProjected < 100) {
                tvProjectedConsumption.setText("" + (Math.round(usedEnergyProjected * 10.0) / 10.0) + " kWh");
                setConsumptionColor(layoutToUpdate, usedEnergyProjected);
            }

            if (tvDrivenDistance != null) {
                if (sinceReset) {
                    tvDrivenDistance.setText("" + (Math.round((tripState.getDrivenDistance() - tripStates.get(resetPointer).getDrivenDistance()) * 10.0) / 10.0) + " km");
                } else {
                    tvDrivenDistance.setText("" + (Math.round(tripState.getDrivenDistance() * 10.0) / 10.0) + " km");
                }
            }
        }
    }

    private double calcUsedEnergy(double earlierSoc, double laterSoc) {
        double usedEnergy = ((earlierSoc - laterSoc) * avEnergyMultiplicator) * 1000;
        return usedEnergy;
    }

    private double calcProjectedEnergy(double usedEnergy, double drivenDistance) {
        double projectedEnergy = usedEnergy / (drivenDistance * 10);
        return  projectedEnergy;
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

    private void initialize() {

        TextView tv = null;

        if (actualPreciseOdometer != 0) {
            preciseOdometerOnCreate = actualPreciseOdometer;
            lastSavedPreciseOdometer = actualPreciseOdometer;

            if (actualSoc != 0) {
                socOnCreate = actualSoc;

                if (actualAvEnergy != 0) {
                    avEnergyOnCreate = actualAvEnergy;
                    avEnergyMultiplicator = avEnergyOnCreate / socOnCreate;
                    TripState newTripState = new TripState(actualSoc, actualPreciseOdometer, 0);
                    tripStates.add(newTripState);
                    startTime = new Date().getTime();
                    resetTime = new Date().getTime();
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
                        resetTime = new Date().getTime();
                        tv = (TextView) findViewById(R.id.textUsedEnergySinceReset);
                        tv.setText("-");
                        tv = (TextView) findViewById(R.id.textDrivenDistanceSinceReset);
                        tv.setText("-");
                        tv = (TextView) findViewById(R.id.textUsedEnergySinceResetProjectedTo100km);
                        tv.setText("-");
                        tv = (TextView) findViewById(R.id.textDrivenTimeSinceReset);
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