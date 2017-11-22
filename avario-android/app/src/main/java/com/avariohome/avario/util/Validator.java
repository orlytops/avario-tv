package com.avariohome.avario.util;


import android.widget.EditText;

public class Validator {

    /**
     * Validate string value if it is a valid url or ip address.
     * Display appropriate error message if there are any.
     * @param editText EditText to be validated.
     * @return boolean.
     */
    public static boolean isValidHost(EditText editText) {
        String urlRegex = "^(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?$";
        String ipRegex =
                "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        if (editText.getText().toString().isEmpty()) {
            editText.setError("Required");
            return false;
        } else if (editText.getText().toString().matches(urlRegex)) {
            return true;
        } else if (editText.getText().toString().matches(ipRegex)) {
            return true;
        } else {
            editText.setError("Invalid Host");
            return false;
        }
    }

    /**
     * Validate EditText to only contain number value.
     * Display appropriate error message if there are any.
     * @param editText EditText to be validated.
     * @return boolean.
     */
    public static boolean isValidPort(EditText editText){
        String numRegex = "[0-9]+";
        if (editText.getText().toString().isEmpty()){
            editText.setError("Required");
            return false;
        }
        if (!editText.getText().toString().matches(numRegex)){
            editText.setError("Number only");
            return false;
        }
        return true;
    }
}
