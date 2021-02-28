package com.example.pokedexdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import java.util.Locale;

public class SliderAdapter extends PagerAdapter {
    Context context;
    LayoutInflater inflater;
    TextToSpeech textToSpeech;
    public SliderAdapter(Context context){
        this.context =context;
    }
    public int[] imageArray =
            {R.drawable.technology,
            R.drawable.selfie,
            R.drawable.technology,
            R.drawable.question,
            R.drawable.question
            };
    public String[] titleArray =
            {"Before Starting",
            "Pokedex",
            "Image Captioning",
            "How to use",
            "null"
            };
    public String[] descriptionArray =
            {"Welcome to Pokedex. In the introduction, you will be informed about the content of the application by voice command. Please scroll the page",
                    "Pokedex is an image captioning system based on a smartphone, which offers low-cost, portable and user-friendly platform for visually impaired. Please scroll the page",
                    "Generating captions and text descriptions of images will enable visually impaired extended accessibility to the real world, thus re" +
                            "ducing their social isolation, improving their well-being, employability and education experience.Please scroll the page",
            "There is a microphone on the left top of the screen for you to give commands by voice or a button where located below of the page for performing physical actions. Therefore let's seize the moment. Please scroll to the main page",
            "null"
            };
    public int[] backgroundColorArray =
                    {Color.rgb(129,143,191),
                    Color.rgb(55,55,55),
                    Color.rgb(239,85,85),
                    Color.rgb(110,49,89),
                    Color.rgb(110,49,89)
                    };

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return (view==object);
    }

    @Override
    public int getCount() {
        return titleArray.length;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((LinearLayout)object);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        inflater = (LayoutInflater)context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.slide,container,false);
        //binding
        LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.linearLayout);
        ImageView imageView = (ImageView)view.findViewById(R.id.imgSlide);
        TextView textViewTitle = (TextView)view.findViewById(R.id.txtTitle);
        TextView textViewDesc = (TextView)view.findViewById(R.id.txtDescription);

        linearLayout.setBackgroundColor(backgroundColorArray[position]);
        imageView.setImageResource(imageArray[position]);
        textViewTitle.setText(titleArray[position]);
        textViewDesc.setText(descriptionArray[position]);
        //textToSpeech
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS){
                int language = textToSpeech.setLanguage(Locale.ENGLISH);
            }
        });
        textViewDesc.setOnClickListener(v -> {
            String text = textViewDesc.getText().toString();
            int speech = textToSpeech.speak(text,TextToSpeech.QUEUE_FLUSH,null);
        });
        container.addView(view);
        //intentToMain
        new Handler().postDelayed(() -> {
            if (position==titleArray.length-1){
                context.startActivity(new Intent(context,MainActivity.class));
                ((Activity)context).finish();
            }
        }, 7000);
        return view;
    }
}
