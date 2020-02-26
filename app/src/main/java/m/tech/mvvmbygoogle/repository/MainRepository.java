package m.tech.mvvmbygoogle.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import java.util.List;

import m.tech.mvvmbygoogle.AppExecutors;
import m.tech.mvvmbygoogle.api.ApiResponse;
import m.tech.mvvmbygoogle.api.ServiceGenerator;
import m.tech.mvvmbygoogle.model.Post;
import m.tech.mvvmbygoogle.persistence.AppDatabase;
import m.tech.mvvmbygoogle.persistence.PostDao;
import m.tech.mvvmbygoogle.util.Resource;

public class MainRepository {

    private static MainRepository instance = null;
    Context context;
    PostDao postDao;

    public static MainRepository getInstance(Context context){
        if(instance == null){
            instance = new MainRepository(context);
        }
        return instance;
    }


    private MainRepository(Context context) {
        this.context = context;
        postDao = AppDatabase.getInstance(context).getPostDao();
    }

    public LiveData<Resource<List<Post>>> getPost(){
        return new NetworkBoundResource<List<Post>, List<Post>>(AppExecutors.getInstance()){

            @Override
            protected void saveCallResult(@NonNull List<Post> item) {
                postDao.insertPost(item);
            }

            @Override
            protected boolean shouldFetch(@Nullable List<Post> data) {
                return true;
            }

            @NonNull
            @Override
            protected LiveData<List<Post>> loadFromDb() {
                return postDao.getPost();
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<Post>>> createCall() {
                return ServiceGenerator.getApiService().getPost();
            }
        }.getAsLiveData();
    }

}
