/*
 *   Copyright 2019 Juanro49
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
 *   Basado en la clase original creada por LaQuay
 *
 */

package org.juanro.feedtv.Http;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Clase que representa y configura un cliente HTTP Volley
 */
public class VolleyController
{
    private static VolleyController instance;
    private RequestQueue requestQueue;
    private VolleyCore volley;

    private VolleyController(Context ctx)
	{
        volley = new VolleyCore(ctx);
        requestQueue = volley.getRequestQueue();
    }

	/**
	 * Obtiene la instancia de volley
	 *
	 * @param ctx
	 * @return
	 */
	public static VolleyController getInstance(Context ctx)
	{
        if (instance == null) {
			instance = new VolleyController(ctx);
        }
        return instance;
    }

	/**
	 * Método para añadir peticiones a la cola
	 *
	 * @param request
	 */
    public void addToQueue(Request request)
	{
        if (request != null)
        {
            request.setTag(this);

            if (requestQueue == null)
			{
				requestQueue = volley.getRequestQueue();
			}

            request.setRetryPolicy(new DefaultRetryPolicy(60000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            requestQueue.add(request);
        }
    }

	/**
	 * Eliminar elementos de la cola
	 */
	public void removeAllDataInQueue()
	{
        if (requestQueue != null)
        {
            requestQueue.cancelAll(this);
        }
    }

    private class VolleyCore
	{
        private RequestQueue mRequestQueue;

        private VolleyCore(Context context)
		{
            mRequestQueue = Volley.newRequestQueue(context);
        }

		/**
		 * Obtiene instancia de la cola de peticiones
		 *
		 * @return
		 */
		public RequestQueue getRequestQueue()
		{
            return mRequestQueue;
        }
    }
}