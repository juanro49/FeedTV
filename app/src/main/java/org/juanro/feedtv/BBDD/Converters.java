/*
 *   Copyright 2026 Juanro49
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
 */

package org.juanro.feedtv.BBDD;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Converters {
    @TypeConverter
    public static List<String> fromString(String value) {
        if (value == null || value.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(value.split(",")));
    }

    @TypeConverter
    public static String fromList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s).append(",");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}
