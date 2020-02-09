package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;

import java.util.List;

import static org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.DEGREES;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesOrder.XYZ;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesReference.EXTRINSIC;

/**
 * Created by Sam W on 9/17/2019.
 */

@com.qualcomm.robotcore.eventloop.opmode.TeleOp(name = "TeleOp", group = "TeleOp")
public class TeleOp extends OpMode{

    private boolean clawPressed = false;
    private boolean clawOpen = false;
    private boolean turning = false;
    private double turnVal = 0;
    private long startTime = 0;

    SolidRobot robot;


    /**
     * init - runs once after the init button is pressed
     */
    public void init(){

        robot = new SolidRobot(hardwareMap, false);


    }

    public void init_loop(){
        robot.doDaSleep(50);

    }

    public void start(){
        robot.resetAngle();
    }

    /**
     * loop - runs continuously after the play button is pressed
     */
    public void loop(){

        telemetry.addData("FLW", robot.FLW.getCurrentPosition());
        telemetry.addData("BLW", robot.BLW.getCurrentPosition());
        telemetry.addData("BRW", robot.BRW.getCurrentPosition());
        telemetry.addData("FRW", robot.FRW.getCurrentPosition());
        telemetry.addData("Turning", turning);
        telemetry.addData("gyro", robot.getAngle());
        telemetry.addData("right claw", robot.rightClaw);
        telemetry.addData("left claw", robot.leftClaw); //0.55 left, 0.8 right

        // telemetry.addData("Visible Target", robot.targetVisible);

        if(gamepad1.a) {
            robot.rightFoundationClaw = 0.36;
            robot.leftFoundationClaw = 0.36;
        }
        else if(gamepad1.b){
            robot.rightFoundationClaw = 0.0;
            robot.leftFoundationClaw = 0.0;
        }

        int liftDivider;
        if(gamepad2.left_bumper) liftDivider = 2;
        else liftDivider = 1;
        robot.leftLift = gamepad2.left_stick_y * 0.7 / liftDivider;
        robot.rightLift = gamepad2.left_stick_y * 0.7 / liftDivider;

        if(gamepad2.a){
            if(!clawPressed){
                clawPressed = true;
                if(clawOpen){
                    clawOpen = false;
                    robot.leftClaw = 1.0;
                    robot.rightClaw = 1.0;
                }
                else{
                    clawOpen = true;
                    robot.leftClaw = 0.4;
                    robot.rightClaw = 0.6;
                }
            }
        }
        else{
            clawPressed = false;
        }

        if(gamepad1.x){
            robot.leftClaw = 1.0;
            robot.rightClaw = 1.0;
        }
        else if(gamepad1.y){
            robot.leftClaw = 0.4;
            robot.rightClaw = 0.6;
        }

        if(gamepad1.dpad_up) robot.tapeMeasure = 0.75;
        else if(gamepad1.dpad_down) robot.tapeMeasure = -0.75;
        else robot.tapeMeasure = 0.0;

        int driveDivider;
        if(gamepad1.right_bumper) driveDivider = 1;
        else if(gamepad1.left_bumper) driveDivider = 8;
        else driveDivider = 2;

        double strafeAddend = 0.0;
        if(gamepad1.right_trigger > 0.2) strafeAddend = 1.0;
        else if(gamepad1.left_trigger > 0.2) strafeAddend = -1.0;

        robot.backLeftWheel = (-gamepad1.left_stick_y - strafeAddend) / driveDivider;
        robot.frontLeftWheel = (-gamepad1.left_stick_y + strafeAddend) / driveDivider;
        robot.backRightWheel = (-gamepad1.right_stick_y + strafeAddend) / driveDivider;
        robot.frontRightWheel = (-gamepad1.right_stick_y - strafeAddend) / driveDivider;

        robot.clipToMotorRange(robot.backLeftWheel);
        robot.clipToMotorRange(robot.frontLeftWheel);
        robot.clipToMotorRange(robot.backRightWheel);
        robot.clipToMotorRange(robot.frontRightWheel);

        if(gamepad1.back && !turning){
            turning = true;
            turnVal = robot.getAngle() + 180;
            startTime = System.currentTimeMillis();
        }
        if(turning){
            if(robot.getAngle() > turnVal || System.currentTimeMillis() > startTime + 1000){ //If it has turn 180 or has gone a second without making it
                turning = false;
            }
            else{
                robot.backLeftWheel = -1.0;
                robot.frontLeftWheel = -1.0;
                robot.backRightWheel = 1.0;
                robot.frontRightWheel = 1.0;
            }
        }

        robot.setAllPositions();
        getVuforiaInfo();
    }

    public void getTFODInfo(){
        if (robot.tfod != null) {
            // getUpdatedRecognitions() will return null if no new information is available since
            // the last time that call was made.
            List<Recognition> updatedRecognitions = robot.tfod.getUpdatedRecognitions();
            if (updatedRecognitions != null) {
                telemetry.addData("# Object Detected", updatedRecognitions.size());

                // step through the list of recognitions and display boundary info.
                int i = 0;
                for (Recognition recognition : updatedRecognitions) {
                    telemetry.addData(String.format("label (%d)", i), recognition.getLabel());
                    telemetry.addData(String.format("  left,top (%d)", i), "%.03f , %.03f",
                            recognition.getLeft(), recognition.getTop());
                    telemetry.addData(String.format("  right,bottom (%d)", i), "%.03f , %.03f",
                            recognition.getRight(), recognition.getBottom());
                }
                telemetry.update();
            }
        }
    }

    public void getVuforiaInfo(){
        // check all the trackable targets to see which one (if any) is visible.
        robot.targetVisible = false;
        for (VuforiaTrackable trackable : robot.allTrackables) {
            if (((VuforiaTrackableDefaultListener)trackable.getListener()).isVisible()) {
                robot.targetVisible = true;
                // getUpdatedRobotLocation() will return null if no new information is available since
                // the last time that call was made, or if the trackable is not currently visible.
                OpenGLMatrix robotLocationTransform = ((VuforiaTrackableDefaultListener)trackable.getListener()).getUpdatedRobotLocation();
                if (robotLocationTransform != null) {
                    robot.lastLocation = robotLocationTransform;
                }
                break;
            }
        }

        // Provide feedback as to where the robot is located (if we know).
        if (robot.targetVisible) {
            // express position (translation) of robot in inches.
            VectorF translation = robot.lastLocation.getTranslation();
            telemetry.addData("Pos (in)", "{X, Y, Z} = %.1f, %.1f, %.1f",
                    translation.get(0) / robot.mmPerInch, translation.get(1) / robot.mmPerInch, translation.get(2) / robot.mmPerInch);



            // express the rotation of the robot in degrees.
            Orientation rotation = Orientation.getOrientation(robot.lastLocation, EXTRINSIC, XYZ, DEGREES);
            telemetry.addData("Rot (deg)", "{Roll, Pitch, Heading} = %.0f, %.0f, %.0f", rotation.firstAngle, rotation.secondAngle, rotation.thirdAngle);
        }
        else {
            telemetry.addData("Visible Target", "none");
        }
        telemetry.update();
    }

}
