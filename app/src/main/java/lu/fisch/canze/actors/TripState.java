package lu.fisch.canze.actors;

/**
 * Created by Florian on 28.08.2016.
 */
public class TripState {

    protected double actualSoc;
    protected double actualOdometer;
    protected double drivenDistance;

    public TripState(double actualSoc, double actualOdometer, double drivenDistance) {
        this.actualSoc = actualSoc;
        this.actualOdometer = actualOdometer;
        this.drivenDistance = drivenDistance;
    }

    public double getActualSoc() {
        return actualSoc;
    }

    public double getActualOdometer() {
        return actualOdometer;
    }

    public double getDrivenDistance() {
        return drivenDistance;
    }
}


