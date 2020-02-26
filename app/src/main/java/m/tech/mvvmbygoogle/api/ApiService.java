package m.tech.mvvmbygoogle.api;

import androidx.lifecycle.LiveData;

import java.util.List;

import m.tech.mvvmbygoogle.model.Post;
import retrofit2.http.GET;

public interface ApiService {

    @GET("posts")
    LiveData<ApiResponse<List<Post>>> getPost();
}
