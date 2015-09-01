package com.promact.gbox;

/**
 * Created by Shreyash on 31-08-2015.
 */
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.StringUtils;
import com.google.api.services.gmail.model.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * An asynchronous task that handles the Gmail API call.
 * Placing the API calls in their own task ensures the UI stays responsive.
 */
public class ApiAsyncTask extends AsyncTask<Void, Void, List<String>> {
    private MainActivity mActivity;
    private ProgressDialog progresDialog;

    /**
     * Constructor.
     * @param activity MainActivity that spawned this task.
     */
    ApiAsyncTask(MainActivity activity) {
        this.mActivity = activity;
        progresDialog = new ProgressDialog(activity);
        progresDialog.setCancelable(false);
        progresDialog.setMessage(activity.getString(R.string.fetching_subscribers));
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progresDialog.show();
    }

    /**
     * Background task to call Gmail API.
     * @param params no parameters needed for this task.
     */
    @Override
    protected List<String> doInBackground(Void... params) {

        List<String> subscriberResult = null;
        try {

            subscriberResult = getDataFromApi();

        } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
            mActivity.showGooglePlayServicesAvailabilityErrorDialog(
                    availabilityException.getConnectionStatusCode());

        } catch (UserRecoverableAuthIOException userRecoverableException) {
            mActivity.startActivityForResult(
                    userRecoverableException.getIntent(),
                    MainActivity.REQUEST_AUTHORIZATION);

        } catch (Exception e) {}
        return subscriberResult;
    }

    @Override
    protected void onPostExecute(List<String> subscriberList) {
        super.onPostExecute(subscriberList);
        progresDialog.dismiss();

        if (subscriberList == null) {
            Toast.makeText(mActivity,mActivity.getString(R.string.null_response),Toast.LENGTH_SHORT).show();
        } else if (subscriberList.size() == 0) {
            Toast.makeText(mActivity,mActivity.getString(R.string.no_subscriber_found),Toast.LENGTH_SHORT).show();
        } else {
            mActivity.updateResultsList(subscriberList);
        }

    }

    /**
     * Fetch a list of Gmail labels attached to the specified account.
     * @return List of Strings labels.
     * @throws IOException
     */
    private List<String> getDataFromApi() throws IOException {
        // Get the labels in the user's account.
        String user = "me";
        HashSet hasSet = new HashSet();
        List<String> labels = new ArrayList<String>();
        //"from:someuser@example.com rfc822msgid: is:unread".
        ListMessagesResponse listResponse = mActivity.mService.users().messages().list(user).setQ("in:inbox Verification OR verification OR confirmation OR Confirmation OR Confirm OR Verify OR Activation OR Active").execute();
        for (Message message : listResponse.getMessages()) {
           // labels.add(StringUtils.newStringUtf8(Base64.decodeBase64(message.getPayload().getBody().getData())));
            Message msg = mActivity.mService.users().messages().get(user, message.getId()).setFormat("metadata").execute();
            JSONObject payload = new JSONObject(msg.getPayload());

            try {
                JSONArray header   = payload.getJSONArray("headers");

                for (int i=0 ; i<header.length() ; ++i)
                {
                    JSONObject from = header.getJSONObject(i);

                    if (from.getString("name").equals("From"))
                    {
                        labels.add(from.getString("value"));
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        hasSet.addAll(labels);
        labels.clear();
        labels.addAll(hasSet);

        return labels;
    }

}