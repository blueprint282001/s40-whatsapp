package com.whatsapp.s40.util;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Lightweight JSON parser designed specifically for J2ME CLDC 1.1 / MIDP 2.0.
 * Zero external dependencies, minimal memory usage.
 */
public class MiniJson {

    private String src;
    private int idx;
    private int len;

    public MiniJson(String json) {
        this.src = json != null ? json : "";
        this.idx = 0;
        this.len = this.src.length();
    }

    public static Object parse(String json) {
        if (json == null || json.trim().length() == 0) return null;
        MiniJson parser = new MiniJson(json.trim());
        return parser.parseValue();
    }

    private void skipWhitespace() {
        while (idx < len) {
            char c = src.charAt(idx);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                idx++;
            } else {
                break;
            }
        }
    }

    private Object parseValue() {
        skipWhitespace();
        if (idx >= len) return null;

        char c = src.charAt(idx);
        if (c == '{') {
            return parseObject();
        } else if (c == '[') {
            return parseArray();
        } else if (c == '"' || c == '\'') {
            return parseString();
        } else if (c == 't' || c == 'T' || c == 'f' || c == 'F') {
            return parseBoolean();
        } else if (c == 'n' || c == 'N') {
            return parseNull();
        } else if ((c >= '0' && c <= '9') || c == '-') {
            return parseNumber();
        }
        return null;
    }

    private Hashtable parseObject() {
        Hashtable map = new Hashtable();
        idx++; // skip '{'

        while (idx < len) {
            skipWhitespace();
            if (idx >= len) break;
            if (src.charAt(idx) == '}') {
                idx++;
                return map;
            }

            // Key
            String key = "";
            char c = src.charAt(idx);
            if (c == '"' || c == '\'') {
                key = (String) parseString();
            } else {
                StringBuffer sb = new StringBuffer();
                while (idx < len && src.charAt(idx) != ':' && src.charAt(idx) != ' ') {
                    sb.append(src.charAt(idx++));
                }
                key = sb.toString();
            }

            skipWhitespace();
            if (idx < len && src.charAt(idx) == ':') {
                idx++; // skip ':'
            }

            Object val = parseValue();
            if (key != null && val != null) {
                map.put(key, val);
            }

            skipWhitespace();
            if (idx < len && src.charAt(idx) == ',') {
                idx++;
            } else if (idx < len && src.charAt(idx) == '}') {
                idx++;
                return map;
            }
        }
        return map;
    }

    private Vector parseArray() {
        Vector list = new Vector();
        idx++; // skip '['

        while (idx < len) {
            skipWhitespace();
            if (idx >= len) break;
            if (src.charAt(idx) == ']') {
                idx++;
                return list;
            }

            Object val = parseValue();
            if (val != null) {
                list.addElement(val);
            }

            skipWhitespace();
            if (idx < len && src.charAt(idx) == ',') {
                idx++;
            } else if (idx < len && src.charAt(idx) == ']') {
                idx++;
                return list;
            }
        }
        return list;
    }

    private String parseString() {
        char quote = src.charAt(idx);
        idx++; // skip opening quote
        StringBuffer sb = new StringBuffer();

        while (idx < len) {
            char c = src.charAt(idx++);
            if (c == quote) {
                return sb.toString();
            } else if (c == '\\' && idx < len) {
                char next = src.charAt(idx++);
                if (next == 'n') sb.append('\n');
                else if (next == 'r') sb.append('\r');
                else if (next == 't') sb.append('\t');
                else if (next == '"') sb.append('"');
                else if (next == '\'') sb.append('\'');
                else if (next == '\\') sb.append('\\');
                else sb.append(next);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Boolean parseBoolean() {
        StringBuffer sb = new StringBuffer();
        while (idx < len) {
            char c = src.charAt(idx);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                sb.append(c);
                idx++;
            } else {
                break;
            }
        }
        String s = sb.toString().toLowerCase();
        if ("true".equals(s)) return new Boolean(true);
        return new Boolean(false);
    }

    private Object parseNull() {
        idx += 4; // skip 'null'
        return null;
    }

    private String parseNumber() {
        StringBuffer sb = new StringBuffer();
        while (idx < len) {
            char c = src.charAt(idx);
            if ((c >= '0' && c <= '9') || c == '-' || c == '.' || c == 'e' || c == 'E') {
                sb.append(c);
                idx++;
            } else {
                break;
            }
        }
        return sb.toString();
    }

    public static String getString(Hashtable obj, String key, String defaultVal) {
        if (obj == null || key == null) return defaultVal;
        Object v = obj.get(key);
        if (v != null) return v.toString();
        return defaultVal;
    }

    public static int getInt(Hashtable obj, String key, int defaultVal) {
        if (obj == null || key == null) return defaultVal;
        Object v = obj.get(key);
        if (v != null) {
            try {
                return Integer.parseInt(v.toString());
            } catch (Exception e) {}
        }
        return defaultVal;
    }

    public static boolean getBoolean(Hashtable obj, String key, boolean defaultVal) {
        if (obj == null || key == null) return defaultVal;
        Object v = obj.get(key);
        if (v instanceof Boolean) return ((Boolean) v).booleanValue();
        if (v != null) {
            return "true".equalsIgnoreCase(v.toString());
        }
        return defaultVal;
    }
}
