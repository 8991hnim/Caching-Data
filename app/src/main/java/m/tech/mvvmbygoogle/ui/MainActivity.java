package m.tech.mvvmbygoogle.ui;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import java.util.List;

import m.tech.mvvmbygoogle.R;
import m.tech.mvvmbygoogle.model.Post;
import m.tech.mvvmbygoogle.util.Resource;
import m.tech.mvvmbygoogle.viewmodel.MainViewModel;

public class MainActivity extends AppCompatActivity {
    MainViewModel viewModel;

    String TAG = "AppDebug";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel =  ViewModelProviders.of(this).get(MainViewModel.class);
        subscribeObserver();
        viewModel.getPost();
    }

    private void subscribeObserver() {
        viewModel.posts().observe(this, resource -> {
            switch (resource.status) {
                case LOADING:
                    Log.d(TAG, "Loading...");
                    break;
                case SUCCESS:
                    Log.d(TAG, "Success..." + resource.data.size());
                    break;
                case ERROR:
                    Log.d(TAG, "Error" + resource.message + " size " + resource.data.size());
                    break;
            }
        });
    }
}
