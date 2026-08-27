package com.telemetry;

public class CarSimulator implements Runnable {

    private double speed = 0.0;
    private int rpm = 800;
    private boolean accelerating = false;
    private boolean braking = false;
    private boolean running = true;
    private double accelerationPower = 1.0;

    public void setAccelerating(boolean accelerating) {
        this.accelerating = accelerating;
    }

    public void setBraking(boolean braking) {
        this.braking = braking;
    }

    public void setAccelerationPower(double power) {
        this.accelerationPower = power;
    }

    public boolean isAccelerating() {
        return accelerating;
    }

    public boolean isBraking() {
        return braking;
    }

    public double getSpeed() {
        return speed;
    }

    public int getRpm() {
        return rpm;
    }

    public void stopSimulation() {
        this.running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(30);

                if (accelerating) {
                    speed += 1.5 * accelerationPower;
                    if (speed > 350) speed = 350;
                    rpm += 300;
                    if (rpm > 11500) rpm = 11500;
                } else if (braking) {
                    speed -= 4.0;
                    if (speed < 0) speed = 0;
                    rpm -= 600;
                    if (rpm < 1000) rpm = 1000;
                } else {
                    if (speed > 0) {
                        speed -= 0.5;
                        if (speed < 0) speed = 0;
                    }
                    if (rpm > 2000) {
                        rpm -= 200;
                    }
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}