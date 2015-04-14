package com.drl.lutz.lullabyapp.logic;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by lutz on 13/04/15.
 * Code taken and modified from
 * http://stackoverflow.com/questions/25727535/record-audio-save-in-wav-file-format-in-android
 * and ?
 */
public class SoundRecorderWav2 extends SoundRecorder {

    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    short[] audioData;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    // Buffer for output
    private byte[] buffer;

    // File writer
    private RandomAccessFile randomAccessWriter;

    //size of the record
    int bytesRecorded;

    public SoundRecorderWav2(Context context) {
        super(context);

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING);
        audioData = new short[bufferSize];
    }

    @Override
    public void prepare(String outputFile) throws Exception {

        randomAccessWriter = new RandomAccessFile(getFilename(), "rw");

        writeWaveFileHeader(randomAccessWriter);

        buffer = new byte[bufferSize];

    }

    @Override
    public void startRecording() throws Exception {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

        int state = recorder.getState();
        if(state == AudioRecord.STATE_INITIALIZED)
            recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile(randomAccessWriter);
            }
        },"AudioRecorder Thread");
        recordingThread.start();
    }

    @Override
    public void stopRecording() {
        if (recorder != null) {
            isRecording = false;

            int state = recorder.getState();
            if( state == AudioRecord.STATE_INITIALIZED)
                recorder.stop();

            recorder.release();

            recorder = null;
            recordingThread = null;

            try {
                writeWavSizeToHeader(randomAccessWriter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void reset() {
        stopRecording();
        //deleteTempFile();
    }

    @Override
    public Uri save() {
        return null;
    }

    @Override
    public void release() {
        reset();

    }

    private String getTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private String getFilename (){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + System.currentTimeMillis() +
                AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private void writeAudioDataToFile(RandomAccessFile fileWriter){

        while (isRecording) {

            // fill buffer
            recorder.read(buffer, 0, buffer.length);

            // write buffer to file
            try {
                fileWriter.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }

            bytesRecorded += buffer.length;

        }
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    private void writeWaveFileHeader(RandomAccessFile fileWriter) throws IOException {

        short bSamples;
        if (RECORDER_AUDIO_ENCODING == AudioFormat.ENCODING_PCM_16BIT) {
            bSamples = 16;
        } else {
            bSamples = 8;
        }

        short nChannels;
        if (RECORDER_CHANNELS == AudioFormat.CHANNEL_IN_MONO) {
            nChannels = 1;
        } else {
            nChannels = 2;
        }

        int sRate = RECORDER_SAMPLERATE;

        fileWriter.setLength(0); // Set file length to 0, to prevent unexpected behavior in case the file already existed
        fileWriter.writeBytes("RIFF");
        fileWriter.writeInt(0); // Final file size not known yet, write 0
        fileWriter.writeBytes("WAVE");
        fileWriter.writeBytes("fmt ");
        fileWriter.writeInt(Integer.reverseBytes(16)); // Sub-chunk size, 16 for PCM
        fileWriter.writeShort(Short.reverseBytes((short) 1)); // AudioFormat, 1 for PCM
        fileWriter.writeShort(Short.reverseBytes(nChannels));// Number of channels, 1 for mono, 2 for stereo
        fileWriter.writeInt(Integer.reverseBytes(sRate)); // Sample rate
        fileWriter.writeInt(Integer.reverseBytes(sRate * bSamples * nChannels / 8)); // Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
        fileWriter.writeShort(Short.reverseBytes((short) (nChannels * bSamples / 8))); // Block align, NumberOfChannels*BitsPerSample/8
        fileWriter.writeShort(Short.reverseBytes(bSamples)); // Bits per sample
        fileWriter.writeBytes("data");
        fileWriter.writeInt(0); // Data chunk size not known yet, write 0
    }


    private void writeWavSizeToHeader(RandomAccessFile fileWriter) throws Exception {

        fileWriter.seek(4); // Write size to RIFF header
        fileWriter.writeInt(Integer.reverseBytes(36 + bytesRecorded));

        fileWriter.seek(40); // Write size to Subchunk2Size field
        fileWriter.writeInt(Integer.reverseBytes(bytesRecorded));

        fileWriter.close();
    }
}