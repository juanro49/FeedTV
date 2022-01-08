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
 *
 */

package org.juanro.feedtv.TV;

import java.io.Serializable;

/**
 * Clase que representa las opciones de un canal
 */
public class Opciones implements Serializable
{
    private String url;
    private String formato;

    public Opciones(String formato, String url)
    {
        this.url = url;
    }

    public String getUrl()
	{
        return url;
    }

    public void setUrl(String url)
	{
        this.url = url;
    }

	public String getFormato()
	{
		return formato;
	}

	public void setFormato(String formato)
	{
		this.formato = formato;
	}
}
