package com.avariohome.avario.util;


import com.avariohome.avario.Constants;
import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by aeroheart-c6 on 23/01/2017.
 */
public class RefStringUtil {
    public static final String TAG = "Avario/RefStringUtil";

    private static final Pattern refPattern = Pattern.compile("\\{\\{(.+?)\\}\\}");
    private static final Pattern valPattern = Pattern.compile("^((#!)? *)(.+)");
    private static Scriptable jsScope;

    public static void initJSInterpreter() {
        Context context = RefStringUtil.getRhinoContext();

        RefStringUtil.jsScope = context.initStandardObjects();

        Context.exit();
    }

    private static Context getRhinoContext() {
        Context context;

        context = Context.enter();
        context.setOptimizationLevel(-1);

        return context;
    }

    /**
     * Processes the {{ip1}}/path/to/api/call url strings
     *
     * @param url
     * @return a map containing the "url" and the "confId" keys containing the appropriate data.
     */
    public static  Map<String, String> processUrl(String url) throws AvarioException {
        List<String> keys = new ArrayList<>();
        Map<String, String> mapping = new HashMap<>(),
                            result = new HashMap<>();

        Matcher matcher = RefStringUtil.extractMarkers(url, keys);
        String confId = keys.isEmpty() ? "ip1" : keys.get(0);

        mapping.put(confId, StateArray.getInstance().getHTTPHost(confId));

        result.put("confId", confId);
        result.put("url", RefStringUtil.replaceMarkers(matcher, mapping));

        return result;
    }

    public static String processRefs(String refstring) throws AvarioException {
        return RefStringUtil.processRefs(
            StateArray.getInstance().getEntities(),
            refstring
        );
    }

    public static String processRefs(JSONObject root, String refstring) {
        List<String> keys = new ArrayList<>();
        Map<String, String> mapping = new HashMap<>();

        Matcher matcher = RefStringUtil.extractMarkers(refstring, keys);

        for (String key : keys)
            try {
                mapping.put(key, RefStringUtil.resolveValue(root, key));
            }
            catch (AvarioException exception) {
                mapping.put(key, "0");
            }

        return RefStringUtil.replaceMarkers(matcher, mapping);
    }

    public static String processCode(String codestring) throws AvarioException {
        return RefStringUtil.processCode(
            StateArray.getInstance().getEntities(),
            codestring
        );
    }

    public static String processCode(JSONObject rootJSON, String codestring) throws AvarioException {
        Matcher matcher = RefStringUtil.valPattern.matcher(codestring);
        Object output;

        if (!matcher.matches())
            return codestring;

        codestring = RefStringUtil.processRefs(rootJSON, matcher.group(3));

        if (matcher.group(2) == null)
            return codestring;

        try {
            output = RefStringUtil
                .getRhinoContext()
                .evaluateString(RefStringUtil.jsScope, codestring, "<cmd>", 1, null);
        }
        catch (RhinoException exception) {
            output = null;
        }
        finally {
            Context.exit();
        }

        return output.toString();
    }

    public static boolean processLogic(String logicString) throws AvarioException {
        boolean result;

        logicString = RefStringUtil.processRefs(logicString);

        try {
            Object output;

            output = RefStringUtil
                .getRhinoContext()
                .evaluateString(RefStringUtil.jsScope, logicString, "<cmd>", 1, null);

            result = output instanceof Boolean
                   ? (Boolean)output
                   : Boolean.valueOf(output.toString());

            Log.i(TAG, String.format("%s resolved: %s", logicString, String.valueOf(result)));
        }
        catch (RhinoException exception) {
            Log.i(TAG, String.format("%s threw an error", logicString));

            throw new AvarioException(
                Constants.ERROR_STATE_API_CONDITION,
                exception,
                new Object[2]
            );
        }
        finally {
            Context.exit();
        }

        return result;
    }

    /**
     * Extracts {{}} markers in the string. If the `replacements` argument is not null, will iterate
     * through the matches and add the string inside of the {{}} as keys to the list.
     *
     * @param refstring
     * @param keys
     * @return
     */
    public static Matcher extractMarkers(String refstring, List<String> keys) {
        Matcher matcher = RefStringUtil.refPattern.matcher(refstring);

        if (keys != null) {
            while (matcher.find())
                keys.add(matcher.group(1));

            matcher.reset();
        }

        return matcher;
    }

    public static String replaceMarkers(Matcher matcher, Map<String, String> replacements) {
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String replacement;

            replacement = replacements.get(matcher.group(1));
            replacement = replacement == null
                        ? matcher.group()
                        : replacement;

            matcher.appendReplacement(buffer, replacement);
        }

        matcher.appendTail(buffer);

        return buffer.toString();
    }

    public static String resolveValue(JSONObject rootJSON, String path) throws AvarioException {
        String[] parts = path.split("/");
        Object value;

        if (parts.length <= 1 || rootJSON == null)
            return null; // there is no value to resolve

        try {
            JSONObject objectJSON = rootJSON;

            for (int index = 0, limit = parts.length - 1; index < limit; index++)
                objectJSON = objectJSON.getJSONObject(parts[index]);

            value = objectJSON.get(parts[parts.length - 1]);
        }
        catch (JSONException exception) {
            throw new AvarioException(
                Constants.ERROR_STATE_MISSINGKEY,
                exception,
                new Object[] { path.replaceAll("/", ".") }
            );
        }

        return value == null ? null : value.toString();
    }
}
