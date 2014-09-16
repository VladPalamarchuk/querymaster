package parser;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeoutException;

import palamarchuk.smartlife.app.FragmentHolderActivity;


public class QueryMaster extends Thread {

    /**
     * Ошибка обработки запроса, возможны проблемы с интернетом
     */
    public static final String ERROR_MESSAGE = "Ошибка обработки запроса, возможны проблемы с интернетом";

    /**
     * Ошибка обработки запроса на сервере
     */
    public static final String SERVER_RETURN_INVALID_DATA = "Ошибка обработки запроса на сервере";

    public static final int QUERY_MASTER_COMPLETE = 1;
    public static final int QUERY_MASTER_ERROR = 2;
    public static final int QUERY_MASTER_NETWORK_ERROR = 3;

    public static final int QUERY_GET = 23;
    public static final int QUERY_POST = 24;
    public static final String UNABLE_PACK_QUERY_DATA = "Не удалось сформировать запрос, возможно введены некорректные данные";

    public void setProgressDialog() {
        this.progressDialog = ProgressDialog.show(context, null, "Загрузка...");
    }

    private ProgressDialog progressDialog;

    private OnCompleteListener onCompleteListener;
    private Handler handler;

    private MultipartEntity entity;
    private Context context;

    private String serverResponse;
    private String url;

    private int queryType;

    public static int timeoutConnection = 10000;


    /**
     * @param context
     * @param url
     * @param queryType QueryMaster.QUERY_GET or QueryMaster.QUERY_POST
     * @param entity
     */
    public QueryMaster(Context context, String url, int queryType, MultipartEntity entity) {
        this(context, url, queryType);
        this.entity = entity;
    }

    public QueryMaster(Context context, String url, int queryType) {
        this.context = context;
        this.url = url;
        this.queryType = queryType;
        initHandler();
    }

    @Override
    public void run() {
        super.run();

        if (!isNetworkConnected()) {
            handler.sendEmptyMessage(QUERY_MASTER_NETWORK_ERROR);
            return;
        }
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
                timeoutConnection);


        DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
//        httpclient.setParams(httpParams);

        HttpPost httpPost;
        HttpGet httpGet;

        HttpResponse response = null;

        try {

            if (queryType == QUERY_GET) {
                httpGet = new HttpGet(url);

                response = httpclient.execute(httpGet);

            } else if (queryType == QUERY_POST) {

                httpPost = new HttpPost(url);
                if (entity != null) {
                    httpPost.setEntity(entity);
                }
                response = httpclient.execute(httpPost);
            }

            serverResponse = EntityUtils.toString(response.getEntity());

            handler.sendEmptyMessage(QUERY_MASTER_COMPLETE);

        } catch (ClientProtocolException e) {
            e.printStackTrace();
            handler.sendEmptyMessage(QUERY_MASTER_ERROR);
        } catch (IOException e) {
            e.printStackTrace();
            handler.sendEmptyMessage(QUERY_MASTER_ERROR);
        } catch (NullPointerException e) {
            e.printStackTrace();
            handler.sendEmptyMessage(QUERY_MASTER_ERROR);
        }
    }

    public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    public interface OnCompleteListener {
        public void complete(String serverResponse);

        public void error(int errorCode);
    }

    private void initHandler() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (onCompleteListener != null) {
                    if (msg.what == QUERY_MASTER_COMPLETE) {
                        onCompleteListener.complete(serverResponse);
                    }
                    if (msg.what == QUERY_MASTER_ERROR) {
                        onCompleteListener.error(QUERY_MASTER_ERROR);
                    }
                    if (msg.what == QUERY_MASTER_NETWORK_ERROR) {
                        onCompleteListener.error(QUERY_MASTER_NETWORK_ERROR);
                    }
                }
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }

            }
        };
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public Context getContext() {
        return context;
    }

    public static AlertDialog alert(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setTitle("Ошибка");

        builder.setPositiveButton("Ok", null);
        return builder.show();
    }

    public static AlertDialog.Builder alertBuilder(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setTitle("Ошибка");

        return builder;
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void toast(Context context, int resource) {
        Toast.makeText(context, context.getString(resource), Toast.LENGTH_SHORT).show();
    }

    public static boolean isSuccess(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("status").equalsIgnoreCase("success");
    }
}
