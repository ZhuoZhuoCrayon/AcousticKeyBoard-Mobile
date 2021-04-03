package tech.oom.idealrecorderdemo.dto;

import java.util.HashMap;

public class AkResBase {
    public boolean result;
    public String code;
    public HashMap<String, Object> data;
    public String message;

    public AkResBase(boolean result, String code, HashMap<String, Object> data, String message) {
        this.result = result;
        this.code = code;
        this.data = data;
        this.message = message;
    }
}
