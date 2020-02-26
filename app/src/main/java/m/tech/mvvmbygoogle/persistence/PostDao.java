package m.tech.mvvmbygoogle.persistence;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import m.tech.mvvmbygoogle.model.Post;

import static androidx.room.OnConflictStrategy.REPLACE;

@Dao
public interface PostDao {
    @Insert(onConflict = REPLACE)
    void insertPost(List<Post> posts);

    @Query("SELECT * FROM post")
    LiveData<List<Post>> getPost();
}
