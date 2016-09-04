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


/*
 * This class manages all know fields.
 * Actually only the simple fields from the free CAN stream are handled.
 */
package lu.fisch.canze.actors;

import android.os.Environment;
import android.widget.TextView;

import lu.fisch.canze.R;
import lu.fisch.canze.activities.MainActivity;
import lu.fisch.canze.classes.FieldLogger;
import lu.fisch.canze.interfaces.MessageListener;
import lu.fisch.canze.interfaces.VirtualFieldAction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author robertfisch test
 */
public class Fields implements MessageListener {

    private static final int FIELD_ID           = 0;
    private static final int FIELD_FROM         = 1;
    private static final int FIELD_TO           = 2;
    private static final int FIELD_RESOLUTION   = 3;
    private static final int FIELD_OFFSET       = 4;
    private static final int FIELD_DECIMALS     = 5;
    private static final int FIELD_UNIT         = 6;
    private static final int FIELD_REQUEST_ID   = 7;
    private static final int FIELD_RESPONSE_ID  = 8;
    private static final int FIELD_OPTIONS      = 9; // to be stated in HEX, no leading 0x

    public static final int TOAST_NONE          = 0;
    public static final int TOAST_DEVICE        = 1;
    public static final int TOAST_ALL           = 2;

    private final ArrayList<Field> fields = new ArrayList<>();
    private final HashMap<String, Field> fieldsBySid = new HashMap<>();

    private static Fields instance = null;
    private double runningUsage = 0;

    //private int car = CAR_ANY;

    private Fields() {
        // the will be called by load(), and only after we know (or have changed) the car
        //fillStatic();
        //addVirtualFields();
    }

    public static boolean initialised()
    {
        return (instance==null);
    }

    public static Fields getInstance()
    {
        if(instance==null) instance=new Fields();
        return instance;
    }

    private void addVirtualFields() {
        addVirtualFieldUsage();
        addVirtualFieldUsageLpf();
        addVirtualFieldFrictionTorque();
        addVirtualFieldFrictionPower();
        addVirtualFieldDcPower();
    }


    private void addVirtualFieldUsage() {

        // It would be easier use SID_Consumption = "1fd.48" (dash kWh) instead of V*A

        final String SID_EVC_TractionBatteryVoltage = "7ec.623203.24";  // unit = V
        final String SID_EVC_TractionBatteryCurrent = "7ec.623204.24";  // unit = A
        final String SID_RealSpeed = "5d7.0";                           // unit = km/h
/*
        // create a list of field this new virtual field will depend on
        HashMap<String, Field> dependantFields = new HashMap<>();
        dependantFields.put(SID_EVC_TractionBatteryVoltage, getBySID(SID_EVC_TractionBatteryVoltage));
        dependantFields.put(SID_EVC_TractionBatteryCurrent, getBySID(SID_EVC_TractionBatteryCurrent));
        dependantFields.put(SID_RealSpeed, getBySID(SID_RealSpeed));
        // create a new virtual field. Define it's ID and how it is being calculated
        VirtualField virtualField = new VirtualField("6100", dependantFields, "kWh/100km", new VirtualFieldAction() {
            @Override
            public double updateValue(HashMap<String, Field> dependantFields) {
                // get voltage
                double dcVolt = dependantFields.get(SID_EVC_TractionBatteryVoltage).getValue();
                // get current
                double dcPwr = dcVolt * dependantFields.get(SID_EVC_TractionBatteryCurrent).getValue() / 1000.0;
                // get real speed
                double realSpeed = dependantFields.get(SID_RealSpeed).getValue();

                if (realSpeed >= 5)
                    return -(Math.round(1000.0 * dcPwr / realSpeed) / 10.0);
                else
                    return 0;
            }
        });
        // add it to the list of fields
        add(virtualField);
*/
        addVirtualFieldCommon ("6100", "kWh/100km", SID_EVC_TractionBatteryVoltage+";"+SID_EVC_TractionBatteryCurrent+";"+SID_RealSpeed, new VirtualFieldAction() {
            @Override
            public double updateValue(HashMap<String, Field> dependantFields) {
                // get voltage
                double dcVolt = dependantFields.get(SID_EVC_TractionBatteryVoltage).getValue();
                // get current
                double dcPwr = dcVolt * dependantFields.get(SID_EVC_TractionBatteryCurrent).getValue() / 1000.0;
                // get real speed
                double realSpeed = dependantFields.get(SID_RealSpeed).getValue();

                if (realSpeed >= 5)
                    return -(Math.round(1000.0 * dcPwr / realSpeed) / 10.0);
                else
                    return 0;
            }
        });
    }

    private void addVirtualFieldFrictionTorque() {
        final String SID_DriverBrakeWheel_Torque_Request        = "130.44"; //UBP braking wheel torque the driver wants
        final String SID_ElecBrakeWheelsTorqueApplied           = "1f8.28"; //10ms
/*
        // create a list of field this new virtual field will depend on
        HashMap<String, Field> dependantFields = new HashMap<>();
        dependantFields.put(SID_DriverBrakeWheel_Torque_Request, getBySID(SID_DriverBrakeWheel_Torque_Request));
        dependantFields.put(SID_ElecBrakeWheelsTorqueApplied, getBySID(SID_ElecBrakeWheelsTorqueApplied));
        // create a new virtual field. Define it's ID and how it is being calculated
        VirtualField virtualField = new VirtualField("6101", dependantFields, "Nm", new VirtualFieldAction() {
            @Override
            public double updateValue(HashMap<String,Field> dependantFields) {

                return dependantFields.get(SID_DriverBrakeWheel_Torque_Request).getValue() - dependantFields.get(SID_ElecBrakeWheelsTorqueApplied).getValue();
            }
        });
        // add it to the list of fields
        add(virtualField);
*/
        addVirtualFieldCommon ("6101", "Nm", SID_DriverBrakeWheel_Torque_Request+";"+SID_ElecBrakeWheelsTorqueApplied, new VirtualFieldAction() {
            @Override
            public double updateValue(HashMap<String,Field> dependantFields) {

                return dependantFields.get(SID_DriverBrakeWheel_Torque_Request).getValue() - dependantFields.get(SID_ElecBrakeWheelsTorqueApplied).getValue();
            }
        });
    }

    private void addVirtualFieldFrictionPower() {
        final String SID_DriverBrakeWheel_Torque_Request        = "130.44"; //UBP braking wheel torque the driver wants
        final String SID_ElecBrakeWheelsTorqueApplied           = "1f8.28"; //10ms
        final String SID_ElecEngineRPM                          = "1f8.40"; //10ms
/*
        // create a list of field this new virtual field will depend on
        HashMap<String, Field> dependantFields = new HashMap<>();
        dependantFields.put(SID_DriverBrakeWheel_Torque_Request,getBySID(SID_DriverBrakeWheel_Torque_Request));
        dependantFields.put(SID_ElecBrakeWheelsTorqueApplied,getBySID(SID_ElecBrakeWheelsTorqueApplied));
        dependantFields.put(SID_ElecEngineRPM,getBySID(SID_ElecEngineRPM));
        // create a new virtual field. Define it's ID and how it is being calculated
        VirtualField virtualField = new VirtualField("6102", dependantFields, "kW", new VirtualFieldAction() {
            @Override
            public double updateValue(HashMap<String,Field> dependantFields) {

                return (dependantFields.get(SID_DriverBrakeWheel_Torque_Request).getValue() - dependantFields.get(SID_ElecBrakeWheelsTorqueApplied).getValue()) * dependantFields.get(SID_ElecEngineRPM).getValue() / 9.3;
            }
        });
        // add it to the list of fields
        add(virtualField);
*/
        addVirtualFieldCommon ("6102", "kW", SID_DriverBrakeWheel_Torque_Request+";"+SID_ElecBrakeWheelsTorqueApplied+";"+SID_ElecEngineRPM, new VirtualFieldAction() {
            @Override
            public double updateValue(HashMap<String,Field> dependantFields) {

                return (dependantFields.get(SID_DriverBrakeWheel_Torque_Request).getValue() - dependantFields.get(SID_ElecBrakeWheelsTorqueApplied).getValue()) * dependantFields.get(SID_ElecEngineRPM).getValue() / 9.3;
            }
        });
    }

    private void addVirtualFieldDcPower() {
        final String SID_TractionBatteryVoltage             = "7ec.623203.24";
        final String SID_TractionBatteryCurrent             = "7ec.623204.24";
/*
        // create a list of field this new virtual field will depend on
        HashMap<String, Field> dependantFields = new HashMap<>();
        dependantFields.put(SID_TractionBatteryVoltage,getBySID(SID_TractionBatteryVoltage));
        dependantFields.put(SID_TractionBatteryCurrent,getBySID(SID_TractionBatteryCurrent));
        // create a new virtual field. Define it's ID and how it is being calculated
        VirtualField virtualField = new VirtualField("6103", dependantFields, "kW", new VirtualFieldAction() {
            @Override
            public double updateValue(HashMap<String,Field> dependantFields) {

                return dependantFields.get(SID_TractionBatteryVoltage).getValue() * dependantFields.get(SID_TractionBatteryCurrent).getValue() / 1000;
            }
        });
        // add it to the list of fields
        add(virtualField);
*/
        addVirtualFieldCommon ("6103", "kW", SID_TractionBatteryVoltage+";"+SID_TractionBatteryCurrent, new VirtualFieldAction() {
            @Override
            public double updateValue(HashMap<String,Field> dependantFields) {

                return dependantFields.get(SID_TractionBatteryVoltage).getValue() * dependantFields.get(SID_TractionBatteryCurrent).getValue() / 1000;
            }
        });

    }

    private void addVirtualFieldUsageLpf() {

        // It would be easier use SID_Consumption = "1fd.48" (dash kWh) instead of V*A

        final String SID_VirtualUsage               = "800.6100.24";
/*
        // create a list of field this new virtual field will depend on
        HashMap<String, Field> dependantFields = new HashMap<>();
        dependantFields.put(SID_VirtualUsage, getBySID(SID_VirtualUsage));
        // create a new virtual field. Define it's ID and how it is being calculated
        VirtualField virtualField = new VirtualField("6104", dependantFields, "kWh/100km", new VirtualFieldAction() {
            @Override
            public double updateValue(HashMap<String, Field> dependantFields) {
                double value = dependantFields.get(SID_VirtualUsage).getValue();
                if (value != 0) {
                    runningUsage = runningUsage * 0.95 + value * 0.05;
                }
                return runningUsage;
            }
        });
        // add it to the list of fields
        add(virtualField);
*/
        addVirtualFieldCommon ("6104", "kWh/100km", SID_VirtualUsage, new VirtualFieldAction() {
            @Override
            public double updateValue(HashMap<String, Field> dependantFields) {
                double value = dependantFields.get(SID_VirtualUsage).getValue();
                if (value != 0) {
                    runningUsage = runningUsage * 0.95 + value * 0.05;
                }
                return runningUsage;
            }
        });
    }

    private void addVirtualFieldCommon (String virtualId, String unit, String dependantIds, VirtualFieldAction virtualFieldAction) {
        // create a list of field this new virtual field will depend on
        HashMap<String, Field> dependantFields = new HashMap<>();
        boolean allOk = true;
        for (String idStr: dependantIds.split(";")){
            Field field = getBySID(idStr);
            if (field != null) {
                dependantFields.put(idStr, field);
            } else {
                allOk = false;
            }
        }
        if (allOk) {
            VirtualField virtualField = new VirtualField(virtualId, dependantFields, unit, virtualFieldAction);
            // add it to the list of fields
            add(virtualField);

        }
    }

    private void fillStatic()
    {
        String fieldDef = // ID, startBit, endBit, resolution, offset, decimals, unit, requestID, responseID, options is HEX
                ""
/*

                        // 2015-11-28

                        +"0x023,0,15,1,0,0,,,,1f\n" // AIRBAGCrash
                        +"0x0c6,0,15,1,0x8000,1,°,,,1f\n" // Steering Position
                        +"0x0c6,16,31,1,0x8000,1,°/s,,,1f\n" // Steering Acceleration
                        +"0x0c6,32,47,1,0x8000,1,°,,,1f\n" // SteeringWheelAngle_Offset
                        +"0x0c6,48,50,1,0,0,,,,1f\n" // SwaSensorInternalStatus
                        +"0x0c6,51,54,1,0,0,,,,1f\n" // SwaClock
                        +"0x0c6,56,53,1,0,0,,,,1f\n" // SwaChecksum
                        +"0x12e,0,7,1,198,0,,,,1f\n" // LongitudinalAccelerationProc
                        +"0x12e,8,23,1,0x8000,0,,,,1f\n" // TransversalAcceleration
                        +"0x12e,24,35,0.1,2047,1,deg/s,,,1f\n" // Yaw rate
                        +"0x130,8,10,1,0,0,,,,1f\n" // UBP_Clock
                        +"0x130,11,12,1,0,0,,,,1f\n" // HBB_Malfunction
                        +"0x130,16,17,1,0,0,,,,1f\n" // EB_Malfunction
                        +"0x130,18,19,1,0,0,,,,1f\n" // EB_inProgress
                        +"0x130,20,31,1,4094,0,Nm,,,1f\n" // ElecBrakeWheelsTorqueRequest
                        +"0x130,32,38,1,0,0,%,,,1f\n" // BrakePedalDriverWill
                        +"0x130,40,41,1,0,0,,,,1f\n" // HBA_ActivationRequest
                        +"0x130,42,43,1,0,0,,,,1f\n" // PressureBuildUp
                        +"0x130,44,55,-3,4094,0,Nm,,,1f\n" // DriverBrakeWheelTq_Req
                        +"0x130,56,63,1,0,0,,,,1f\n" // CheckSum_UBP
                        +"0x17a,24,27,1,0,0,,,,1f\n" // Transmission Range
                        +"0x17a,48,63,0.5,12800,1,Nm,,,\n" // Estimated Wheen Torque
                        +"0x17e,40,41,1,0,0,,,,1f\n" // CrankingAuthorisation_AT
                        +"0x17e,48,51,1,0,0,,,,1f\n" // GearLeverPosition
                        +"0x186,0,15,0.125,0,2,rpm,,,1f\n" // Speed
                        +"0x186,16,27,0.5,800,1,Nm,,,1f\n" // MeanEffectiveTorque
                        +"0x186,28,39,0.5,800,0,Nm,,,1f\n" // RequestedTorqueAfterProc
                        +"0x186,40,49,0.125,0,1,%,,,1f\n" // Throttle
                        +"0x186,50,50,1,0,0,,,,1f\n" // ASR_MSRAcknowledgement
                        +"0x186,51,52,1,0,0,,,,1f\n" // ECM_TorqueRequestStatus
                        +"0x18a,16,25,0.125,0,2,%,,,1f\n" // Throttle
                        +"0x18a,27,38,0.5,800,1,Nm,,,1f\n" // Coasting Torque
                        +"0x1f6,0,1,1,0,0,,,,1f\n" // Engine Fan Speed
                        +"0x1f6,3,7,100,0,0,W,,,1f\n" // Max Electrical Power Allowed
                        +"0x1f6,8,9,1,0,0,,,,1f\n" // ElectricalPowerCutFreeze
                        +"0x1f6,10,11,1,0,0,,,,1f\n" // EngineStatus_R
                        +"0x1f6,12,15,1,0,0,,,,1f\n" // EngineStopRequestOrigine
                        +"0x1f6,16,17,1,0,0,,,,1f\n" // CrankingAuthorization_ECM
                        +"0x1f6,19,20,1,0,1,,,,1f\n" // Break Pedal
                        +"0x1f6,23,31,0.1,0,1,bar,,,1f\n" // AC High Pressure Sensor
                        +"0x1f8,0,7,1,0,0,,,,1f\n" // Checksum EVC
                        +"0x1f8,12,13,1,0,0,,,,1f\n" // EVCReadyAsActuator
                        +"0x1f8,16,27,1,4096,0,Nm,,,1f\n" // TotalPotentialResistiveWheelsTorque
                        +"0x1f8,28,39,-1,4096,0,Nm,,,1f\n" // ElecBrakeWheelsTorqueApplied
                        +"0x1f8,40,50,10,0,0,Rpm,,,1f\n" // ElecEngineRPM
                        +"0x1f8,52,54,1,0,0,,,,1f\n" // EVC_Clock
                        +"0x1f8,56,58,1,0,0,,,,1f\n" // GearRangeEngagedCurrent
                        +"0x1f8,62,63,1,0,0,,,,1f\n" // DeclutchInProgress
                        +"0x1fd,0,7,0.390625,0,1,%,,,1f\n" // 12V Battery Current?
                        +"0x1fd,8,9,1,0,0,,,,1f\n" // SCH Refuse to Sleep
                        +"0x1fd,17,18,1,0,0,,,,1f\n" // Stop Preheating Counter
                        +"0x1fd,19,20,1,0,0,,,,1f\n" // Start Preheating Counter
                        +"0x1fd,21,31,1,0,0,min,,,1f\n" // Time left before vehicle wakeup
                        +"0x1fd,32,32,1,0,0,,,,1f\n" // Pre heating activation
                        +"0x1fd,33,39,1,0,0,min,,,1f\n" // LeftTimeToScheduledTime
                        +"0x1fd,40,47,25,0,0,W,,,1f\n" // ClimAvailablePower
                        +"0x1fd,48,55,1,0x50,0,kW,,,1f\n" // Consumption
                        +"0x212,8,9,1,0,0,,,,1f\n" // StarterStatus
                        +"0x212,10,11,1,0,0,,,,1f\n" // RearGearEngaged
                        +"0x242,0,0,1,0,0,,,,\n" // ABSinRegulation
                        +"0x242,1,1,1,0,0,,,,\n" // ABSMalfunction
                        +"0x242,2,2,1,0,0,,,,\n" // ASRinRegulation
                        +"0x242,3,3,1,0,0,,,,\n" // ASRMalfunction
                        +"0x242,5,5,1,0,0,,,,\n" // AYCinRegulation
                        +"0x242,6,6,1,0,0,,,,\n" // AYCMalfunction
                        +"0x242,7,7,1,0,0,,,,\n" // MSRinRegulation
                        +"0x242,8,8,1,0,0,,,,\n" // MSRMalfunction
                        +"0x242,9,12,1,0,0,,,,\n" // ESP_Clock
                        +"0x242,13,15,1,0,0,,,,\n" // ESP_TorqueControlType
                        +"0x242,16,27,0.5,800,1,Nm,,,\n" // ASRDynamicTorqueRequest
                        +"0x242,28,39,0.5,800,1,Nm,,,\n" // ASRStaticTorqueRequest
                        +"0x242,40,51,0.5,800,1,Nm,,,\n" // MSRTorqueRequest
                        +"0x29a,0,15,0.04166666667,0,2,rpm,,,1f\n" // Rpm Front Right
                        +"0x29a,16,31,0.04166666667,0,2,rpm,,,1f\n" // Rpm Front Left
                        +"0x29a,32,47,0.01,0,2,km/h,,,1f\n" // Vehicle Speed
                        +"0x29a,52,55,1,0,0,,,,1f\n" // Vehicle Speed Clock
                        +"0x29a,56,63,1,0,0,,,,1f\n" // Vehicle Speed Checksum
                        +"0x29c,0,15,0.04166666667,0,2,rpm,,,1f\n" // Rpm Rear Right
                        +"0x29c,16,31,0.04166666667,0,2,rpm,,,1f\n" // Rpm Rear Left
                        +"0x29c,48,63,0.01,0,2,km/h,,,1f\n" // Vehicle Speed
                        +"0x2b7,32,33,1,0,0,,,,1f\n" // EBD Active
                        +"0x2b7,34,35,1,0,0,,,,1f\n" // HBA Active
                        +"0x2b7,36,37,1,0,0,,,,1f\n" // ESC HBB Malfunction
                        +"0x352,0,1,1,0,0,,,,1f\n" // ABS Warning Request
                        +"0x352,2,3,1,0,0,,,,1f\n" // ESP_StopLampRequest
                        +"0x352,24,31,1,0,0,,,,1f\n" // Break pressure
                        +"0x35c,0,1,1,0,0,,,,1f\n" // BCM_WakeUpSleepCommand
                        +"0x35c,4,4,1,0,0,,,,1f\n" // WakeUpType
                        +"0x35c,5,7,1,0,0,,,,1f\n" // VehicleState
                        +"0x35c,8,8,1,0,0,,,,1f\n" // DiagMuxOn_BCM
                        +"0x35c,9,10,1,0,0,,,,1f\n" // StartingMode_BCM_R
                        +"0x35c,11,11,1,0,0,,,,1f\n" // EngineStopDriverRequested
                        +"0x35c,12,12,1,0,0,,,,1f\n" // SwitchOffSESDisturbers
                        +"0x35c,15,15,1,0,0,,,,1f\n" // DeliveryModeInformation
                        +"0x35c,16,39,1,0,0,min,,,1f\n" // AbsoluteTimeSince1rstIgnition
                        +"0x35c,40,42,1,0,0,,,,1f\n" // BrakeInfoStatus
                        +"0x35c,47,47,1,0,0,,,,1f\n" // ProbableCustomerFeedBackNeed
                        +"0x35c,48,51,1,0,0,,,,1f\n" // EmergencyEngineStop
                        +"0x35c,52,52,1,0,0,,,,1f\n" // WelcomePhaseState
                        +"0x35c,53,54,1,0,0,,,,1f\n" // SupposedCustomerDeparture
                        +"0x35c,55,55,1,0,0,,,,1f\n" // VehicleOutsideLockedState
                        +"0x35c,58,59,1,0,0,,,,1f\n" // GenericApplicativeDiagEnable
                        +"0x35c,60,61,1,0,0,,,,1f\n" // ParkingBrakeStatus
                        +"0x3f7,2,3,1,0,0,,,,2\n" // Gear?
                        +"0x427,0,1,1,0,0,,,,1f\n" // HVConnectionStatus
                        +"0x427,2,3,1,0,0,,,,1f\n" // ChargingAlert
                        +"0x427,4,5,1,0,0,,,,1f\n" // HVBatteryLocked
                        +"0x427,26,28,1,0,0,,,,1f\n" // PreHeatingProgress
                        +"0x427,40,47,0.3,0,0,kW,,,2\n" // AvailableChargingPower
                        +"0x427,49,57,0.1,0,1,kWh,,,1f\n" // AvailableEnergy
                        +"0x427,58,58,1,0,0,,,,1f\n" // ChargeAvailable
                        +"0x42a,0,0,1,0,0,,,,1f\n" // PreHeatingRequest
                        +"0x42a,6,15,0.1,40,1,°C,,,1f\n" // EvaporatorTempSetPoint
                        +"0x42a,24,29,1,0,0,%,,,1f\n" // ClimAirFlow
                        +"0x42a,30,39,0.1,40,1,°C,,,1f\n" // EvaporatorTempMeasure
                        +"0x42a,45,46,1,0,0,,,,1f\n" // ImmediatePreheatingAuthorizationStatus
                        +"0x42a,48,49,1,0,0,,,,1f\n" // ClimLoopMode
                        +"0x42a,51,52,1,0,0,,,,1f\n" // PTCActivationRequest
                        +"0x42a,56,60,5,0,0,%,,,1f\n" // EngineFanSpeedRequestPWM
                        +"0x42e,0,12,0.02,0,2,%,,,1f\n" // State of Charge
                        +"0x42e,18,19,1,0,0,,,,1f\n" // HVBatLevel2Failure
                        +"0x42e,20,24,5,0,0,%,,,1f\n" // EngineFanSpeed
                        +"0x42e,25,34,0.5,0,0,V,,,1f\n" // HVNetworkVoltage
                        +"0x42e,38,43,1,0,1,A,,,1f\n" // Charging Pilot Current
                        +"0x42e,44,50,1,40,0,°C,,,1f\n" // HVBatteryTemp
                        +"0x42e,56,63,0.3,0,1,kW,,,1f\n" // ChargingPower
                        +"0x430,40,49,0.1,40,1,°C,,,2\n" // HV Battery Evaporator Temp
                        +"0x430,50,59,0.1,40,1,°C,,,2\n" // HV Battery Evaporator Setpoint
                        +"0x4f8,0,1,-1,-2,0,,,,1f\n" // Start
                        +"0x4f8,4,5,-1,-2,0,,,,1f\n" // Parking Break
                        +"0x4f8,8,9,1,0,0,,,,1f\n" // AIRBAGMalfunctionLampState
                        +"0x4f8,12,12,1,0,0,,,,1f\n" // ClusterDrivenLampsAutoCheck
                        +"0x4f8,13,13,1,0,0,,,,1f\n" // DisplayedSpeedUnit
                        +"0x4f8,24,39,0.01,0,2,,,,2\n" // Speed on Display
                        +"0x534,32,40,1,40,0,°C,,,5\n" // Temp out
                        +"0x5d7,0,15,0.01,0,2,km/h,,,1f\n" // Speed
                        +"0x5d7,16,43,0.01,0,2,km,,,1f\n" // Odometer
                        +"0x5d7,44,45,1,0,0,?,,,1f\n" // WheelsLockingState
                        +"0x5d7,48,49,1,0,0,?,,,1f\n" // VehicleSpeedSign
                        +"0x5d7,50,54,0.04,0,2,cm,,,1f\n" // Fine distance
                        +"0x5da,0,7,1,40,0,ºC,,,5\n" // Water temperature
                        +"0x5de,1,1,1,0,0,,,,1f\n" // Right Indicator
                        +"0x5de,2,2,1,0,0,,,,1f\n" // Left Indicator
                        +"0x5de,3,3,1,0,0,,,,1f\n" // Rear Fog Light
                        +"0x5de,5,5,1,0,0,,,,1f\n" // Park Light
                        +"0x5de,6,6,1,0,0,,,,1f\n" // Head Light
                        +"0x5de,7,7,1,0,0,,,,1f\n" // Beam Light
                        +"0x5de,8,9,1,0,0,,,,1f\n" // PositionLightsOmissionWarning
                        +"0x5de,10,10,1,0,0,,,,1f\n" // ALS malfunction
                        +"0x5de,11,12,1,0,0,,,,1f\n" // Door Front Left
                        +"0x5de,13,14,1,0,0,,,,1f\n" // Dort Front Right
                        +"0x5de,16,17,1,0,0,,,,1f\n" // Door Rear Left
                        +"0x5de,18,19,1,0,0,,,,1f\n" // Door Rear Right
                        +"0x5de,21,22,1,0,0,,,,\n" // Steering Lock Failure
                        +"0x5de,23,23,1,0,0,,,,\n" // Unlocking Steering Column Warning
                        +"0x5de,24,24,1,0,0,,,,\n" // Automatic Lock Up Activation State
                        +"0x5de,25,25,1,0,0,,,,\n" // Badge Battery Low
                        +"0x5de,28,29,1,0,0,,,,\n" // Trip Display Scrolling Request
                        +"0x5de,32,35,1,0,0,,,,\n" // Smart Keyless Information Display
                        +"0x5de,36,36,1,0,0,,,,\n" // Keyless Info Reemission Request
                        +"0x5de,37,37,1,0,0,,,,\n" // Keyless Card Reader Failure Display
                        +"0x5de,47,47,1,0,0,,,,\n" // Brake Switch Fault Display
                        +"0x5de,49,49,1,0,0,,,,\n" // Stop Lamp Failure Display
                        +"0x5de,56,57,1,0,0,,,,\n" // Rear Wiper Status
                        +"0x5de,58,59,1,0,0,,,,1f\n" // Boot Open Warning
                        +"0x5ee,0,0,1,0,0,,,,1f\n" // Park Light
                        +"0x5ee,1,1,1,0,0,,,,1f\n" // Head Light
                        +"0x5ee,2,2,1,0,0,,,,1f\n" // Beam Light
                        +"0x5ee,16,19,1,0,0,,,,1f\n" // Door Locks
                        +"0x5ee,20,24,1,0,0,,,,1f\n" // Indicators
                        +"0x5ee,24,27,1,0,0,,,,1f\n" // Doors
                        +"0x5ee,40,40,1,0,0,,,,1f\n" // LightSensorStatus
                        +"0x646,8,15,0.1,0,1,kWh/100km,,,1f\n" // Average trip B consumpion
                        +"0x646,16,32,0.1,0,1,km,,,1f\n" // Trip B distance
                        +"0x646,33,47,0.1,0,1,kWh,,,1f\n" // trip B consumption
                        +"0x646,48,59,0.1,0,1,km/h,,,1f\n" // Averahe trip B speed
                        +"0x653,9,9,1,0,0,,,,1f\n" // Driver seatbelt
                        +"0x654,2,2,1,0,0,,,,1f\n" // ChargingPlugConnected
                        +"0x654,3,3,1,0,0,,,,1f\n" // DriverWalkAwayEngineON
                        +"0x654,4,4,1,0,0,,,,1f\n" // HVBatteryUnballastAlert
                        +"0x654,25,31,1,0,0,,,,1f\n" // State of Charge
                        +"0x654,32,41,1,0,0,min,,,1f\n" // Time to Full
                        +"0x654,42,51,1,0,0,km,,,1f\n" // Available Distance
                        +"0x654,52,61,0.1,0,1,,,,1f\n" // AverageConsumption
                        +"0x654,62,62,1,0,0,,,,1f\n" // HVBatteryLow
                        +"0x656,3,3,1,0,0,,,,1f\n" // Trip Data Reset
                        +"0x656,21,31,1,0,0,min,,,1f\n" // Cluste rScheduled Time
                        +"0x656,32,42,1,0,0,min,,,1f\n" // Cluster Scheduled Time 2
                        +"0x656,48,55,1,40,0,°C,,,2\n" // External Temp
                        +"0x656,56,57,1,0,0,,,,2\n" // ClimPCCustomerActiv
                        +"0x658,0,31,1,0,0,,,,1f\n" // Battery Serial N°
                        +"0x658,33,39,1,0,0,%,,,1f\n" // Battery Health
                        +"0x658,42,42,1,0,0,,,,1f\n" // Charging
                        +"0x65b,0,10,1,0,0,min,,,1f\n" // Schedule timer 1 min
                        +"0x65b,12,22,1,0,0,min,,,1f\n" // Schedule timer 2 min
                        +"0x65b,24,30,1,0,0,%,,,1f\n" // Fluent driver
                        +"0x65b,25,26,1,0,0,,,,1f\n" // Economy Mode
                        +"0x65b,33,34,1,0,0,,,,1f\n" // Economy Mode displayed
                        +"0x65b,39,40,1,0,0,,,,1f\n" // Consider eco mode
                        +"0x65b,41,43,1,0,0,,,,1f\n" // Charging Status Display
                        +"0x65b,44,45,1,0,0,,,,1f\n" // Set park for charging
                        +"0x66a,5,7,1,0,0,,,,1f\n" // Cruise Control Mode
                        +"0x66a,8,15,1,0,0,km/h,,,1f\n" // Cruise Control Speed
                        +"0x66a,16,16,1,0,0,,,,1f\n" // Cruise Control OverSpeed
                        +"0x673,0,0,1,0,0,,,,1f\n" // Speed pressure misadaptation
                        +"0x673,2,4,1,0,0,,,,1f\n" // Rear right wheel state
                        +"0x673,5,7,1,0,0,,,,1f\n" // Rear left wheel state
                        +"0x673,8,10,1,0,0,,,,1f\n" // Front right wheel state
                        +"0x673,11,13,1,0,0,,,,1f\n" // Front left wheel state
                        +"0x673,16,23,13.725,0,0,mbar,,,1f\n" // Rear right wheel pressure
                        +"0x673,24,31,13.725,0,0,mbar,,,1f\n" // Rear left wheel pressure
                        +"0x673,32,39,13.725,0,0,mbar,,,1f\n" // Front right wheel pressure
                        +"0x673,40,47,13.725,0,0,mbar,,,1f\n" // Front left wheel pressure
                        +"0x68b,0,3,1,0,0,,,,1f\n" // MM action counter
                        +"0x699,0,1,1,0,0,,,,2\n" // Clima off Request display
                        +"0x699,2,3,1,0,0,,,,2\n" // Clima read defrost Reuqest display
                        +"0x699,4,4,-1,-1,0,,,,2\n" //
                        +"0x699,5,5,1,0,0,,,,2\n" // Maximum defrost
                        +"0x699,6,6,1,0,0,,,,2\n" // Autofan
                        +"0x699,10,14,0.5,0,0,°C,,,2\n" // Temperature
                        +"0x699,16,16,1,0,0,,,,2\n" // Windshield
                        +"0x699,18,18,1,0,0,,,,2\n" // Face
                        +"0x699,19,19,1,0,0,,,,2\n" // Feet
                        +"0x699,20,21,1,0,0,,,,2\n" // Forced recycling
                        +"0x699,22,23,1,0,0,,,,2\n" //
                        +"0x699,24,27,1,0,0,,,,2\n" //
                        +"0x699,28,31,1,0,0,,,,2\n" //
                        +"0x699,52,53,1,0,0,,,,2\n" //
                        +"0x699,54,55,1,0,0,,,,2\n" //
                        +"0x699,56,56,1,0,0,,,,2\n" //
                        +"0x69f,0,31,1,0,0,,,,1f\n" // Car Serial N°
                        +"0x6f8,16,23,6.25,0,2,V,,,1f\n" // 12V Battery Voltage
                        +"0x760,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x760,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number %04lx
                        +"0x760,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x760,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x760,24,31,1,0,0,bar,0x224b0e,0x624b0e,2\n" // Master cylinder pressure
                        +"0x762,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x762,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number %04lx
                        +"0x762,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x762,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x762,24,39,0.390625,100,0,V,0x22012f,0x62012f,1f\n" // 12V Battery Voltage
                        +"0x763,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x763,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number %04lx
                        +"0x763,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x763,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x763,24,31,1,0,0,,0x222001,0x622001,1f\n" // Parking Break
                        +"0x763,3,3,1,0,0,,0x2220f0,0x6220f0,1f\n" // VOL+
                        +"0x763,4,4,1,0,0,,0x2220f0,0x6220f0,1f\n" // VOL-
                        +"0x763,2,2,1,0,0,,0x2220f0,0x6220f0,1f\n" // Mute
                        +"0x763,5,5,1,0,0,,0x2220f0,0x6220f0,1f\n" // Media
                        +"0x763,6,6,1,0,0,,0x2220f0,0x6220f0,1f\n" // Radio
                        +"0x764,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x764,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x764,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x764,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x764,8,15,0.4,40,1,,0x2121,0x6121,5\n" // Interior temperature
                        +"0x765,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x765,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x765,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x765,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x76d,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x76d,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x76d,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x76d,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x76e,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x76e,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x76e,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x76e,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x772,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x772,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x772,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x772,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x77e,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x77e,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x77e,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x77e,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x77e,24,31,1,0,0,,0x22300f,0x62300f,1f\n" // dcdc state
                        +"0x77e,24,31,31.25,0,3,V,0x22300e,0x62300e,1f\n" // traction battery voltage
                        +"0x77e,24,39,0.015625,0,2,ºC,0x223018,0x623018,5\n" // DCDC converter temperature
                        +"0x77e,24,31,0.03125,0,0,Nm,0x223024,0x623024,1f\n" // torque requested
                        +"0x77e,24,31,0.03125,0,0,Nm,0x223025,0x623025,1f\n" // torque applied
                        +"0x77e,24,31,0.015625,0,2,°C,0x22302b,0x62302b,1f\n" // inverter temperature
                        +"0x77e,24,31,6.25,0,2,A,0x22301d,0x62301d,1f\n" // Current
                        +"0x793,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x793,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number %04lx
                        +"0x793,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x793,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x7b6,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x7b6,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x7b6,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x7b6,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x7bb,192,207,0.01,0,2,kW,0x2101,0x6101,5\n" // Maximum battery input power
                        +"0x7bb,208,223,0.01,0,2,kW,0x2101,0x6101,5\n" // Maximum battery output power
                        +"0x7bb,348,367,0.0001,0,4,Ah,0x2101,0x6101,5\n" // Ah of the battery
                        +"0x7bb,316,335,0.0001,0,4,%,0x2101,0x6101,5\n" // Real State of Charge
                        +"0x7bb,336,351,0.01,0,2,kW,0x2101,0x6101,2\n" // Maximum battery input power
                        +"0x7bb,56,71,0.1,0,1,°C,0x2103,0x6103,5\n" // Mean compartment temp
                        +"0x7bb,16,31,1,0,0,stravinsky1124,0x2104,0x6104,2\n" // Module 1 raw NTC
                        +"0x7bb,32,39,1,40,0,°C,0x2104,0x6104,2\n" // Cell 1 Temperature
                        +"0x7bb,40,55,1,0,0,,0x2104,0x6104,2\n" // Module 2 raw NTC
                        +"0x7bb,56,63,1,40,0,°C,0x2104,0x6104,2\n" // Cell 2 Temperature
                        +"0x7bb,64,79,1,0,0,,0x2104,0x6104,2\n" // Module 3 raw NTC
                        +"0x7bb,80,87,1,40,0,°C,0x2104,0x6104,2\n" // Cell 3 Temperature
                        +"0x7bb,88,103,1,0,0,,0x2104,0x6104,2\n" // Module 4 raw NTC
                        +"0x7bb,104,111,1,40,0,°C,0x2104,0x6104,2\n" // Cell 4 Temperature
                        +"0x7bb,112,127,1,0,0,,0x2104,0x6104,2\n" // Module 5 raw NTC
                        +"0x7bb,128,135,1,40,0,°C,0x2104,0x6104,2\n" // Cell 5 Temperature
                        +"0x7bb,136,151,1,0,0,,0x2104,0x6104,2\n" // Module 6 raw NTC
                        +"0x7bb,152,159,1,40,0,°C,0x2104,0x6104,2\n" // Cell 6 Temperature
                        +"0x7bb,160,175,1,0,0,,0x2104,0x6104,2\n" // Module 7 raw NTC
                        +"0x7bb,176,183,1,40,0,°C,0x2104,0x6104,2\n" // Cell 7 Temperature
                        +"0x7bb,184,199,1,0,0,,0x2104,0x6104,2\n" // Module 8 raw NTC
                        +"0x7bb,200,207,1,40,0,°C,0x2104,0x6104,2\n" // Cell 8 Temperature
                        +"0x7bb,208,223,1,0,0,,0x2104,0x6104,2\n" // Module 9 raw NTC
                        +"0x7bb,224,231,1,40,0,°C,0x2104,0x6104,2\n" // Cell 9 Temperature
                        +"0x7bb,232,247,1,0,0,,0x2104,0x6104,2\n" // Module 10 raw NTC
                        +"0x7bb,248,255,1,40,0,°C,0x2104,0x6104,2\n" // Cell 10 Temperature
                        +"0x7bb,256,271,1,0,0,,0x2104,0x6104,2\n" // Module 11 raw NTC
                        +"0x7bb,272,279,1,40,0,°C,0x2104,0x6104,2\n" // Cell 11 Temperature
                        +"0x7bb,280,295,1,0,0,,0x2104,0x6104,2\n" // Module 12 raw NTC
                        +"0x7bb,296,303,1,40,0,°C,0x2104,0x6104,2\n" // Cell 12 Temperature
                        +"0x7bb,16,31,1,0,0,,0x2104,0x6104,5\n" // Module 1 raw NTC
                        +"0x7bb,32,39,1,0,0,°C,0x2104,0x6104,25\n" // Cell 1 Temperature
                        +"0x7bb,40,55,1,0,0,,0x2104,0x6104,5\n" // Module 2 raw NTC
                        +"0x7bb,56,63,1,0,0,°C,0x2104,0x6104,25\n" // Cell 2 Temperature
                        +"0x7bb,64,79,1,0,0,,0x2104,0x6104,5\n" // Module 3 raw NTC
                        +"0x7bb,80,87,1,0,0,°C,0x2104,0x6104,25\n" // Cell 3 Temperature
                        +"0x7bb,88,103,1,0,0,,0x2104,0x6104,5\n" // Module 4 raw NTC
                        +"0x7bb,104,111,1,0,0,°C,0x2104,0x6104,25\n" // Cell 4 Temperature
                        +"0x7bb,64,79,0.001,0,3,V,0x2105,0x6105,5\n" // Threshold bad cell
                        +"0x7bb,80,95,0.001,0,3,V,0x2105,0x6105,5\n" // Threshol weak cell
                        +"0x7bb,16,31,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 01 V
                        +"0x7bb,32,47,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 02 V
                        +"0x7bb,48,63,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 03 V
                        +"0x7bb,64,79,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 04 V
                        +"0x7bb,80,95,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 05 V
                        +"0x7bb,96,111,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 06 V
                        +"0x7bb,112,127,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 07 V
                        +"0x7bb,128,143,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 08 V
                        +"0x7bb,144,159,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 09 V
                        +"0x7bb,160,175,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 10 V
                        +"0x7bb,176,191,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 11 V
                        +"0x7bb,192,207,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 12 V
                        +"0x7bb,208,223,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 13 V
                        +"0x7bb,224,239,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 14 V
                        +"0x7bb,240,255,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 15 V
                        +"0x7bb,256,271,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 16 V
                        +"0x7bb,272,287,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 17 V
                        +"0x7bb,288,303,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 18 V
                        +"0x7bb,304,319,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 19 V
                        +"0x7bb,320,335,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 20 V
                        +"0x7bb,336,351,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 21 V
                        +"0x7bb,352,367,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 22 V
                        +"0x7bb,368,383,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 23 V
                        +"0x7bb,384,399,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 24 V
                        +"0x7bb,400,415,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 25 V
                        +"0x7bb,416,431,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 26 V
                        +"0x7bb,432,447,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 27 V
                        +"0x7bb,448,463,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 28 V
                        +"0x7bb,464,479,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 29 V
                        +"0x7bb,480,495,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 30 V
                        +"0x7bb,496,511,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 31 V
                        +"0x7bb,512,527,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 32 V
                        +"0x7bb,528,543,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 33 V
                        +"0x7bb,544,559,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 34 V
                        +"0x7bb,560,575,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 35 V
                        +"0x7bb,576,591,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 36 V
                        +"0x7bb,592,607,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 37 V
                        +"0x7bb,608,623,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 38 V
                        +"0x7bb,624,639,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 39 V
                        +"0x7bb,640,655,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 40 V
                        +"0x7bb,656,671,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 41 V
                        +"0x7bb,672,687,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 42 V
                        +"0x7bb,688,703,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 43 V
                        +"0x7bb,704,719,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 44 V
                        +"0x7bb,720,735,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 45 V
                        +"0x7bb,736,751,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 46 V
                        +"0x7bb,752,767,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 47 V
                        +"0x7bb,768,783,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 48 V
                        +"0x7bb,784,799,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 49 V
                        +"0x7bb,800,815,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 50 V
                        +"0x7bb,816,831,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 51 V
                        +"0x7bb,832,847,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 52 V
                        +"0x7bb,848,863,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 53 V
                        +"0x7bb,864,879,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 54 V
                        +"0x7bb,880,895,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 55 V
                        +"0x7bb,896,911,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 56 V
                        +"0x7bb,912,927,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 57 V
                        +"0x7bb,928,943,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 58 V
                        +"0x7bb,944,959,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 59 V
                        +"0x7bb,960,975,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 60 V
                        +"0x7bb,976,991,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 61 V
                        +"0x7bb,992,1007,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 62 V
                        +"0x7bb,16,31,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 63 V
                        +"0x7bb,32,47,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 64 V
                        +"0x7bb,48,63,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 65 V
                        +"0x7bb,64,79,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 66 V
                        +"0x7bb,80,95,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 67 V
                        +"0x7bb,96,111,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 68 V
                        +"0x7bb,112,127,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 69 V
                        +"0x7bb,128,143,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 70 V
                        +"0x7bb,144,159,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 71 V
                        +"0x7bb,160,175,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 72 V
                        +"0x7bb,176,191,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 73 V
                        +"0x7bb,192,207,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 74 V
                        +"0x7bb,208,223,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 75 V
                        +"0x7bb,224,239,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 76 V
                        +"0x7bb,240,255,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 77 V
                        +"0x7bb,256,271,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 78 V
                        +"0x7bb,272,287,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 79 V
                        +"0x7bb,288,303,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 80 V
                        +"0x7bb,304,319,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 81 V
                        +"0x7bb,320,335,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 82 V
                        +"0x7bb,336,351,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 83 V
                        +"0x7bb,352,367,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 84 V
                        +"0x7bb,368,383,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 85 V
                        +"0x7bb,384,399,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 86 V
                        +"0x7bb,400,415,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 87 V
                        +"0x7bb,416,431,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 88 V
                        +"0x7bb,432,447,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 89 V
                        +"0x7bb,448,463,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 90 V
                        +"0x7bb,464,479,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 91 V
                        +"0x7bb,480,495,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 92 V
                        +"0x7bb,496,511,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 93 V
                        +"0x7bb,512,527,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 94 V
                        +"0x7bb,528,543,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 95 V
                        +"0x7bb,544,559,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 96 V
                        +"0x7bb,60,79,0.0001,0,4,Ah,0x2161,0x6161,5\n" // Ah of the battery
                        +"0x7bb,80,87,0.05,0,2,%,0x2161,0x6161,5\n" // Battery State of Health
                        +"0x7bb,104,119,1,0,0,km,0x2161,0x6161,5\n" // Battery mileage in km
                        +"0x7bb,136,151,1,0,0,kWh,0x2161,0x6161,5\n" // Total energy output of battery?
                        +"0x7bb,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x7bb,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number %04lx
                        +"0x7bb,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x7bb,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x7bc,144,159,1,0,0,,0x2180,0x6180,1f\n" // Request firmware version
                        +"0x7bc,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x7bc,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x7bc,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x7da,144,159,1,0,0,,0x2180,0x6180,1f\n" // Request firmware version
                        +"0x7da,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x7da,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x7da,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x7ec,144,159,1,0,0,,0x2180,0x6180,1f\n" // Request firmware version
                        +"0x7ec,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x7ec,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x7ec,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x7ec,24,39,2,0,2,%,0x222002,0x622002,2\n" // SOC
                        +"0x7ec,24,39,2.083333333,0,2,%,0x222002,0x622002,5\n" // SOC
                        +"0x7ec,24,39,0.01,0,2,km/h,0x222003,0x622003,5\n" // Speed
                        +"0x7ec,24,39,0.5,0,2,V,0x222004,0x622004,1f\n" // Motor Voltage
                        +"0x7ec,24,39,0.01,0,2,V,0x222005,0x622005,1f\n" // 12V battery voltage
                        +"0x7ec,24,47,1,0,0,km,0x222006,0x622006,1f\n" // Odometer
                        +"0x7ec,24,39,1,0,0,,0x22202e,0x62202e,1f\n" // Pedal
                        +"0x7ec,24,31,1,0,0,,0x22204b,0x62204b,5\n" // Steering wheel CC/SL buttons
                        +"0x7ec,24,39,0.5,0,2,V,0x223203,0x623203,1f\n" // Battery voltage
                        +"0x7ec,24,39,0.25,0x8000,2,A,0x223204,0x623204,1f\n" // Battery current
                        +"0x7ec,24,31,1,0,0,%,0x223206,0x623206,1f\n" // Battery health in %
                        +"0x7ec,24,31,1,1,0,,0x223318,0x623318,5\n" // Motor Water pump speed
                        +"0x7ec,24,31,1,1,0,,0x223319,0x623319,5\n" // Charger pump speed
                        +"0x7ec,24,31,1,1,0,,0x22331A,0x62331A,5\n" // Heater water pump speed
                        +"0x7ec,24,31,1,40,0,°C,0x2233b1,0x6233b1,1f\n" // Ext temp
                        */

                        // 2016-01-27

//+"0x023,0,15,1,0,0,,,,1f\n" // AIRBAGCrash
//+"0x0c6,0,15,1,0x8000,1,°,,,1f\n" // Steering Position
//+"0x0c6,16,31,1,0x8000,1,°/s,,,1f\n" // Steering Acceleration
//+"0x0c6,32,47,1,0x8000,1,°,,,1f\n" // SteeringWheelAngle_Offset
//+"0x0c6,48,50,1,0,0,,,,1f\n" // SwaSensorInternalStatus
//+"0x0c6,51,54,1,0,0,,,,1f\n" // SwaClock
//+"0x0c6,56,53,1,0,0,,,,1f\n" // SwaChecksum
//+"0x12e,0,7,1,198,0,,,,1f\n" // LongitudinalAccelerationProc
//+"0x12e,8,23,1,0x8000,0,,,,1f\n" // TransversalAcceleration
//+"0x12e,24,35,0.1,2047,1,deg/s,,,1f\n" // Yaw rate
//+"0x130,8,10,1,0,0,,,,1f\n" // UBP_Clock
//+"0x130,11,12,1,0,0,,,,1f\n" // HBB_Malfunction
//+"0x130,16,17,1,0,0,,,,1f\n" // EB_Malfunction
//+"0x130,18,19,1,0,0,,,,1f\n" // EB_inProgress
//+"0x130,20,31,1,4094,0,Nm,,,1f\n" // ElecBrakeWheelsTorqueRequest
//+"0x130,32,38,1,0,0,%,,,1f\n" // BrakePedalDriverWill
//+"0x130,40,41,1,0,0,,,,1f\n" // HBA_ActivationRequest
//+"0x130,42,43,1,0,0,,,,1f\n" // PressureBuildUp
                        +"0x130,44,55,-3,4094,0,Nm,,,1f\n" // DriverBrakeWheelTq_Req
//+"0x130,56,63,1,0,0,,,,1f\n" // CheckSum_UBP
//+"0x17a,24,27,1,0,0,,,,1f\n" // Transmission Range
//+"0x17a,48,63,0.5,12800,1,Nm,,,1f\n" // Estimated Wheel Torque
//+"0x17e,40,41,1,0,0,,,,1f\n" // CrankingAuthorisation_AT
//+"0x17e,48,51,1,0,0,,,,1f\n" // GearLeverPosition
//+"0x186,0,15,0.125,0,2,rpm,,,1f\n" // Engine RPM
                        +"0x186,16,27,0.5,800,1,Nm,,,1f\n" // MeanEffectiveTorque
//+"0x186,28,39,0.5,800,0,Nm,,,1f\n" // RequestedTorqueAfterProc
                        +"0x186,40,49,0.125,0,1,%,,,1f\n" // Throttle
//+"0x186,50,50,1,0,0,,,,1f\n" // ASR_MSRAcknowledgement
//+"0x186,51,52,1,0,0,,,,1f\n" // ECM_TorqueRequestStatus
//+"0x18a,16,25,0.125,0,2,%,,,1f\n" // Throttle
                        +"0x18a,27,38,0.5,800,1,Nm,,,1f\n" // Coasting Torque
//+"0x1f6,0,1,1,0,0,,,,1f\n" // Engine Fan Speed
//+"0x1f6,3,7,100,0,0,W,,,1f\n" // Max Electrical Power Allowed
//+"0x1f6,8,9,1,0,0,,,,1f\n" // ElectricalPowerCutFreeze
//+"0x1f6,10,11,1,0,0,,,,1f\n" // EngineStatus_R
//+"0x1f6,12,15,1,0,0,,,,1f\n" // EngineStopRequestOrigine
//+"0x1f6,16,17,1,0,0,,,,1f\n" // CrankingAuthorization_ECM
//+"0x1f6,19,20,1,0,1,,,,1f\n" // Break Pedal
//+"0x1f6,23,31,0.1,0,1,bar,,,1f\n" // AC High Pressure Sensor
//+"0x1f8,0,7,1,0,0,,,,1f\n" // Checksum EVC
//+"0x1f8,12,13,1,0,0,,,,1f\n" // EVCReadyAsActuator
                        +"0x1f8,16,27,1,4096,0,Nm,,,1f\n" // TotalPotentialResistiveWheelsTorque
                        +"0x1f8,28,39,-1,4096,0,Nm,,,1f\n" // ElecBrakeWheelsTorqueApplied
                        +"0x1f8,40,50,10,0,0,Rpm,,,1f\n" // ElecEngineRPM
//+"0x1f8,52,54,1,0,0,,,,1f\n" // EVC_Clock
//+"0x1f8,56,58,1,0,0,,,,1f\n" // GearRangeEngagedCurrent
//+"0x1f8,62,63,1,0,0,,,,1f\n" // DeclutchInProgress
                        +"0x1fd,0,7,0.390625,0,1,%,,,1f\n" // 12V Battery Current?
//+"0x1fd,8,9,1,0,0,,,,1f\n" // SCH Refuse to Sleep
//+"0x1fd,17,18,1,0,0,,,,1f\n" // Stop Preheating Counter
//+"0x1fd,19,20,1,0,0,,,,1f\n" // Start Preheating Counter
//+"0x1fd,21,31,1,0,0,min,,,1f\n" // Time left before vehicle wakeup
//+"0x1fd,32,32,1,0,0,,,,1f\n" // Pre heating activation
//+"0x1fd,33,39,1,0,0,min,,,1f\n" // LeftTimeToScheduledTime
//+"0x1fd,40,47,25,0,0,W,,,1f\n" // Climate Available Power
                        +"0x1fd,48,55,1,0x50,0,kW,,,1f\n" // Consumption
//+"0x212,8,9,1,0,0,,,,1f\n" // Starter Status
//+"0x212,10,11,1,0,0,,,,1f\n" // Rear Gear Engaged
//+"0x242,0,0,1,0,0,,,,1f\n" // ABS in Regulation
//+"0x242,1,1,1,0,0,,,,1f\n" // ABS Malfunction
//+"0x242,2,2,1,0,0,,,,1f\n" // ASR in Regulation
//+"0x242,3,3,1,0,0,,,,1f\n" // ASR Malfunction
//+"0x242,5,5,1,0,0,,,,1f\n" // AYC in Regulation
//+"0x242,6,6,1,0,0,,,,1f\n" // AYC Malfunction
//+"0x242,7,7,1,0,0,,,,1f\n" // MSR in Regulation
//+"0x242,8,8,1,0,0,,,,1f\n" // MSR Malfunction
//+"0x242,9,12,1,0,0,,,,1f\n" // ESP Clock
//+"0x242,13,15,1,0,0,,,,1f\n" // ESP Torque Control Type
//+"0x242,16,27,0.5,800,1,Nm,,,1f\n" // ASR Dynamic Torque Request
//+"0x242,28,39,0.5,800,1,Nm,,,1f\n" // ASR Static Torque Request
//+"0x242,40,51,0.5,800,1,Nm,,,1f\n" // MSR Torque Request
//+"0x29a,0,15,0.04166666667,0,2,rpm,,,1f\n" // Rpm Front Right
//+"0x29a,16,31,0.04166666667,0,2,rpm,,,1f\n" // Rpm Front Left
//+"0x29a,32,47,0.01,0,2,km/h,,,1f\n" // Vehicle Speed
//+"0x29a,52,55,1,0,0,,,,1f\n" // Vehicle Speed Clock
//+"0x29a,56,63,1,0,0,,,,1f\n" // Vehicle Speed Checksum
//+"0x29c,0,15,0.04166666667,0,2,rpm,,,1f\n" // Rpm Rear Right
//+"0x29c,16,31,0.04166666667,0,2,rpm,,,1f\n" // Rpm Rear Left
//+"0x29c,48,63,0.01,0,2,km/h,,,1f\n" // Vehicle Speed
//+"0x2b7,32,33,1,0,0,,,,1f\n" // EBD Active
//+"0x2b7,34,35,1,0,0,,,,1f\n" // HBA Active
//+"0x2b7,36,37,1,0,0,,,,1f\n" // ESC HBB Malfunction
//+"0x352,0,1,1,0,0,,,,1f\n" // ABS Warning Request
//+"0x352,2,3,1,0,0,,,,1f\n" // ESP Stop Lamp Request
//+"0x352,24,31,1,0,0,,,,1f\n" // Break pressure
//+"0x35c,0,1,1,0,0,,,,1f\n" // BCM Wake Up Sleep Command
//+"0x35c,4,4,1,0,0,,,,1f\n" // Wake Up Type
//+"0x35c,5,7,1,0,0,,,,1f\n" // Vehicle State
//+"0x35c,8,8,1,0,0,,,,1f\n" // Diag Mux On BCM
//+"0x35c,9,10,1,0,0,,,,1f\n" // Starting Mode BCM R
//+"0x35c,11,11,1,0,0,,,,1f\n" // Engine Stop Driver Requested
//+"0x35c,12,12,1,0,0,,,,1f\n" // Switch Off SES Disturbers
//+"0x35c,15,15,1,0,0,,,,1f\n" // Delivery Mode Information
//+"0x35c,16,39,1,0,0,min,,,1f\n" // Absolute Time Since 1rst Ignition
//+"0x35c,40,42,1,0,0,,,,1f\n" // Brake Info Status
//+"0x35c,47,47,1,0,0,,,,1f\n" // ProbableCustomer Feed Back Need
//+"0x35c,48,51,1,0,0,,,,1f\n" // Emergency Engine Stop
//+"0x35c,52,52,1,0,0,,,,1f\n" // Welcome Phase State
//+"0x35c,53,54,1,0,0,,,,1f\n" // Supposed Customer Departure
//+"0x35c,55,55,1,0,0,,,,1f\n" // VehicleOutside Locked State
//+"0x35c,58,59,1,0,0,,,,1f\n" // Generic Applicative Diag Enable
//+"0x35c,60,61,1,0,0,,,,1f\n" // Parking Brake Status
//+"0x391,15,15,1,0,0,,,,2\n" // Climate Cooling Select
//+"0x391,16,16,1,0,0,,,,2\n" // Blower State
//+"0x391,22,22,1,0,0,,,,2\n" // ACVbat Tempo Maintain
//+"0x391,28,28,1,0,0,,,,2\n" // Rear Defrost Request
//+"0x391,32,33,1,0,0,,,,2\n" // Clim Customer Action
//+"0x391,36,39,1,0,0,,,,2\n" // PTC Number Thermal Request
//+"0x3b7,17,25,1,0,0,day,,,1f\n" // Time Before Draining
//+"0x3b7,32,38,1,0,0,,,,1f\n" // Global Eco Score
//+"0x3f7,0,4,1,0,0,,,,2\n" // Range
//+"0x3f7,10,10,1,0,0,,,,2\n" // AT Open Door Warning
//+"0x3f7,11,11,1,0,0,,,,2\n" // AT Press Brake Pedal Request
//+"0x3f7,12,12,1,0,0,,,,2\n" // AT Gear Shift Refused
//+"0x427,0,1,1,0,0,,,,1f\n" // HV Connection Status
//+"0x427,2,3,1,0,0,,,,1f\n" // Charging Alert
//+"0x427,4,5,1,0,0,,,,1f\n" // HV Battery Locked
//+"0x427,26,28,1,0,0,,,,1f\n" // Pre Heating Progress
                        +"0x427,40,47,0.3,0,0,kW,,,2\n" // Available Charging Power
                        +"0x427,49,57,0.1,0,1,kWh,,,2\n" // Available Energy
//+"0x427,58,58,1,0,0,,,,2\n" // Charge Available
//+"0x42a,0,0,1,0,0,,,,2\n" // PreHeating Request
//+"0x42a,6,15,0.1,40,1,°C,,,2\n" // Evaporator Temp Set Point
//+"0x42a,24,29,1,0,0,%,,,2\n" // Clim Air Flow
                        +"0x42a,30,39,0.1,40,1,°C,,,1f\n" // Evaporator Temp Measure
//+"0x42a,48,49,1,0,0,,,,2\n" // Clim Loop Mode
//+"0x42a,51,52,1,0,0,,,,2\n" // PTC Activation Request
//+"0x42a,56,60,5,0,0,%,,,2\n" // Engine Fan Speed Request PWM
                        +"0x42e,0,12,0.02,0,2,%,,,3\n" // State of Charge
//+"0x42e,18,19,1,0,0,,,,1f\n" // HV Bat Level2 Failure
                        +"0x42e,20,24,5,0,0,%,,,2\n" // Engine Fan Speed
//+"0x42e,25,34,0.5,0,0,V,,,1f\n" // HV Network Voltage
                        +"0x42e,38,43,1,0,1,A,,,3\n" // Charging Pilot Current
                        +"0x42e,44,50,1,40,0,°C,,,3\n" // HV Battery Temp
                        +"0x42e,56,63,0.3,0,1,kW,,,1f\n" // Charging Power
//+"0x430,0,9,10,0,0,rpm,,,2\n" // Clim Compressor Speed RPM Request
//+"0x430,16,22,1,0,0,%,,,2\n" // High Voltage PTC Request PWM
                        +"0x430,24,33,0.5,30,1,°C,,,2\n" // Comp Temperature Discharge
//+"0x430,34,35,1,0,0,,,,2\n" // DeIcing Request
//+"0x430,36,37,1,0,0,,,,2\n" // Clim Panel PC Activation Request
                        +"0x430,38,39,1,0,0,,,,2\n" // HV Battery Cooling State
                        +"0x430,40,49,0.1,40,1,°C,,,2\n" // HV Battery Evaporator Temp
//+"0x430,50,59,0.1,40,1,°C,,,2\n" // HV Battery Evaporator Setpoint
//+"0x432,0,1,1,0,0,,,,2\n" // Bat VE Shut Down Alert
//+"0x432,2,3,1,0,0,,,,2\n" // Immediate Preheating Authorization Status
//+"0x432,4,5,1,0,0,,,,2\n" // HV Battery Level Alert
//+"0x432,6,9,1,0,0,,,,2\n" // HVBatCondPriorityLevel
//+"0x432,10,19,10,0,0,rpm,,,2\n" // Clim Comp RPM Status
//+"0x432,20,22,1,0,0,,,,2\n" // Clim Comp Default Status
//+"0x432,26,28,1,0,0,,,,2\n" // PTC Default Status
//+"0x432,29,35,1,40,0,°C,,,2\n" // HV Batt Cond Temp Average
                        +"0x432,36,37,1,0,0,,,,2\n" // HV Bat Conditionning Mode
//+"0x432,40,41,1,0,0,,,,2\n" // Eco Mode Request
//+"0x432,42,48,100,0,0,Wh,,,2\n" // Climate Available Energy
//+"0x432,56,57,1,0,0,,,,2\n" // Low Voltage Unballast Request
//+"0x432,59,60,1,0,0,,,,2\n" // DeIcing Authorisation
//+"0x433,0,2,1,0,0,,,,2\n" // AQM Frag Select Request
//+"0x433,7,8,1,0,0,,,,2\n" // AQM Ioniser Mode Selection Req
//+"0x433,9,12,1,0,0,,,,2\n" // AQM Frag Intensity Request
//+"0x433,15,16,1,0,0,,,,2\n" // Clim AQS Activation Request
//+"0x433,28,29,1,0,0,,,,2\n" // Ioniser Auto Launch Request
//+"0x4f8,0,1,-1,-2,0,,,,1f\n" // Start
//+"0x4f8,4,5,-1,-2,0,,,,1f\n" // Parking Break
//+"0x4f8,8,9,1,0,0,,,,1f\n" // AIRBAG Malfunction Lamp State
//+"0x4f8,12,12,1,0,0,,,,1f\n" // Cluster Driven Lamps Auto Check
//+"0x4f8,13,13,1,0,0,,,,1f\n" // Displayed Speed Unit
//+"0x4f8,24,39,0.01,0,2,,,,1f\n" // Speed on Display
                        +"0x534,32,40,1,40,0,°C,,,5\n" // Temp out
                        +"0x5d7,0,15,0.01,0,2,km/h,,,1f\n" // Speed
                        +"0x5d7,16,43,0.01,0,2,km,,,1f\n" // Odometer
                        +"0x5d7,50,54,0.04,0,2,cm,,,1f\n" // Fine distance
                        +"0x5da,0,7,1,40,0,ºC,,,5\n" // Water temperature
//+"0x5de,1,1,1,0,0,,,,1f\n" // Right Indicator
//+"0x5de,2,2,1,0,0,,,,1f\n" // Left Indicator
//+"0x5de,3,3,1,0,0,,,,1f\n" // Rear Fog Light
//+"0x5de,5,5,1,0,0,,,,1f\n" // Park Light
//+"0x5de,6,6,1,0,0,,,,1f\n" // Head Light
//+"0x5de,7,7,1,0,0,,,,1f\n" // Beam Light
//+"0x5de,8,9,1,0,0,,,,1f\n" // Position Lights Omission Warning
//+"0x5de,10,10,1,0,0,,,,1f\n" // ALS malfunction
//+"0x5de,11,12,1,0,0,,,,1f\n" // Door Front Left
//+"0x5de,13,14,1,0,0,,,,1f\n" // Dort Front Right
//+"0x5de,16,17,1,0,0,,,,1f\n" // Door Rear Left
//+"0x5de,18,19,1,0,0,,,,1f\n" // Door Rear Right
//+"0x5de,21,22,1,0,0,,,,1f\n" // Steering Lock Failure
//+"0x5de,23,23,1,0,0,,,,1f\n" // Unlocking Steering Column Warning
//+"0x5de,24,24,1,0,0,,,,1f\n" // Automatic Lock Up Activation State
//+"0x5de,25,25,1,0,0,,,,1f\n" // Badge Battery Low
//+"0x5de,28,29,1,0,0,,,,1f\n" // Trip Display Scrolling Request
//+"0x5de,32,35,1,0,0,,,,1f\n" // Smart Keyless Information Display
//+"0x5de,36,36,1,0,0,,,,1f\n" // Keyless Info Reemission Request
//+"0x5de,37,37,1,0,0,,,,1f\n" // Keyless Card Reader Failure Display
//+"0x5de,47,47,1,0,0,,,,1f\n" // Brake Switch Fault Display
//+"0x5de,49,49,1,0,0,,,,1f\n" // Stop Lamp Failure Display
//+"0x5de,56,57,1,0,0,,,,1f\n" // Rear Wiper Status
//+"0x5de,58,59,1,0,0,,,,1f\n" // Boot Open Warning
//+"0x5ee,0,0,1,0,0,,,,1f\n" // Park Light
//+"0x5e9,0,0,1,0,0,,,,1f\n" // UPAFailureDisplayRequest
//+"0x5e9,9,11,1,0,0,,,,1f\n" // FrontParkAssistVolState
//+"0x5e9,12,12,1,0,0,,,,1f\n" // RearParkAssistState
//+"0x5e9,28,31,1,0,0,,,,1f\n" // RearLeftObstacleZone
//+"0x5e9,32,35,1,0,0,,,,1f\n" // RearCenterObstacleZone
//+"0x5e9,36,39,1,0,0,,,,1f\n" // RearRightObstacleZone
//+"0x5e9,40,42,1,0,0,,,,1f\n" // UPAMode
//+"0x5e9,48,48,1,0,0,,,,1f\n" // UPA_SoundRecurrenceType
//+"0x5e9,49,55,10,0,1,Hz,,,1f\n" // UPA_SoundRecurrencePeriod
//+"0x5e9,56,58,1,0,0,,,,1f\n" // UPA_SoundObstacleZone
//+"0x5e9,59,59,1,0,0,,,,1f\n" // UPA_SoundActivationBeep
//+"0x5e9,60,60,1,0,0,,,,1f\n" // UPA_SoundErrorBeep
//+"0x5e9,61,62,1,0,0,,,,1f\n" // UPA_SoundUseContext
//+"0x5ee,0,0,1,0,0,,,,1f\n" // Parking Light
//+"0x5ee,1,1,1,0,0,,,,1f\n" // Head Light
//+"0x5ee,2,2,1,0,0,,,,1f\n" // Beam Light
//+"0x5ee,8,10,1,0,0,,,,1f\n" // Front Wiping Request
//+"0x5ee,11,15,100,0,0,W,,,1f\n" // Electrical Power Drived
//+"0x5ee,16,16,1,0,0,,,,1f\n" // Climate Cooling Request
//+"0x5ee,19,19,1,0,0,,,,1f\n" // PTC Thermal Regulator Freeze
//+"0x5ee,21,23,1,0,0,,,,1f\n" // User Identification
//+"0x5ee,24,24,1,0,0,,,,1f\n" // Day Night Status For Backlights
//+"0x5ee,27,27,1,0,0,,,,1f\n" // Driver Door State
//+"0x5ee,28,28,1,0,0,,,,1f\n" // Passenger Door State
//+"0x5ee,29,31,1,0,0,,,,1f\n" // Start Button Pushed
//+"0x5ee,32,39,0.4,0,0,%,,,1f\n" // Night Rheostated Light Max Percent
//+"0x5ee,40,40,1,0,0,,,,1f\n" // Light Sensor Status
//+"0x5ee,43,47,50,0,0,W,,,1f\n" // Right Solar Level Info
//+"0x5ee,48,52,50,0,0,W,,,1f\n" // Left Solar Level Info
//+"0x5ee,59,59,1,0,0,,,,1f\n" // Day Running Light Request
//+"0x5ee,60,63,50,0,0,W/m2,,,1f\n" // Visible Solar Level Info
//+"0x62c,0,1,1,0,0,,,,1f\n" // EPS Warning
//+"0x62d,0,9,10,0,0,kWh/100km,,,1f\n" // Worst Average Consumption
//+"0x62d,10,19,10,0,0,kWh/100km,,,1f\n" // Best Average Consumption
//+"0x62d,20,28,0.01,0,0,W,,,1f\n" // BCB Power Mains
//+"0x634,0,1,1,0,0,,,,1f\n" // TCU Refuse to Sleep
//+"0x634,2,3,1,0,0,,,,1f\n" // Ecall Function Failure Display
//+"0x634,4,7,1,0,0,,,,1f\n" // ECALL State Display
//+"0x634,8,14,15,0,0,min,,,1f\n" // Charging Timer Value Status
//+"0x634,15,15,1,0,0,,,,1f\n" // Remote Pre AC Activation
//+"0x634,16,17,1,0,0,,,,1f\n" // Charging Timer Status
//+"0x634,18,19,1,0,0,,,,1f\n" // Charge Prohibited
//+"0x634,20,21,1,0,0,,,,1f\n" // Charge Authorization
//+"0x634,22,23,1,0,0,,,,1f\n" // External Charging Manager
//+"0x637,0,9,10,0,0,kWh,,,1f\n" // Consumption Since Mission Start
//+"0x637,10,19,10,0,0,kWh,,,1f\n" // Recovery Since Mission Start
//+"0x637,20,29,10,0,0,kWh,,,1f\n" // Aux Consumption Since Mission Start
//+"0x637,32,38,1,0,0,%,,,1f\n" // Speed Score Indicator Display
//+"0x637,40,51,1,0,0,kWh,,,1f\n" // Total Recovery
//+"0x637,52,52,1,0,0,,,,1f\n" // Open Charge Flap Warning Display
//+"0x638,0,7,1,80,0,kW,,,1f\n" // Traction Instant Consumption
//+"0x638,8,17,1,0,0,km,,,1f\n" // Vehicle Autonomy Min
//+"0x638,18,27,1,0,0,km,,,1f\n" // Vehicle Autonomy Max
//+"0x638,32,36,1,0,0,kW,,,1f\n" // AuxInstant Consumption
//+"0x638,37,39,1,0,0,,,,1f\n" // Battery 14v To Be Changed Display
//+"0x646,1,3,1,0,0,,,,1f\n" // Trip Unit Consumption
//+"0x646,4,5,1,0,0,,,,1f\n" // Trip Unit Distance
//+"0x646,6,15,0.1,0,1,kWh/100km,,,1f\n" // Average trip B consumpion
//+"0x646,16,32,0.1,0,1,km,,,1f\n" // Trip B distance
//+"0x646,33,47,0.1,0,1,kWh,,,1f\n" // trip B consumption
//+"0x646,48,59,0.1,0,1,km/h,,,1f\n" // Average trip B speed
//+"0x650,1,2,1,0,0,,,,1f\n" // Energy Flow For Energy Recovering Display
//+"0x650,6,7,1,0,0,,,,1f\n" // Energy Flow For Traction Display
//+"0x650,8,9,1,0,0,,,,1f\n" // Short Range Display
//+"0x650,10,11,1,0,0,,,,1f\n" // Quick Drop Iteration Exceeded Display
//+"0x650,12,13,1,0,0,,,,1f\n" // Quick Drop Lock Failure Display
//+"0x650,14,15,1,0,0,,,,1f\n" // Quick Drop Unlocked Display
//+"0x650,16,22,1,0,0,%,,,1f\n" // Advisor Econometer
//+"0x650,30,31,1,0,0,,,,1f\n" // Cranking Plugged Display
//+"0x650,40,41,1,0,0,,,,1f\n" // Clim Programmed PC Display
//+"0x650,42,44,1,0,0,,,,1f\n" // Pre Heating State Display
//+"0x653,0,0,1,0,0,,,,1f\n" // Crash Detected
//+"0x653,1,1,1,0,0,,,,1f\n" // Crash DetectionOutOfOrder
//+"0x653,8,9,1,0,0,,,,1f\n" // Driver Safety Belt Reminder
//+"0x653,10,11,1,0,0,,,,1f\n" // Front Passenger Safety Belt Reminder
//+"0x653,12,12,1,0,0,,,,1f\n" // Passenger AIRBAG Inhibition
//+"0x653,13,13,1,0,0,,,,1f\n" // AIRBAG Malfunction
//+"0x653,14,15,1,0,0,,,,1f\n" // Second Row Center Safety Belt State
//+"0x653,16,17,1,0,0,,,,1f\n" // Second Row Left Safety Belt State
//+"0x653,18,19,1,0,0,,,,1f\n" // Second Row Right Safety Belt State
//+"0x653,20,20,1,0,0,,,,1f\n" // Valid AIRBAG Information
                        +"0x654,2,2,1,0,0,,,,1f\n" // Charging Plug Connected
//+"0x654,3,3,1,0,0,,,,1f\n" // Driver Walk Away Engine ON
//+"0x654,4,4,1,0,0,,,,1f\n" // HVBatteryUnballastAlert
                        +"0x654,25,31,1,0,0,,,,1f\n" // State of Charge
                        +"0x654,32,41,1,0,0,min,,,1f\n" // Time to Full
                        +"0x654,42,51,1,0,0,km,,,1f\n" // Available Distance
//+"0x654,52,61,0.1,0,1,kWh/100km,,,1f\n" // Average Consumption
//+"0x654,62,62,1,0,0,,,,1f\n" // HV Battery Low
//+"0x656,3,3,1,0,0,,,,1f\n" // Trip Data Reset
//+"0x656,21,31,1,0,0,min,,,1f\n" // Cluster Scheduled Time
//+"0x656,32,42,1,0,0,min,,,1f\n" // Cluster Scheduled Time 2
                        +"0x656,48,55,1,40,0,°C,,,2\n" // External Temp
//+"0x656,56,57,1,0,0,,,,2\n" // Clim PC Customer Activation
//+"0x657,0,1,1,0,0,,,,1f\n" // PreHeatingActivationRequest
//+"0x657,8,9,1,0,0,,,,1f\n" // PreHeatingActivationRequestedByKey
//+"0x657,10,11,1,0,0,,,,1f\n" // TechnicalWakeUpType
//+"0x657,12,13,1,0,0,,,,1f\n" // UnlockChargingPlugRequestedByKey
//+"0x658,0,31,1,0,0,,,,1f\n" // Battery Serial N°
                        +"0x658,33,39,1,0,0,%,,,1f\n" // Battery Health
//+"0x658,42,42,1,0,0,,,,1f\n" // Charging
//+"0x65b,0,10,1,0,0,min,,,1f\n" // Schedule timer 1 min
//+"0x65b,12,22,1,0,0,min,,,1f\n" // Schedule timer 2 min
//+"0x65b,24,30,1,0,0,%,,,1f\n" // Fluent driver
//+"0x65b,33,34,1,0,0,,,,1f\n" // Economy Mode displayed
//+"0x65b,39,40,1,0,0,,,,1f\n" // Consider eco mode
                        +"0x65b,41,43,1,0,0,,,,1f\n" // Charging Status Display
//+"0x65b,44,45,1,0,0,,,,1f\n" // Set park for charging
//+"0x665,0,1,1,0,0,,,,1f\n" // Auto Lock Up Activation Request
//+"0x665,13,15,1,0,0,,,,1f\n" // Front Park Assist Volume Req
//+"0x665,16,17,1,0,0,,,,1f\n" // Rear Park Assist Activation Req
//+"0x665,28,29,1,0,0,,,,1f\n" // Auto Rear Wiper Activation Request
//+"0x665,40,41,1,0,0,,,,1f\n" // Charging Timer Request
//+"0x665,42,48,15,0,0,min,,,1f\n" // Charging Timer Value Request
//+"0x666,0,0,1,0,0,,,,1f\n" // ESP In Regulation Display Request
//+"0x666,1,1,1,0,0,,,,1f\n" // ESP In Default Display Request
//+"0x666,3,3,1,0,0,,,,1f\n" // ASR Activation State For Display
//+"0x666,4,4,1,0,0,,,,1f\n" // ABS In Default Display Request
//+"0x666,6,6,1,0,0,,,,1f\n" // EBV In Default Display Request
//+"0x666,7,7,1,0,0,,,,1f\n" // Emergency Braking Failure
//+"0x666,8,8,1,0,0,,,,1f\n" // ABS-ESP Lamps Auto Check
//+"0x666,14,14,1,0,0,,,,1f\n" // ABS or ESP In Calibrating Diag
//+"0x666,15,15,1,0,0,,,,1f\n" // ABS or ESP To Be Calibrated
//+"0x666,23,23,1,0,0,,,,1f\n" // HSA Failure Display Request
//+"0x668,0,1,1,0,0,,,,1f\n" // Clim AQS Activation State
//+"0x668,4,7,1,0,0,,,,1f\n" // AQM Frag Intensity State
//+"0x668,8,10,1,0,0,,,,1f\n" // AQM Frag Select State
//+"0x668,11,13,1,0,0,,,,1f\n" // AQM Ioniser Mode State
//+"0x668,14,15,1,0,0,,,,1f\n" // Ioniser Auto Launch State
//+"0x66a,5,7,1,0,0,,,,1f\n" // Cruise Control Mode
//+"0x66a,8,15,1,0,0,km/h,,,1f\n" // Cruise Control Speed
//+"0x66a,16,16,1,0,0,,,,1f\n" // Cruise Control OverSpeed
//+"0x66d,0,1,1,0,0,,,,1f\n" // Braking System Defective Display
//+"0x66d,2,3,1,0,0,,,,1f\n" // Braking System To Be Checked Display
//+"0x66d,4,5,1,0,0,,,,1f\n" // UBP To Be Calibrated
//+"0x66d,6,7,1,0,0,,,,1f\n" // UBP In Calibrating Diag
//+"0x66d,8,9,1,0,0,,,,1f\n" // UBP Lamp Auto Check
                        +"0x673,0,0,1,0,0,,,,1f\n" // Speed pressure misadaptation
                        +"0x673,2,4,1,0,0,,,,1f\n" // Rear right wheel state
                        +"0x673,5,7,1,0,0,,,,1f\n" // Rear left wheel state
                        +"0x673,8,10,1,0,0,,,,1f\n" // Front right wheel state
                        +"0x673,11,13,1,0,0,,,,1f\n" // Front left wheel state
                        +"0x673,16,23,13.725,0,0,mbar,,,1f\n" // Rear right wheel pressure
                        +"0x673,24,31,13.725,0,0,mbar,,,1f\n" // Rear left wheel pressure
                        +"0x673,32,39,13.725,0,0,mbar,,,1f\n" // Front right wheel pressure
                        +"0x673,40,47,13.725,0,0,mbar,,,1f\n" // Front left wheel pressure
//+"0x68b,0,3,1,0,0,,,,1f\n" // MM action counter
//+"0x68c,21,31,1,0,0,min,,,1f\n" // Local Time
//+"0x699,0,1,1,0,0,,,,2\n" // Clima off Request display
//+"0x699,2,3,1,0,0,,,,2\n" // Clima read defrost Request display
//+"0x699,4,6,1,1,0,,,,2\n" // Cima mode
                        +"0x699,8,15,0.5,0,0,°C,,,2\n" // Temperature
//+"0x699,16,19,1,0,0,,,,2\n" // Clima Flow
//+"0x699,20,21,1,0,0,,,,2\n" // Forced recycling
//+"0x699,22,23,1,0,0,,,,2\n" //
//+"0x699,24,27,1,0,0,,,,2\n" //
//+"0x699,28,31,1,0,0,,,,2\n" // Clim Last Func Modified By Customer
//+"0x699,32,33,1,0,0,,,,2\n" // Clim MMI Activation Request
//+"0x699,34,39,2,0,0,%,,,2\n" // Clim AQS Indicator
//+"0x699,40,45,1,0,0,min,,,2\n" // Clim AQM Ioniser Max Timer Display
//+"0x699,46,51,1,0,0,min,,,2\n" // Clim AQM Ioniser Timer Display
//+"0x699,52,53,1,0,0,,,,2\n" // Clim Auto Display
//+"0x699,54,55,1,0,0,,,,2\n" // Clim AC Off Display
//+"0x699,56,57,1,0,0,,,,2\n" // Clim Clearness Display
//+"0x699,58,59,1,0,0,,,,2\n" // Clim Display Menu PC
//+"0x699,60,61,1,0,0,,,,2\n" // Energy Flow For Thermal Comfort Display
//+"0x699,63,63,1,0,0,,,,2\n" // Clim Eco Low Soc Display
//+"0x69f,0,31,1,0,0,,,,1f\n" // Car Serial N°
//+"0x6f8,0,1,1,0,0,,,,1f\n" // USM Refuse to Sleep
//+"0x6f8,4,4,1,0,0,,,,1f\n" // Ignition Supply Confirmation
//+"0x6f8,5,5,1,0,0,,,,1f\n" // Front Wiper Stop Position
//+"0x6f8,6,7,1,0,0,,,,1f\n" // Front Wiper Status
//+"0x6f8,11,11,1,0,0,,,,1f\n" // Ignition Control State
//+"0x6f8,16,23,0.0625,0,2,V,,,1f\n" // 12V Battery Voltage
//+"0x6fb,8,9,1,0,0,,,,1f\n" // Global Vehicle Warning State
//+"0x6fb,32,39,250,0,0,km,,,1f\n" // Fixed Maintenance Range
                        +"0x760,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x760,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number %04lx
                        +"0x760,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x760,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
//+"0x760,24,31,1,0,0,bar,0x224b0e,0x624b0e,2\n" // Master cylinder pressure
                        +"0x762,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x762,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number %04lx
                        +"0x762,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x762,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
//+"0x762,24,39,0.390625,100,0,V,0x22012f,0x62012f,1f\n" // 12V Battery Voltage
                        +"0x763,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x763,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number %04lx
                        +"0x763,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x763,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
//+"0x763,24,31,1,0,0,,0x222001,0x622001,1f\n" // Parking Break
//+"0x763,3,3,1,0,0,,0x2220f0,0x6220f0,1f\n" // VOL+
//+"0x763,4,4,1,0,0,,0x2220f0,0x6220f0,1f\n" // VOL-
//+"0x763,2,2,1,0,0,,0x2220f0,0x6220f0,1f\n" // Mute
//+"0x763,5,5,1,0,0,,0x2220f0,0x6220f0,1f\n" // Media
//+"0x763,6,6,1,0,0,,0x2220f0,0x6220f0,1f\n" // Radio
                        +"0x764,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x764,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x764,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x764,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
//+"0x764,8,15,0.4,40,1,,0x2121,0x6121,5\n" // Interior temperature
                        +"0x765,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x765,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x765,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x765,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x76d,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x76d,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x76d,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x76d,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x76e,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x76e,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x76e,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x76e,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x772,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x772,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x772,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x772,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x77e,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x77e,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x77e,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x77e,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
//+"0x77e,24,31,1,0,0,,0x22300f,0x62300f,1f\n" // dcdc state
//+"0x77e,24,31,31.25,0,3,V,0x22300e,0x62300e,1f\n" // traction battery voltage
                        +"0x77e,24,39,0.015625,0,2,ºC,0x223018,0x623018,1f\n" // DCDC converter temperature
//+"0x77e,24,31,0.03125,0,0,Nm,0x223024,0x623024,1f\n" // torque requested
//+"0x77e,24,31,0.03125,0,0,Nm,0x223025,0x623025,1f\n" // torque applied
                        +"0x77e,24,31,0.015625,0,2,°C,0x22302b,0x62302b,1f\n" // inverter temperature
//+"0x77e,24,31,6.25,0,2,A,0x22301d,0x62301d,1f\n" // Current
                        +"0x793,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x793,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number %04lx
                        +"0x793,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x793,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x7b6,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x7b6,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x7b6,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x7b6,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
//+"0x7bb,192,207,0.01,0,2,kW,0x2101,0x6101,5\n" // Maximum battery input power
//+"0x7bb,208,223,0.01,0,2,kW,0x2101,0x6101,5\n" // Maximum battery output power
//+"0x7bb,348,367,0.0001,0,4,Ah,0x2101,0x6101,5\n" // Ah of the battery
//+"0x7bb,316,335,0.0001,0,4,%,0x2101,0x6101,5\n" // Real State of Charge
                        +"0x7bb,336,351,0.01,0,2,kW,0x2101,0x6101,2\n" // Maximum battery input power
                        +"0x7bb,56,71,10,0,0,°C,0x2103,0x6103,5\n" // Mean battery compartment temp
//+"0x7bb,104,119,1,0,0,mV,0x2103,0x6103,5\n" // Highest cell voltage
//+"0x7bb,120,135,1,0,0,mV,0x2103,0x6103,5\n" // Lowest cell voltage
                        +"0x7bb,192,207,0.01,0,2,%,0x2103,0x6103,2\n" // Real State of Charge
//+"0x7bb,16,31,1,0,0,,0x2104,0x6104,2\n" // Module 1 raw NTC
                        +"0x7bb,32,39,1,40,0,°C,0x2104,0x6104,2\n" // Cell 1 Temperature
//+"0x7bb,40,55,1,0,0,,0x2104,0x6104,2\n" // Module 2 raw NTC
                        +"0x7bb,56,63,1,40,0,°C,0x2104,0x6104,2\n" // Cell 2 Temperature
//+"0x7bb,64,79,1,0,0,,0x2104,0x6104,2\n" // Module 3 raw NTC
                        +"0x7bb,80,87,1,40,0,°C,0x2104,0x6104,2\n" // Cell 3 Temperature
//+"0x7bb,88,103,1,0,0,,0x2104,0x6104,2\n" // Module 4 raw NTC
                        +"0x7bb,104,111,1,40,0,°C,0x2104,0x6104,2\n" // Cell 4 Temperature
//+"0x7bb,112,127,1,0,0,,0x2104,0x6104,2\n" // Module 5 raw NTC
                        +"0x7bb,128,135,1,40,0,°C,0x2104,0x6104,2\n" // Cell 5 Temperature
//+"0x7bb,136,151,1,0,0,,0x2104,0x6104,2\n" // Module 6 raw NTC
                        +"0x7bb,152,159,1,40,0,°C,0x2104,0x6104,2\n" // Cell 6 Temperature
//+"0x7bb,160,175,1,0,0,,0x2104,0x6104,2\n" // Module 7 raw NTC
                        +"0x7bb,176,183,1,40,0,°C,0x2104,0x6104,2\n" // Cell 7 Temperature
//+"0x7bb,184,199,1,0,0,,0x2104,0x6104,2\n" // Module 8 raw NTC
                        +"0x7bb,200,207,1,40,0,°C,0x2104,0x6104,2\n" // Cell 8 Temperature
//+"0x7bb,208,223,1,0,0,,0x2104,0x6104,2\n" // Module 9 raw NTC
                        +"0x7bb,224,231,1,40,0,°C,0x2104,0x6104,2\n" // Cell 9 Temperature
//+"0x7bb,232,247,1,0,0,,0x2104,0x6104,2\n" // Module 10 raw NTC
                        +"0x7bb,248,255,1,40,0,°C,0x2104,0x6104,2\n" // Cell 10 Temperature
//+"0x7bb,256,271,1,0,0,,0x2104,0x6104,2\n" // Module 11 raw NTC
                        +"0x7bb,272,279,1,40,0,°C,0x2104,0x6104,2\n" // Cell 11 Temperature
//+"0x7bb,280,295,1,0,0,,0x2104,0x6104,2\n" // Module 12 raw NTC
                        +"0x7bb,296,303,1,40,0,°C,0x2104,0x6104,2\n" // Cell 12 Temperature
//+"0x7bb,16,31,1,0,0,,0x2104,0x6104,1\n" // Module 1 raw NTC
                        +"0x7bb,32,39,1,0,0,°C,0x2104,0x6104,25\n" // Cell 1 Temperature
//+"0x7bb,40,55,1,0,0,,0x2104,0x6104,1\n" // Module 2 raw NTC
                        +"0x7bb,56,63,1,0,0,°C,0x2104,0x6104,25\n" // Cell 2 Temperature
//+"0x7bb,64,79,1,0,0,,0x2104,0x6104,1\n" // Module 3 raw NTC
                        +"0x7bb,80,87,1,0,0,°C,0x2104,0x6104,25\n" // Cell 3 Temperature
//+"0x7bb,88,103,1,0,0,,0x2104,0x6104,1\n" // Module 4 raw NTC
                        +"0x7bb,104,111,1,0,0,°C,0x2104,0x6104,25\n" // Cell 4 Temperature
//+"0x7bb,64,79,0.001,0,3,V,0x2105,0x6105,1\n" // Threshold bad cell
//+"0x7bb,80,95,0.001,0,3,V,0x2105,0x6105,1\n" // Threshol weak cell
//+"0x7bb,20,20,1,0,0,,0x2107,0x6107,1\n" // Cell 01 Balancing Shunt Active
//+"0x7bb,21,21,1,0,0,,0x2107,0x6107,1\n" // Cell 02 Balancing Shunt Active
//+"0x7bb,22,22,1,0,0,,0x2107,0x6107,1\n" // Cell 03 Balancing Shunt Active
//+"0x7bb,23,23,1,0,0,,0x2107,0x6107,1\n" // Cell 04 Balancing Shunt Active
//+"0x7bb,28,28,1,0,0,,0x2107,0x6107,1\n" // Cell 05 Balancing Shunt Active
                        +"0x7bb,16,31,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 01 V
                        +"0x7bb,32,47,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 02 V
                        +"0x7bb,48,63,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 03 V
                        +"0x7bb,64,79,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 04 V
                        +"0x7bb,80,95,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 05 V
                        +"0x7bb,96,111,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 06 V
                        +"0x7bb,112,127,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 07 V
                        +"0x7bb,128,143,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 08 V
                        +"0x7bb,144,159,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 09 V
                        +"0x7bb,160,175,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 10 V
                        +"0x7bb,176,191,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 11 V
                        +"0x7bb,192,207,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 12 V
                        +"0x7bb,208,223,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 13 V
                        +"0x7bb,224,239,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 14 V
                        +"0x7bb,240,255,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 15 V
                        +"0x7bb,256,271,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 16 V
                        +"0x7bb,272,287,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 17 V
                        +"0x7bb,288,303,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 18 V
                        +"0x7bb,304,319,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 19 V
                        +"0x7bb,320,335,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 20 V
                        +"0x7bb,336,351,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 21 V
                        +"0x7bb,352,367,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 22 V
                        +"0x7bb,368,383,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 23 V
                        +"0x7bb,384,399,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 24 V
                        +"0x7bb,400,415,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 25 V
                        +"0x7bb,416,431,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 26 V
                        +"0x7bb,432,447,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 27 V
                        +"0x7bb,448,463,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 28 V
                        +"0x7bb,464,479,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 29 V
                        +"0x7bb,480,495,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 30 V
                        +"0x7bb,496,511,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 31 V
                        +"0x7bb,512,527,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 32 V
                        +"0x7bb,528,543,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 33 V
                        +"0x7bb,544,559,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 34 V
                        +"0x7bb,560,575,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 35 V
                        +"0x7bb,576,591,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 36 V
                        +"0x7bb,592,607,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 37 V
                        +"0x7bb,608,623,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 38 V
                        +"0x7bb,624,639,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 39 V
                        +"0x7bb,640,655,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 40 V
                        +"0x7bb,656,671,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 41 V
                        +"0x7bb,672,687,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 42 V
                        +"0x7bb,688,703,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 43 V
                        +"0x7bb,704,719,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 44 V
                        +"0x7bb,720,735,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 45 V
                        +"0x7bb,736,751,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 46 V
                        +"0x7bb,752,767,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 47 V
                        +"0x7bb,768,783,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 48 V
                        +"0x7bb,784,799,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 49 V
                        +"0x7bb,800,815,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 50 V
                        +"0x7bb,816,831,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 51 V
                        +"0x7bb,832,847,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 52 V
                        +"0x7bb,848,863,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 53 V
                        +"0x7bb,864,879,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 54 V
                        +"0x7bb,880,895,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 55 V
                        +"0x7bb,896,911,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 56 V
                        +"0x7bb,912,927,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 57 V
                        +"0x7bb,928,943,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 58 V
                        +"0x7bb,944,959,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 59 V
                        +"0x7bb,960,975,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 60 V
                        +"0x7bb,976,991,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 61 V
                        +"0x7bb,992,1007,0.001,0,3,V,0x2141,0x6141,1f\n" // Cell 62 V
                        +"0x7bb,16,31,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 63 V
                        +"0x7bb,32,47,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 64 Vl
                        +"0x7bb,48,63,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 65 V
                        +"0x7bb,64,79,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 66 V
                        +"0x7bb,80,95,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 67 V
                        +"0x7bb,96,111,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 68 V
                        +"0x7bb,112,127,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 69 V
                        +"0x7bb,128,143,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 70 V
                        +"0x7bb,144,159,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 71 V
                        +"0x7bb,160,175,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 72 V
                        +"0x7bb,176,191,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 73 V
                        +"0x7bb,192,207,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 74 V
                        +"0x7bb,208,223,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 75 V
                        +"0x7bb,224,239,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 76 V
                        +"0x7bb,240,255,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 77 V
                        +"0x7bb,256,271,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 78 V
                        +"0x7bb,272,287,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 79 V
                        +"0x7bb,288,303,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 80 V
                        +"0x7bb,304,319,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 81 V
                        +"0x7bb,320,335,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 82 V
                        +"0x7bb,336,351,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 83 V
                        +"0x7bb,352,367,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 84 V
                        +"0x7bb,368,383,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 85 V
                        +"0x7bb,384,399,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 86 V
                        +"0x7bb,400,415,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 87 V
                        +"0x7bb,416,431,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 88 V
                        +"0x7bb,432,447,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 89 V
                        +"0x7bb,448,463,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 90 V
                        +"0x7bb,464,479,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 91 V
                        +"0x7bb,480,495,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 92 V
                        +"0x7bb,496,511,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 93 V
                        +"0x7bb,512,527,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 94 V
                        +"0x7bb,528,543,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 95 V
                        +"0x7bb,544,559,0.001,0,3,V,0x2142,0x6142,1f\n" // Cell 96 V
//+"0x7bb,60,79,0.0001,0,4,Ah,0x2161,0x6161,5\n" // Ah of the battery
//+"0x7bb,80,87,0.05,0,2,%,0x2161,0x6161,5\n" // Battery State of Health
                        +"0x7bb,96,119,1,0,0,km,0x2161,0x6161,1f\n" // Battery mileage in km
//+"0x7bb,136,151,1,0,0,kWh,0x2161,0x6161,5\n" // Total energy output of battery?
                        +"0x7bb,144,159,1,0,0,,0x2180,0x6180,1f\n" // Software version
                        +"0x7bb,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number %04lx
                        +"0x7bb,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x7bb,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x7bc,144,159,1,0,0,,0x2180,0x6180,1f\n" // Request firmware version
                        +"0x7bc,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x7bc,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x7bc,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
//+"0x7ca,0,0,1,0,,,0x2180,0x2180,\n" // Request firmware version
//+"0x7ca,0,0,1,0,,,0x2180,0x14ffffff,\n" // PG number
//+"0x7ca,0,0,1,0,,,0x14ffff,0x19023b,\n" // Reset DTC
//+"0x7ca,0,23,1,0,,,0x19023b,,\n" // Query DTC
                        +"0x7da,144,159,1,0,0,,0x2180,0x6180,1f\n" // Request firmware version
                        +"0x7da,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x7da,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x7da,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
                        +"0x7ec,144,159,1,0,0,,0x2180,0x6180,1f\n" // Request firmware version
                        +"0x7ec,128,143,1,0,0,,0x2180,0x6180,1f\n" // PG number
                        +"0x7ec,0,7,1,0,0,,0x14ffff,0x54,1f\n" // Reset DTC
                        +"0x7ec,0,23,1,0,0,,0x19023b,0x5902ff,1f\n" // Query DTC
//+"0x7ec,24,31,1,40,0,C,0x222001,0x622001,1f\n" // Bat temp
                        +"0x7ec,24,39,2,0,2,%,0x222002,0x622002,2\n" // SOC
                        +"0x7ec,24,39,2.083333333,0,2,%,0x222002,0x622002,5\n" // SOC
//+"0x7ec,24,39,0.01,0,2,km/h,0x222003,0x622003,5\n" // Speed
//+"0x7ec,24,39,0.5,0,2,V,0x222004,0x622004,1f\n" // Motor Voltage
                        +"0x7ec,24,39,0.01,0,2,V,0x222005,0x622005,1f\n" // 12V battery voltage
                        +"0x7ec,24,47,1,0,0,km,0x222006,0x622006,1f\n" // Odometer
//+"0x7ec,24,39,1,0,0,,0x22200b,0x62200b,1f\n" // Accelerator potentiometer 1
//+"0x7ec,24,39,1,0,0,,0x22200c,0x62200c,1f\n" // Accelerator potentiometer 2
//+"0x7ec,24,31,1,0,0,,0x222026,0x622026,1f\n" // Brake pedal
//+"0x7ec,24,39,1,0,0,,0x22202e,0x62202e,1f\n" // Accelerator Pedal
//+"0x7ec,24,31,1,0,0,km/h,0x222035,0x622035,1f\n" // CC/SL set speed
//+"0x7ec,24,39,0.01,0,2,,0x222050,0x622050,1f\n" // Speed
//+"0x7ec,24,31,1,0,0,,0x22204b,0x62204b,5\n" // Steering wheel CC/SL buttons
//+"0x7ec,24,31,1,0,0,,0x222c04,0x622c04,1f\n" // Gear
                        +"0x7ec,24,31,0.5,0,1,A,0x223028,0x623028,1f\n" // 14V current?
                        +"0x7ec,24,39,0.5,0,2,V,0x223203,0x623203,1f\n" // HV Battery voltage
                        +"0x7ec,24,39,0.25,0x8000,2,A,0x223204,0x623204,1f\n" // HV Battery current
                        +"0x7ec,24,31,1,0,0,%,0x223206,0x623206,1f\n" // Battery health in %
                        +"0x7ec,24,31,1,1,0,,0x223318,0x623318,1f\n" // Motor Water pump speed
                        +"0x7ec,24,31,1,1,0,,0x223319,0x623319,1f\n" // Charger pump speed
                        +"0x7ec,24,31,1,1,0,,0x22331A,0x62331A,1f\n" // Heater water pump speed
//+"0x7ec,24,31,1,40,0,°C,0x2233b1,0x6233b1,1f\n" // Ext temp
//+"0x7ec,24,31,2,0,0,,0x223437,0x623437,1f\n" // Pack voltage
//+"0x7ec,24,31,0.3,0,0,kW,0x223444,0x623444,1f\n" // Maximum charger power

                ;

//        try {
//            //fieldDef += readFromLocalFile();
//        }
//        catch(Exception e)
//        {
//            // ignore
//        }

        String[] lines = fieldDef.split("\n");
        for (String line : lines) {
            //MainActivity.debug("Fields: Reading > "+line);
            //Get all tokens available in line
            String[] tokens = line.split(",");
            if (tokens.length == 10) {
                int frameId = Integer.parseInt(tokens[FIELD_ID].trim().replace("0x", ""), 16);
                Frame frame = Frames.getInstance().getById(frameId);
                if (frame == null) {
                    MainActivity.debug("frame does not exist:" + tokens[FIELD_ID].trim());
                } else {
                    short options = Short.parseShort(tokens[FIELD_OPTIONS].trim(), 16);
                    // ensure this field matches the selected car
                    if ((options & MainActivity.car) != 0) {
                        //Create a new field object and fill his  data
                        MainActivity.debug(tokens[FIELD_ID] + "," + tokens[FIELD_FROM]);
                        Field field = new Field(
                                frame,
                                Short.parseShort(tokens[FIELD_FROM].trim()),
                                Short.parseShort(tokens[FIELD_TO].trim()),
                                Double.parseDouble(tokens[FIELD_RESOLUTION].trim()),
                                Integer.parseInt(tokens[FIELD_DECIMALS].trim()),
                                (
                                        tokens[FIELD_OFFSET].trim().contains("0x")
                                                ?
                                                Integer.parseInt(tokens[FIELD_OFFSET].trim().replace("0x", ""), 16)
                                                :
                                                Double.parseDouble(tokens[FIELD_OFFSET].trim())
                                ),
                                tokens[FIELD_UNIT].trim(),
                                tokens[FIELD_REQUEST_ID].trim().replace("0x", ""),
                                tokens[FIELD_RESPONSE_ID].trim().replace("0x", ""),
                                options
                        );
                        // add the field to the list of available fields
                        add(field);
                    }
                }
            }
        }
    }

    private String readFromLocalFile()
    {
        //*Don't* hardcode "/sdcard"
        File sdcard = Environment.getExternalStorageDirectory();
        MainActivity.debug("SD: "+sdcard.getAbsolutePath());

        //Get the text file
        File file = new File(sdcard,"fields.csv");

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
       return text.toString();
    }
/*
    private void readFromFile(String filename) throws IOException // FileNotFoundException,
    {
        BufferedReader fileReader = new BufferedReader(new FileReader(filename));
        String line;
        //Read the file line by line starting from the second line
        while ((line = fileReader.readLine()) != null)
        {
            //Get all tokens available in line
            String[] tokens = line.split(",");
            if (tokens.length == 15)
            {
                int divider = Integer.parseInt(tokens[FIELD_DIVIDER].trim());
                int multiplier = Integer.parseInt(tokens[FIELD_MULTIPLIER].trim());
                int decimals = Integer.parseInt(tokens[FIELD_DECIMALS].trim());
                double multi = ((double) multiplier/divider)/(decimals==0?1:decimals); // <<<<<< Probably wrong, as decimals have completely changed, and that should be in file too
                int frameId = Integer.parseInt(tokens[FIELD_ID].trim().replace("0x", ""), 16);

                Frame frame = Frames.getInstance().getById(frameId);
                Field field = new Field(
                        frame,
                        Integer.parseInt(tokens[FIELD_FROM].trim()),
                        Integer.parseInt(tokens[FIELD_TO].trim()),
                        multi,
                        Integer.parseInt(tokens[FIELD_DECIMALS].trim()),
                        (
                                tokens[FIELD_OFFSET].trim().contains("0x")
                                        ?
                                        Integer.parseInt(tokens[FIELD_OFFSET].trim().replace("0x", ""), 16)
                                        :
                                        Integer.parseInt(tokens[FIELD_OFFSET].trim())
                        ),
                        tokens[FIELD_UNIT].intern(),
                        tokens[FIELD_REQUEST_ID].trim().replace("0x", "").intern(),
                        tokens[FIELD_RESPONSE_ID].trim().replace("0x", "").intern(),
                        Integer.parseInt(tokens[FIELD_CAR].trim())
                        //Integer.parseInt(tokens[FIELD_SKIPS].trim())
                        //Integer.parseInt(tokens[FIELD_FREQ].trim())
                );
                // add the field to the list of available fields
                add(field);
            }
        }
    }
*/
    public Field getBySID(String sid) {
        sid=sid.toLowerCase();

/*
        // first let's try to get the field that is bound to the selected car
        Field tryField = fieldsBySid.get(MainActivity.car + "."+sid);
        if(tryField!=null) return tryField;

        // if none is found, try the other one, starting with 0 = CAR_ANY
        for(int i=0; i<5; i++) {
            tryField = fieldsBySid.get(i + "." + sid);
            if (tryField != null) return tryField;
        }
*/
        // since we changed logic to initialize the hashmaps with only the current car's fields, we're always fine just looking for the SID
        Field tryField = fieldsBySid.get(sid);
        if(tryField!=null) return tryField;


        return null;
    }

    public int size() {
        return fields.size();
    }

    public Field get(int index) {
        return fields.get(index);
    }

    public Object[] toArray() {
        return fields.toArray();
    }


    @Override
    public void onMessageCompleteEvent(Message message) {
        for(int i=0; i< fields.size(); i++)
        {
            Field field = fields.get(i);

            // we can stop iterating if we're above the requested ID
            if (field.getId() > message.getField().getId()) break;

            if(field.getId()== message.getField().getId() &&
                    (
                            message.getField().getResponseId()==null
                            ||
                            message.getField().getResponseId().trim().equals(field.getResponseId().trim())
                    ))
            {
                String binString = message.getAsBinaryString();
                if(binString.length()>= field.getTo()) {
                    // parseInt --> signed, so the first bit is "cut-off"!
                    try {
                        // experiment with unavailable: any field >= 5 bits whose value contains only 1's
                        binString = binString.substring(field.getFrom(), field.getTo() + 1);
                        if (binString.length() <= 4 || binString.contains("0")) {
                            int val;
                            if (field.isSigned() && binString.startsWith("1")) {
                                // ugly :-(
                                val = Integer.parseInt("-" + binString.replace('0', 'q').replace('1','0').replace('q','1'), 2) - 1;
                            } else {
                                val = Integer.parseInt("0" + binString, 2);
                            }
                            //MainActivity.debug("Value of " + field.getHexId() + "." + field.getResponseId() + "." + field.getFrom()+" = "+val);
                            //MainActivity.debug("Fields: onMessageCompleteEvent > "+field.getSID()+" = "+val);
                            field.setValue(val);
                            // update the fields last request date
                            field.updateLastRequest();
                            // do field logging
                            if(MainActivity.fieldLogMode)
                                FieldLogger.getInstance().log(field.getSID()+","+val);
                        } else {
                            field.setValue(Double.NaN);
                        }
/*
                        int val = Integer.parseInt("0" + binString.substring(field.getFrom(), field.getTo() + 1), 2);
                        //MainActivity.debug("Value of " + field.getHexId() + "." + field.getResponseId() + "." + field.getFrom()+" = "+val);
                        //MainActivity.debug("Fields: onMessageCompleteEvent > "+field.getSID()+" = "+val);
                        field.setValue(val);
                        // update the fields last request date
                        field.updateLastRequest();
*/
                    } catch (Exception e)
                    {
                        // ignore
                    }
                }
            }
        }
    }

    public void add(Field field) {
        fields.add(field);
        fieldsBySid.put(field.getSID(),field);
        //fieldsBySid.put(field.getCar()+"."+field.getSID(),field);
    }

    public void notifyAllFieldListeners()
    {
        for(int i=0; i< fields.size(); i++) {
            fields.get(i).notifyFieldListeners();
        }
    }

    public void clearAllFields()
    {
        for(int i=0; i< fields.size(); i++) {
            fields.get(i).setValue(0);
        }
    }

    public void load ()
    {
        fields.clear();
        fieldsBySid.clear();
        fillStatic();
        addVirtualFields();
    }

    /* --------------------------------
     * Tests ...
     \ ------------------------------ */
    
    public static void main(String[] args)
    {
        
    }
    
}
