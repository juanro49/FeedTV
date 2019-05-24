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

package org.juanro.feedtv.JSONParser;


import java.io.Serializable;
import java.util.ArrayList;

/**
 * Clase que representa un canal
 */
public class Canal implements Serializable
{
    private String nombre;
    private String web;
    private String logo;

    private ArrayList<Opciones> opciones;

    public Canal(String nombre, String web, String logo, ArrayList<Opciones> opciones)
    {
        this.nombre = nombre;
        this.web = web;
        this.logo = logo;
        this.opciones = opciones;
    }

    public String getNombre()
    {
        return nombre;
    }

    public void setNombre(String nombre)
    {
        this.nombre = nombre;
    }

    public String getWeb()
    {
        return web;
    }

    public void setWeb(String web)
    {
        this.web = web;
    }

    public String getLogo()
    {
        return logo;
    }

    public void setLogo(String logo)
    {
        this.logo = logo;
    }

    public ArrayList<Opciones> getOpciones()
    {
        return opciones;
    }

    public void setOpciones(ArrayList<Opciones> opciones)
    {
        this.opciones = opciones;
    }
}
