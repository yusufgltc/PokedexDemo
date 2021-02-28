package com.example.pokedexdemo;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.pokedexdemo.databinding.ActivityIntroBinding;


public class IntroActivity extends AppCompatActivity {
    private ActivityIntroBinding binding;
    SliderAdapter sliderAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       //binding
        binding = ActivityIntroBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        //adapter
        sliderAdapter = new SliderAdapter(this);
        binding.viewPager.setAdapter(sliderAdapter);
    }
}