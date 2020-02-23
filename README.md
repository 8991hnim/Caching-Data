# Caching-Data
Giảm tải cho api sever bằng cách caching dữ liệu

## Mở đầu
Hướng dẫn các bạn cách cache dữ liệu ở local, giúp cho việc giảm số lượng request đến server và tăng hiệu năng của chương trình. Các bạn hoàn toàn không cần sử dụng cache mà có thể trực tiếp request tới server.

## Vai trò bộ nhớ Cache
Trước tiên, chúng ta cần hiểu vai trò của bộ nhớ cached (bộ nhớ đệm) Bộ nhớ cached rất hữu ích trong các tình huống sau:
- Giảm các kết nối api tới server: Bởi vì sử dụng cache chúng ta sẽ lưu trữ dữ liệu ở dưới local, nên ngay khi có thể chúng ta sẽ load từ local ra chứ k phải call api server. Điều này sẽ giúp giảm các kết nối và làm tăng performance của server. Tránh tình trạng server chết do request quá nhiều.
- Lấy dữ liệu rất nhanh: Vì lấy dữ liệu từ local nên thời gian lấy dữ liệu sẽ được nhanh hơn.

## Phân loại bộ nhớ cache
- Memory cache : Nó lưu trữ dữ liệu trong bộ nhớ của ứng dụng. Nếu ứng dụng bị kill, dữ liệu sẽ bị mất. Chỉ hữu ích trong cùng một phiên sử dụng ứng dụng. Memory cache là bộ đệm nhanh nhất để lấy dữ liệu vì nó được lưu trữ trong RAM.
- Disk Cache: Nó lưu dữ liệu vào ổ đĩa (như Sharepreference, database, file) . Nếu ứng dụng bị kill, dữ liệu được giữ lại. Hữu ích ngay cả sau khi ứng dụng khởi động lại. Chậm hơn memory cache, vì đây là thao tác I / O.

## Cách hoạt động của bộ nhớ cache
Lần đầu tiên, người dùng mở ứng dụng, sẽ không có dữ liệu. Nó sẽ lấy dữ liệu từ mạng và lưu nó vào disk cahe và trả lại dữ liệu.
Nếu người dùng kill app và khởi động lại, trong trường hợp này, tùy thuộc vào người phát triển, nó sẽ lấy dữ liệu từ trong disk cache hoặc api và trả về dữ liệu.

## Triển khai
### Single Source of Truth Principal
- Dữ liệu phải lấy từ 1 nguồn duy nhất.
- Chúng ta sẽ lấy dữ liệu từ rest api và local cache. Vậy là 2 nguồn dữ liệu. Trái ngược với nguyên lý.
-> Phải đảm bảo dữ liệu hiển thị lên màn hình toàn bộ là dữ liệu được load từ cache

### Retrofit Caching và lý do không sử dụng
- Sử dụng key/value pair, đường dẫn là key, raw data là value. -> Lưu trữ dữ liệu khổng lồ
- Không thể tùy chỉnh các tìm kiếm
- Không đảm bảo Single Source of Truth Principal

### NetworkBoundResource - 1 class rất mạnh mẽ được Google phát triển
Class này sẽ nằm ở Repository trong sơ đồ dưới đây
![alt text](https://developer.android.com/topic/libraries/architecture/images/final-architecture.png)

Trước tiên, chúng ta cần 1 class AppExecutors để dễ dàng triển khai code ở background hoặc main thread.
AppExecutor
```
public class AppExecutors {

    private static AppExecutors instance;

    public static AppExecutors getInstance() {
        if (instance == null) {
            instance = new AppExecutors();
        }
        return instance;
    }

    private final Executor mDiskIO = Executors.newSingleThreadExecutor();

    private final Executor mMainThreadExecutor = new MainThreadExecutor();

    public Executor diskIO() {
        return mDiskIO;
    }

    public Executor mainThread() {
        return mMainThreadExecutor;
    }

    private static class MainThreadExecutor implements Executor {

        private Handler mainThreadHandler = new Handler(Looper.myLooper());

        @Override
        public void execute(Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}

```

Các class sau giúp Retrofit trả về LiveData. Việc này sẽ giúp tối ưu hơn cho class chính của chúng ta là NetworkBoundResource. Hoặc có lựa chọn khác là impl RxJava và ReactiveStreams để retrofit trả về 1 Observable và từ đó chuyển sang LiveData (khá dài dòng)
ApiResponse.java - Class này sẽ giúp chúng ta rút ngắn được thao tác handle dữ liệu từ retrofit
```
public class ApiResponse<T> {

    public ApiResponse<T> create(Throwable error) {
        return new ApiErrorResponse<>(!error.getMessage().equals("") ? error.getMessage() : "Unknown error\nCheck network connection");
    }

    public ApiResponse<T> create(Response<T> response) {
        if (response.isSuccessful()) {
            T body = response.body();
            
            if (body == null || response.code() == 204) { // 204 is empty response code
                return new ApiEmptyResponse<>();
            } else {
                return new ApiSuccessResponse<>(body);
            }
        } else {
            String errorMsg = "";
            try {
                errorMsg = response.errorBody().string();
            } catch (IOException e) {
                errorMsg = response.message();
            }
            return new ApiErrorResponse<>(errorMsg);
        }
    }

    public class ApiSuccessResponse<T> extends ApiResponse<T> {
        private T body;

        ApiSuccessResponse(T body) {
            this.body = body;
        }

        public T getBody() {
            return body;
        }
    }

    public class ApiErrorResponse<T> extends ApiResponse<T> {
        private String errorMessage;


        ApiErrorResponse(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public class ApiEmptyResponse<T> extends ApiResponse<T> {

    }
}
```
LiveDataCallAdapter.java
```
public class LiveDataCallAdapter<R> implements CallAdapter<R, LiveData<ApiResponse<R>>> {

    private Type responseType;

    public LiveDataCallAdapter(Type responseType) {
        this.responseType = responseType;
    }

    @Override
    public Type responseType() {
        return responseType;
    }

    @Override
    public LiveData<ApiResponse<R>> adapt(final Call<R> call) {
        return new LiveData<ApiResponse<R>>() {
            @Override
            protected void onActive() {
                super.onActive();
                final ApiResponse apiResponse = new ApiResponse();
                call.enqueue(new Callback<R>() {
                    @Override
                    public void onResponse(Call<R> call, Response<R> response) {
                        postValue(apiResponse.create(response));
                    }

                    @Override
                    public void onFailure(Call<R> call, Throwable t) {
                        postValue(apiResponse.create(t));
                    }
                });
            }
        };
    }
}
```
LiveDataCallAdapterFactory.java
```
public class LiveDataCallAdapterFactory extends CallAdapter.Factory {

    /**
     * This method performs a number of checks and then returns the Response type for the Retrofit requests.
     * (@bodyType is the ResponseType)
     * <p>
     * CHECK #1) returnType returns LiveData
     * CHECK #2) Type LiveData<T> is of ApiResponse.class
     * CHECK #3) Make sure ApiResponse is parameterized. AKA: ApiResponse<T> exists.
     */

    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        //Check #1
        //Make sure the Call Adapter is returning a type of LiveData
        if (CallAdapter.Factory.getRawType(returnType) != LiveData.class) {
            return null;
        }

        //Check #2
        //Type that LiveData is wrapping
        Type observableType = CallAdapter.Factory.getParameterUpperBound(0, (ParameterizedType) returnType);

        //Check if it's of Type ApiResponse
        Type rawObservableType = CallAdapter.Factory.getRawType(observableType);
        if (rawObservableType != ApiResponse.class) {
            throw new IllegalArgumentException("Type must be a defined resource.");
        }

        //Check #3
        //Check if ApiResponse is parameterized. AKA: Does ApiResponse<T> exists? (Must wrap around T)
        if (!(observableType instanceof ParameterizedType)) {
            throw new IllegalArgumentException("resource must be parameterized.");
        }

        Type bodyType = CallAdapter.Factory.getParameterUpperBound(0, (ParameterizedType) observableType);

        return new LiveDataCallAdapter<Type>(bodyType);
    }
}
```
Resource.java - class giúp giữ data và trạng thái của nó
```
public class Resource<T> {

    @NonNull
    public final Status status;

    @Nullable
    public final T data;

    @Nullable
    public final String message;

    public Resource(@NonNull Status status, @Nullable T data, @Nullable String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> Resource<T> success(@NonNull T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    public static <T> Resource<T> error(@NonNull String msg, @Nullable T data) {
        return new Resource<>(Status.ERROR, data, msg);
    }

    public static <T> Resource<T> loading(@Nullable T data) {
        return new Resource<>(Status.LOADING, data, null);
    }

    public enum Status { SUCCESS, ERROR, LOADING}
}
```
Cuối cùng, class NetworkBoundResource - nơi xử lý tất cả việc liên quan caching data
NetworkBoundResource.java
```
public abstract class NetworkBoundResource<CacheObject, RequestObject> {

    private static final String TAG = "NetworkBoundResource";

    private AppExecutors appExecutors;
    private MediatorLiveData<Resource<CacheObject>> results = new MediatorLiveData<>();

    public NetworkBoundResource(AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
        init();
    }

    private void init() {
        //update LiveData for Loading status
        results.setValue((Resource<CacheObject>) Resource.loading(null));

        //observer LiveData source from local database
        final LiveData<CacheObject> dbSource = loadFromDb();

        results.addSource(dbSource, new Observer<CacheObject>() {
            @Override
            public void onChanged(@Nullable CacheObject cacheObject) {
                //triggered if there was some data in cache
                results.removeSource(dbSource);

                if (shouldFetch(cacheObject)) {
                    //get data from network
                    fetchFromNetwork(dbSource);
                } else {
                    results.addSource(dbSource, new Observer<CacheObject>() {
                        @Override
                        public void onChanged(@Nullable CacheObject cacheObject) {
                            setValue(Resource.success(cacheObject));
                        }
                    });
                }
            }
        });
    }

    /**
     * 1) observe local db
     * 2) if <condition/> query the network
     * 3) stop observing the local db
     * 4) insert new data into local db
     * 5) begin observing local db again to see the refreshed data from network
     * * @param dbSource
     */
    private void fetchFromNetwork(final LiveData<CacheObject> dbSource) {
        Log.d(TAG, "fetchFromNetwork: called");

        //update LiveData for loading status
        results.addSource(dbSource, new Observer<CacheObject>() {
            @Override
            public void onChanged(@Nullable CacheObject cacheObject) {
                setValue(Resource.loading(cacheObject));
            }
        });

        final LiveData<ApiResponse<RequestObject>> apiResponse = createCall();

        results.addSource(apiResponse, new Observer<ApiResponse<RequestObject>>() {
            @Override
            public void onChanged(@Nullable final ApiResponse<RequestObject> requestObjectApiResponse) {
                results.removeSource(dbSource);
                results.removeSource(apiResponse);

                /*
                    3 cases:
                        1) ApiSuccessResponse
                        2) ApiErrorResponse
                        3) ApiEmptyResponse
                 */

                if (requestObjectApiResponse instanceof ApiResponse.ApiSuccessResponse) {
                    Log.d(TAG, "onChanged: ApiSuccessResponse.");
                    appExecutors.diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            //save the response to the local db
                            saveCallResult((RequestObject) processResponse((ApiResponse.ApiSuccessResponse) requestObjectApiResponse));

                            appExecutors.mainThread().execute(new Runnable() {
                                @Override
                                public void run() {
                                    results.addSource(loadFromDb(), new Observer<CacheObject>() {
                                        @Override
                                        public void onChanged(@Nullable CacheObject cacheObject) {
                                            setValue(Resource.success(cacheObject));
                                        }
                                    });
                                }
                            });
                        }
                    });
                } else if (requestObjectApiResponse instanceof ApiResponse.ApiEmptyResponse) {
                    Log.d(TAG, "onChanged: ApiEmptyResponse");
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            results.addSource(loadFromDb(), new Observer<CacheObject>() {
                                @Override
                                public void onChanged(@Nullable CacheObject cacheObject) {
                                    setValue(Resource.success(cacheObject));
                                }
                            });
                        }
                    });
                } else if (requestObjectApiResponse instanceof ApiResponse.ApiErrorResponse) {
                    Log.d(TAG, "onChanged: ApiErrorResponse");
                    results.addSource(dbSource, new Observer<CacheObject>() {
                        @Override
                        public void onChanged(@Nullable CacheObject cacheObject) {
                            setValue(Resource.error(((ApiResponse.ApiErrorResponse) requestObjectApiResponse).getErrorMessage(), cacheObject));
                        }
                    });
                }
            }
        });
    }

    private CacheObject processResponse(ApiResponse.ApiSuccessResponse response) {
        return (CacheObject) response.getBody();
    }

    private void setValue(Resource<CacheObject> newValue) {
        if (results.getValue() != newValue) {
            results.setValue(newValue);
        }
    }

    // Called to save the result of the API response into the database.
    @WorkerThread
    protected abstract void saveCallResult(@NonNull RequestObject item);

    // Called with the data in the database to decide whether to fetch
    // potentially updated data from the network.
    @MainThread
    protected abstract boolean shouldFetch(@Nullable CacheObject data);

    // Called to get the cached data from the database.
    @NonNull
    @MainThread
    protected abstract LiveData<CacheObject> loadFromDb();

    // Called to create the API call.
    @NonNull
    @MainThread
    protected abstract LiveData<ApiResponse<RequestObject>> createCall();

    // Returns a LiveData object that represents the resource that's implemented
    // in the base class.
    public final LiveData<Resource<CacheObject>> getAsLiveData() {
        return results;
    }
    
}
```
Các hàm cần chú ý
- createCall(): nơi gọi đến api
vd: apiService.getFood("Chicken");
- loadFromDb(): lấy dữ liệu của database ở đây
vd: foodDao.search("Chicken");
- shouldFetch(): Có nên gọi api không, nếu return false sẽ chỉ lấy dữ liệu từ db
- saveCallResult(RequestObject item): Lưu dữ liệu nhận được từ api vào db
vd: foodDao.insert(...)

1 ví dụ thực tế khi sử dụng class NetworkBoundResource:
```
 public LiveData<Resource<List<Recipe>>> searchRecipeApi(final String query, final int pageNumber) {
        return new NetworkBoundResource<List<Recipe>, RecipeSearchResponse>(AppExecutors.getInstance()) {

            @Override
            protected void saveCallResult(@NonNull RecipeSearchResponse item) {
                recipeDao.insertRecipes(item.getRecipes().toArray(recipes)); 
            }

            @Override
            protected boolean shouldFetch(@Nullable List<Recipe> data) {
                return true;
            }

            @NonNull
            @Override
            protected LiveData<List<Recipe>> loadFromDb() {
                return recipeDao.searchRecipes(query, pageNumber);
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<RecipeSearchResponse>> createCall() {
                return ServiceGenerator.getRecipeApi()
                        .searchRecipe(Constants.API_KEY,
                                query,
                                String.valueOf(pageNumber));
            }
        }.getAsLiveData();
    }
```

### Hướng phát triển
- Cài đặt thời gian timeout request
- Xử lý nhiều case hơn (ví dụ chỉ cần gọi request nhưng k cần lưu kết quả vào db như login, register...)
- Hủy các request khi đang được thực thi (ví dụ request đang thực thi thì người dùng thoát app -> hủy request, tránh crash)



