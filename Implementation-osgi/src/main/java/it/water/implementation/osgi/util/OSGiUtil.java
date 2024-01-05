/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.water.implementation.osgi.util;

import java.util.*;

public class OSGiUtil {

    private OSGiUtil() {
    }

    public static Dictionary<String, Object> toDictionary(Map<Object, Object> props) {
        Map<String, Object> map = new HashMap<>();
        Iterator<?> it = props.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            map.put(key, props.get(key));
        }
        return new Hashtable<>(map);
    }
}
