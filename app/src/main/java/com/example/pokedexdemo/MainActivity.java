package com.example.pokedexdemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.pokedexdemo.databinding.ActivityMainBinding;

import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.PyTorchAndroid;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    //binding
    private ActivityMainBinding binding;
    //speechRec
    TextToSpeech textToSpeech;
    public static final Integer RecordAudioRequestCode = 1;
    private static final int MY_CAMERA_REQUEST_CODE = 2;
    private static final int HIDDEN_SIZE = 2048;
    private static final int EOS_TOKEN = 2;
    private static final int MAX_LENGTH = 50;
    private static final int PERMISSION_CODE = 1002;

    private SpeechRecognizer speechRecognizer;
    ArrayList<String> data;
    private Module mModuleEncoder;
    private Module mModuleDecoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       //binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        //permission
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }
        //speechRec
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {
               binding.txtMic.setText("");
               binding.txtMic.setHint("Listening...");
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle bundle) {
                binding.imgMic.setImageResource(R.drawable.ic_baseline_mic_none_24);
                data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                binding.txtMic.setText(data.get(0));

                if (data.get(0).equalsIgnoreCase("camera")||
                        data.get(0).equalsIgnoreCase("take a picture")){
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent,0);
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
        //textToSpeech
        textToSpeech();
        //camera
        binding.imgViewCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent,0);
        });
        binding.imgMic.setOnTouchListener((view1, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                speechRecognizer.stopListening();
            }
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                binding.imgMic.setImageResource(R.drawable.ic_mic_black_24red);
                speechRecognizer.startListening(speechRecognizerIntent);
            }
            return false;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechRecognizer.destroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap bitmap =(Bitmap)data.getExtras().get("data");
        binding.imgViewCamera.setImageBitmap(bitmap);
        String result = generateCaption(bitmap);
        binding.txtDescriptionPic.setText(result);
    }

    private void textToSpeech(){
        textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS){
                int language = textToSpeech.setLanguage(Locale.ENGLISH);
            }
        });
        binding.txtDescriptionPic.setOnClickListener(v -> {
            String text = binding.txtDescriptionPic.getText().toString();
            int speech = textToSpeech.speak(text,TextToSpeech.QUEUE_FLUSH,null);
        });
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},RecordAudioRequestCode);
            //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0 ){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
        }
    }


    private String generateCaption(Bitmap inputImage) {
        mModuleEncoder = PyTorchAndroid.loadModuleFromAsset(getAssets(), "encoder.pth");
        mModuleDecoder = PyTorchAndroid.loadModuleFromAsset(getAssets(), "decoder.pth");

        String json;
        JSONObject wrd2idx;
        JSONObject idx2wrd;
        try {
            InputStream is = getAssets().open("index2word.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            json = new String(buffer, "UTF-8");
            idx2wrd = new JSONObject(json);

            is = getAssets().open("word2index.json");
            size = is.available();
            buffer = new byte[size];
            is.read(buffer);
            is.close();

            json = new String(buffer, "UTF-8");
            wrd2idx = new JSONObject(json);
        } catch (JSONException | IOException e) {
            android.util.Log.e("TAG", "JSONException | IOException ", e);
            return null;
        }

        // preparing input tensor
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(inputImage,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

        // running the model
        final Tensor featureTensor = mModuleEncoder.forward(IValue.from(inputTensor)).toTensor();
        final long[] outputsShape = new long[]{MAX_LENGTH, HIDDEN_SIZE};
        final FloatBuffer outputsTensorBuffer =
                Tensor.allocateFloatBuffer(MAX_LENGTH  * HIDDEN_SIZE);

        // DECODER GENERATE CAPTION
        final long[] decoderInputShape = new long[]{1, 1};
        LongBuffer mInputTensorBuffer = Tensor.allocateLongBuffer(1);
        mInputTensorBuffer.put(1);
        Tensor mInputTensor = Tensor.fromBlob(mInputTensorBuffer, decoderInputShape);

        Tensor hiddenTensor = featureTensor;




        Tensor outputsTensor = Tensor.fromBlob(outputsTensorBuffer, outputsShape);


//        IValue.from(outputsTensor)
        ArrayList<Integer> result = new ArrayList<>(MAX_LENGTH);
        for (int i=0; i<MAX_LENGTH; i++) {
            final IValue[] outputTuple = mModuleDecoder.forward(
                    IValue.from(mInputTensor),
                    IValue.from(hiddenTensor)).toTuple();
            final Tensor decoderOutputTensor = outputTuple[0].toTensor();
            hiddenTensor = outputTuple[1].toTensor();
            float[] outputs = decoderOutputTensor.getDataAsFloatArray();
            int topIdx = 0;
            double topVal = -Double.MAX_VALUE;
            for (int j=0; j<outputs.length; j++) {
                if (outputs[j] > topVal) {
                    topVal = outputs[j];
                    topIdx = j;
                }
            }

            if (topIdx == EOS_TOKEN) break;

            result.add(topIdx);
            mInputTensorBuffer = Tensor.allocateLongBuffer(1);
            mInputTensorBuffer.put(topIdx);
            mInputTensor = Tensor.fromBlob(mInputTensorBuffer, decoderInputShape);
        }
        String english = "";
        try {
            for (int i = 0; i < result.size(); i++)
                english += " " + idx2wrd.getString("" + result.get(i));
        }
        catch (JSONException e) {
            android.util.Log.e("TAG", "JSONException ", e);
        }
        return english;


    }
}