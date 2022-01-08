/*
 *   Copyright 2021 Juanro49
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */

package org.juanro.feedtv.Http;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.util.HashMap;
import java.util.Map;

public class InputStreamVolleyRequest extends Request<byte[]>
{
    private final Response.Listener<byte[]> mListener;
    private Map<String, String> mParams;

    //create a static map for directly accessing headers
    public Map<String, String> responseHeaders;

    public InputStreamVolleyRequest(int method, String mUrl ,Response.Listener<byte[]> listener,
                                    Response.ErrorListener errorListener, HashMap<String, String> params)
    {
        // TODO Auto-generated constructor stub
        super(method, mUrl, errorListener);
        // this request would never use cache.
        setShouldCache(false);
        mListener = listener;
        mParams=params;
    }

    @Override
    protected Map<String, String> getParams()
            throws com.android.volley.AuthFailureError
    {
        return mParams;
    };


    @Override
    protected void deliverResponse(byte[] response)
    {
        mListener.onResponse(response);
    }

    @Override
    protected Response<byte[]> parseNetworkResponse(NetworkResponse response)
    {
        //Initialise local responseHeaders map with response headers received
        responseHeaders = response.headers;

        //Pass the response data here
        return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
    }
}
