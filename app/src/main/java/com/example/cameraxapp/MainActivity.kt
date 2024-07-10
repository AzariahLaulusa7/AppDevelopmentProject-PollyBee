// Copyright 2024 Azariah Laulusa
package com.example.cameraxapp

import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import java.util.Random
import kotlin.math.abs

class MainActivity : AppCompatActivity(), SensorEventListener {
    private val sequence = mutableListOf<Int>()
    private var mSensorManager: SensorManager? = null
    private var mSensorAccelerometer: Sensor? = null
    private var mSensorMagnetometer: Sensor? = null
    private var mAccelerometerData = FloatArray(3)
    private var mMagnetometerData = FloatArray(3)
    private val handler = Handler()
    private var bee: ImageView? = null
    private var flower: ImageView? = null
    private var bullseye: ImageView? = null
    private var flower2: ImageView? = null
    private var flower3: ImageView? = null
    private var flower4: ImageView? = null
    private var bullseye2: ImageView? = null
    private var bullseye3: ImageView? = null
    private var bullseye4: ImageView? = null
    private var vibrate: Vibrator? = null
    private var sequenceIndex = 0
    private var difficultyLevel = 1
    private var time: Long = 2000
    private var tracker = 0
    private var myRef: DatabaseReference? = null
    var levelText : TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        bee = findViewById<View>(R.id.bee) as ImageView
        flower = findViewById<View>(R.id.flower) as ImageView
        flower2 = findViewById<View>(R.id.flower2) as ImageView
        flower3 = findViewById<View>(R.id.flower3) as ImageView
        flower4 = findViewById<View>(R.id.flower4) as ImageView
        bullseye = findViewById<View>(R.id.bullseye) as ImageView
        bullseye2 = findViewById<View>(R.id.bullseye2) as ImageView
        bullseye3 = findViewById<View>(R.id.bullseye3) as ImageView
        bullseye4 = findViewById<View>(R.id.bullseye4) as ImageView
        vibrate = getSystemService(VIBRATOR_SERVICE) as Vibrator
        levelText = findViewById(R.id.level)
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mSensorAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensorMagnetometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        // Write a message to the database
        val database = Firebase.database
        myRef = database.getReference("CurrentScore")
    }

    override fun onStart() {
        super.onStart()
        play()
    }

    private fun play() {
        levelText?.text  = "Level $difficultyLevel"
        generateSequence(difficultyLevel)
        flashSequence()
    }

    private fun sense() {
        if (mSensorAccelerometer != null) {
            mSensorManager!!.registerListener(
                this, mSensorAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        if (mSensorMagnetometer != null) {
            mSensorManager!!.registerListener(
                this, mSensorMagnetometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onStop() {
        super.onStop()
        mSensorManager!!.unregisterListener(this)
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val metrics = resources.displayMetrics
        val deviceWidth = metrics.widthPixels
        val deviceHeight = metrics.heightPixels
        when (sensorEvent.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> mAccelerometerData = sensorEvent.values.clone()
            Sensor.TYPE_MAGNETIC_FIELD -> mMagnetometerData = sensorEvent.values.clone()
            else -> return
        }
        val rotationMatrix = FloatArray(9)
        val rotationOK = SensorManager.getRotationMatrix(
            rotationMatrix, null,
            mAccelerometerData, mMagnetometerData
        )
        val orientationValues = FloatArray(3)
        if (rotationOK) {
            SensorManager.getOrientation(rotationMatrix, orientationValues)
        }
        var pitch = orientationValues[1]
        var roll = orientationValues[2]
        if (abs(pitch) < VALUE_DRIFT) {
            pitch = 0f
        }
        if (abs(roll) < VALUE_DRIFT) {
            roll = 0f
        }

        //radian values overflow 1.0 max, but it's okay
        //  we don't care about tracking the full device tilt
        //referenced code for bee movement:
        //https://stackoverflow.com/questions/31359371/android-studio-how-to-get-an-imageview-to-move-in-the-direction-of-rotation
        val speed = 200.0
        if (pitch > 0) {
            if (bee!!.y > 0) {
                bee!!.y = (bee!!.y + -pitch * speed).toFloat()
            }
        } else {
            if (bee!!.y <= deviceHeight - bee!!.measuredHeight) {
                bee!!.y = (bee!!.y + -pitch * speed).toFloat()
            }
        }
        if (roll > 0) {
            if (bee!!.x <= deviceWidth - bee!!.measuredWidth) {
                bee!!.setImageResource(R.drawable.right_bee)
                bee!!.x = (bee!!.x + roll * speed).toFloat()
            }
        } else {
            if (bee!!.x > 0) {
                bee!!.setImageResource(R.drawable.left_bee)
                bee!!.x = (bee!!.x + roll * speed).toFloat()
            }
        }

        //referenced code for touching bullseye:
        //https://www.youtube.com/watch?v=gfX8UHTpq3o
        if (bee!!.x + bee!!.width >= bullseye!!.x && bee!!.x <= bullseye!!.x + bullseye!!.width && bee!!.y + bee!!.height >= bullseye!!.y && bee!!.y <= bullseye!!.y + bullseye!!.height) {
            mSensorManager!!.unregisterListener(this)
            flower!!.setImageResource(R.drawable.pollen_flower)
            //referenced code for vibration:
            //https://stackoverflow.com/questions/13950338/how-to-make-an-android-device-vibrate-with-different-frequency
            vibrate!!.vibrate(500)
            if (sequence[tracker] == 0 && tracker == difficultyLevel-1) {
                difficultyLevel++
                time = (2000 * difficultyLevel).toLong()
                tracker = 0
                flower!!.setImageResource(R.drawable.flower)
                bee!!.x = ((deviceWidth - bee!!.measuredWidth)/2).toFloat()
                bee!!.y = ((deviceHeight - bee!!.measuredHeight)/2).toFloat()
                play()
            } else if (sequence[tracker] == 0) {
                tracker++
                bee!!.x = ((deviceWidth - bee!!.measuredWidth)/2).toFloat()
                bee!!.y = ((deviceHeight - bee!!.measuredHeight)/2).toFloat()
                sense()
            } else {
                val intent = Intent(this@MainActivity, GameOverActivity::class.java)
                myRef?.setValue(difficultyLevel-1)
                finish()
                startActivity(intent)
            }
        } else {
            flower!!.setImageResource(R.drawable.flower)
        }

        if (bee!!.x + bee!!.width >= bullseye2!!.x && bee!!.x <= bullseye2!!.x + bullseye2!!.width && bee!!.y + bee!!.height >= bullseye2!!.y && bee!!.y <= bullseye2!!.y + bullseye2!!.height) {
            mSensorManager!!.unregisterListener(this)
            flower2!!.setImageResource(R.drawable.flower_red)
            //referenced code for vibration:
            //https://stackoverflow.com/questions/13950338/how-to-make-an-android-device-vibrate-with-different-frequency
            vibrate!!.vibrate(500)
            if (sequence[tracker] == 1 && tracker == difficultyLevel-1) {
                difficultyLevel++
                time = (2000 * difficultyLevel).toLong()
                tracker = 0
                flower2!!.setImageResource(R.drawable.flower)
                bee!!.x = ((deviceWidth - bee!!.measuredWidth)/2).toFloat()
                bee!!.y = ((deviceHeight - bee!!.measuredHeight)/2).toFloat()
                play()
            } else if (sequence[tracker] == 1) {
                tracker++
                bee!!.x = ((deviceWidth - bee!!.measuredWidth)/2).toFloat()
                bee!!.y = ((deviceHeight - bee!!.measuredHeight)/2).toFloat()
                sense()
            } else {
                val intent = Intent(this@MainActivity, GameOverActivity::class.java)
                myRef?.setValue(difficultyLevel-1)
                finish()
                startActivity(intent)
            }
        } else {
            flower2!!.setImageResource(R.drawable.flower)
        }

        if (bee!!.x + bee!!.width >= bullseye3!!.x && bee!!.x <= bullseye3!!.x + bullseye3!!.width && bee!!.y + bee!!.height >= bullseye3!!.y && bee!!.y <= bullseye3!!.y + bullseye3!!.height) {
            mSensorManager!!.unregisterListener(this)
            flower3!!.setImageResource(R.drawable.flower_green)
            //referenced code for vibration:
            //https://stackoverflow.com/questions/13950338/how-to-make-an-android-device-vibrate-with-different-frequency
            vibrate!!.vibrate(500)
            if (sequence[tracker] == 2 && tracker == difficultyLevel-1) {
                difficultyLevel++
                time = (2000 * difficultyLevel).toLong()
                tracker = 0
                flower3!!.setImageResource(R.drawable.flower)
                bee!!.x = ((deviceWidth - bee!!.measuredWidth)/2).toFloat()
                bee!!.y = ((deviceHeight - bee!!.measuredHeight)/2).toFloat()
                play()
            } else if (sequence[tracker] == 2) {
                tracker++
                bee!!.x = ((deviceWidth - bee!!.measuredWidth)/2).toFloat()
                bee!!.y = ((deviceHeight - bee!!.measuredHeight)/2).toFloat()
                sense()
            } else {
                val intent = Intent(this@MainActivity, GameOverActivity::class.java)
                myRef?.setValue(difficultyLevel-1)
                finish()
                startActivity(intent)
            }
        } else {
            flower3!!.setImageResource(R.drawable.flower)
        }

        if (bee!!.x + bee!!.width >= bullseye4!!.x && bee!!.x <= bullseye4!!.x + bullseye4!!.width && bee!!.y + bee!!.height >= bullseye4!!.y && bee!!.y <= bullseye4!!.y + bullseye4!!.height) {
            mSensorManager!!.unregisterListener(this)
            flower4!!.setImageResource(R.drawable.flower_blue)
            //referenced code for vibration:
            //https://stackoverflow.com/questions/13950338/how-to-make-an-android-device-vibrate-with-different-frequency
            vibrate!!.vibrate(500)
            if (sequence[tracker] == 3 && tracker == difficultyLevel-1) {
                difficultyLevel++
                time = (2000 * difficultyLevel).toLong()
                tracker = 0
                flower4!!.setImageResource(R.drawable.flower)
                bee!!.x = ((deviceWidth - bee!!.measuredWidth)/2).toFloat()
                bee!!.y = ((deviceHeight - bee!!.measuredHeight)/2).toFloat()
                play()
            } else if (sequence[tracker] == 3) {
                tracker++
                bee!!.x = ((deviceWidth - bee!!.measuredWidth)/2).toFloat()
                bee!!.y = ((deviceHeight - bee!!.measuredHeight)/2).toFloat()
                sense()
            } else {
                val intent = Intent(this@MainActivity, GameOverActivity::class.java)
                myRef?.setValue(difficultyLevel-1)
                finish()
                startActivity(intent)
            }
        } else {
            flower4!!.setImageResource(R.drawable.flower)
        }
    }

    private fun generateSequence(length: Int) {
        sequence.clear()
        var randomNum = Random()
        var previous = -1
        for (i in 0 until length) {
            while (true) {
                var pickedRandom: Int = randomNum.nextInt(4 - 0) + 0
                if (previous != pickedRandom) {
                    sequence.add(pickedRandom)
                    previous = pickedRandom
                    break
                }
            }
        }
    }

    private fun flashSequence() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (sequenceIndex < sequence.size) {
                    if (sequence[sequenceIndex] == 0) {
                        flower!!.setImageResource(R.drawable.pollen_flower)
                        flower2!!.setImageResource(R.drawable.flower)
                        flower3!!.setImageResource(R.drawable.flower)
                        flower4!!.setImageResource(R.drawable.flower)
                    } else if (sequence[sequenceIndex] == 1) {
                        flower2!!.setImageResource(R.drawable.flower_red)
                        flower!!.setImageResource(R.drawable.flower)
                        flower3!!.setImageResource(R.drawable.flower)
                        flower4!!.setImageResource(R.drawable.flower)
                    } else if (sequence[sequenceIndex] == 2) {
                        flower3!!.setImageResource(R.drawable.flower_green)
                        flower!!.setImageResource(R.drawable.flower)
                        flower2!!.setImageResource(R.drawable.flower)
                        flower4!!.setImageResource(R.drawable.flower)
                    } else {
                        flower4!!.setImageResource(R.drawable.flower_blue)
                        flower!!.setImageResource(R.drawable.flower)
                        flower2!!.setImageResource(R.drawable.flower)
                        flower3!!.setImageResource(R.drawable.flower)
                    }
                    sequenceIndex++
                    handler.postDelayed(this, 2000)
                } else {
                    sequenceIndex = 0
                    sense()
                }
            }
        }, 2000)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        //intentionally blank
    }

    companion object {
        private const val VALUE_DRIFT = 0.05f
    }
}
