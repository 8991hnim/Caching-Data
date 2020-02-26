package m.tech.mvvmbygoogle.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "post")
public class Post {
    @SerializedName("id")
    @ColumnInfo
    @PrimaryKey
    private int id;

    @SerializedName("title")
    @ColumnInfo
    private String title;


    @ColumnInfo
    @SerializedName("body")
    private String body;

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
