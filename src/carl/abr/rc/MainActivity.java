/*******************************************************************************************************
Copyright (c) 2011 Regents of the University of California.
All rights reserved.

This software was developed at the University of California, Irvine.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.

3. All advertising materials mentioning features or use of this
   software must display the following acknowledgment:
   "This product includes software developed at the University of
   California, Irvine by Nicolas Oros, Ph.D.
   (http://www.cogsci.uci.edu/~noros/)."

4. The name of the University may not be used to endorse or promote
   products derived from this software without specific prior written
   permission.

5. Redistributions of any form whatsoever must retain the following
   acknowledgment:
   "This product includes software developed at the University of
   California, Irvine by Nicolas Oros, Ph.D.
   (http://www.cogsci.uci.edu/~noros/)."

THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
IN NO EVENT SHALL THE UNIVERSITY OR THE PROGRAM CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*******************************************************************************************************/

package carl.abr.rc;

import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends IOIOActivity implements SensorEventListener 
{
	static final String TAG = "ABR_RC";
		
	/***************************************************************  GUI   ***************************************************************/	
	ToggleButton toggleDrive;
	TextView accelView = null;
	TextView compassView = null;
	TextView gyroView = null;
	
	/***************************************************************  sensors   ***************************************************************/	
	SensorManager mSensorManager;
	Sensor accel;
	Sensor compass;
	Sensor gyro;

	/***************************************************************  Values for orientation and acceleration   ***************************************************************/
	float[] mMagneticValues;
	float[] mAccelerometerValues;
	float[] gyroscope_values;
	float[] orientation;
	float[] Rot;	
	
	/***************************************************************  IOIO   ***************************************************************/
	static final int MOTOR_PIN = 5;
	static final int SERVO_PIN = 7;
	
	/***************************************************************  Values for controlling car   ***************************************************************/
	float pwm_motor, pwm_servo;
	int p_servo, p_motor;
	float PWM_STOP = 1500;
	boolean REVERSE_PWM = true;
	int COEFF_SERVO = 50, COEFF_MOTOR = 10;
	
	/***************************************************************  activity  ***************************************************************/
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		accel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		compass = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		gyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

		accelView = (TextView) findViewById(R.id.textAccel);
		compassView = (TextView) findViewById(R.id.textCompass);
		gyroView = (TextView) findViewById(R.id.textGyro);
		toggleDrive = (ToggleButton) findViewById(R.id.drive_btn);
	}	

	@Override
	protected void onResume() 
	{
		super.onResume();
		mSensorManager.registerListener(this, accel,SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, compass,SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, gyro,	SensorManager.SENSOR_DELAY_GAME);
		
		Rot = new float[9];		
		mMagneticValues = new float[3];
		mAccelerometerValues = new float[3];
		orientation = new float[3];
		gyroscope_values = new float[3];
	}

	@Override
	protected void onPause() 
	{
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	
	/***************************************************************  sensors  ***************************************************************/
	@Override
	public void onSensorChanged(SensorEvent event) 
	{
		switch (event.sensor.getType()) 
		{
		case Sensor.TYPE_MAGNETIC_FIELD:
			mMagneticValues[0] = event.values[0];
			mMagneticValues[1] = event.values[1];
			mMagneticValues[2] = event.values[2];			
			break;
			
		case Sensor.TYPE_GYROSCOPE:
			gyroscope_values[0] = event.values[0];
			gyroscope_values[1] = event.values[1];
			gyroscope_values[2] = event.values[2];
			gyroView.setText("Gyro: " + "\n"+ gyroscope_values[0] + "\n" + gyroscope_values[1] +"\n"+ gyroscope_values[2]);
			break;
			
		case Sensor.TYPE_ACCELEROMETER:
			mAccelerometerValues[0] = event.values[0];
			mAccelerometerValues[1] = event.values[1];
			mAccelerometerValues[2] = event.values[2];
			accelView.setText("Accel: " + "\n"+ mAccelerometerValues[0] + "\n" + mAccelerometerValues[1] +"\n"+ mAccelerometerValues[2]);
			
			p_servo = (int) (mAccelerometerValues[0] * COEFF_SERVO);
			p_motor = (int) (mAccelerometerValues[1] * COEFF_MOTOR);
			break;
		}

		SensorManager.getRotationMatrix(Rot, null, mAccelerometerValues, mMagneticValues);	        
		SensorManager.getOrientation(Rot, orientation);
		orientation[0] = (float) Math.toDegrees(orientation[0]);
		orientation[1] = (float) Math.toDegrees(orientation[1]);
		orientation[2] = (float) Math.toDegrees(orientation[2]);
		compassView.setText("Compass: " + "\n"+ orientation[0] + "\n" + orientation[1] +"\n"+ orientation[2]);	
	}
	
	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {}
	
	
	/***************************************************************  IOIO   ***************************************************************/
	class Looper extends BaseIOIOLooper 
	{
		PwmOutput motor;	// motor to move forward and backward
		PwmOutput servo;	// servo to steer/turn

		@Override
		protected void setup() throws ConnectionLostException 
		{
			motor = ioio_.openPwmOutput(MOTOR_PIN, 100);
			servo = ioio_.openPwmOutput(SERVO_PIN, 100);
		}

		@Override
		public void loop() throws ConnectionLostException 
		{
			try 
			{
				if (toggleDrive.isChecked())				// transition from off to on 
				{				
					pwm_motor = PWM_STOP - p_motor;
					pwm_servo = PWM_STOP - p_servo;					
					
					if(REVERSE_PWM == true)
					{
						pwm_motor = PWM_STOP - (pwm_motor-PWM_STOP);
						pwm_servo = PWM_STOP - (pwm_servo-PWM_STOP);
					}
				}
				else			// transition from on to off 
				{
					pwm_motor = PWM_STOP;
					pwm_servo = PWM_STOP;
				} 				
//				Log.d(TAG, "PWM: " + pwm_motor + "  " + pwm_servo);
				
				motor.setPulseWidth(pwm_motor);
				servo.setPulseWidth(pwm_servo);
				Thread.sleep(10);
			} 
			catch (InterruptedException exc) {Log.e(TAG, "Error: ", exc);	}
		}
	}

	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
}
