package m.tech.mvvmbygoogle.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import java.util.List;

import m.tech.mvvmbygoogle.model.Post;
import m.tech.mvvmbygoogle.repository.MainRepository;
import m.tech.mvvmbygoogle.util.Resource;

public class MainViewModel extends AndroidViewModel {

    MainRepository repository;

    MediatorLiveData<Resource<List<Post>>> posts = new MediatorLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = MainRepository.getInstance(application);
    }

    public void getPost() {
        final LiveData<Resource<List<Post>>> repoSource = repository.getPost();
        posts.addSource(repoSource, listResource -> {
            posts.setValue(listResource);
            if(listResource.status != Resource.Status.LOADING){
                posts.removeSource(repoSource);
            }
        });
    }

    public LiveData<Resource<List<Post>>> posts() {
        return posts;
    }
}
