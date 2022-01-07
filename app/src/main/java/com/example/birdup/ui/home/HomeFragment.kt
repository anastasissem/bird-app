package com.example.birdup.ui.home


import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.birdup.databinding.FragmentHomeBinding
import android.Manifest

import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.Chronometer
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getDrawable
import com.example.birdup.R
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

private const val LOG_TAG = "AudioRecordTest"

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // HomeFragment exclusive vars
    private var fileName: String? = null
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    private var timeWhenStopped: Long = 0

    private var isRecording = false
    private var isPaused = false

    // Requesting permission to use device microphone
    private fun recordPermissionSetup() {
        val permission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.RECORD_AUDIO)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            permissionsResultCallback.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            onRecord()
        }
    }

    // Requesting permission to write to device storage
    private fun writePermissionSetup(){
        val permission = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            permissionsResultCallback.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            //onRecord()
        }
    }

    private val permissionsResultCallback = registerForActivityResult(
        ActivityResultContracts.RequestPermission()){
        when (it) {
            true -> {
                onRecord()
                println("Permission has been granted by user")
            }
            false -> {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val meter: Chronometer = root.findViewById(R.id.chronometer)

        // INTERNAL FUNCTIONS
        fun timer() {

            if (!isPaused) {
                meter.base = SystemClock.elapsedRealtime() + timeWhenStopped
                meter.start()
            } else {
                timeWhenStopped = meter.base - SystemClock.elapsedRealtime()
                meter.stop()
            }

            if (timeWhenStopped == 0L) {
                if (!isPaused){
                    Toast.makeText(requireContext(), "RECORDING", Toast.LENGTH_LONG).show()
                }
            } else {
                if (isPaused){
                    Toast.makeText(requireContext(), "PAUSED", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "RESUMED", Toast.LENGTH_LONG).show()
                }
            }
        }

        /* START/STOP */
        val recordButton: ImageButton = root.findViewById(R.id.RecordButton)
        recordButton.setOnClickListener {
            recordPermissionSetup()
            timer()

            /* Change bird icon between start/pause */
            if (isPaused) {
                val icon = getDrawable(requireContext(), R.drawable.still_stork)
                recordButton.setImageDrawable(icon)
            } else {
                val icon = getDrawable(requireContext(), R.drawable.stork_medium)
                recordButton.setImageDrawable(icon)
            }
        }

        /* PLAYER FUNCTION*/
        val playButton: ImageButton = root.findViewById(R.id.playButton)
        playButton.setOnClickListener  {
            // Not interactive when recording is in progress
            if ((!isRecording && !isPaused) || (isRecording && isPaused)) {
                // move to save button
                writePermissionSetup()
                if (context?.filesDir?.listFiles()?.isEmpty() == true) {
                    Toast.makeText(requireContext(), "Nothing to Play!", Toast.LENGTH_SHORT).show()
                } else {
                    releaseRecorder()
                    player = MediaPlayer().apply {
                        try {
                            setDataSource(fileName.toString())
                            prepare()
                            start()
                            //disable playback if already playing
                            playButton.isEnabled = false
                            Log.d(LOG_TAG, "PLAY ON")
                            val icon = getDrawable(requireContext(), R.drawable.pause_black_small)
                            playButton.setImageDrawable(icon)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        setOnCompletionListener {
                            val icon = getDrawable(requireContext(), R.drawable.play_black_small)
                            playButton.setImageDrawable(icon)
                            reset()
                            //enable playback for future clicks
                            playButton.isEnabled = true
                            Log.d(LOG_TAG, "PLAY END")
                        }
                    }
                }
            }
        }

        /* RESET & PREDICT */
        val analyzeButton: Button = root.findViewById(R.id.analyzeButton)
        analyzeButton.setOnClickListener {
            // Not interactive when recording is in progress
            if ((!isRecording && !isPaused) || (isRecording && isPaused)) {
                if (context?.filesDir?.listFiles()?.isEmpty() == true) {
                    Toast.makeText(requireContext(), "Nothing to Predict!", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Log.d(LOG_TAG, "RESET")
                    /* reset meter after completing recording*/
                    finish()
                    Toast.makeText(requireContext(), "STOPPED", Toast.LENGTH_LONG).show()
                    meter.base = SystemClock.elapsedRealtime()
                }
            }
        }

        /* DISCARD & RESET */
        val trashButton: ImageButton = root.findViewById((R.id.trashButton))
        trashButton.setOnClickListener {
            // Not interactive when recording is in progress
            if ((!isRecording && !isPaused) || (isRecording && isPaused)) {
                Log.d(LOG_TAG, "DISCARD")
                finish()
                meter.base = SystemClock.elapsedRealtime()
                if (context?.filesDir?.listFiles()?.isNotEmpty() == true) {
                    for (f in requireContext().filesDir?.listFiles()!!) {
                        Log.d(LOG_TAG, f.absolutePath)
                        f.delete()
                        Toast.makeText(requireContext(), "Trash emptied!", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Log.d(LOG_TAG, "No Files.\n")
                    Toast.makeText(requireContext(), "Nothing in trash", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        return root
    }

    // DECLARE FUNCTIONS TO BE USED

    private fun finish() {
        Log.d(LOG_TAG, "STOP")
        releaseRecorder()
        timeWhenStopped = 0
        isPaused = false
        isRecording = false
    }

    /* RECORDER FUNCTIONS START*/
    private fun onRecord() {
        when{
            isPaused -> resumeRecording()
            isRecording -> pauseRecording()
            else -> startRecording()
        }
    }

    private fun releaseRecorder() {
        recorder?.stop()
        recorder?.release()
        recorder = null
    }

    private fun startRecording() {
//        val simpleDateFormat = SimpleDateFormat("d MM yyyy", Locale.getDefault())
//        val date : String = simpleDateFormat.format(Date())
//        fileName = requireContext().filesDir?.path+"/TEST_$date.3gp"
        fileName = requireContext().filesDir?.path+"/audiorecordtest.3gp"
        File(fileName?:"").createNewFile()

        /* Branch for if MediaRecorder() is not deprecated */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            recorder = MediaRecorder(requireContext()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(fileName)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC_ELD)
                setAudioChannels(2)
                setAudioEncodingBitRate(32*44100)
                setAudioSamplingRate(44100)

                try {
                    prepare()
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "prepare() failed")
                }

                try {
                    Log.d(LOG_TAG, "START")
                    start()
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "start() failed")
                }
            }
            isRecording = true
            isPaused = false
        }
        else {
            /* Branch for if MediaRecorder() is deprecated */
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(fileName)
                setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
                setAudioChannels(1)
                setAudioEncodingBitRate(32*44100)
                setAudioSamplingRate(44100)

                try {
                    prepare()
                } catch (e: IOException) {
                    Log.i(LOG_TAG, "prepare() failed")
                }

                try {
                    Log.d(LOG_TAG, "START")
                    start()
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "start() failed")
                }
            }
        }
        isRecording = true
        isPaused = false
    }

    private fun pauseRecording(){
        /* pause() requires Android 7 or higher(API 24) */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(LOG_TAG, "PAUSE")
            try {
                recorder?.pause()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "pause() failed")
            }
        }
        isPaused = true
    }

    private fun resumeRecording(){
        /* resume() requires Android 7 or higher(API 24) */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(LOG_TAG, "RESUME")
            try {
                recorder?.resume()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "resume() failed")
            }
        }
        isPaused = false
    }
    /* RECORDER FUNCTIONS END */

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onStop() {
        super.onStop()
        recorder?.release()
        recorder = null
        player?.release()
        player = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}