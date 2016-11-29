package ch.fhnw.ip5.emotionhunt.tasks;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.fhnw.ip5.emotionhunt.models.Experience;
import ch.fhnw.ip5.emotionhunt.models.ReceivedExperience;

/**
 * EmotionHunt ch.fhnw.ip5.emotionhunt.tasks
 *
 * @author Benjamin Bur
 */

public class RestExperienceListTask extends RestTask {

    boolean isPublic;

    public RestExperienceListTask(Context context, String url, List<NameValuePair> nameValuePairs, boolean isPublic)
    {
        super(context, url, nameValuePairs);
        this.isPublic = isPublic;
    }

    @Override
    protected Boolean doInBackground(String... urls) {
        try {
            Log.d(TAG, "Execute: " + mUrl);
            HttpPost httppost = new HttpPost(mUrl);
            httppost.setEntity(new UrlEncodedFormEntity(mNameValuePairs));
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(httppost);

            int status = response.getStatusLine().getStatusCode();

            Log.d(TAG, "Status: " + status);

            if (status == 200) {
                HttpEntity entity = response.getEntity();
                JSONObject jsonObject = new JSONObject(EntityUtils.toString(entity));
                JSONArray jData = jsonObject.getJSONArray("data");

                String data = jData.toString();

                //read json values from response
                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();
                List<ReceivedExperience> experiences = Arrays.asList(gson.fromJson(data, ReceivedExperience[].class));

                //save all received experiences into db
                for (Experience experience : experiences) {
                    experience.saveDb(mContext);
                }

                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}