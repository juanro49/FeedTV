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

package org.juanro.feedtv.TV;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Clase que representa un ámbito
 */
public class Ambito implements Serializable
{
    private String nombre;
    private ArrayList<Canal> canales;

    public Ambito(String nombre, ArrayList<Canal> canales)
    {
        this.nombre = nombre;
        this.canales = canales;
    }

    public String getNombre()
    {
        return nombre;
    }

    public void setNombre(String nombre)
    {
        this.nombre = nombre;
    }

    public ArrayList<Canal> getCanales()
    {
        return canales;
    }

    public void setCanales(ArrayList<Canal> canales)
    {
        this.canales = canales;
    }
}
